package com.volna.app.catalog.data

import com.volna.app.catalog.Page
import com.volna.app.domain.model.GeoPoint
import com.volna.app.domain.model.Instructor
import com.volna.app.domain.model.InstructorId
import com.volna.app.domain.model.MeetingPoint
import com.volna.app.domain.model.MoneyRub
import com.volna.app.domain.model.Route
import com.volna.app.domain.model.RouteId
import com.volna.app.domain.model.RouteType
import com.volna.app.domain.model.Slot
import com.volna.app.domain.model.SlotId
import com.volna.app.domain.model.SlotStatus

fun SlotListResponseDto.toDomain(): Page<Slot> = Page(
    items = items.map { it.toDomain() },
    limit = meta.limit,
    offset = meta.offset,
    total = meta.total,
)

fun InstructorListResponseDto.toDomain(): Page<Instructor> = Page(
    items = items.map { it.toDomain() },
    limit = meta.limit,
    offset = meta.offset,
    total = meta.total,
)

fun SlotDto.toDomain(): Slot = Slot(
    id = SlotId(id),
    startAt = startAt,
    route = route.toDomain(),
    instructor = instructor.toDomain(),
    totalSeats = totalSeats,
    freeSeats = freeSeats,
    freeRentalBoards = freeRentalBoards,
    price = MoneyRub(price),
    rentalPrice = MoneyRub(rentalPrice),
    meetingPoint = MeetingPoint(
        title = meetingPoint.orEmpty(),
        coordinates = GeoPoint(
            lat = meetingPointLat ?: 0.0,
            lng = meetingPointLng ?: 0.0,
        ),
    ),
    status = when (status) {
        "cancelled" -> SlotStatus.Cancelled
        else -> SlotStatus.Scheduled
    },
)

fun RouteDto.toDomain(): Route = Route(
    id = RouteId(id),
    name = name,
    type = when (type) {
        "experienced" -> RouteType.Experienced
        else -> RouteType.Novice
    },
    capacityCap = capacityCap,
    durationMin = durationMin,
)

fun InstructorDto.toDomain(): Instructor = Instructor(
    id = InstructorId(id),
    name = name,
)
