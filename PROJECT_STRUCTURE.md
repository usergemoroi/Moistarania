# Структура проекта

## Обзор

```
standoff2-injector/
├── app/                          # Android приложение
│   ├── build.gradle             # Gradle конфигурация модуля
│   ├── proguard-rules.pro       # ProGuard правила
│   └── src/main/
│       ├── AndroidManifest.xml  # Манифест приложения
│       ├── assets/              # Статические файлы
│       │   └── so2-external-main.zip  # Архив native кода
│       ├── java/com/standoff2/injector/
│       │   ├── MainActivity.kt        # Главная активность
│       │   ├── InjectorService.kt     # Сервис управления overlay
│       │   ├── RootChecker.kt         # Проверка root
│       │   └── NativeBuilder.kt       # Сборка native кода
│       └── res/                 # Ресурсы Android
│           ├── drawable/        # Векторная графика
│           ├── layout/          # XML макеты
│           ├── mipmap-*/        # Иконки приложения
│           └── values/          # Строки, цвета, темы
├── gradle/                      # Gradle wrapper
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── so2-external-main/           # Native C++ overlay (из архива)
│   ├── Android.mk              # NDK build скрипт
│   ├── Application.mk          # NDK конфигурация
│   ├── include/                # Заголовочные файлы
│   │   ├── Android_vulkan/    # Vulkan заголовки
│   │   └── ImGui/             # ImGui заголовки
│   └── src/                    # Исходный код C++
│       ├── main.cpp           # Точка входа
│       ├── Android_draw/      # Модуль отрисовки
│       ├── Android_touch/     # Модуль касаний
│       ├── Android_vulkan/    # Vulkan поддержка
│       ├── ImGui/             # ImGui реализация
│       ├── standoff/          # Игровая логика
│       │   ├── menu.h        # ESP меню
│       │   └── player.h      # Чтение игроков
│       └── structs/           # Вспомогательные структуры
├── build.gradle                # Root Gradle конфигурация
├── settings.gradle             # Gradle настройки
├── gradle.properties           # Gradle свойства
├── local.properties            # Локальные пути SDK
├── gradlew                     # Gradle wrapper (Unix)
├── gradlew.bat                # Gradle wrapper (Windows)
├── build_native.sh            # Скрипт сборки native части
├── .gitignore                 # Git игнорирование
├── README.md                  # Главное описание
├── INSTALL.md                 # Инструкции по установке
├── USAGE.md                   # Руководство пользователя
├── BUILD.md                   # Инструкции по сборке
├── CHANGELOG.md               # История изменений
├── LICENSE                    # Лицензия MIT
└── so2-external-main.zip      # Архив исходников overlay
```

## Описание компонентов

### Android Application Layer

#### MainActivity.kt
**Назначение:** Главный UI и точка входа приложения

**Функции:**
- Проверка и запрос разрешений (root, storage, overlay)
- UI управление кнопками START/STOP
- Отображение статуса overlay
- Запуск Standoff 2

**Жизненный цикл:**
```kotlin
onCreate() -> checkPermissions() -> checkRoot() -> Ready
    |                                              |
    └─────────────> startInjection() ────────────┘
```

#### InjectorService.kt
**Назначение:** Background сервис для управления overlay процессом

**Функции:**
- Запуск native executable с root правами
- Мониторинг состояния overlay процесса
- Чтение stdout/stderr overlay
- Остановка overlay

**Процесс:**
```
START_OVERLAY -> Проверка игры -> Запуск binary -> Мониторинг
STOP_OVERLAY  -> Убить процесс -> Очистка
```

#### RootChecker.kt
**Назначение:** Проверка наличия root и выполнение команд

**Методы:**
- `isRooted()` - Проверяет наличие root через несколько методов
- `executeRootCommand()` - Выполняет команду через `su`
- `executeCommand()` - Обычное выполнение команды

**Проверки root:**
1. Проверка build tags
2. Поиск su бинарника
3. Выполнение тестовой команды

#### NativeBuilder.kt
**Назначение:** Сборка или копирование native overlay

**Логика:**
```
Если NDK установлен:
    Распаковать исходники -> Собрать через ndk-build
Иначе:
    Использовать prebuilt binary из assets
```

**Пути:**
- Рабочая директория: `/data/local/tmp/so2_external`
- Исходники: `$WORK_DIR/so2-external-main`
- Binary: `$WORK_DIR/so2-external-main/libs/arm64-v8a/standof.sh`

### Native Overlay Layer

#### main.cpp
**Назначение:** Точка входа overlay, инициализация и главный цикл

**Функции:**
- `getProcessID()` - Поиск PID игры по имени пакета
- `get_module_base()` - Получение базового адреса libunity.so
- `rpm<T>()` - Чтение памяти через process_vm_readv
- `wpm<T>()` - Запись памяти через process_vm_writev
- `main()` - Главный цикл отрисовки

**Цикл:**
```cpp
while (main_thread_flag) {
    drawBegin();           // Начало кадра
    player();              // Отрисовка ESP
    RenderMenu();          // Отрисовка меню
    drawEnd();             // Конец кадра
    usleep(16000);         // ~60 FPS
}
```

#### Android_draw/draw.cpp
**Назначение:** Модуль отрисовки (EGL/OpenGL или Vulkan)

**Функции:**
- `initGUI_draw()` - Инициализация окна и контекста
- `drawBegin()` - Начало кадра (ImGui::NewFrame)
- `drawEnd()` - Конец кадра (ImGui::Render + swap buffers)
- `shutdown()` - Очистка ресурсов

**Технологии:**
- ANativeWindow для overlay window
- EGL для контекста OpenGL
- ImGui для UI

#### Android_touch/TouchHelperA.cpp
**Назначение:** Обработка касаний и ввода

**Функции:**
- `Touch_Init()` - Инициализация input stream
- Чтение событий из `/dev/input/event*`
- Передача событий в ImGui::IO
- Пассивный режим (touch passthrough)

#### standoff/player.h
**Назначение:** Игровая логика - чтение и отрисовка игроков

**Структуры:**
```cpp
GameManager -> PlayerList -> Player -> Transform -> Position
                                    -> Health
                                    -> Team
```

**ESP логика:**
- Итерация по списку игроков
- Фильтрация (дистанция, видимость)
- World-to-Screen проекция
- Отрисовка box/health/team

#### standoff/menu.h
**Назначение:** ImGui меню настроек

**Элементы:**
- Checkbox для включения ESP
- Слайдеры для настройки
- Кнопка Exit

### Build System

#### Android.mk
**Назначение:** NDK build конфигурация

**Важные параметры:**
```makefile
LOCAL_MODULE := standof.sh          # Имя выходного файла
BUILD_EXECUTABLE                    # Собрать как executable
OPENGL_DRAW = 1                    # Использовать OpenGL
```

#### Application.mk
**Назначение:** NDK application конфигурация

```makefile
APP_ABI := arm64-v8a               # Только ARM64
APP_PLATFORM := android-21         # Min API 21
APP_STL := c++_static              # Статическая STL
```

#### build_native.sh
**Назначение:** Скрипт предварительной сборки

**Этапы:**
1. Проверка наличия NDK
2. Распаковка исходников
3. Вызов ndk-build
4. Копирование binary в assets

### Resource Files

#### AndroidManifest.xml
**Разрешения:**
- `READ_EXTERNAL_STORAGE`
- `WRITE_EXTERNAL_STORAGE`
- `INTERNET`
- `QUERY_ALL_PACKAGES`
- `SYSTEM_ALERT_WINDOW`

**Компоненты:**
- MainActivity (exported, launcher)
- InjectorService (не exported)

#### res/layout/activity_main.xml
**UI элементы:**
- Logo ImageView
- Title TextView
- Status Card (CardView с TextView + ProgressBar)
- Info Card (CardView с описанием)
- START/STOP Button
- Launch Game Button
- Version TextView

#### res/values/
**Ресурсы:**
- `strings.xml` - Все текстовые строки
- `colors.xml` - Цветовая палитра
- `themes.xml` - Material 3 Dark тема

## Потоки данных

### Инжект процесс
```
MainActivity (UI Thread)
    ↓
startInjection() (Coroutine)
    ↓
NativeBuilder.buildNative()
    ↓ [su -c ndk-build]
Native Binary
    ↓
InjectorService.startOverlay()
    ↓ [su -c /path/to/binary]
Overlay Process (Root)
```

### ESP отрисовка
```
main() [Native]
    ↓
getProcessID("com.axlebolt.standoff2")
    ↓
rpm<GameManager>(il2cpp_base + offset)
    ↓
Итерация player list
    ↓
Для каждого игрока:
    ├─ rpm<Position>(player + offset)
    ├─ WorldToScreen(position)
    └─ ImGui::DrawList->AddRect()
```

### Touch обработка
```
/dev/input/event* (Kernel)
    ↓
TouchHelperA.cpp (Native)
    ↓
ImGui::IO.AddMousePosEvent()
    ↓
ImGui::IsWindowHovered()
    ├─ true  → ImGui обрабатывает
    └─ false → uinput passthrough
```

## Безопасность

### Root команды
Все root команды выполняются через:
```kotlin
Runtime.getRuntime().exec(arrayOf("su", "-c", command))
```

### Изоляция
- Overlay работает в отдельном процессе
- Не модифицирует игровой процесс
- Только чтение памяти (process_vm_readv)

### Обнаружение
Возможные векторы обнаружения:
1. Наличие su процесса
2. Активное чтение памяти
3. Overlay window
4. Известные offsets/сигнатуры

## Расширение

### Добавление новых ESP функций

1. Добавить структуру в `standoff/player.h`:
```cpp
struct NewFeature {
    uint64_t value1;
    float value2;
};
```

2. Добавить чтение в `player()`:
```cpp
auto feature = rpm<NewFeature>(playerAddr + OFFSET_FEATURE);
```

3. Добавить отрисовку:
```cpp
ImGui::GetBackgroundDrawList()->AddText(...);
```

4. Добавить настройку в `menu.h`:
```cpp
ImGui::Checkbox("Show Feature", &showFeature);
```

### Поддержка других игр

1. Изменить `GAME_PACKAGE` в MainActivity.kt
2. Обновить offsets в `player.h`
3. Адаптировать структуры под новую игру
4. Обновить логику чтения игроков

## Debugging

### Логи Android
```bash
adb logcat -s MainActivity:* InjectorService:* NativeBuilder:*
```

### Логи Native
```bash
adb shell su -c "logcat | grep -E 'standof|ESP'"
```

### Проверка процессов
```bash
# Проверить overlay
adb shell ps | grep standof.sh

# Проверить игру
adb shell ps | grep standoff2
```

## Performance

### Оптимизация
- Используйте `usleep()` для контроля FPS
- Минимизируйте количество `rpm()` вызовов
- Кешируйте неизменяемые данные
- Используйте batch отрисовку ImGui

### Мониторинг
```bash
# CPU usage
adb shell top -n 1 | grep standof.sh

# Memory
adb shell dumpsys meminfo | grep standof
```

## Maintenance

### Обновление offsets
После патча игры:
1. Запустить игру
2. Дамп libunity.so
3. Найти новые offsets с помощью IDA/Ghidra
4. Обновить в `player.h`

### Обновление зависимостей
```bash
# Обновить Gradle
./gradlew wrapper --gradle-version 8.2

# Обновить зависимости
./gradlew dependencyUpdates
```
