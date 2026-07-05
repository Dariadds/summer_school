# Отчёт полного ревью ТЗ и аналитических артефактов «Апекс»

## 1. Объект ревью и источник правды

Ревью проведено по локальному репозиторию [01analysis](01analysis) как по основному источнику правды. Сверка выполнена по следующим артефактам:

- [01analysis/0-customer-brief/customer-brief.md](01analysis/0-customer-brief/customer-brief.md)
- [01analysis/1-elicitation](01analysis/1-elicitation)
- [01analysis/2-requirements/functional-requirements.md](01analysis/2-requirements/functional-requirements.md)
- [01analysis/2-requirements/non-functional-requirements.md](01analysis/2-requirements/non-functional-requirements.md)
- [01analysis/3-design-brief/design-brief.md](01analysis/3-design-brief/design-brief.md)
- [01analysis/4-design/data-model.md](01analysis/4-design/data-model.md)
- [01analysis/4-design/api-sequence.md](01analysis/4-design/api-sequence.md)
- [01analysis/5-mobile-app-spec](01analysis/5-mobile-app-spec)
- [01analysis/api](01analysis/api)

Отдельно был проведён код-уровневый импакт-анализ по локальному клону приложения в [client](client). Для этого использовались shared-модули KMP, в которых уже реализованы основные потоки бронирования, деталей записи, авторизации и профиля.



## 2. Краткий вывод

ТЗ в целом достаточно зрелое для перехода к проектированию и реализации MVP, но оно ещё не готово к прямой передаче в разработку без доработки ряда критических пунктов. Главные риски лежат не только в самом функционале, но и в том, что часть требований уже реализована как UX/контрактная логика в аналитике, а часть ещё не приведена к единой формулировке.

Статус ревью: требует доработки перед разработкой.

## 3. Что именно влияет на внедрение

### 3.1 Поток записи на слот

Наиболее широкий импакт имеет сценарий записи, потому что он пересекает несколько экранов, логик, API и состояний:

- [01analysis/3-design-brief/SCR-004-booking.md](01analysis/3-design-brief/SCR-004-booking.md)
- [01analysis/5-mobile-app-spec/SCR-004-booking.md](01analysis/5-mobile-app-spec/SCR-004-booking.md)
- [01analysis/5-mobile-app-spec/09_Логики/LOGIC-002_Расчёт-доступности.md](01analysis/5-mobile-app-spec/09_Логики/LOGIC-002_Расчёт-доступности.md)
- [01analysis/5-mobile-app-spec/09_Логики/LOGIC-003_Расчёт-цены-брони.md](01analysis/5-mobile-app-spec/09_Логики/LOGIC-003_Расчёт-цены-брони.md)
- [01analysis/api/bookings/api.yaml](01analysis/api/bookings/api.yaml)
- [01analysis/api/bookings/models.yaml](01analysis/api/bookings/models.yaml)
- [01analysis/api/slots/models.yaml](01analysis/api/slots/models.yaml)

Это влияет на:
- экран списка слотов и карточки слота;
- экран оформления записи;
- bottom sheet подтверждения и успеха записи;
- поведение ошибки при конфликте/гонке бронирований;
- навигацию обратно к списку после недоступности слота;
- состояние формы, лимиты и локальный idempotency flow.

Код-уровневая связка уже присутствует в [client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingFormStore.kt](client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingFormStore.kt), где реализованы:
- валидация доступности;
- расчёт лимитов по местам и прокату;
- генерация idempotency key;
- обработка конфликтов при недоступности мест/проката;
- обновление состояния слота после ошибок.

### 3.2 Поток просмотра и отмены броней

Сценарий моих броней и деталей также имеет широкий импакт:

- [01analysis/3-design-brief/SCR-005-my-bookings.md](01analysis/3-design-brief/SCR-005-my-bookings.md)
- [01analysis/3-design-brief/SCR-006-booking-details.md](01analysis/3-design-brief/SCR-006-booking-details.md)
- [01analysis/5-mobile-app-spec/SCR-005-my-bookings.md](01analysis/5-mobile-app-spec/SCR-005-my-bookings.md)
- [01analysis/5-mobile-app-spec/09_Логики/LOGIC-004_Отмена-правило-2-часов.md](01analysis/5-mobile-app-spec/09_Логики/LOGIC-004_Отмена-правило-2-часов.md)
- [01analysis/api/bookings/api.yaml](01analysis/api/bookings/api.yaml)

Это влияет на:
- разделение предстоящих/прошедших записей;
- отображение статусов `active`, `cancelled`, `late_cancel`, `club_cancelled`;
- поведение при отмене менее/более чем за 2 часа;
- текстовые сценарии и причины отмены;
- навигацию из списка в детали и обратно.

Код-уровневая реализация этого находится в [client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingDetailsStore.kt](client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingDetailsStore.kt) и [client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingListStore.kt](client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingListStore.kt).

### 3.3 Авторизация, профиль и push

Потоки входа/регистрации, профиля и push-уведомлений затрагивают не только экранный слой, но и хранение сессии и устройство:

- [01analysis/3-design-brief/SCR-001-auth.md](01analysis/3-design-brief/SCR-001-auth.md)
- [01analysis/5-mobile-app-spec/09_Логики/LOGIC-001_OTP-авторизация.md](01analysis/5-mobile-app-spec/09_Логики/LOGIC-001_OTP-авторизация.md)
- [01analysis/5-mobile-app-spec/09_Логики/LOGIC-007_Запрос-push-разрешения.md](01analysis/5-mobile-app-spec/09_Логики/LOGIC-007_Запрос-push-разрешения.md)
- [01analysis/api/auth/api.yaml](01analysis/api/auth/api.yaml)
- [01analysis/api/auth/models.yaml](01analysis/api/auth/models.yaml)
- [01analysis/api/profile/api.yaml](01analysis/api/profile/api.yaml)

Импакт распространяется на:
- OTP flow и повторную отправку кода;
- хранение access/refresh токенов;
- регистрацию push-токена устройства;
- сценарии первого входа и выхода;
- редактирование профиля и удаление аккаунта.

В коде это отражено в [client/shared/src/commonMain/kotlin/com/volna/app/auth/presentation/AuthStore.kt](client/shared/src/commonMain/kotlin/com/volna/app/auth/presentation/AuthStore.kt), [client/shared/src/commonMain/kotlin/com/volna/app/profile/presentation/ProfileStore.kt](client/shared/src/commonMain/kotlin/com/volna/app/profile/presentation/ProfileStore.kt) и связанных data-репозиториях.

## 4. Найденные противоречия и тонкости

### 4.1 Критично: несогласованная платформа MVP

В [01analysis/2-requirements/non-functional-requirements.md](01analysis/2-requirements/non-functional-requirements.md) заявлено про мобильный браузер без установки, а в [01analysis/3-design-brief/design-brief.md](01analysis/3-design-brief/design-brief.md) и в мобильной спецификации уже закладывается поведение нативного приложения: push-разрешения, защищённое хранилище, системная навигация и сценарии запуска.

Почему это важно:
- влияет на архитектуру, push, хранение токенов и приемку;
- меняет набор платформенных ограничений и тестовой матрицы.

### 4.2 Критично: формула доступности мест не приведена к единому правилу

В [01analysis/2-requirements/functional-requirements.md](01analysis/2-requirements/functional-requirements.md) сказано, что запись не расходует прокатный фонд, но в мобильной спецификации и API отражена более сложная логика: свободные места, потолок маршрута.

Это критично, потому что напрямую влияет на:
- экран карточки слота;
- экран оформления записи;
- состояние кнопки записи и лимиты выбора;
- ошибки 409 при нехватке мест.
### 4.3 Критично: лимит мест противоречит между требованиями и дизайном

Функциональные требования допускают запись от 1 до 4 мест, тогда как экранная спецификация и дизайн-бриф уже фиксируют лимит 3 мест: «себя + до 2 гостей».

Риск:
- UI и API могут быть реализованы под разные правила;
- тестовые сценарии и приемка будут разными.

### 4.4 Критично: статус отмены центра не унифицирован

В [01analysis/4-design/data-model.md](01analysis/4-design/data-model.md) используется `center_cancelled`, а в [01analysis/api/bookings/models.yaml](01analysis/api/bookings/models.yaml) — `club_cancelled`.

Это влияет на:
- отображение статуса в списке/деталях;
- перевод текстов и микрокопии;
- логику доступа к повторной записи;
- обработку ошибок и обновления после отмены слота.

### 4.5 Высокий риск: цена брони описана разными способами

В требованиях цена фигурирует как общая «стоимость заезда», в модели данных и API — как производная итоговая сумма, а в логике расчёта — как сочетание тарифа за место и тарифа за прокат.

Проблема в том, что UI и API могут по-разному трактовать:
- где хранится итоговая сумма;
- где брать цену для предпросмотра;
- где показывать цену после создания брони.

### 4.6 Высокий риск: сценарий push не закреплён как единый контракт

OpenAPI уже содержит endpoints для регистрации push-токена, но требования и экранные документы не задают единый продуктовый контракт: когда показывать разрешение, когда отправлять напоминания, что делать при отказе, как обрабатывать повторный вход и восстановление токена.

### 4.7 Средний риск: профиль и удаление аккаунта присутствуют в API и дизайне, но не полностью синхронизированы с требованиями

В [01analysis/2-requirements/functional-requirements.md](01analysis/2-requirements/functional-requirements.md) профиль описан минимально, в то время как дизайн и OpenAPI уже содержат сценарии редактирования имени/телефона, выхода и удаления аккаунта.

## 5. Код-уровневые зоны импакта в локальном клиенте

Локальный клиент уже содержит отдельные части, которые напрямую завязаны на ТЗ. Если менять требования, нужно учитывать следующие точки:

- [client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingFormStore.kt](client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingFormStore.kt)
  - правила доступности, лимиты и выбор досок;
  - расчёт цены и состояние валидации;
  - idempotency flow.

- [client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingDetailsStore.kt](client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingDetailsStore.kt)
  - логика отмены и статусов;
  - обработка поздней отмены и невозможности отмены после старта.

- [client/shared/src/commonMain/kotlin/com/volna/app/domain/policy/BookingPriceCalculator.kt](client/shared/src/commonMain/kotlin/com/volna/app/domain/policy/BookingPriceCalculator.kt)
  - формула цены для предпросмотра и расчёта итоговой суммы.

- [client/shared/src/commonMain/kotlin/com/volna/app/auth/presentation/AuthStore.kt](client/shared/src/commonMain/kotlin/com/volna/app/auth/presentation/AuthStore.kt) и [client/shared/src/commonMain/kotlin/com/volna/app/profile/presentation/ProfileStore.kt](client/shared/src/commonMain/kotlin/com/volna/app/profile/presentation/ProfileStore.kt)
  - поток входа, сессии и профиля.

- [client/shared/src/commonMain/kotlin/com/volna/app/booking/data/KtorBookingRepository.kt](client/shared/src/commonMain/kotlin/com/volna/app/booking/data/KtorBookingRepository.kt) и [client/shared/src/commonMain/kotlin/com/volna/app/booking/data/BookingMappers.kt](client/shared/src/commonMain/kotlin/com/volna/app/booking/data/BookingMappers.kt)
  - маппинг API DTO в доменные модели; любые изменения в контрактах здесь напрямую повлияют на клиент.

## 6. Неочевидные зависимости и риски регресса

Помимо явно описанного функционала, изменения в ТЗ могут повлиять и на следующие аспекты:

- состояние навигации после ошибки и после возврата с экрана деталей;
- поведение empty/error states на списках и карточках;
- кэш/состояние слота после ошибок 409 и повторной загрузки;
- поведение после отмены слота центром и после повторной попытки записи;
- тексты ошибок и подсказки, которые завязаны на API-коды;
- совместимость с существующим сервером и контрактом API, если поля/статусы изменятся.

## 7. Вопросы к BA / продукту

1. Какая целевая платформа для MVP: нативное мобильное приложение, PWA или мобильный web?
2. Какой лимит мест считать каноническим: 3 или 4?
3. Какой единый алгоритм доступности должен применяться в UI и API: по свободным местам, по маршруту и по отдельному фонду проката?
4. Является ли push-уведомление обязательным для MVP или это optional enhancement?
5. Какой контракт статусов отмены должен быть каноническим: `club_cancelled` или `center_cancelled`?
6. Нужно ли включать в MVP сценарии редактирования профиля, выхода и удаления аккаунта как полноценный функционал, или это следует вынести в следующую фазу?

## 8. Рекомендация по следующему шагу

Перед передачей в разработку требуется:

- зафиксировать единую платформу MVP;
- привести к одному виду правила доступности мест и лимит мест;
- согласовать контракт цены и статус отмены клуба;
- определить, какие push-сценарии входят в MVP;
- обновить требования и acceptance criteria так, чтобы они совпадали с экранной спецификацией и OpenAPI.

## 9. Итог

ТЗ уже содержит хорошую основу для MVP, но оно пока не завершено как единый контракт между продуктом, дизайном, API и клиентской реализацией. Наиболее опасные зоны — доступность и лимиты, отмена и статусы, цена и push. Без их уточнения высок риск расхождения между аналитикой, UI и кодом, что приведёт к регрессиям уже на ранних этапах разработки.
