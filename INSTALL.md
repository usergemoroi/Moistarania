# Руководство по установке

## Требования

### Обязательно:
1. **Устройство с root правами**
   - Magisk (рекомендуется)
   - SuperSU
   - KernelSU
   - Другие root решения

2. **Android 5.0+** (API Level 21+)

3. **ARM64 устройство** (arm64-v8a архитектура)

4. **Standoff 2** установлен на устройстве

### Рекомендуется:
- Свободное место: минимум 100MB
- Android 8.0+ для лучшей совместимости
- Termux + Android NDK (для сборки на устройстве)

## Установка APK

### Метод 1: Установка готового APK

1. Скачайте `Standoff2Injector-v1.0.apk` из релизов
2. Включите "Неизвестные источники" в настройках Android
3. Установите APK файл
4. Запустите приложение

### Метод 2: Сборка из исходников

#### На компьютере:

**Требования:**
- JDK 11 или выше
- Android SDK
- Android NDK (опционально, для prebuilt binary)

**Шаги:**

```bash
# Клонировать репозиторий
git clone <repository-url>
cd standoff2-injector

# (Опционально) Собрать native библиотеку
export ANDROID_NDK_HOME=/path/to/ndk
./build_native.sh

# Собрать APK
./gradlew assembleRelease

# APK будет в:
# app/build/outputs/apk/release/app-release.apk

# Установить через ADB
adb install -r app/build/outputs/apk/release/app-release.apk
```

#### На устройстве (Termux):

```bash
# Установить Termux и зависимости
pkg install git openjdk-17 wget

# Клонировать репозиторий
git clone <repository-url>
cd standoff2-injector

# Собрать APK
./gradlew assembleDebug

# Установить
termux-open app/build/outputs/apk/debug/app-debug.apk
```

## Первый запуск

### 1. Предоставление разрешений

При первом запуске приложение запросит несколько разрешений:

#### Root доступ
- Приложение покажет диалог SuperUser/Magisk
- Нажмите **"Разрешить"**
- ⚠️ Без root прав приложение не будет работать

#### Доступ к хранилищу
- Android 11+: "Доступ ко всем файлам"
- Android 10-: "Чтение и запись хранилища"
- Необходимо для копирования native файлов

#### Отображение поверх других приложений
- Необходимо для показа overlay
- Включите разрешение в настройках

### 2. Проверка root

После предоставления разрешений приложение автоматически проверит root доступ.

**Если проверка не прошла:**
1. Убедитесь, что устройство рутировано
2. Откройте приложение Magisk/SuperUser
3. Найдите Standoff2Injector в списке
4. Предоставьте root права
5. Перезапустите приложение

### 3. Готовность к использованию

Когда статус покажет "Ready to inject" - приложение готово к работе.

## Устранение проблем установки

### "Parse error" при установке APK

**Причины:**
- Поврежденный APK файл
- Несовместимая архитектура устройства
- Недостаточно места

**Решение:**
```bash
# Проверить архитектуру устройства
adb shell getprop ro.product.cpu.abi

# Должно быть: arm64-v8a
# Если другое - приложение не совместимо
```

### "App not installed"

**Причины:**
- Старая версия уже установлена
- Подпись APK не совпадает

**Решение:**
```bash
# Удалить старую версию
adb uninstall com.standoff2.injector

# Установить заново
adb install app-release.apk
```

### Root права не предоставляются

**Проверка root:**
```bash
# Через ADB
adb shell su -c "id"

# Должно показать: uid=0(root)
```

**Решение:**
1. Переустановите Magisk
2. Очистите данные приложения SuperUser
3. Перезагрузите устройство

### "Permission denied" ошибки

**Решение:**
```bash
# Предоставить все разрешения через ADB
adb shell pm grant com.standoff2.injector android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.standoff2.injector android.permission.WRITE_EXTERNAL_STORAGE

# Для Android 11+
adb shell appops set com.standoff2.injector MANAGE_EXTERNAL_STORAGE allow
```

## Продвинутая установка

### Установка с кастомной подписью

```bash
# Создать keystore
keytool -genkey -v -keystore my-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias my-alias

# Подписать APK
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
  -keystore my-release-key.jks \
  app-release-unsigned.apk my-alias

# Выровнять APK
zipalign -v 4 app-release-unsigned.apk app-release.apk
```

### Установка в системный раздел (требует root)

```bash
# Примонтировать /system как rw
adb shell su -c "mount -o remount,rw /system"

# Скопировать APK
adb push app-release.apk /system/app/Standoff2Injector/
adb shell su -c "chmod 644 /system/app/Standoff2Injector/app-release.apk"

# Перезагрузить
adb reboot
```

### Установка через Recovery

1. Создать flashable ZIP:
```
standoff2-injector-flashable.zip
├── META-INF/
│   └── com/
│       └── google/
│           └── android/
│               ├── update-binary
│               └── updater-script
└── system/
    └── app/
        └── Standoff2Injector/
            └── app-release.apk
```

2. Прошить через TWRP/Recovery

## Проверка установки

```bash
# Проверить, что APK установлен
adb shell pm list packages | grep standoff2.injector

# Проверить версию
adb shell dumpsys package com.standoff2.injector | grep versionName

# Проверить разрешения
adb shell dumpsys package com.standoff2.injector | grep permission
```

## Обновление

### Через новый APK:
```bash
# Установить поверх (сохраняет настройки)
adb install -r app-release-new.apk
```

### Через чистую установку:
```bash
# Удалить старую версию
adb uninstall com.standoff2.injector

# Установить новую
adb install app-release-new.apk
```

## Удаление

### Обычное удаление:
```bash
adb uninstall com.standoff2.injector
```

### Полное удаление (с данными):
```bash
# Остановить приложение
adb shell am force-stop com.standoff2.injector

# Удалить native файлы
adb shell su -c "rm -rf /data/local/tmp/so2_external"

# Удалить APK
adb uninstall com.standoff2.injector
```

## Следующие шаги

После успешной установки перейдите к [USAGE.md](USAGE.md) для инструкций по использованию.
