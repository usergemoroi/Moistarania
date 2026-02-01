# Релизные APK файлы

Эта папка содержит собранные APK файлы приложения.

## v1test.apk

Упрощённая версия приложения:
- Убран весь Kotlin Material Design GUI
- MainActivity автоматически запускает InjectorService при открытии
- Прозрачный минималистичный интерфейс (без кнопок и UI элементов)
- ImGui overlay из so2-external-main.zip остаётся без изменений
- Все функции запуска overlay сохранены (root check, native builder, injector service)
- Все разрешения в AndroidManifest.xml сохранены

### Установка

```bash
adb install -r v1test.apk
```

### Использование

1. Запустите приложение - оно автоматически:
   - Проверит root доступ
   - Проверит разрешения
   - Соберёт native overlay
   - Запустит InjectorService
   - Запустит Standoff 2 (если установлена)
   - Закроется после успешного запуска overlay

2. Взаимодействие с overlay происходит через ImGui интерфейс
