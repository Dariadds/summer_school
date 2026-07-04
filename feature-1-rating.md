# Feature 1 — Оценка инструктора

## Цель
Добавить возможность в приложении "Волна" оценивать инструктора после просмотра деталей бронирования.

## Что реализовано
- Добавлен доменный класс `InstructorRating`.
- Создан интерфейс `RatingsRepository` и локальная реализация `LocalRatingsRepository`.
- Введена общая абстракция `PlatformKeyValueStorage` для хранения строковых значений.
- Реализованы актуальные методы для Android (`SharedPreferences`), iOS (`NSUserDefaults`) и WASM (`localStorage`).
- Расширен `BookingDetailsStore`:
  - загрузка сохранённого рейтинга при открытии деталей бронирования;
  - отображение текущего рейтинга/комментария;
  - обработка действий `ShowRatingSheet`, `DismissRatingSheet` и `SubmitRating`.
- Расширен `BookingDetailsScreen`:
  - добавлена кнопка "Оценить инструктора";
  - реализован `ModalBottomSheet` для ввода оценки и комментария;
  - сохранение рейтинга через `RatingsRepository`.
- Android: инициализация `PlatformKeyValueStorage` в `MainActivity`.

## Основные изменения
- `client/shared/src/commonMain/kotlin/com/volna/app/domain/model/InstructorRating.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/ratings/RatingsRepository.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/ratings/LocalRatingsRepository.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/core/storage/PlatformKeyValueStorage.kt`
- `client/shared/src/androidMain/kotlin/com/volna/app/core/storage/PlatformKeyValueStorage.android.kt`
- `client/shared/src/iosMain/kotlin/com/volna/app/core/storage/PlatformKeyValueStorage.ios.kt`
- `client/shared/src/wasmJsMain/kotlin/com/volna/app/core/storage/PlatformKeyValueStorage.wasm.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/di/AppModule.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingDetailsStore.kt`
- `client/shared/src/commonMain/kotlin/com/volna/app/booking/presentation/BookingDetailsScreen.kt`
- `client/androidApp/src/main/kotlin/com/volna/app/android/MainActivity.kt`

## Проверка
- Локальные проверки синтаксиса: ошибок нет.
- Сборка Gradle упала на загрузке дистрибутива из-за SSL-проверки:
  - `PKIX path building failed`
  - необходимо решить сетевую/сертификатную проблему, чтобы завершить компиляцию.

## Как вручную протестировать
1. Открыть экран деталей бронирования.
2. Нажать кнопку `Оценить инструктора`.
3. Выбрать количество звезд.
4. Опционально ввести комментарий.
5. Нажать `Отправить`.
6. Закрыть и снова открыть детали бронирования — оценка должна загрузиться из локального хранилища.

## Что важно проверить
- Отображается модальное окно `ModalBottomSheet`.
- Оценка сохраняется и загружается после перезапуска экрана.
- Состояние `showRatingSheet` корректно закрывается при `DismissRatingSheet`.
- Android `MainActivity` инициализирует `PlatformKeyValueStorage`.
