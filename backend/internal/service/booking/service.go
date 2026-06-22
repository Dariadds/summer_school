package booking

import (
	"context"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"
	"time"

	"summer-school-2026/backend/internal/service/auth"
)

var (
	ErrUnauthorized        = errors.New("unauthorized")
	ErrInvalidRequest      = errors.New("invalid booking request")
	ErrSlotFull            = errors.New("slot full")
	ErrDoubleBooking       = errors.New("double booking")
	ErrSlotCancelled       = errors.New("slot cancelled")
	ErrSlotStarted         = errors.New("slot started")
	ErrIdempotencyConflict = errors.New("idempotency conflict")
)

type Client struct {
	ID string
}

type Booking struct {
	ID          string
	SlotID      string
	ClientID    string
	SeatsCount  int
	RentalCount int
	Status      string
	PriceTotal  int
	CreatedAt   time.Time
	Slot        Slot
}

type Slot struct {
	ID               string
	RouteID          string
	RouteName        string
	RouteType        string
	RouteCapacityCap int
	RouteDurationMin int
	InstructorID     string
	InstructorName   string
	StartAt          time.Time
	TotalSeats       int
	FreeSeats        int
	FreeRentalBoards int
	Price            int
	RentalPrice      int
	MeetingPoint     string
	MeetingPointLat  float64
	MeetingPointLng  float64
	Status           string
}

type CreateCommand struct {
	Token          string
	IdempotencyKey string
	SlotID         string
	SeatsCount     int
	RentalCount    int
}

type Availability struct {
	AvailableSeats        int
	AvailableRentalBoards int
}

type AvailabilityError struct {
	Err          error
	Availability Availability
}

func (e AvailabilityError) Error() string { return e.Err.Error() }

func (e AvailabilityError) Unwrap() error { return e.Err }

type Repository interface {
	ClientBySessionTokenHash(ctx context.Context, tokenHash string) (Client, bool, error)
	Create(ctx context.Context, clientID string, command CreateCommand, requestHash string, now time.Time) (Booking, error)
}

type Service struct {
	repo Repository
	now  func() time.Time
}

func NewService(repo Repository) *Service {
	return &Service{repo: repo, now: time.Now}
}

func (s *Service) Create(ctx context.Context, command CreateCommand) (Booking, error) {
	if command.Token == "" {
		return Booking{}, ErrUnauthorized
	}
	if command.SeatsCount < 1 || command.SeatsCount > 3 || command.RentalCount < 0 || command.RentalCount > command.SeatsCount || command.SlotID == "" {
		return Booking{}, ErrInvalidRequest
	}

	client, ok, err := s.repo.ClientBySessionTokenHash(ctx, auth.HashToken(command.Token))
	if err != nil {
		return Booking{}, err
	}
	if !ok {
		return Booking{}, ErrUnauthorized
	}

	return s.repo.Create(ctx, client.ID, command, requestHash(command), s.now().UTC())
}

func requestHash(command CreateCommand) string {
	sum := sha256.Sum256([]byte(fmt.Sprintf("%s|%d|%d", command.SlotID, command.SeatsCount, command.RentalCount)))
	return base64.RawStdEncoding.EncodeToString(sum[:])
}
