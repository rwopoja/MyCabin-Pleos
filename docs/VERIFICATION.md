# 검증 기록

검증일: 2026-11-13

## 완료한 확인

- JDK 17 + Android Platform 35 / Build Tools 35.0.0을 이용한 대체 빌드: javac, D8, AAPT2, zipalign, apksigner 완료
- 실제 Pleos Vehicle SDK 2.3.2 및 명시한 의존성 포함
- release APK v3 서명 검증 통과. 환경 갱신 후 Google apksig 8.8.2 `ApkVerifier`로도 `verified=true v3=true` 확인
- ZIP CRC 확인, APK에 포함된 `assets/app.js`와 현재 `web/app.js` 일치 확인, 폰트 파일 일치 확인
- Node 도메인 테스트 **9개 통과**: 마이씬 CRUD·복원, 모의 적용/복원, 주행/수면 조건, 학습자료·진도, 시간대 맞춤, 음성 선택 파서, 유튜브 URL 검증
- Happy DOM UI smoke: 편집·저장·자료 재시작 복원·모의 적용/복원·학습 구간 이어가기·음성 이벤트 선택·새 학습·주행 제한 동작 확인
- AI 서버 Python unittest **3개 통과**: 입력 제한, 키/모델 미설정 오류, 가짜 전송 계층을 사용한 실제 API 형식의 요청/응답 계약
- 영상 파일 확인: H.264 / 1920×1080 / 89초, H.264 / 1080×1920 / 45초
- 렌더된 홈·편집·휴식·학습 화면 육안 확인
- GitHub Actions Gradle `assembleRelease`와 Node/Python/DOM 테스트 통과: run `43444674390`
- **실제 제출 APK**를 일반 Android 14 / Google APIs / x86_64 AVD에 설치·실행, 접근성 노드로 휴식 선택→편집→저장→모의 시작→종료/복원, 학습 시작→다음→이어가기 통과: run `43444674319`
- AVD 화면 1920×1080 / density 160, 원본 83.8초 녹화 확보. 실제 WebView 홈·편집·휴식·이어학습 화면 육안 확인. 차량 설정은 모의이며 Pleos Service는 없음

## 제출 APK

경로: `apk/김재윤_마이캐빈_release.apk`

파일 크기: **5,087,015 bytes**

SHA-256:

```text
8aeec3ae33fb35bbe3624a2aa91ed1e49bcb30fa819ff0e110a72e6b2c90ee89
```

## 아직 검증하지 않은 사항

오디오 장치 출력, Pleos 전용 AVD 실행, 실제 Vehicle Service 응답, 차량 상태 읽기 권한, SDK 쓰기 결과, 실제 마이크·음성 인식 서비스, Gemini 실응답, 외부 유튜브 앱 재생은 미검증입니다. 저장소의 소스·APK·영상이 이 검증을 대신하지 않습니다.

실제 실행한 일반 AVD: Android API 34 Google APIs x86_64, Pixel 2 프로필, 표시 해상도 1920×1080. Pleos 전용 AVD를 대체하는 차량 서비스 검증은 아닙니다.

권장 Pleos AVD 기준: Pleos 공식 샘플의 `Pleos_Connect_v2.1`, Android 14, Vehicle Service 2.3.22. **사용한 AVD 버전·실측 화면 규격: 없음(실행 전)**. 공식 화면 참고 이미지의 픽셀 수를 AVD 사용 기록으로 적지 않습니다.

## GitHub Actions

Source checks의 Node/Python 테스트와 Gradle release 빌드가 통과했습니다. Android APK runtime smoke도 통과했습니다. 실행 결과와 구체적인 run ID는 위 기록과 저장소 Actions 탭에서 확인하세요. CI 산출물은 unsigned APK이며 위 제출 APK는 별도 서명된 파일입니다.
