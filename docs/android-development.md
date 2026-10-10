# Android 개발과 실행

## 프로젝트

| 항목 | 값 |
| --- | --- |
| 프로젝트 이름 | KitchenMoodRadio |
| 앱 이름 | Kitchen Mood Radio |
| 패키지 | `com.cwnu.kitchenmood` |
| 언어 | Java 17 |
| 최소 Android 버전 | Android 8.0 · API 26 |
| compileSdk / targetSdk | 36 / 36 |
| Android Gradle Plugin | 8.13.2 |
| Gradle | 8.13 |
| Build Tools | 35.0.0 |
| 앱 버전 | 0.2.0 |

## Android Studio

1. 저장소 루트 폴더를 Android Studio에서 연다.
2. Gradle JDK를 17로 설정한다.
3. SDK Manager에서 Android SDK Platform 36과 Build Tools 35.0.0을 준비한다.
4. Gradle 동기화가 완료되면 `app` 실행 구성을 고른다.
5. Android 8.0 이상의 기기 또는 에뮬레이터에서 Run을 누른다.

## 명령줄 빌드

Windows:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

macOS / Linux:

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

SDK 경로는 개발 환경의 `local.properties` 또는 `ANDROID_HOME`으로 설정한다. `local.properties`는 저장소에 포함하지 않는다.

Windows에서 단위 테스트 실행 시 한글 경로 때문에 테스트 클래스를 찾지 못하면 프로젝트를 영문 경로에서 빌드한다. `local.properties`에는 SDK 경로의 드라이브 콜론을 이스케이프하고 슬래시를 사용한다. 예: `sdk.dir=C\:/Android/Sdk`.

생성되는 APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

기기에 설치하려면:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.cwnu.kitchenmood/.MainActivity
```

## 현재 동작 범위

무드 선택, 합성 배경음, 볼륨, 수동 타이머, 한마디 전환을 앱 화면에서 사용한다. 마지막 무드는 앱 설정에 저장한다. 화면을 벗어나면 소리는 멈춘다. 타이머는 종료 시각을 유지하고 앱으로 돌아왔을 때 남은 시간을 다시 계산한다.

앱 프로세스가 종료된 뒤의 알림이나 백그라운드 음악 서비스는 현재 구현 범위에 포함하지 않는다.

## 자동 빌드

GitHub의 Android prototype 워크플로가 디버그 APK, 단위 검사, Android Lint를 실행한다. 성공한 실행의 `kitchen-mood-radio-debug` 아티팩트에서 APK를 받을 수 있다.

## 웹 미리보기

화면 콘셉트를 확인하는 웹 버전은 `web` 폴더에 있다.

```bash
cd web
npm run build
python -m http.server 8769 --directory dist
```

Android 앱은 브라우저 화면을 감싸지 않고 플랫폼 View와 AudioTrack으로 구현한다.

## 확인한 범위

- 디버그 APK 빌드 완료
- 단위 테스트 9개 통과: 타이머 7개, 무드 데이터 2개
- Android Lint 오류 0개, Gradle 새 버전 안내 경고 1개
- Android API 35 가상 기기에 APK 설치 및 앱 실행 확인
- 가상 기기에서 무드 전환과 재생 상태 전환, 화면 이탈 후 음악 정지 확인
- 3분 프리셋 카운트다운, 일시정지 시 값 유지, 재개 후 화면 복귀, 초기화 확인
- 문장 전환과 GitHub 자동 빌드의 APK 아티팩트 생성 확인

실기기의 소리 품질과 다른 앱의 오디오 포커스 전환은 후속 확인 항목이다.
