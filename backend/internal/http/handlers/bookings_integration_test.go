package handlers_test

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"sync"
	"testing"
	"time"

	httpapi "summer-school-2026/backend/internal/http"
	"summer-school-2026/backend/internal/http/handlers"
	"summer-school-2026/backend/internal/service/auth"
	"summer-school-2026/backend/internal/service/booking"
	"summer-school-2026/backend/internal/storage/postgres"
	"summer-school-2026/backend/internal/storage/postgres/testutil"

	"github.com/jackc/pgx/v5/pgxpool"
)

func TestCreateBookingFlowAndIdempotency(t *testing.T) {
	databaseURL := testutil.PrepareDatabase(t)

	ctx := context.Background()
	db, err := postgres.Connect(ctx, databaseURL)
	if err != nil {
		t.Fatalf("connect postgres: %v", err)
	}
	t.Cleanup(db.Close)

	token := "booking-token"
	clientID := "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
	insertClientSession(t, ctx, db, clientID, "+79990001001", token)

	router := bookingRouter(db)
	body := `{"slot_id":"55555555-5555-5555-5555-555555555555","seats_count":2,"rental_count":1}`
	idempotencyKey := "77777777-7777-7777-7777-777777777777"

	first := performCreateBooking(router, token, idempotencyKey, body)
	if first.Code != http.StatusCreated {
		t.Fatalf("first status = %d, body = %s", first.Code, first.Body.String())
	}
	var firstResponse struct {
		ID         string `json:"id"`
		PriceTotal int    `json:"price_total"`
		Slot       struct {
			FreeSeats        int `json:"free_seats"`
			FreeRentalBoards int `json:"free_rental_boards"`
		} `json:"slot"`
	}
	if err := json.Unmarshal(first.Body.Bytes(), &firstResponse); err != nil {
		t.Fatalf("decode first response: %v", err)
	}
	if firstResponse.PriceTotal != 5800 || firstResponse.Slot.FreeSeats != 6 || firstResponse.Slot.FreeRentalBoards != 11 {
		t.Fatalf("unexpected first response: %+v", firstResponse)
	}

	retry := performCreateBooking(router, token, idempotencyKey, body)
	if retry.Code != http.StatusCreated {
		t.Fatalf("retry status = %d, body = %s", retry.Code, retry.Body.String())
	}
	var retryResponse struct {
		ID string `json:"id"`
	}
	if err := json.Unmarshal(retry.Body.Bytes(), &retryResponse); err != nil {
		t.Fatalf("decode retry response: %v", err)
	}
	if retryResponse.ID != firstResponse.ID {
		t.Fatalf("retry booking id = %q, want %q", retryResponse.ID, firstResponse.ID)
	}

	var bookingCount int
	if err := db.QueryRow(ctx, `SELECT count(*) FROM bookings WHERE client_id = $1`, clientID).Scan(&bookingCount); err != nil {
		t.Fatalf("count bookings: %v", err)
	}
	if bookingCount != 1 {
		t.Fatalf("booking count = %d, want 1", bookingCount)
	}

	doubleBooking := performCreateBooking(router, token, "88888888-8888-8888-8888-888888888888", body)
	if doubleBooking.Code != http.StatusConflict {
		t.Fatalf("double booking status = %d, want %d", doubleBooking.Code, http.StatusConflict)
	}
}

func TestCreateBookingConcurrencyDoesNotOverbook(t *testing.T) {
	databaseURL := testutil.PrepareDatabase(t)

	ctx := context.Background()
	db, err := postgres.Connect(ctx, databaseURL)
	if err != nil {
		t.Fatalf("connect postgres: %v", err)
	}
	t.Cleanup(db.Close)

	router := bookingRouter(db)
	body := `{"slot_id":"55555555-5555-5555-5555-555555555555","seats_count":1,"rental_count":1}`

	const requests = 12
	var wg sync.WaitGroup
	statuses := make(chan int, requests)
	for i := 0; i < requests; i++ {
		i := i
		clientID := fmt.Sprintf("aaaaaaaa-aaaa-aaaa-aaaa-%012d", i+1)
		token := fmt.Sprintf("booking-token-%d", i+1)
		insertClientSession(t, ctx, db, clientID, fmt.Sprintf("+79990002%03d", i+1), token)
		wg.Add(1)
		go func() {
			defer wg.Done()
			idempotencyKey := fmt.Sprintf("99999999-9999-9999-9999-%012d", i+1)
			statuses <- performCreateBooking(router, token, idempotencyKey, body).Code
		}()
	}
	wg.Wait()
	close(statuses)

	successes := 0
	conflicts := 0
	for status := range statuses {
		switch status {
		case http.StatusCreated:
			successes++
		case http.StatusConflict:
			conflicts++
		default:
			t.Fatalf("unexpected status = %d", status)
		}
	}
	if successes != 8 || conflicts != 4 {
		t.Fatalf("successes=%d conflicts=%d, want 8/4", successes, conflicts)
	}

	var freeSeats, freeBoards int
	if err := db.QueryRow(ctx, `SELECT free_seats, free_rental_boards FROM slots WHERE id = '55555555-5555-5555-5555-555555555555'`).Scan(&freeSeats, &freeBoards); err != nil {
		t.Fatalf("read availability: %v", err)
	}
	if freeSeats != 0 || freeBoards != 4 {
		t.Fatalf("availability seats=%d boards=%d, want 0/4", freeSeats, freeBoards)
	}
}

func bookingRouter(db *pgxpool.Pool) http.Handler {
	service := booking.NewService(postgres.NewBookingRepository(db))
	return httpapi.NewRouter(slog.Default(), httpapi.RouterOptions{Bookings: handlers.NewBookingHandler(service)})
}

func insertClientSession(t *testing.T, ctx context.Context, db *pgxpool.Pool, clientID, phone, token string) {
	t.Helper()
	if _, err := db.Exec(ctx, `INSERT INTO clients (id, phone) VALUES ($1, $2)`, clientID, phone); err != nil {
		t.Fatalf("insert client: %v", err)
	}
	if _, err := db.Exec(ctx, `INSERT INTO auth_sessions (client_id, token_hash, expires_at) VALUES ($1, $2, $3)`, clientID, auth.HashToken(token), time.Now().Add(time.Hour)); err != nil {
		t.Fatalf("insert session: %v", err)
	}
}

func performCreateBooking(router http.Handler, token, idempotencyKey, body string) *httptest.ResponseRecorder {
	recorder := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/bookings", bytes.NewBufferString(body))
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("Idempotency-Key", idempotencyKey)
	router.ServeHTTP(recorder, req)
	return recorder
}
