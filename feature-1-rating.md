# Feature 1 — Оценка маршала
# Цель
Добавить возможность в приложении "Апекс" оценивать маршала после завершения заезда.

# Что реализовано
Добавлен доменный класс MarshalRating.

Создан интерфейс RatingsRepository и локальная реализация LocalRatingsRepository.

Введена общая абстракция PlatformKeyValueStorage для хранения строковых значений.

Реализованы актуальные методы для Android (SharedPreferences), iOS (NSUserDefaults) и WASM (localStorage).

Расширен BookingDetailsStore:

загрузка сохранённого рейтинга при открытии деталей бронирования;

отображение текущего рейтинга/комментария;

обработка действий ShowRatingSheet, DismissRatingSheet и SubmitRating.

Расширен BookingDetailsScreen:

добавлена кнопка "Оценить маршала";

реализован ModalBottomSheet для ввода оценки и комментария;

сохранение рейтинга через RatingsRepository.

Android: инициализация PlatformKeyValueStorage в MainActivity.

# Основные изменения
client/shared/src/commonMain/kotlin/com/apex/app/domain/model/MarshalRating.kt

client/shared/src/commonMain/kotlin/com/apex/app/ratings/RatingsRepository.kt

client/shared/src/commonMain/kotlin/com/apex/app/ratings/LocalRatingsRepository.kt

client/shared/src/commonMain/kotlin/com/apex/app/core/storage/PlatformKeyValueStorage.kt

client/shared/src/androidMain/kotlin/com/apex/app/core/storage/PlatformKeyValueStorage.android.kt

client/shared/src/iosMain/kotlin/com/apex/app/core/storage/PlatformKeyValueStorage.ios.kt

client/shared/src/wasmJsMain/kotlin/com/apex/app/core/storage/PlatformKeyValueStorage.wasm.kt

client/shared/src/commonMain/kotlin/com/apex/app/di/AppModule.kt

client/shared/src/commonMain/kotlin/com/apex/app/booking/presentation/BookingDetailsStore.kt

client/shared/src/commonMain/kotlin/com/apex/app/booking/presentation/BookingDetailsScreen.kt

client/androidApp/src/main/kotlin/com/apex/app/android/MainActivity.kt

# Проверка
Локальные проверки синтаксиса: ошибок нет.

Сборка Gradle упала на загрузке дистрибутива из-за SSL-проверки:

PKIX path building failed

необходимо решить сетевую/сертификатную проблему, чтобы завершить компиляцию.

Как вручную протестировать
Открыть экран деталей бронирования завершённого заезда.

Нажать кнопку Оценить маршала.

Выбрать количество звезд (1–5).

Опционально ввести комментарий.

Нажать Отправить.

Закрыть и снова открыть детали бронирования — оценка должна загрузиться из локального хранилища.

Что важно проверить
Отображается модальное окно ModalBottomSheet.

Оценка сохраняется и загружается после перезапуска экрана.

Состояние showRatingSheet корректно закрывается при DismissRatingSheet.

Android MainActivity инициализирует PlatformKeyValueStorage.

Оценка доступна только владельцу центра (не публикуется для других клиентов).

Кнопка "Оценить маршала" отображается только для завершённых заездов.