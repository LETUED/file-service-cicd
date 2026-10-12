# lab-file-service

파일 업로드 · 목록 · 다운로드 · 삭제 서비스. 내 PC 에서는 디스크, 서버에서는 S3 에 저장함. TODO 3 을 채워 완성함 · 앞 태그의 TODO 1 · 2 는 채워진 상태로 제공됨.

## 구성

| 항목 | 값 |
| --- | --- |
| JDK | 21 |
| 프레임워크 | Spring Boot 4.1.0 |
| 빌드 | Gradle Wrapper 8.14 · Groovy DSL |
| AWS SDK | AWS SDK for Java 2.x · `software.amazon.awssdk:s3` |
| 저장소 | 디스크 `local` · S3 `prod` |
| 포트 | 8080 |
| 패키지 | `com.example.file` |
| 앱 · 이미지 · 컨테이너 이름 | `file-service` |

**데이터베이스 · 캐시 · 로그인 없음.** 파일 목록은 메모리에만 둠.

## 문서

| 파일 | 담는 것 |
| --- | --- |
| `README.md` | 저장소 구성 · 실행 · 태그 |
| `TODO.md` | 채울 자리 · 주석 읽는 법 · 확인 기준 |
| `docs/api.md` | 주소 · 요청 · 응답 · 실패 코드 |

## 태그

주제 순서대로 코드가 자람. 태그를 옮기면 그 주제 시점의 코드가 됨.

| 주제 | 태그 | 추가되는 것 |
| --- | --- | --- |
| 파일 서비스와 디스크 저장소 | `file-storage-local` | 디스크 저장소 · 업로드 · 목록 · 다운로드 · 삭제 · 화면 · 검증 (TODO 1 · 2) · `FileValidatorTest` |
| S3 저장소와 역할 | `file-storage-s3` | S3 저장소 `prod` · IAM 역할로 접근 · S3 상태 지표 |
| 프리사인 다운로드 | `file-storage-presign-get` | `/api/files/{id}/url` · TODO 3 · `FileServiceTest` |
| 컨테이너 이미지와 배포 설정 | `file-deploy-container` | Dockerfile · compose · `.env.example` · 이미지 `1.0.0` |
| 직접 업로드와 무중단 배포 | `file-storage-presign-put` | `/presign` → PUT → `/complete` · 버킷 CORS · 이미지 `1.1.0` |

```
git clone https://github.com/<github_org>/lab-file-service.git
cd lab-file-service
git checkout file-storage-local
git diff file-storage-local file-storage-s3 --stat
```

**`--stat` 이 다음 주제에서 바뀌는 파일 목록.** 컨트롤러가 목록에 없으면 규약이 지켜진 것.

## 실행

```
./gradlew bootRun
```

```
http://localhost:8080
```

| 확인 | 값 |
| --- | --- |
| 상단 상태 줄 | `storage=local  host=…  files=0` |
| 업로드한 파일 | `./data/uploads/` |
| `GET /api/status` | `{"storage":"local","host":"…","files":0}` |

처음 기동은 의존성 내려받기로 1 ~ 3분 걸림. 업로드 경로는 `application-local.yml` 의 `file.local.path`.

```
./gradlew test
```

## 서버 실행

EC2 역할과 S3 버킷이 준비된 인스턴스에서 jar 로 기동함. 컨테이너 배포는 다음 태그 `file-deploy-container`.

```
./gradlew clean bootJar -x test
export SPRING_PROFILES_ACTIVE=prod APP_S3_BUCKET=<bucket_name> AWS_REGION=ap-northeast-2
java -jar build/libs/file-service-1.0.0.jar
```

| 프로파일 | 사용처 | 저장소 | AWS 자격 증명 |
| --- | --- | --- | --- |
| `local` | 내 PC | 디스크 `file.local.path` | 불필요 |
| `prod` | EC2 · 컨테이너 | S3 `APP_S3_BUCKET` | EC2 역할 · 코드 · 설정 · user-data 에 키 없음 |

**`prod` 에서 `APP_S3_BUCKET` 이 비어 있으면 기동 실패.** 잘못된 버킷으로 조용히 기동되는 것을 막기 위한 동작.

## 무엇을 채우는가

**완성 코드에서 메서드 하나(TODO 3)의 본문만 비어 있음.** TODO 1 · 2 는 채워진 상태. 선언문과 주석은 있으므로 그대로 컴파일되고 기동됨. 본문을 쓰고, 주석 처리된 테스트를 풀어 통과시킴.

| 번호 | 파일 · 메서드 | 하는 일 | 확인 |
| --- | --- | --- | --- |
| 1 | `common/FileValidator.validateExtension` | 허용하지 않는 확장자 거부 | `run.exe` 업로드가 400 · `FileValidatorTest` |
| 2 | `common/FileValidator.validateSize` | 업무 상한 초과 거부 | `FileValidatorTest` |
| 3 | `service/FileService.downloadUrl` | 목록에서 찾아 저장소의 프리사인 URL 을 돌려줌 | `FileServiceTest` · 서버에서 `/url` 200 |

```
./gradlew test
```

주석 읽는 법과 확인 기준은 `TODO.md`. **먼저 직접 쓰고 테스트로 확인한 뒤, 수업에서 같이 작성한 결과와 비교함.**

## 구조

```
  com.example.file
  ├─ FileServiceApplication        기동
  ├─ api
  │  ├─ FileController             표현 계층 · /api/files · /api/status
  │  └─ dto                        FileInfo · StatusResponse · DownloadUrlResponse
  ├─ service
  │  ├─ FileService                검증 → 저장 → 목록 등록
  │  └─ FileRegistry               파일 목록 · 메모리 Map
  ├─ storage
  │  ├─ FileStorage                저장소 규약 · save · load · delete · downloadUrl
  │  ├─ LocalFileStorage           @Profile("local") · 디스크
  │  ├─ S3FileStorage              @Profile("prod") · S3 · 프리사인 GET
  │  └─ StoredFile                 저장된 파일 한 건
  ├─ config
  │  └─ S3Config                   @Profile("prod") · S3Client · S3Presigner · 키 없음 → 역할
  └─ common
     ├─ FileValidator              이름 · 확장자(TODO 1) · 크기(TODO 2) 검증
     ├─ S3HealthIndicator          @Profile("prod") · /actuator/health 의 s3 항목
     ├─ GlobalExceptionHandler     400 · 404 · 413 · 500 을 한 형태로
     ├─ ErrorResponse
     └─ NotFoundException

  src/test/java/com/example/file
  ├─ common/FileValidatorTest        TODO 1 · 2 확인
  ├─ service/FileServiceTest         TODO 3 확인 · 가짜 저장소(FakeStorage)
  └─ FileServiceApplicationTest     컨텍스트 기동
```

## 요청 흐름

```
  브라우저
     │  POST /api/files
     ▼
  FileController
     │
     ▼
  FileService ──▶ FileValidator          확장자 · 크기
     │
     ├──▶ FileStorage.save()             local  ./data/uploads/<id>_<name>
     │                                   prod   s3://<bucket>/uploads/<id>_<name>
     └──▶ FileRegistry.put()             메모리
```

```
  브라우저
     │  GET /api/files/{id}/url
     ▼
  FileController ──▶ FileService.downloadUrl() ──▶ S3FileStorage.downloadUrl()
     │
     ▼  { url: https://<bucket>.s3…?X-Amz-Expires=300 }
  브라우저 ──GET <url>──▶ S3                 앱 서버를 거치지 않음
```

## 알아 둘 것

**컨트롤러는 `FileStorage` 규약만 호출함.** `LocalFileStorage` · `S3FileStorage` 중 어느 구현이 주입되는지는 프로파일이 지정함. 저장소가 바뀌어도 컨트롤러 · 화면 · 검증 코드의 diff 는 0 줄.

**파일 목록은 메모리에만 있음.** 앱을 다시 기동하면 목록이 비고, ALB 뒤에 인스턴스가 둘이면 목록이 서로 다름. 화면 상단 `host=` 로 확인 가능. "상태를 인스턴스에 두면 안 된다" 를 눈으로 보기 위한 의도된 구조.

**S3 접근에 키가 없음.** `S3Config` 는 자격 증명을 지정하지 않음. SDK 의 기본 자격 증명 체인이 환경 변수 → 프로파일 파일 → 인스턴스 메타데이터(EC2 역할) 순으로 탐색함.

**프리사인 URL 은 역할의 권한을 빌려 주는 것.** 서명한 쪽(EC2 역할)의 권한으로 그 객체 하나를 그 시간(기본 300초) 동안 받을 수 있음. 버킷은 계속 비공개.

| | `/content` 앱 경유 | `/url` 프리사인 |
| --- | --- | --- |
| 데이터 경로 | S3 → EC2 → 브라우저 | S3 → 브라우저 |
| EC2 대역폭 · 메모리 | 사용 | 미사용 |
| 접근 제어 | 요청마다 앱이 판단 | 발급 시 한 번 판단 · 이후 S3 가 서명만 확인 |

## 막혔을 때

| 증상 | 확인 |
| --- | --- |
| `./gradlew: Permission denied` | `chmod +x gradlew` |
| 기동 중 `Port 8080 was already in use` | 다른 앱이 8080 사용 · `application-local.yml` 의 `server.port` |
| 업로드 400 | 응답의 `message` · 확장자 · 빈 파일 · 경로 문자 |
| 업로드 413 | 멀티파트 상한 10MB · `spring.servlet.multipart.max-file-size` |
| `prod` 기동 실패 · `APP_S3_BUCKET` | `.env` 의 버킷 이름 |
| `health` 가 `DOWN` · `S3Exception 403` | 역할 정책의 `s3:ListBucket` · 버킷 ARN |
| `NoSuchBucketException` | 버킷 이름 오타 |
| `SdkClientException … connect timed out` | 프라이빗 서브넷에서 S3 로 가는 경로 · 게이트웨이 엔드포인트 · NAT |
| `Unable to load credentials` | 인스턴스에 역할 미연결 |
| `/url` 이 409 | `local` 프로파일 · 디스크 저장소에는 프리사인 없음 |

---

&copy; SUPERB DEVOPS. All rights reserved.
