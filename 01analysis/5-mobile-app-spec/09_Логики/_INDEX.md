# 09. Логики — индекс

> Переиспользуемая бизнес- и UI-логика мобильного приложения картинг-центра. Логики выносятся в отдельные документы и подключаются к экранам через раздел «Применяемые логики».

**Статус:** Черновик · **Дата:** 2026-07-03

---

## Реестр логик

| ID | Логика | Приоритет | Назначение | Применяется на |
|----|--------|-----------|------------|----------------|
| **LOGIC-001** | [OTP-авторизация и сессия](LOGIC-001_OTP-авторизация.md) | Critical | Вход по номеру телефона и SMS, сохранение сессии, выход | [SCR-001-auth.md](../SCR-001-auth.md), [SCR-007-profile.md](../SCR-007-profile.md) |
| **LOGIC-002** | [Расчёт доступности слота](LOGIC-002_Расчёт-доступности.md) | Critical | Ограничение числа мест и доступности выбора по данным слота | [SCR-003-ride-details.md](../SCR-003-ride-details.md), [SCR-004-booking.md](../SCR-004-booking.md) |
| **LOGIC-003** | [Расчёт цены брони](LOGIC-003_Расчёт-цены-брони.md) | High | Превью стоимости и итог по уже созданной броне | [SCR-004-booking.md](../SCR-004-booking.md), [BS-002-booking-confirm.md](../BS-002-booking-confirm.md), [SCR-005-my-bookings.md](../SCR-005-my-bookings.md), [SCR-006-booking-details.md](../SCR-006-booking-details.md) |
| **LOGIC-004** | [Отмена: правило 2 часов](LOGIC-004_Отмена-правило-2-часов.md) | Critical | Ранняя и поздняя отмена, сохранение/освобождение мест | [SCR-006-booking-details.md](../SCR-006-booking-details.md), [BS-004-cancel-confirm.md](../BS-004-cancel-confirm.md) |
| **LOGIC-005** | [Фильтрация расписания](LOGIC-005_Фильтрация-слотов.md) | High | Фильтр по дате и видимости свободных слотов | [SCR-002-schedule.md](../SCR-002-schedule.md), [BS-001-date-filter.md](../BS-001-date-filter.md) |
| **LOGIC-006** | [Карта маршрута и точки встречи](LOGIC-006_Карта-маршрута.md) | Medium | Превью карты, текстовый фолбэк и переход к навигации | [SCR-003-ride-details.md](../SCR-003-ride-details.md), [SCR-006-booking-details.md](../SCR-006-booking-details.md) |
| **LOGIC-007** | [Запрос push-разрешения](LOGIC-007_Запрос-push-разрешения.md) | Medium | Запрос разрешения на напоминания после первой брони | [BS-003-booking-success.md](../BS-003-booking-success.md) |
| **LOGIC-008** | [Паттерн состояний экрана](LOGIC-008_Паттерн-состояний-экрана.md) | High | Loading → Content → Empty → Error и состояния действий | Все экраны с запросами |

---

## Карта «экран → логики»

| Экран | Логики |
|-------|--------|
| [SCR-001-auth.md](../SCR-001-auth.md) | LOGIC-001 |
| [SCR-002-schedule.md](../SCR-002-schedule.md) | LOGIC-005, LOGIC-008 |
| [BS-001-date-filter.md](../BS-001-date-filter.md) | LOGIC-005 |
| [SCR-003-ride-details.md](../SCR-003-ride-details.md) | LOGIC-002, LOGIC-003, LOGIC-006, LOGIC-008 |
| [SCR-004-booking.md](../SCR-004-booking.md) | LOGIC-002, LOGIC-003 |
| [BS-002-booking-confirm.md](../BS-002-booking-confirm.md) | LOGIC-003, LOGIC-007 |
| [BS-003-booking-success.md](../BS-003-booking-success.md) | LOGIC-007 |
| [SCR-005-my-bookings.md](../SCR-005-my-bookings.md) | LOGIC-003, LOGIC-008 |
| [SCR-006-booking-details.md](../SCR-006-booking-details.md) | LOGIC-003, LOGIC-004, LOGIC-006, LOGIC-008 |
| [BS-004-cancel-confirm.md](../BS-004-cancel-confirm.md) | LOGIC-004 |
| [SCR-007-profile.md](../SCR-007-profile.md) | LOGIC-001, LOGIC-008 |
