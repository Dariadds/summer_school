package postgres

import (
	"context"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Slot struct {
	ID               string
	RouteID          string
	RouteName        string
	RouteType        string
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

type SlotRepository struct {
	db *pgxpool.Pool
}

func NewSlotRepository(db *pgxpool.Pool) *SlotRepository {
	return &SlotRepository{db: db}
}

func (r *SlotRepository) List(ctx context.Context, limit, offset int) ([]Slot, error) {
	rows, err := r.db.Query(ctx, `
SELECT
    s.id::text,
    r.id::text,
    r.name,
    r.type,
    i.id::text,
    i.name,
    s.start_at,
    s.total_seats,
    s.free_seats,
    s.free_rental_boards,
    s.price,
    s.rental_price,
    s.meeting_point,
    s.meeting_point_lat,
    s.meeting_point_lng,
    s.status
FROM slots s
JOIN routes r ON r.id = s.route_id
JOIN instructors i ON i.id = s.instructor_id
ORDER BY s.start_at ASC
LIMIT $1 OFFSET $2`, limit, offset)
	if err != nil {
		return nil, fmt.Errorf("query slots: %w", err)
	}
	defer rows.Close()

	slots := make([]Slot, 0)
	for rows.Next() {
		var slot Slot
		if err := rows.Scan(
			&slot.ID,
			&slot.RouteID,
			&slot.RouteName,
			&slot.RouteType,
			&slot.InstructorID,
			&slot.InstructorName,
			&slot.StartAt,
			&slot.TotalSeats,
			&slot.FreeSeats,
			&slot.FreeRentalBoards,
			&slot.Price,
			&slot.RentalPrice,
			&slot.MeetingPoint,
			&slot.MeetingPointLat,
			&slot.MeetingPointLng,
			&slot.Status,
		); err != nil {
			return nil, fmt.Errorf("scan slot: %w", err)
		}
		slots = append(slots, slot)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate slots: %w", err)
	}

	return slots, nil
}
