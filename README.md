# 여행 기록 앱 (Travel Record App)

>모바일프로그래밍 기말 프로젝트 
> 순천향대학교 컴퓨터소프트웨어공학과  
> 담당교수: 송유정 교수님님
> 학번/이름: 20233658/손서영영

---

## 1. 프로젝트 개요

사용자가 다녀온 여행지를 기록·조회·수정·삭제하고, 사진과 메모를 함께 관리할 수 있는 Android 앱.  
한 학기 동안 학습한 Android 핵심 기술(Fragment, RecyclerView, SQLite, Intent, 메뉴, 비동기 처리 등)을 실제 앱에 적용.

| 항목 | 내용 |
|------|------|
| 앱 이름 | 여행 기록 |
| 패키지명 | `com.example.webprograming` |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| 언어 | Kotlin |
| GitHub | https://github.com/seoyoung06178/WebPrograming.git |

---

## 2. 앱 구조

```
MainActivity (BottomNavigationView)
├── HomeFragment      ← 여행 목록 (RecyclerView)
│   ├── 항목 클릭  → DetailFragment (상세 보기, 백스택)
│   └── FAB (+)     → AddEditActivity (기록 추가)
├── MapFragment       ← OSMDroid 지도 + GPS 마커
└── AddEditActivity   ← 기록 추가/수정 (별도 Activity)
```

- Fragment 3개: `HomeFragment`, `MapFragment`, `DetailFragment`
- Activity 2개: `MainActivity`, `AddEditActivity`
- BottomNavigationView로 홈(목록) ↔ 지도 화면 전환
- 상세 화면 진입 시 `addToBackStack()`으로 백스택 관리 (뒤로가기 시 목록 복귀)

---

## 3. 필수 요구사항 구현 현황

| # | 요구사항 | 구현 여부 | 구현 방법 요약 |
|---|----------|:--------:|----------------|
| 1 | Fragment 2개 이상 + 백스택 | ✅ | Home / Map / Detail 3개 Fragment, Detail 진입 시 `addToBackStack()` |
| 2 | RecyclerView + Adapter/ViewHolder 직접 구현 | ✅ | `TravelAdapter` + `TravelViewHolder` (`adapter/TravelAdapter.kt`) |
| 3 | 목록: 여행지명, 날짜, 썸네일 | ✅ | `item_travel.xml` 레이아웃, 사진 경로 기반 썸네일 표시 |
| 4 | 항목 클릭 → 상세 화면 | ✅ | `HomeFragment.showDetail()` → `DetailFragment` |
| 5 | 기록 추가/수정 → 별도 Activity | ✅ | `AddEditActivity` (FAB / 컨텍스트 메뉴 / 상세 화면 수정 버튼) |
| 6 | SQLiteOpenHelper 직접 구현 + CRUD | ✅ | `TravelDBHelper` — insert / query / update / delete / deleteAll |
| 7 | 앱 종료 후에도 데이터 유지 | ✅ | SQLite 로컬 DB (`travel.db`) 영구 저장 |
| 8 | BottomNavigationView Fragment 전환 | ✅ | `MainActivity` + `bottom_nav_menu.xml` |
| 9 | 카메라/갤러리 Intent | ✅ | `AddEditActivity` — `ACTION_IMAGE_CAPTURE`, `ACTION_OPEN_DOCUMENT` |
| 10 | 상세 화면에서 사진 표시 | ✅ | `DetailFragment` — DB 경로로 Bitmap 로드 |
| 11 | 옵션 메뉴 2개 이상 | ✅ | 날짜순 정렬, 이름순 정렬, 전체 삭제 (3개) |
| 12 | 컨텍스트 메뉴 | ✅ | 목록 항목 롱클릭 → 수정 / 삭제 |
| 13 | 삭제 시 AlertDialog 확인 | ✅ | 개별 삭제, 전체 삭제, 상세 화면 삭제 모두 확인 다이얼로그 |
| 14 | Room/Firebase/Retrofit 미사용 | ✅ | `SQLiteOpenHelper`만 사용 |
| 15 | 비정상 종료 방지 (기본 예외 처리) | ✅ | DB/이미지/GPS 처리에 try-catch, null 검사 |

---

## 4. 가산점(추가 구현) 현황

| 가산점 항목 | 배점 | 구현 여부 | 구현 방법 |
|-------------|:----:|:--------:|-----------|
| 지도 API 활용 (지도 표시) | +2 | ✅ | OSMDroid 라이브러리 — API Key 불필요, `MapFragment` |
| 사진 GPS 추출 + 지도 마커 | +4 | ✅ | ExifInterface로 EXIF GPS 파싱 → DB 저장 → `MapFragment`에 Marker 표시 |
| 코루틴 비동기 처리 + ProgressBar | +2 | ✅ | `lifecycleScope` + `Dispatchers.IO`, 목록/상세/저장/지도/썸네일 로딩 시 ProgressBar |

> OSMDroid를 선택한 이유: Google Maps API Key를 Git에 올릴 필요가 없고, 별도 클라우드 설정 없이 오프라인·온라인 타일 지도를 사용할 수 있다.

---

## 5. 주요 기능 설명

### 5.1 여행 기록 목록 (HomeFragment)

- `RecyclerView` + `LinearLayoutManager`로 목록 표시
- 각 항목: 여행지명, 방문 날짜, 대표 사진 썸네일- FAB(+) 버튼 → 새 기록 추가 (`AddEditActivity`)
- 짧게 클릭 → 상세 화면 (`DetailFragment`)
- 길게 누르기 → 컨텍스트 메뉴 (수정 / 삭제)
- 기록 없을 때 빈 상태 안내 UI 표시

### 5.2 기록 추가/수정 (AddEditActivity)

- 입력 항목: 여행지명, 방문 날짜(DatePicker), 메모, 사진
- 사진 추가: AlertDialog로 카메라 촬영 / 갤러리 선택
- 카메라: `FileProvider` + `MediaStore.ACTION_IMAGE_CAPTURE`
- 갤러리: `Intent.ACTION_OPEN_DOCUMENT` (URI → 앱 내부 파일 복사)
- 저장 시 입력 유효성 검사 (제목·날짜 필수)
- 수정 모드: Intent `record_id`로 기존 데이터 로드

### 5.3 상세 보기 (DetailFragment)

- 여행지명, 날짜, 메모, 전체 사진, GPS 좌표(있을 경우) 표시
- 수정 / 삭제 / 목록으로 돌아가기 버튼
- 삭제 시 AlertDialog 확인 후 DB 삭제 및 목록 복귀

### 5.4 SQLite 데이터베이스 (TravelDBHelper)

테이블: `travel_records`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| `_id` | INTEGER PK AUTOINCREMENT | 기록 ID |
| `title` | TEXT NOT NULL | 여행지명 |
| `visit_date` | TEXT NOT NULL | 방문 날짜 (yyyy-MM-dd) |
| `memo` | TEXT | 메모 |
| `photo_path` | TEXT | 사진 파일 경로 |
| `latitude` | REAL | GPS 위도 |
| `longitude` | REAL | GPS 경도 |
| `created_at` | TEXT | 생성 시각 |

제공 메서드
- `insertRecord()` — Create
- `getAllRecords()` / `getRecordById()` — Read
- `updateRecord()` — Update
- `deleteRecord()` / `deleteAllRecords()` — Delete

### 5.5 옵션 메뉴 (3개)

| 메뉴 | 동작 |
|------|------|
| 날짜순 정렬 | 최신 방문일 기준 내림차순 (SharedPreferences에 설정 저장) |
| 이름순 정렬 | 여행지명 가나다/알파벳 순 |
| 전체 삭제 | AlertDialog 확인 후 모든 기록 삭제 |

### 5.6 컨텍스트 메뉴

- 목록 항목 롱클릭 → `수정` / `삭제`
- 삭제 시 AlertDialog로 재확인

### 5.7 지도 + GPS 마커 (MapFragment)

1. `AddEditActivity`에서 사진 선택/촬영 후 `GpsUtil.extractGpsFromPhoto()`로 EXIF GPS 추출
2. 위·경도를 DB에 함께 저장
3. `MapFragment`에서 GPS 정보가 있는 기록만 조회하여 OSMDroid Marker 생성
4. 마커 탭 시 여행지명·날짜 표시 (title / snippet)

### 5.8 코루틴 비동기 처리

| 위치 | 비동기 작업 | UI 피드백 |
|------|-------------|-----------|
| HomeFragment | DB 목록 조회, 삭제 | ProgressBar |
| DetailFragment | DB 조회, 이미지 디코딩 | ProgressBar |
| AddEditActivity | DB 저장/조회, 갤러리 복사, GPS 추출 | ProgressBar |
| MapFragment | DB 조회, 마커 생성 | ProgressBar |
| TravelAdapter | 썸네일 이미지 로딩 | 항목별 ProgressBar |

- `viewLifecycleOwner.lifecycleScope` / `lifecycleScope` 사용 (Fragment/Activity 생명주기 연동)
- DB·파일 I/O는 `Dispatchers.IO`, UI 갱신은 Main 스레드

---

## 6. 프로젝트 파일 구조

```
app/src/main/java/com/example/webprograming/
├── MainActivity.kt                 # BottomNav + Fragment 호스트, 백스택 처리
├── AddEditActivity.kt              # 기록 추가/수정, 카메라/갤러리, GPS 추출
├── fragment/
│   ├── HomeFragment.kt             # RecyclerView 목록, 메뉴, 컨텍스트 메뉴
│   ├── DetailFragment.kt           # 상세 보기, 수정/삭제
│   └── MapFragment.kt              # OSMDroid 지도 + 마커
├── adapter/
│   └── TravelAdapter.kt            # RecyclerView Adapter + ViewHolder
├── db/
│   └── TravelDBHelper.kt           # SQLiteOpenHelper CRUD
├── model/
│   └── TravelRecord.kt             # 데이터 클래스
└── util/
    └── GpsUtil.kt                  # ExifInterface GPS 추출
```

---

## 7. 사용 라이브러리

| 라이브러리 | 용도 |
|-----------|------|
| AndroidX AppCompat / Material | UI 컴포넌트, Toolbar, FAB, BottomNavigation |
| Kotlin Coroutines | 비동기 DB/이미지 처리 |
| OSMDroid 6.x | 오픈소스 지도 (API Key 불필요) |
| AndroidX ExifInterface | 사진 EXIF GPS 정보 추출 |

> 미사용 : Room, Firebase, Retrofit

---

## 8. 빌드 및 실행 방법

### 사전 요구사항

- Android Studio (최신 권장)
- JDK 11
- Android SDK 26 이상
- 실기기 또는 에뮬레이터 (API 26+)


### APK 빌드

```
Android Studio → Build → Build Bundle(s) / APK(s) → Build APK(s)
```

생성 경로: `app/build/outputs/apk/debug/app-debug.apk`

---

## 9. 기능별 테스트

| 기능 | 테스트 방법 |
|------|-------------|
| 기록 추가 | 홈 FAB(+) → 제목·날짜 입력 → 저장 → 목록에 표시 |
| 기록 수정 | 목록 롱클릭 → 수정, 또는 상세 → 수정 버튼 |
| 기록 삭제 | 롱클릭 → 삭제 (AlertDialog 확인) |
| 전체 삭제 | ⋮ 옵션 메뉴 → 전체 삭제 |
| 정렬 | ⋮ → 날짜순 / 이름순 정렬 |
| 상세 화면 | 목록 항목 클릭 → 사진·메모 확인 |
| 카메라 | 추가/수정 → 사진 선택 → 카메라 촬영 |
| 갤러리 | 추가/수정 → 사진 선택 → 갤러리에서 선택 |
| 데이터 유지 | 기록 저장 → 앱 완전 종료 → 재실행 → 데이터 유지 확인 |
| Fragment 전환 | 하단 탭 홈 ↔ 지도 |
| 백스택 | 상세 진입 → 뒤로가기 → 목록 복귀 |
| 지도 | GPS 포함 사진으로 기록 추가 → 지도 탭 → 마커 확인 |
| GPS 추출 | 위치 정보 포함 사진 선택 → Toast로 좌표 표시 |
| 비동기/ProgressBar | 목록·상세·저장 시 로딩 표시 확인 |

---

## 10. 권한 (AndroidManifest)

| 권한 | 용도 |
|------|------|
| `CAMERA` | 카메라 촬영 |
| `READ_MEDIA_IMAGES` | 갤러리 이미지 접근 (Android 13+) |
| `READ_EXTERNAL_STORAGE` | 구버전 갤러리 접근 (maxSdk 32) |
| `INTERNET` / `ACCESS_NETWORK_STATE` | OSMDroid 지도 타일 다운로드 |

---

## 11. 개발 일지 (Git 커밋 상세)

개발은 약 6일간 진행되었으며, 기능 구현 → 비동기 처리 → 예외 처리 → UI 개선 순으로 단계적으로 커밋했습니다.

---

### Day 1 — 2026-06-03 | 프로젝트 초기 설정 및 핵심 기능 구현

#### `1500727` — 커밋 테스트
- Android Studio 기본 프로젝트 생성
- Gradle, `.gitignore`, AndroidManifest, MainActivity 기본 틀 구성
- Git 저장소 연동 및 첫 커밋

#### `86f2ee2` — 프로젝트 구조 설정 (1차)
- `build.gradle.kts`, `libs.versions.toml` 의존성 정리
  - OSMDroid, Kotlin Coroutines, ExifInterface 추가
- `MainActivity` — BottomNavigationView + Fragment 컨테이너 구조 구현
- `activity_main.xml` — Toolbar + Fragment 영역 + 하단 네비게이션 레이아웃
- Material 테마·색상 기본 설정 (`themes.xml`, `colors.xml`)
- AndroidManifest 권한 선언 (카메라, 인터넷, 저장소)

#### `86f4d72` — 프로젝트 구조 설정 (2차) — 핵심 기능 일괄 구현
| 분류 | 추가/구현 파일 | 내용 |
|------|----------------|------|
| 데이터 | `TravelRecord.kt`, `TravelDBHelper.kt` | SQLiteOpenHelper CRUD, `travel_records` 테이블 생성 |
| 화면 | `HomeFragment`, `MapFragment`, `DetailFragment` | Fragment 3개, 백스택 연동 |
| Activity | `AddEditActivity.kt` | 기록 추가/수정 별도 Activity |
| 목록 | `TravelAdapter.kt`, `item_travel.xml` | RecyclerView Adapter/ViewHolder 직접 구현 |
| 지도 | `MapFragment.kt`, `GpsUtil.kt` | OSMDroid 지도 + ExifInterface GPS 추출 |
| 카메라 | `AddEditActivity` + `file_paths.xml` | 카메라/갤러리 Intent, FileProvider 설정 |
| 메뉴 | `options_menu.xml`, `context_menu.xml` | 옵션 메뉴 3개, 컨텍스트 메뉴(수정/삭제) |
| UI | 각 Fragment/Activity 레이아웃 XML | 홈·상세·지도·추가/수정 화면 레이아웃 |


---

### Day 2 — 2026-06-04 | 코루틴 비동기 처리 (가산점)

#### `e3ee8a4` — 코루틴 비동기 처리 · ProgressBar · 이미지 최적화
| 파일 | 변경 내용 |
|------|-----------|
| `HomeFragment.kt` | `lifecycleScope` + `Dispatchers.IO`로 DB 목록 조회/삭제 비동기화, ProgressBar 표시 |
| `DetailFragment.kt` | DB 조회·이미지 디코딩 코루틴 처리, 상세 레이아웃 개선 |
| `AddEditActivity.kt` | 저장/수정/갤러리 복사/GPS 추출 비동기화, `inSampleSize` 이미지 다운샘플링 |
| `MapFragment.kt` | DB 조회 및 마커 생성 비동기 처리 |
| `TravelAdapter.kt` | 썸네일 로딩 코루틴 + 항목별 ProgressBar, `onViewRecycled`에서 Job 취소 |
| 레이아웃 XML | 각 화면에 ProgressBar UI 추가 |


---

### Day 3 — 2026-06-06 | 예외 처리 및 UI 1차 개선

#### `7155828` — 예외 처리 강화 · UI/UX 개선
| 파일 | 변경 내용 |
|------|-----------|
| `TravelDBHelper.kt` | 모든 CRUD 메서드에 try-catch 적용, 실패 시 -1/0/null 반환으로 앱 크래시 방지 |
| `MapFragment.kt` | 마커 생성 try-catch, GPS 없는 기록 빈 상태 안내 텍스트 추가 |
| `DetailFragment.kt` | 기록 없을 때 Toast + popBackStack 처리 |
| `HomeFragment.kt` | 날짜 파싱 예외 처리 (정렬 시) |
| 레이아웃 XML | 상세·홈·지도·추가 화면 레이아웃 구조 개선, 빈 상태 UI 추가 |
| `colors.xml`, `themes.xml` | 색상 팔레트 및 Material 테마 보강 |


---

### Day 4 — 2026-06-07 | UI/UX 2차 개선

#### `d55e453` — UI 변경 및 UX 개선
| 파일 | 변경 내용 |
|------|-----------|
| `activity_main.xml` | Toolbar·BottomNav 레이아웃 정리 |
| `activity_add_edit.xml` | 추가/수정 화면 Material Card 스타일 적용, 입력 폼 간격·가독성 개선 |
| `fragment_detail.xml` | 상세 화면 정보 카드 레이아웃 재구성 |
| `fragment_home.xml` | FAB 위치·빈 목록 안내 UI 조정 |
| `item_travel.xml` | 목록 항목 썸네일·텍스트 배치 개선 |
| `colors.xml`, `themes.xml` | 라이트/다크 테마 색상 통일 |
| `MainActivity.kt`, `AddEditActivity.kt` | Toolbar 타이틀·네비게이션 동작 미세 조정 |

---

### Day 5 — 2026-06-08 | 상세 화면 UX 보완

#### `5a44de7` — 상세 화면 "목록으로 돌아가기" 버튼 추가
| 파일 | 변경 내용 |
|------|-----------|
| `fragment_detail.xml` | 수정/삭제 버튼 아래 "목록으로 돌아가기" 버튼 추가 |
| `DetailFragment.kt` | 버튼 클릭 시 `popBackStack()`으로 HomeFragment 목록 복귀 |
| `AddEditActivity.kt` | 갤러리 권한 처리 보완 |
| `AndroidManifest.xml` | 권한 선언 정리 |

---

### 개발 단계 요약

| 단계 | 기간 | 주요 성과 |
|------|------|-----------|
| 1단계 — 기반 구축 | 06-03 | 프로젝트 생성, DB·Fragment·Activity·메뉴·Intent·지도 기본 구현 |
| 2단계 — 비동기 | 06-04 | 코루틴 + ProgressBar 전 화면 적용 (가산점) |
| 3단계 — 안정화 | 06-06 | try-catch 예외 처리, null 안전, 빈 상태 UI |
| 4단계 — UI polish | 06-07 | Material 테마, 레이아웃·색상·UX 개선 |
| 5단계 — 마무리 | 06-08 | 상세 화면 네비게이션 보완, 권한 정리 |

### 커밋 이력 

| 날짜 | 커밋 해시 | 커밋 메시지 |
|------|-----------|-------------|
| 2026-06-03 | `1500727` | 커밋 테스트 |
| 2026-06-03 | `86f2ee2` | 프로젝트 구조 설정 — 의존성, MainActivity, BottomNav |
| 2026-06-03 | `86f4d72` | 프로젝트 구조 설정 — TravelRecord, TravelDBHelper, Fragment/Activity 전체 |
| 2026-06-04 | `e3ee8a4` | 코루틴 비동기 처리, ProgressBar, 상세화면·이미지 최적화 |
| 2026-06-06 | `7155828` | 예외 처리 강화, UI/UX 개선 |
| 2026-06-07 | `d55e453` | UI 변경 및 UX 개선 |
| 2026-06-08 | `5a44de7` | 상세 화면 "목록으로 돌아가기" 버튼 추가 |

---

## 12. 제출물

-  debug APK (`app-debug.apk`)
-  GitHub Repository URL: https://github.com/seoyoung06178/WebPrograming.git

---

## 13. 참고

- 본 프로젝트는 SQLiteOpenHelper 직접 구현을 원칙으로 하며, 강의 범위 외 ORM/네트워크 라이브러리는 사용하지 않았습니다.
- 지도는 OSMDroid를 사용하여 API Key를 Git에 포함하지 않았습니다.
