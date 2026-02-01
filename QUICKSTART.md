# Quick Start Guide

## Для пользователей (5 минут)

### Шаг 1: Скачать и установить
```bash
# Скачайте APK из релизов
# ИЛИ соберите сами:
git clone <repo-url>
cd standoff2-injector
./gradlew assembleRelease

# Установите на устройство
adb install app/build/outputs/apk/release/app-release.apk
```

### Шаг 2: Первый запуск
1. Откройте **Standoff2 Injector**
2. Нажмите **"Разрешить"** когда запросит root
3. Предоставьте все разрешения (storage, overlay)
4. Дождитесь статуса **"Ready to inject"**

### Шаг 3: Запуск
1. Нажмите **"START"**
2. Подождите ~2-5 минут (первая сборка)
3. Дождитесь **"Overlay is running"**
4. Нажмите **"Launch Standoff 2"**
5. Играйте с ESP! 🎮

### Остановка
1. Сверните игру
2. Откройте инжектор
3. Нажмите **"STOP"**

---

## Для разработчиков (10 минут)

### Быстрая сборка
```bash
# Клонировать
git clone <repo-url>
cd standoff2-injector

# Настроить local.properties
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# Собрать debug
./gradlew assembleDebug

# Установить на устройство
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Сборка с prebuilt native
```bash
# Установить NDK
export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/25.2.9519653

# Собрать native
./build_native.sh

# Собрать APK
./gradlew assembleRelease
```

### Структура проекта
```
app/src/main/
├── java/com/standoff2/injector/   # Kotlin код
│   ├── MainActivity.kt            # UI
│   ├── InjectorService.kt         # Overlay manager
│   ├── RootChecker.kt             # Root utils
│   └── NativeBuilder.kt           # Native builder
├── res/                           # Ресурсы
│   ├── layout/                    # XML layouts
│   └── values/                    # Strings, colors
└── assets/
    └── so2-external-main.zip      # Native overlay

Native overlay: so2-external-main/src/
├── main.cpp                       # Entry point
├── standoff/player.h              # ESP logic
└── standoff/menu.h                # ImGui menu
```

### Ключевые файлы
- **MainActivity.kt** - UI и lifecycle
- **InjectorService.kt** - Background overlay manager
- **main.cpp** - Native overlay entry
- **player.h** - ESP drawing logic

### Debugging
```bash
# Android logs
adb logcat -s MainActivity:* InjectorService:*

# Native overlay logs
adb shell su -c "logcat | grep standof"

# Check overlay process
adb shell ps | grep standof.sh
```

### Модификация ESP

**Изменить цвета:**
```cpp
// В standoff/menu.h или player.h
ImColor boxColor = ImColor(255, 0, 0); // Красный
```

**Добавить новую функцию:**
```cpp
// В standoff/player.h
if (showDistance) {
    ImGui::GetBackgroundDrawList()->AddText(
        screenPos,
        ImColor(255, 255, 255),
        std::to_string((int)distance).c_str()
    );
}
```

**Добавить UI элемент:**
```cpp
// В standoff/menu.h
ImGui::Checkbox("Show Distance", &showDistance);
```

---

## Troubleshooting (Быстрые решения)

### ❌ "Root access denied"
```bash
# Проверить root
adb shell su -c "id"
# Должно показать: uid=0(root)
```
**Решение:** Откройте Magisk и предоставьте права приложению

### ❌ "Build failed"
**Решение:** Приложение использует prebuilt binary при отсутствии NDK

### ❌ "Overlay not visible"
```bash
# Проверить разрешение
adb shell appops get com.standoff2.injector SYSTEM_ALERT_WINDOW
```
**Решение:** Настройки → Приложения → Standoff2 Injector → Разрешения → "Поверх других"

### ❌ "Game crashes"
**Решение:**
1. Закройте все фоновые приложения
2. Запустите игру СНАЧАЛА
3. Затем запустите overlay

### ❌ APK не устанавливается
```bash
# Удалить старую версию
adb uninstall com.standoff2.injector

# Установить снова
adb install -r app-release.apk
```

---

## Команды для копипаста

### Сборка
```bash
# Debug
./gradlew clean assembleDebug

# Release
./gradlew clean assembleRelease

# Install
./gradlew installDebug
```

### ADB
```bash
# Install
adb install -r app.apk

# Uninstall
adb uninstall com.standoff2.injector

# Start app
adb shell am start -n com.standoff2.injector/.MainActivity

# Logs
adb logcat -s MainActivity:* InjectorService:* -v brief

# Kill overlay
adb shell su -c "pkill -f standof.sh"
```

### Root commands (на устройстве)
```bash
# Check overlay process
su -c "ps | grep standof"

# Kill overlay
su -c "pkill -f standof.sh"

# Check logs
su -c "logcat | grep -i esp"

# Manual run
su -c "/data/local/tmp/so2_external/so2-external-main/libs/arm64-v8a/standof.sh"
```

---

## Что дальше?

### 📖 Детальная документация
- [README.md](README.md) - Полное описание
- [INSTALL.md](INSTALL.md) - Подробная установка
- [USAGE.md](USAGE.md) - Руководство пользователя
- [BUILD.md](BUILD.md) - Инструкции по сборке

### 🔧 Разработка
- [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) - Архитектура
- [CHANGELOG.md](CHANGELOG.md) - История изменений

### 🎮 Использование
1. Прочитайте [USAGE.md](USAGE.md) для всех возможностей
2. Настройте ESP под себя
3. Используйте на альтернативном аккаунте!

### ⚠️ Важно
- **НЕ используйте на основном аккаунте!**
- **Риск бана - ваша ответственность!**
- **Только для образовательных целей!**

---

## Контакты и помощь

- **Issues:** [GitHub Issues](https://github.com/your-repo/issues)
- **Bugs:** Создайте Issue с логами
- **Questions:** Проверьте FAQ в [USAGE.md](USAGE.md)

## Готово! 🚀

Теперь у вас есть рабочий Standoff 2 ESP Injector!

Наслаждайтесь, но **используйте ответственно**! ⚡
