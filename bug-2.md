## Симптом
Сетевые ошибки (например, потеря соединения) не всегда распознаются как `NetworkUnavailable`; в логике обработки ошибок вместо корректного сообщения пользователю может показываться общее `Не удалось загрузить` или `Не удалось войти`.

## Требования
При возникновении сетевых исключений они должны корректно маппиться в `AppFailure.NetworkUnavailable`, чтобы UI показывал понятное сообщение и корректно обрабатывал состояние сети.

## Исправление
Файл: `client/shared/src/commonMain/kotlin/com/volna/app/core/network/VolnaApiClient.kt`

Найти внизу файла импорт и маппинг исключений:
```
import kotlinx.io.IOException
...
is IOException -> AppFailureException(AppFailure.NetworkUnavailable)
```

Заменить импорт на стандартный `java.io.IOException`:
```
import java.io.IOException
```

Причина: `kotlinx.io.IOException` — другой класс исключения, Ktor/платформенные сетевые ошибки бросают `java.io.IOException` (или подклассы), поэтому текущая ветка никогда не срабатывает.

## Промпты
- Промпт 1: `Открыть client/shared/src/commonMain/kotlin/com/volna/app/core/network/VolnaApiClient.kt`
- Промпт 2: `Проверка toAppFailureException() на импорт IOException и соответствие типам исключений Ktor` 

## Проверка
- Смоделировать отключение сети и убедиться, что в UI появляется сообщение: "Не удалось загрузить. Проверьте соединение и попробуйте снова" (или соответствующий текст), а в логах ошибка мапится в `AppFailure.NetworkUnavailable`.
- Юнит/интеграционные тесты на `toAppFailureException()` возвращают `AppFailure.NetworkUnavailable` при броске `IOException`.
