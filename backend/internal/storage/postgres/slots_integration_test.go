package postgres_test

import (
	"context"
	"testing"

	"summer-school-2026/backend/internal/storage/postgres"
	"summer-school-2026/backend/internal/storage/postgres/testutil"
)

func TestSlotRepositoryListReadsSeedSlots(t *testing.T) {
	databaseURL := testutil.PrepareDatabase(t)

	ctx := context.Background()
	db, err := postgres.Connect(ctx, databaseURL)
	if err != nil {
		t.Fatalf("connect postgres: %v", err)
	}
	t.Cleanup(db.Close)

	repo := postgres.NewSlotRepository(db)
	slots, err := repo.List(ctx, 20, 0)
	if err != nil {
		t.Fatalf("List() error = %v", err)
	}

	if len(slots) != 2 {
		t.Fatalf("len(slots) = %d, want %d", len(slots), 2)
	}
	if slots[0].ID != "55555555-5555-5555-5555-555555555555" {
		t.Fatalf("first slot id = %q", slots[0].ID)
	}
	if slots[0].FreeSeats != 8 || slots[0].FreeRentalBoards != 12 {
		t.Fatalf("unexpected availability: seats=%d boards=%d", slots[0].FreeSeats, slots[0].FreeRentalBoards)
	}
	if slots[0].RouteName == "" || slots[0].InstructorName == "" {
		t.Fatal("slot must include route and instructor data")
	}
}
