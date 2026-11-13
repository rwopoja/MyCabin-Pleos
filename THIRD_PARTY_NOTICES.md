# Third-party notices

## Pleos Connect SDK and public reference screenshots

- Vehicle SDK: `ai.pleos.playground:Vehicle:2.3.2` with transitive AAR/JAR dependencies. Vendor SDK rights remain with the vendor; its distribution/use terms apply. SDK AAR source archives are not redistributed in this repository. APK bundles runtime code as an integration artifact.
- Official guide: https://document.pleos.ai/en/docs/pleos-only/tutorial/vehicle-sdk-sample-app
- Official sample: https://github.com/Pleos-Playground/pleos-sample-vehicle-app
- Public screenshot sources: Pleos Playground official documentation, emulator Quick Controls and application library illustrations. Files: `web/assets/pleos-reference.png` and `pleos-library.png`.
- The two screenshots are reference material used to demonstrate an entry-path mockup, not our artwork or an exact, operating system implementation. Rights belong to their respective owners. The Android app opens its own UI, not an operating system clone. No Hyundai trademark license or endorsement is claimed.
- Maven source URLs and SHA-256 digests for the manual build are recorded in `docs/dependency-sha256.json`. AndroidX, Kotlin, Kotlin coroutines and Gson retain their original licenses (principally Apache-2.0), with dependency-specific terms controlling. Gradle Wrapper is from the official Gradle project, Apache-2.0: https://github.com/gradle/gradle .

## Font

`web/assets/cabin-ko.ttf` is a static, subsetted Noto Sans KR font derived from Google Fonts NotoSansKR[wght].ttf (weight 450). It includes Hangul syllables and UI symbols. Source: https://github.com/google/fonts/tree/main/ofl/notosanskr . SIL Open Font License 1.1 is included in `licenses/NotoSansKR-OFL.txt`. The same font family may be used by the system to render prototype video captions.

## Audio

`lesson-1.wav` through `lesson-4.wav` are synthesized English examples produced with FFmpeg/libflite and the SLT voice. Example sentences were written for this project. Flite: https://github.com/festvox/flite (CMU permissive license and voice-specific terms); voice model is not redistributed. `forest.wav` is algorithmically generated filtered noise/tone, labeled as quiet ambience in the UI; it is not a location/forest field recording.

## Optional AI and voice services

The optional backend uses Google's documented Gemini generateContent REST interface. It requires the participant's own account/key/model and service terms. No model or Google credential is included. Android TTS/STT uses services available on the device; service-provider privacy and network behavior may vary. Offline preference for STT is a request, not an offline guarantee.

## Source rights

Project source is provided for contest review. No additional open-source license is granted for original project code at this stage. Third-party components keep their respective licenses. Contact details and signed application documents are intentionally excluded from this repository.
