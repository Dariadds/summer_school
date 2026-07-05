# Code Review Report: автотесты и инфраструктура автоматизации

## 1. Объект ревью

Ревью проведено по локальным артефактам репозитория [02-development](02-development), а также по фактически существующим тестам и сценариям в [backend](backend) и [client](client).

### Источники правды

- [02-development/BE_IMPLEMENTATION_PLAN.md](02-development/BE_IMPLEMENTATION_PLAN.md)
- [02-development/CMP_CLIENT_IMPLEMENTATION_PLAN.md](02-development/CMP_CLIENT_IMPLEMENTATION_PLAN.md)
- [backend/Makefile](backend/Makefile)
- [backend/README.md](backend/README.md)
- [backend/k6](backend/k6)
- [backend/internal/http](backend/internal/http)
- [backend/internal/service](backend/internal/service)
- [backend/internal/storage/postgres](backend/internal/storage/postgres)
- [client/shared/src/commonTest](client/shared/src/commonTest)

> В папке [02-development](02-development) отсутствуют отдельные каталоги тестов, Page Object, UI-автотесты, конфиги Playwright/Cypress/Appium/Selenium и CI workflow. Поэтому анализ проведён по реально присутствующим тестам и по планам реализации, а не по выдуманным файлам.

---

## 2. Краткий вывод

На текущий момент автоматизация в репозитории находится на уровне backend unit/integration тестов и нагрузочных сценариев k6. Это хороший базовый слой для критических серверных сценариев, но он не покрывает полноценную UI-автоматизацию для iOS/Android/Web и не обеспечивает CI/CD-пайплайн для автономных end-to-end тестов.

### Итоговая оценка

- Сильная сторона: серверные сценарии и бизнес-логика покрыты достаточно глубоко.
- Ключевой пробел: отсутствуют настоящие e2e/GUI автотесты под iOS + Android + Web.
- Риск: текущая автоматизация может не обнаружить regressions на уровне UI, навигации, локаторов, платформенных различий и пользовательского сценария.

---

## 3. Что реально присутствует в репозитории

### 3.1 Backend тесты

В backend уже есть набор Go тестов, включающий:

- unit tests для auth: [backend/internal/service/auth/service_test.go](backend/internal/service/auth/service_test.go)
- unit tests для booking service: [backend/internal/service/booking/service_test.go](backend/internal/service/booking/service_test.go)
- boundary tests для правила отмены: [backend/internal/service/booking/cancel_test.go](backend/internal/service/booking/cancel_test.go)
- integration tests для HTTP handlers: [backend/internal/http/handlers/auth_integration_test.go](backend/internal/http/handlers/auth_integration_test.go), [backend/internal/http/handlers/catalog_integration_test.go](backend/internal/http/handlers/catalog_integration_test.go), [backend/internal/http/handlers/bookings_integration_test.go](backend/internal/http/handlers/bookings_integration_test.go), [backend/internal/http/handlers/profile_integration_test.go](backend/internal/http/handlers/profile_integration_test.go)
- contract/error tests: [backend/internal/http/router_contract_test.go](backend/internal/http/router_contract_test.go), [backend/internal/http/infrastructure_test.go](backend/internal/http/infrastructure_test.go)
- repository/database tests: [backend/internal/storage/postgres/slots_integration_test.go](backend/internal/storage/postgres/slots_integration_test.go), [backend/internal/storage/postgres/constraints_integration_test.go](backend/internal/storage/postgres/constraints_integration_test.go)

### 3.2 Performance/load tests

В репозитории есть k6 сценарии:

- smoke: [backend/k6/smoke.js](backend/k6/smoke.js)
- booking load: [backend/k6/booking_300_vu.js](backend/k6/booking_300_vu.js)
- cancel load: [backend/k6/cancel_300_vu.js](backend/k6/cancel_300_vu.js)

### 3.3 Client-side tests

Для клиента есть common tests, но они не являются e2e и относятся к domain/presentation logic:

- [client/shared/src/commonTest/kotlin/com/volna/app/domain/policy/DomainPolicyTest.kt](client/shared/src/commonTest/kotlin/com/volna/app/domain/policy/DomainPolicyTest.kt)
- [client/shared/src/commonTest/kotlin/com/volna/app/booking/presentation/BookingListStoreTest.kt](client/shared/src/commonTest/kotlin/com/volna/app/booking/presentation/BookingListStoreTest.kt)
- [client/shared/src/commonTest/kotlin/com/volna/app/booking/presentation/BookingDetailsStoreTest.kt](client/shared/src/commonTest/kotlin/com/volna/app/booking/presentation/BookingDetailsStoreTest.kt)
- [client/shared/src/commonTest/kotlin/com/volna/app/core/phone/PhoneInputCoreTest.kt](client/shared/src/commonTest/kotlin/com/volna/app/core/phone/PhoneInputCoreTest.kt)

### 3.4 CI / automation pipeline

Ни одного workflow-файла в [02-development](02-development) и в корне репозитория не обнаружено. Также отсутствуют конфиги для Playwright/Cypress/Appium/Selenium/Detox.

---

## 4. Что покрыто тестами

### 4.1 Полностью или почти полностью покрытые сценарии

1. Auth flow
   - запрос кода OTP, verify, logout: [backend/internal/http/handlers/auth_integration_test.go](backend/internal/http/handlers/auth_integration_test.go)
   - rate limit и валидация OTP: [backend/internal/service/auth/service_test.go](backend/internal/service/auth/service_test.go)

2. Booking flow
   - создание брони, идемпотентность, конфликт при повторе: [backend/internal/http/handlers/bookings_integration_test.go](backend/internal/http/handlers/bookings_integration_test.go)
   - конкурентное создание и защита от overbooking: [backend/internal/http/handlers/bookings_integration_test.go](backend/internal/http/handlers/bookings_integration_test.go)

3. Cancellation flow
   - ранняя/поздняя/после старта отмена: [backend/internal/http/handlers/bookings_integration_test.go](backend/internal/http/handlers/bookings_integration_test.go)
   - границы правила 2 часов: [backend/internal/service/booking/cancel_test.go](backend/internal/service/booking/cancel_test.go)

4. Slots/catalog and profile
   - фильтры, details, pagination: [backend/internal/http/handlers/catalog_integration_test.go](backend/internal/http/handlers/catalog_integration_test.go)
   - profile flow and phone conflict: [backend/internal/http/handlers/profile_integration_test.go](backend/internal/http/handlers/profile_integration_test.go)

5. Contract and error handling
   - validation errors, JSON content type, missing auth, unknown path: [backend/internal/http/infrastructure_test.go](backend/internal/http/infrastructure_test.go), [backend/internal/http/router_contract_test.go](backend/internal/http/router_contract_test.go)

6. Client domain rules
   - availability logic, pricing, cancellation policy, phone normalization: [client/shared/src/commonTest/kotlin/com/volna/app/domain/policy/DomainPolicyTest.kt](client/shared/src/commonTest/kotlin/com/volna/app/domain/policy/DomainPolicyTest.kt), [client/shared/src/commonTest/kotlin/com/volna/app/core/phone/PhoneInputCoreTest.kt](client/shared/src/commonTest/kotlin/com/volna/app/core/phone/PhoneInputCoreTest.kt)

### 4.2 Что осталось без автоматизации

1. UI/e2e для iOS/Android/Web
   - нет специфичных тестов для экранов [01analysis/5-mobile-app-spec](01analysis/5-mobile-app-spec)
   - нет Page Object / screen objects / locators
   - нет UI-assertions на реальные пользовательские сценарии

2. Cross-platform regressions
   - нет автоматизации для Android/iOS/Web в одном наборе тестов
   - нет проверки навигации, bottom sheets, permissions, push flow, карты и ошибок на уровне UI

3. CI/CD
   - нет workflow для запуска unit/integration/e2e тестов автоматически на PR/merge

4. Manual test cases traceability
   - ручные тест-кейсы в репозитории не обнаружены; оценка полноты покрытия относительно требований ограничена существующими тестами и аналитикой, а не отдельным TC-репозиторием

---

## 5. Сильные стороны текущей автоматизации

### 5.1 Хорошая база для backend

- Интеграционные тесты реально проверяют HTTP слой и реальные сценарии, а не только mocks.
- Есть тесты на идемпотентность, concurrency и conflict handling, что особенно важно для booking/cancel.
- Есть k6 нагрузочные сценарии, ориентированные на 300 VUs и конкурентные операции.
- Тесты используют отдельную подготовку БД через [backend/internal/storage/postgres/testutil](backend/internal/storage/postgres/testutil), что снижает риск загрязнения данных.

### 5.2 Бизнес-правила покрыты на уровне domain logic

- Доступность и цены проверяются через [client/shared/src/commonTest/kotlin/com/volna/app/domain/policy/DomainPolicyTest.kt](client/shared/src/commonTest/kotlin/com/volna/app/domain/policy/DomainPolicyTest.kt).
- Логика отмены и границы времени проверяются явно.

---

## 6. Недостатки и риски

### 6.1 Отсутствует инфраструктура UI-автотестов

Это главный пробел. В репозитории нет ни одного файла, который можно было бы назвать полноценным e2e framework для мобильных/web приложений. Следовательно, критические сценарии пользователя не проверяются на уровне UI.

### 6.2 Жесткая зависимость от фиксированных данных и seed

Тесты часто используют жестко заданные UUID, телефоны, значения слотов и ожидаемые числа. Примеры:

- [backend/internal/http/handlers/bookings_integration_test.go](backend/internal/http/handlers/bookings_integration_test.go)
- [backend/internal/storage/postgres/slots_integration_test.go](backend/internal/storage/postgres/slots_integration_test.go)

Это создаёт риск, что при изменении seed или структуры данных тесты начнут падать даже при корректной реализации.

### 6.3 Риск flakiness на concurrency и integration слоях

- Конкурентные тесты на booking/cancel завязаны на порядок выполнения множества параллельных запросов.
- Там, где используется реальная БД и real timing, возможны intermittent failures при медленном CI или высокой нагрузке.
- В [backend/k6/smoke.js](backend/k6/smoke.js) есть искусственная задержка `sleep(1)` — это не критичный антипаттерн, но снижает скорость и делает сценарий менее чувствительным к реальному пользовательскому поведению.

### 6.4 Нет CI/CD-пайплайна для автоматизации

Отсутствие workflow-файлов означает, что локальные тесты не защищают main branch автоматически. Это особенно критично для:

- PR проверок;
- regressions после изменений в booking/cancel;
- e2e UI regression.

### 6.5 Нет отдельного teardown / fixture management для роста набора тестов

Хотя часть тестов создаёт fresh DB через testutil, по мере роста тестовой матрицы потребуется отдельная стратегия для fixtures, seed, cleanup и isolation. Сейчас это не выражено как явная архитектура test infrastructure.

---

## 7. Противоречия и тонкости

### 7.1 Реализация тестов не соответствует уровню продукта

Серверные тесты сильные, но они не покрывают UI/UX сценарии из [01analysis/5-mobile-app-spec](01analysis/5-mobile-app-spec). В частности, отсутствуют проверки для:

- экранов списка/деталей слота;
- оформления брони;
- подтверждения/успеха/отмены;
- bottom sheets и навигации;
- push/permissions flow.

### 7.2 Нет явного тестового слоя для платформенных различий

Для KMP-клиента и платформенных таргетов ожидается отдельная проверка Android/iOS/Web, но в репозитории нет такой инфраструктуры. Это может скрыть платформенные баги.

### 7.3 Нет связи между ручными тест-кейсами и автоматизацией

В репозитории отсутствуют отдельные manual test cases / QA matrix / TC tracking files, поэтому нельзя уверенно оценить полноту покрытия относительно требований.

---

## 8. Рекомендации по улучшению

### Приоритет P0

1. Добавить полноценную e2e инфраструктуру для iOS/Android/Web.
   - Playwright для Web
   - Appium/Detox для мобильных платформ, если это поддерживается архитектурой проекта

2. Создать Page Object / Screen Object layer.
   - для экранов auth, slots, booking, bookings, profile

3. Настроить CI workflow.
   - запуск unit/integration tests на PR
   - запуск e2e на main/staging
   - отчёты и artifacts для падений

### Приоритет P1

4. Вынести тестовые данные в fixtures / factory layer.
   - убрать хардкод UUID / phones / slot IDs из основных тестов
   - добавить seed generation per test case

5. Добавить тесты на реальные пользовательские сценарии.
   - auth -> slot list -> slot details -> booking -> booking success -> my bookings
   - auth -> booking -> cancel -> verify state change

6. Добавить smoke/regression suite для клиентских экранов.

### Приоритет P2

7. Добавить visual regression / accessibility checks.
8. Ввести test tagging (smoke / regression / performance) и parallelization strategy.

---

## 9. Вердикт

Текущая автоматизация уже имеет зрелую backend-базу и полезные contract/performance тесты, но пока недостаточна как полноценная QA automation suite для продукта «Апекс». Главная проблема — отсутствие UI/e2e-автоматизации и CI-пайплайна. Без этого часть пользовательских сценариев остаётся незащищённой, а регрессии на уровне экранов и платформ могут уходить в production.

### Рекомендованное решение

Считать текущую базу «готовой для backend-уровня» и «недостаточной для полного QA automation» по продукту в целом.
