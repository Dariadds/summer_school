## Симптом
Неправильная обработка ввода телефонного номера: номера, введённые в распространённом российском формате с ведущей `8` (например `8 912 345 6789`), обрезаются некорректно — последний символ теряется или формат становится неверным. Это приводит к ошибкам в валидации номера и невозможности запросить код.

## Требования
Функция очистки ввода номера должна корректно обрабатывать варианты с префиксами `8` и `7`, приводя ввод к 10 значному локальному набору цифр (без префикса) перед форматированием/нормализацией в E.164.

## Исправление
Файл: `client/shared/src/commonMain/kotlin/com/volna/app/core/phone/PhoneInputCore.kt`

Найти функцию `sanitizePhoneInput` и строку:
```
    if (digits.length >= 11 && digits.startsWith("7")) {
        val withoutPrefix = digits.drop(1)
        return if (maxDigits > 0) withoutPrefix.take(maxDigits) else withoutPrefix
    }
```

Заменить на (учёт обеих возможных префиксов):
```
    if (digits.length >= 11 && (digits.startsWith("7") || digits.startsWith("8"))) {
        val withoutPrefix = digits.drop(1)
        return if (maxDigits > 0) withoutPrefix.take(maxDigits) else withoutPrefix
    }
```

Это гарантирует, что при вводе `8XXXXXXXXXX` и `7XXXXXXXXXX` мы убираем ведущий код и оставляем 10 значный локальный номер.

## Промпты
- Промпт 1: `Открыть client/shared/src/commonMain/kotlin/com/volna/app/core/phone/PhoneInputCore.kt и проверить sanitizePhoneInput()`
- Промпт 2: `Проверить normalizePhoneE164 и форматирование телефонов (formatPhoneNumberWithPositions)`

## Проверка
- Ввести телефон `8 912 345 6789` в поле на экране — поле должно признать ввод полным и корректно отформатировать в `+7 (912) 345-67-89` / при нормализации получить `+79123456789`.
- Функции `isRussianPhoneInputComplete` и `normalizePhoneE164` корректно работают для вводов с `8` и `+7`.
