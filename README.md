# Standoff 2 External ESP Injector

Полнофункциональный Android инжектор для игры Standoff 2 (com.axlebolt.standoff2).

## Описание

Это приложение-инжектор создано для запуска external ESP overlay на Android устройствах. Overlay работает как отдельный процесс с правами root и отображает ESP (Extra Sensory Perception) информацию поверх игры Standoff 2.

### Основные возможности

- ✅ External ESP overlay (не модифицирует игровой процесс)
- ✅ Простой и понятный GUI интерфейс
- ✅ Автоматическая сборка native библиотеки
- ✅ Поддержка OpenGL ES 3.0 и Vulkan
- ✅ Интеграция с ImGui для меню настроек
- ✅ Автоматический запуск игры после инжекта
- ✅ Поддержка arm64-v8a архитектуры

## Требования

### Обязательно:
- ✅ **Root права** (Magisk, SuperSU и т.д.)
- ✅ **Android 5.0+** (API 21+)
- ✅ **ARM64 устройство** (arm64-v8a)
- ✅ **Standoff 2** установлен на устройстве

### Разрешения:
- READ/WRITE_EXTERNAL_STORAGE
- INTERNET
- SYSTEM_ALERT_WINDOW
- QUERY_ALL_PACKAGES

### Опционально:
- Android NDK на устройстве (для сборки на месте)
- Termux с NDK (если нужна пересборка)

## Установка

### Способ 1: Использование готового APK

1. Скачайте готовый APK из релизов
2. Установите APK на устройство
3. Откройте приложение и предоставьте root права
4. Нажмите "START" для запуска overlay
5. Нажмите "Launch Standoff 2" для запуска игры

### Способ 2: Сборка из исходников

1. Клонируйте репозиторий:
```bash
git clone <repository-url>
cd standoff2-injector
```

2. (Опционально) Предварительно соберите native библиотеку:
```bash
export ANDROID_NDK_HOME=/path/to/ndk
./build_native.sh
```

3. Соберите APK:
```bash
./gradlew assembleRelease
```

4. Установите APK:
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Использование

### Первый запуск:

1. **Предоставьте разрешения:**
   - Root права (через SuperUser/Magisk)
   - Доступ к хранилищу
   - Разрешение на отображение поверх других приложений

2. **Запустите overlay:**
   - Нажмите кнопку "START"
   - Приложение автоматически соберет и запустит overlay
   - Дождитесь сообщения "Overlay is running"

3. **Запустите игру:**
   - Нажмите "Launch Standoff 2" ИЛИ
   - Запустите игру вручную из лаунчера

4. **Используйте overlay:**
   - Overlay появится поверх игры
   - Используйте меню для настройки ESP
   - ESP отобразит информацию о игроках

### Остановка overlay:

1. Вернитесь в приложение инжектора
2. Нажмите кнопку "STOP"

## Архитектура проекта

```
.
├── app/                        # Android приложение
│   ├── src/main/
│   │   ├── java/com/standoff2/injector/
│   │   │   ├── MainActivity.kt       # Главная активность
│   │   │   ├── InjectorService.kt    # Сервис запуска overlay
│   │   │   ├── RootChecker.kt        # Проверка и выполнение root команд
│   │   │   └── NativeBuilder.kt      # Сборка native библиотеки
│   │   ├── res/                      # Ресурсы приложения
│   │   └── assets/
│   │       └── so2-external-main.zip # Исходники overlay
│   └── build.gradle
├── so2-external-main/          # Native C++ код overlay
│   ├── src/
│   │   ├── main.cpp           # Точка входа
│   │   ├── Android_draw/      # Отрисовка (OpenGL/Vulkan)
│   │   ├── Android_touch/     # Обработка касаний
│   │   ├── standoff/          # Логика игры (ESP, menu)
│   │   └── ImGui/             # UI библиотека
│   ├── Android.mk             # NDK build script
│   └── Application.mk         # NDK конфигурация
├── build_native.sh            # Скрипт сборки native части
└── README.md
```

## Технические детали

### Native Overlay (C++17)

- **Язык:** C++17
- **Сборка:** Android NDK (ndk-build)
- **Архитектура:** arm64-v8a
- **Рендеринг:** EGL + OpenGL ES 3.0 (опционально Vulkan)
- **GUI:** Dear ImGui
- **API:** process_vm_readv/writev для чтения памяти игры

### Android App (Kotlin)

- **Язык:** Kotlin
- **Min SDK:** 21 (Android 5.0)
- **Target SDK:** 34 (Android 14)
- **UI:** Material Design 3
- **Архитектура:** Service + Activity

### Основные компоненты:

1. **MainActivity:** UI для управления overlay
2. **InjectorService:** Background сервис для запуска overlay процесса
3. **RootChecker:** Проверка root и выполнение команд с повышенными правами
4. **NativeBuilder:** Компиляция native overlay на устройстве или использование prebuilt

## ESP Возможности

- ✅ **Box ESP:** Рамки вокруг игроков
- ✅ **Health Bar:** Отображение здоровья
- ✅ **Team ESP:** Различие команд по цвету
- ✅ **Distance:** Дистанция до игроков
- ✅ **World-to-Screen:** Проекция 3D координат на экран

## Безопасность

⚠️ **ВАЖНО:**
- Это external overlay, он НЕ модифицирует процесс игры
- Использование любых читов может привести к бану аккаунта
- Используйте на свой риск
- Разработчики не несут ответственности за последствия

## Отладка

### Логи:

Просмотр логов через ADB:
```bash
adb logcat -s InjectorService:* NativeBuilder:* MainActivity:*
```

### Проблемы и решения:

**"Root access denied"**
- Убедитесь, что устройство правильно рутировано
- Проверьте SuperUser/Magisk настройки
- Дайте приложению root права

**"Game not found"**
- Убедитесь, что Standoff 2 установлен
- Проверьте имя пакета: com.axlebolt.standoff2

**"Build failed"**
- NDK не установлен на устройстве
- Приложение попытается использовать prebuilt binary
- Установите Termux с NDK для сборки на месте

**"Overlay not visible"**
- Предоставьте разрешение "Поверх других приложений"
- Проверьте, что игра запущена
- Перезапустите overlay

## Сборка на устройстве

Если у вас установлен Termux с NDK:

```bash
# В Termux
pkg install android-ndk
export ANDROID_NDK_HOME=$PREFIX/lib/android-ndk
cd /data/local/tmp/so2_external/so2-external-main
ndk-build
```

## Обновления

- v1.0 - Первоначальный релиз
  - External ESP overlay
  - OpenGL ES поддержка
  - Базовое ESP меню

## Лицензия

Этот проект предназначен только для образовательных целей.

## Контакты

По вопросам и предложениям создавайте Issues в репозитории.

---

**Disclaimer:** Использование читов в онлайн играх нарушает правила большинства игр и может привести к перманентной блокировке аккаунта. Этот проект создан исключительно в образовательных целях для изучения Android NDK, межпроцессного взаимодействия и графических API.
