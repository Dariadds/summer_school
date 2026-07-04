# Feature 2 — Недавние маршруты

## Цель
Сохранить историю недавно просмотренных маршрутов, чтобы пользователь мог быстро вернуться к последним маршрутам.

## Что реализовано
- Создана модель `RecentRoute`.
- Добавлен интерфейс `RecentRoutesRepository`.
- Реализован `LocalRecentRoutesRepository` на базе `PlatformKeyValueStorage`.
- В `SlotDetailsStore` добавлено сохранение recent route после успешной загрузки слота.
- В `SlotListStore` добавлено состояние `recentRoutes`, intent `LoadRecentRoutes` и загрузка истории при загрузке списка.
- В `SlotListScreen` добавлена секция `Недавние` до списка слотов и горизонтальный `LazyRow`.
- В DI `AppModule.kt` зарегистрирован `RecentRoutesRepository` и прокинут в сторы.

## Основные изменения
- `client/shared/src/commonMain/kotlin/com/volna/app/domain/model/RecentRoute.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/recent/RecentRoutesRepository.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/recent/LocalRecentRoutesRepository.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/catalog/presentation/SlotDetailsStore.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/catalog/presentation/SlotListStore.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/catalog/presentation/SlotListScreen.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/di/AppModule.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/MainTabs.kt`

## Проверка
- VS Code diagnostics: ошибок нет.
- Локальная Gradle-сборка сейчас не проверялась, так как раннее было SSL-исключение при загрузке Gradle.

## Как вручную протестировать
1. Открыть экран списка слотов.
2. Выбрать слот, открыть детали.
3. Убедиться, что после загрузки детали слот сохраняется в `Недавние`.
4. Вернуться к списку — секция `Недавние` должна появиться.
5. Нажать на недавний маршрут — должен открыться экран деталей для этого слота.
6. История должна сохраняться максимум 5 последних маршрутов.

## Важно
- Логика удаления дубликатов: при повторном просмотре маршрут перемещается в начало.
- Локальное хранение использует тот же Settings-подход, что и фича 1.
