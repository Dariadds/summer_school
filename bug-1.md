## Симптом
При сборке/запуске Android/desktop/web версии приложение не компилируется: вызов `TermsText(...)` из UI вызывает ошибку компиляции о том, что функция не является `@Composable`.

## Требования
`TermsText` — это UI-компонент, используемый внутри Compose-экрана, поэтому он должен быть помечен как `@Composable`. Вызовы `TermsText(...)` должны компилироваться и корректно рендериться.

## Исправление
Изменить объявление функции в файле:
- `client/shared/src/commonMain/kotlin/com/volna/app/auth/presentation/AuthScreen.kt`

Найти строку:
```
private fun TermsText(text: String) {
```

Заменить на:
```
@Composable
private fun TermsText(text: String) {
```

Это добавит аннотацию Compose и устранит ошибку компиляции.

## Промпты
- Промпт 1: `grep TODO|FIXME` и поиск по коду, затем открыть подозрительные файлы `AuthScreen.kt`
- Промпт 2: `Открыть client/shared/src/commonMain/kotlin/com/volna/app/auth/presentation/AuthScreen.kt и найти объявление TermsText`

## Проверка
- Сборка проекта (`./gradlew assemble` / запуск нужного модуля) проходит без ошибок компиляции.
- Экран аутентификации отображается, текст с условиями виден внизу экрана.
