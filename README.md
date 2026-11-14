# My Cabin / 마이캐빈

**차 안의 휴식과 배움을, 내가 편했던 설정으로 이어가는 개인화 앱.**

현대자동차그룹 차량용 앱 개발 공모전 출품용 프로토타입입니다. 학습·휴식·수면·식사 모드에서 마이씬을 만들고 편집·저장·다시 실행합니다. Android APK에 **실제 Pleos Vehicle SDK 2.3.2**를 포함했습니다.

## 바로 확인하기

- [저장소에 보관된 이전 APK](apk/김재윤_마이캐빈_release.apk) — 현재 브랜치의 최신 소스가 반영된 파일인지 확인하고 교체해야 합니다.\n- [GitHub Actions APK 빌드 결과](https://github.com/rwopoja/MyCabin-Pleos/actions/workflows/submission-demo.yml) — 각 실행의 커밋과 테스트 결과를 확인한 뒤 산출물을 내려받으세요. 이 빌드는 개발용 임시 키로 서명됩니다.
- [실제 APK 실행 녹화 / 약 84초](media/MyCabin_APK_Demo.mp4)
- [원본 Android AVD 녹화](media/MyCabin_Android_AVD_Raw.mp4)
- [기능 흐름 영상 / 89초](media/MyCabin_Prototype_Demo.mp4)
- [세로형 숏폼 초안 / 45초](media/MyCabin_Shortform.mp4)
- [시연 순서](docs/DEMO_GUIDE.md)
- [기능별 구현·검증 현황](docs/IMPLEMENTATION_STATUS.md)
- [검증 기록 및 APK SHA-256](docs/VERIFICATION.md)

**실제 APK 실행 녹화**는 제출 APK를 일반 Android 14 AVD에 설치해 휴식 선택·편집·저장·타이머·복원과 학습 구간 이동·이어가기를 확인한 화면입니다. **Pleos 전용 AVD 녹화가 아니며 차량 동작은 모의입니다.** 별도의 프로토타입 기능 흐름 영상과 숏폼은 테스트 DOM을 렌더링해 편집한 목업 영상입니다. Pleos Vehicle Service 응답과 실제 차량 쓰기는 아직 검증하지 않았습니다.

## 구현 범위

- 기본 마이씬 제공, 생성·편집·복제·삭제, 로컬 저장과 재실행
- 온도·무드등 밝기/색상·시트 메모리·이용 시간·오디오 취향 설정
- 모의 차량 적용·이전 값 복원, 앱 활성 중 휴식 타이머·환경음·종료 알림
- 시간대 맞춤: 19~07시 저장 밝기의 50% 적용. 날씨·외부 조도·온도 센서를 이용하는 기능은 아닙니다.
- 직접 등록한 텍스트 학습자료, 구간별 듣기, 구간 위치 저장·이어듣기, 목록 읽어주기
- Android SpeechRecognizer로 번호·이름 선택 코드. 마이크 권한과 기기의 인식 서비스가 필요합니다.
- 원문 발췌와 AI 생성 설명을 구분. Gemini 연결용 Python 서버와 HTTPS 클라이언트 구현. API 키·모델·서버 배포 설정은 사용자가 준비해야 하며 실제 모델 응답은 미검증입니다.
- 유튜브 HTTPS 원본 주소 등록·외부 앱 열기 코드. 재생목록 동기화·영상 다운로드·주행 중 영상 재생은 구현하지 않습니다.
- 별도 **실제 SDK 패널**: HVAC·무드등·좌석 메모리 capability 조회, 공조·밝기 설정 요청과 readback, 좌석 메모리 호출 요청

일반 마이씬 화면은 **모의 차량**으로 동작합니다. 실제 SDK 패널과 혼합해 실제 적용 성공으로 표시하지 않습니다. 실제 쓰기는 Android Automotive API에서 P·속도 0 근처·주차브레이크를 확인하고 지원 범위와 권한을 충족할 때만 요청합니다. 실제 주차 상태를 확인하지 못하면 쓰기·AI 화면 요청·외부 영상 열기를 잠급니다. 좌석 메모리 API의 오류 콜백만으로 이동 완료를 판정하지 않습니다.

## 실행 방법

### 1. Pleos Connect에서 APK 실행

1. Pleos Playground 공식 안내에 따라 **Pleos Connect 전용 AVD**를 설치합니다.
2. AVD를 켜고 다음 명령을 실행합니다.

```bash
adb install -r "apk/김재윤_마이캐빈_release.apk"
adb shell am start -n kr.mycabin.app/.MainActivity
```

기존 설치본과 서명 키가 다르면 업데이트 설치가 실패합니다. 이 경우 데이터를 먼저 내보낸 뒤 기존 앱을 제거하고 설치하세요.

추천 확인 기준은 공식 샘플의 **Pleos_Connect_v2.1 / Android 14 / Vehicle Service 2.3.22**입니다. **이 저장소에서 해당 AVD를 실제 실행한 기록은 아직 없습니다.** 일반 Android 에뮬레이터에는 Pleos Vehicle Service가 없어 SDK 호출 확인을 대체할 수 없습니다.

설정(⚙) → `지원 기능 조회`에서 실제 SDK 로그를 확인합니다. 일반 화면의 P/D 전환은 테스트 입력이며 실제 차량 상태를 바꾸지 않습니다. SDK 패널의 쓰기 판단에는 이 테스트 입력을 사용하지 않습니다.

### 2. PC에서 화면·모의 동작 확인

```bash
python -m http.server 8080 --directory web
```

브라우저에서 `http://localhost:8080`을 엽니다. 별도 빌드나 서버 키 없이 마이씬과 학습 기능을 확인할 수 있습니다. 음성 합성 지원 여부는 브라우저·OS에 따라 다릅니다. 마이크·AI·차량 SDK는 설치형 앱 경로를 사용합니다.

`http://localhost:8080/?shell=1`은 공식 공개 화면을 배경으로 앱 목록 → My Cabin 진입 동선을 보여주는 **참고 목업**입니다. 실제 런처를 복제한 설치형 앱이 아니며 공식 화면을 앱의 기능으로 제출하지 않습니다. APK는 My Cabin 자체 화면으로 시작하고 시스템 런처는 Pleos가 제공합니다.

### 3. 소스에서 빌드

- JDK 17
- Android SDK Platform 35, Build Tools 35.0.0
- Gradle Wrapper 8.10.2 / Android Gradle Plugin 8.8.2
- Maven 접근: Google, Maven Central, Pleos Playground Nexus

```bash
# ANDROID_HOME 또는 local.properties의 sdk.dir 설정
./gradlew assembleRelease
```

Gradle 결과는 `app/build/outputs/apk/release/`에 생성되며 기본으로 서명하지 않습니다. 별도 release 키로 서명하세요. 제출 APK는 아래 대체 빌드 경로로 생성·서명 검증했습니다.

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
python scripts/build_local.py
```

대체 빌드는 Maven 의존성을 내려받아 javac → D8 → AAPT2 → zipalign → apksigner로 서명합니다. APK 위치는 `apk/김재윤_마이캐빈_release.apk`입니다. 서명 키는 저장소 바깥 `/tmp/mycabin-contest-signing.jks`에 생성합니다. 계속 업데이트하려면 본인 키를 안전하게 보관하고 `MYCABIN_KEYSTORE`, `MYCABIN_KEYSTORE_PASSWORD`를 설정하세요. 기본 생성 키와 비밀번호는 개발 편의용이며 배포 운영용 키 관리 방식이 아닙니다. 빌드 스크립트는 현재 의존성에 필요한 Vehicle binding 권한·query·메타데이터를 명시한 앱 manifest를 사용합니다.

## 테스트 환경과 화면 규격

- 검증 호스트: Linux, JDK 17, Android Build Tools 35.0.0, Node 24, Python 3
- 앱은 WebView 창 크기에 맞춰 반응형으로 배치합니다. 가로·세로 전환 시 가독성은 추가 검증 중입니다.
- 공개 Pleos 화면 참고 목업: **1903 × 1068** 이미지. 참고 이미지 해상도이며 AVD 실제 디스플레이 규격 측정값이 아닙니다.
- 앱 화면과 OS 화면을 구분하며 시스템 버튼을 앱에서 가로채지 않습니다.
- UI 테스트: Happy DOM 이벤트·저장 상태 검증 + 일반 Android 14 Google APIs x86_64 AVD에서 제출 APK 설치·WebView 화면 전환 확인. AVD 표시 해상도 1920×1080, density 160. 음성 장치 출력은 미검증입니다.
- Pleos 전용 AVD 버전·실제 화면 규격·서비스 응답 결과는 실행 후 `docs/VERIFICATION.md`에 추가 기록해야 합니다.

```bash
npm ci
npm test
npm run test:ui
python -m unittest discover -s server -p 'test_*.py'
```

## 선택 기능: AI 서버

```bash
# API 키는 서버 환경변수에만 설정하고 GitHub에 올리지 마세요.
export GEMINI_API_KEY=본인키
export GEMINI_MODEL=계정에서_사용_가능한_모델_ID
python server/ai_server.py
```

서버는 `127.0.0.1:8787`에만 바인딩됩니다. 원격 연결은 HTTPS 역방향 프록시와 접근 제어를 별도로 구성해야 합니다. 인증 없이 공용 인터넷에 공개하지 마세요. 앱 설정에 본인이 관리하는 `https://서버/explain` 주소를 저장한 뒤 학습 화면의 AI 설명을 선택합니다. 앱은 전송 동의를 받은 등록 원문을 서버와 Gemini에 전송합니다. 실제 주차 상태도 확인해야 합니다. 코드·APK에 공급자 API 키를 넣지 않습니다. 기본 오프라인 학습은 이 서버 없이 동작합니다.

## 영상 다시 만들기 / 실제 에뮬레이터 녹화

```bash
# ffmpeg, Noto CJK 폰트가 설치된 호스트
pip install -r scripts/render-requirements.txt
npm run test:ui
python scripts/render_demo.py
```

위 명령은 프로토타입 흐름 영상을 만들며 실기 화면 녹화가 아닙니다. 실제 AVD가 준비되면 `scripts/record_pleos.sh`로 화면 녹화를 시작하고 `docs/DEMO_GUIDE.md`의 순서를 직접 실행해 실제 녹화본으로 교체할 수 있습니다.

## 파일 구성

| 위치 | 내용 |
|---|---|
| `app/src/main/java/kr/mycabin/app/` | Android WebView, 로컬 저장·TTS·STT, Pleos SDK, AI HTTPS 어댑터 |
| `web/` | 앱 화면, 도메인 모델, 모의 차량, 학습 기능 |
| `server/` | 선택형 Gemini 학습 서버·계약 테스트 |
| `tests/` | 도메인·DOM 이벤트 테스트 |
| `scripts/` | 대체 APK 빌드·프로토타입 영상 제작·AVD 녹화 |
| `apk/` | 제출용 서명 APK |
| `media/` | 영상·미리보기 이미지 |
| `docs/` | 시연 안내·구현 범위·검증 기록·의존성 SHA-256 |

## 참고·권리

Pleos 및 현대자동차그룹의 공식 출품작·공식 앱이 아닙니다. 공식 참고 이미지, 폰트, 의존성의 출처는 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)에 기록했습니다. 개발용 예제 문장과 합성 환경음을 사용하며 참가자 연락처·제출서류·서명·비밀 키는 저장소에 포함하지 않습니다.
