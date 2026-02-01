# Инструкции по сборке

## Быстрая сборка

```bash
# Клонировать репозиторий
git clone <repository-url>
cd standoff2-injector

# Собрать debug APK
./gradlew assembleDebug

# Собрать release APK
./gradlew assembleRelease
```

## Детальные инструкции

### Требования для сборки

1. **JDK 11 или выше**
```bash
java -version
# Должно показать версию 11+
```

2. **Android SDK**
   - Установите через Android Studio ИЛИ
   - Скачайте command line tools

3. **Android NDK** (опционально, для prebuilt binary)
   - Версия r21 или выше
   - Нужен только для `./build_native.sh`

### Настройка окружения

#### Linux/macOS:

```bash
# Установить ANDROID_HOME
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/platform-tools

# (Опционально) Установить NDK_HOME для prebuilt
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/25.2.9519653
```

#### Windows:

```cmd
set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\tools;%ANDROID_HOME%\platform-tools
```

### Шаг 1: Настройка local.properties

Создайте или отредактируйте `local.properties`:

```properties
sdk.dir=/path/to/android/sdk
ndk.dir=/path/to/android/ndk
```

### Шаг 2: (Опционально) Сборка native библиотеки

Если у вас установлен NDK и вы хотите включить prebuilt binary в APK:

```bash
./build_native.sh
```

Это создаст `app/src/main/assets/standof.sh`

**Примечание:** Этот шаг опционален. Если его пропустить, приложение попытается собрать native код на устройстве при первом запуске.

### Шаг 3: Сборка APK

#### Debug сборка:

```bash
./gradlew assembleDebug
```

**Результат:**
- `app/build/outputs/apk/debug/app-debug.apk`
- Подписан debug ключом
- Включает отладочную информацию

#### Release сборка:

```bash
./gradlew assembleRelease
```

**Результат:**
- `app/build/outputs/apk/release/app-release-unsigned.apk`
- Не подписан (нужна подпись для установки)
- Оптимизирован и минифицирован

### Шаг 4: Подпись release APK

#### Создать keystore (первый раз):

```bash
keytool -genkey -v -keystore release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias standoff2-injector

# Заполните информацию и создайте пароль
```

#### Настроить gradle для автоматической подписи:

Создайте `keystore.properties`:

```properties
storePassword=your_store_password
keyPassword=your_key_password
keyAlias=standoff2-injector
storeFile=/path/to/release-key.jks
```

Добавьте в `app/build.gradle`:

```gradle
def keystorePropertiesFile = rootProject.file("keystore.properties")
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        release {
            keyAlias keystoreProperties['keyAlias']
            keyPassword keystoreProperties['keyPassword']
            storeFile file(keystoreProperties['storeFile'])
            storePassword keystoreProperties['storePassword']
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

Теперь release APK будет автоматически подписан:

```bash
./gradlew assembleRelease
# Создаст app/build/outputs/apk/release/app-release.apk (подписанный)
```

#### Ручная подпись APK:

```bash
# Подписать
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore release-key.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  standoff2-injector

# Выровнять
zipalign -v 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/apk/release/app-release.apk
```

## Gradle таски

```bash
# Очистить проект
./gradlew clean

# Собрать debug
./gradlew assembleDebug

# Собрать release
./gradlew assembleRelease

# Установить debug на подключенное устройство
./gradlew installDebug

# Запустить lint проверки
./gradlew lint

# Запустить тесты
./gradlew test

# Собрать и установить
./gradlew installDebug
```

## Сборка с Docker

Для воспроизводимых сборок используйте Docker:

```dockerfile
FROM gradle:7.6-jdk11

# Установить Android SDK
ENV ANDROID_HOME=/opt/android-sdk
RUN mkdir -p $ANDROID_HOME && \
    cd $ANDROID_HOME && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip && \
    unzip commandlinetools-linux-9477386_latest.zip && \
    yes | cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME \
        "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Скопировать проект
COPY . /app
WORKDIR /app

# Собрать
RUN ./gradlew assembleRelease

# Вывести APK
CMD ["cp", "app/build/outputs/apk/release/app-release.apk", "/output/"]
```

Использование:

```bash
docker build -t standoff2-injector-builder .
docker run -v $(pwd)/output:/output standoff2-injector-builder
```

## Сборка на устройстве (Termux)

### Требования:
```bash
pkg install git openjdk-17 wget
```

### Сборка:
```bash
git clone <repository-url>
cd standoff2-injector

# Установить Android SDK (упрощенная версия)
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip -d android-sdk
export ANDROID_HOME=$PWD/android-sdk

# Собрать
./gradlew assembleDebug --no-daemon

# Открыть APK
termux-open app/build/outputs/apk/debug/app-debug.apk
```

## Устранение проблем сборки

### "SDK location not found"

```bash
# Создать local.properties
echo "sdk.dir=/path/to/android/sdk" > local.properties
```

### "Unsupported class file major version"

Ваша версия JDK слишком новая или старая:

```bash
# Проверить версию
java -version

# Установить JDK 11 или 17
```

### "Execution failed for task ':app:mergeDebugResources'"

Проблема с ресурсами. Очистить и пересобрать:

```bash
./gradlew clean
rm -rf app/build
./gradlew assembleDebug
```

### "Failed to install the following Android SDK packages"

```bash
# Принять лицензии SDK
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
```

### Медленная сборка

```bash
# Включить Gradle daemon
echo "org.gradle.daemon=true" >> gradle.properties

# Увеличить память
echo "org.gradle.jvmargs=-Xmx4096m" >> gradle.properties

# Использовать параллельную сборку
./gradlew assembleDebug --parallel
```

## CI/CD сборка

### GitHub Actions example:

```yaml
name: Build APK

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
      
    - name: Build with Gradle
      run: ./gradlew assembleRelease
      
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-release
        path: app/build/outputs/apk/release/app-release.apk
```

## Размер APK

Типичные размеры:
- Debug APK: ~20-30 MB
- Release APK (без native): ~10-15 MB
- Release APK (с prebuilt native): ~15-20 MB

Для уменьшения размера:
```gradle
android {
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
        }
    }
}
```

## Проверка сборки

```bash
# Информация об APK
aapt dump badging app/build/outputs/apk/release/app-release.apk

# Список файлов в APK
unzip -l app/build/outputs/apk/release/app-release.apk

# Проверить подпись
jarsigner -verify -verbose app/build/outputs/apk/release/app-release.apk
```

## Следующие шаги

После успешной сборки перейдите к [INSTALL.md](INSTALL.md) для инструкций по установке.
