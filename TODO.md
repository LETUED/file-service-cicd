# TODO 안내

이 태그에서는 비어 있는 메서드가 없음. 앞 태그의 TODO 1 · 2 는 채워진 상태 — 본인 코드와 `git diff file-storage-local file-storage-s3 -- src/main/java/com/example/file/common/FileValidator.java` 로 비교함. 아래는 참고용.

---

## 01. 주석 읽는 법

    void validateSize(MultipartFile file) {
        // TODO 2 · 크기 상한 검증
        //
        // 기능        file.getSize() 가 maxBytes 보다 크면 IllegalArgumentException
        // 활용        file.getSize() → long · maxBytes → 생성자에서 주입됨
        // 동작결과    상한 초과 업로드가 400 BAD_REQUEST · FileValidatorTest 통과
    }

| 줄 | 뜻 | 이렇게 씀 |
| --- | --- | --- |
| 기능 | 이 자리에서 만들 것 | 적힌 조건대로 판단함 |
| 활용 | 이미 있어서 가져다 쓸 것 · 형식은 `이름 → 반환 타입 · 설명` | 새로 만들지 않고 찾아 씀 |
| 동작결과 | 만들고 나면 이렇게 됨 | curl · 브라우저 · 테스트로 직접 확인 |

**다 만들면 주석을 지움.** IDE 아래쪽 도구 창의 `TODO` 탭에서 남은 개수를 확인 가능함.

---

## 02. 목록

| 번호 | 파일 · 메서드 | 하는 일 | 활용 | 동작결과 |
| --- | --- | --- | --- | --- |
| 1 | `common/FileValidator.validateExtension(String name)` | 허용하지 않는 확장자 거부 | `extensionOf(name)` · `ALLOWED_EXTENSIONS.contains(ext)` | `run.exe` 가 400 · `note.txt` 는 201 · `FileValidatorTest` 의 주석 처리된 테스트를 풀어 통과 |
| 2 | `common/FileValidator.validateSize(MultipartFile file)` | 업무 상한 초과 거부 | `file.getSize()` · `maxBytes` | 상한 초과가 400 · `FileValidatorTest` 의 주석 처리된 테스트를 풀어 통과 |

### 1 · 확장자 검증

**src/main/java/com/example/file/common/FileValidator.java**

```java
void validateExtension(String name) {
    // TODO 1 · 확장자 검증
    // 기능        name 의 확장자가 ALLOWED_EXTENSIONS 에 없으면 IllegalArgumentException
    // 활용        extensionOf(name) → 소문자 확장자 · ALLOWED_EXTENSIONS.contains(ext)
    // 동작결과    run.exe 업로드가 400 · FileValidatorTest 통과
}
```

```
./gradlew test
curl -s -o /dev/null -w "%{http_code}\n" -F "file=@run.exe" http://localhost:8080/api/files
```

| 입력 | 작성 전 | 작성 후 |
| --- | --- | --- |
| `note.txt` | 201 | 201 |
| `run.exe` | 201 (거르지 못함) | 400 `허용하지 않는 확장자: exe` |

**`FileValidatorTest` 의 `허용하지_않는_확장자는_거부함` 주석을 풀어야 검증됨.**

### 2 · 크기 상한 검증

같은 파일의 `validateSize`. 멀티파트 자체 상한(`spring.servlet.multipart.max-file-size` · 10MB)은 413 으로 먼저 걸리므로, 이 검증은 그보다 작은 **업무 상한** 을 둘 때 동작함.

```
./gradlew test
```

**`FileValidatorTest` 의 `상한을_넘는_파일은_거부함` 주석을 풀어야 검증됨.** 테스트는 `new FileValidator(4)` 로 상한을 4바이트로 두고 5바이트 파일을 넣음.

---

## 03. 응답 형태

### FileInfo

    {
      "id": "b7f1…",
      "originalName": "hello.txt",
      "size": 42,
      "contentType": "text/plain",
      "uploadedAt": "2026-10-12T01:02:03Z"
    }

### ErrorResponse

실패는 모두 이 형태로 옴.

    {
      "status": 400,
      "code": "BAD_REQUEST",
      "message": "허용하지 않는 확장자: exe",
      "timestamp": "2026-10-12T01:02:03Z"
    }

| 코드 | 응답 | 사유 |
| --- | --- | --- |
| `BAD_REQUEST` | 400 | 빈 파일 · 경로 문자 · 허용하지 않는 확장자 (TODO 1) · 업무 상한 초과 (TODO 2) |
| `NOT_FOUND` | 404 | 목록에 없는 id |
| `PAYLOAD_TOO_LARGE` | 413 | 멀티파트 상한 초과 · 컨트롤러에 닿기 전에 거부됨 |
| `STORAGE_ERROR` | 500 | 디스크 · S3 입출력 실패 |

---

## 04. 막혔을 때

**먼저 20분은 스스로 찾아봄.**

| 증상 | 확인 |
| --- | --- |
| `run.exe` 가 여전히 201 | `validateExtension` 본문이 비어 있음 · `contains` 앞의 `!` |
| 테스트가 빨강 | 주석을 푼 테스트가 `IllegalArgumentException` 을 기대함 · 다른 예외 타입이면 실패 |
| 테스트가 안 보임 | 테스트 메서드의 `// @Test` 주석도 풀었는지 |
| `validateSize` 를 썼는데 10MB 넘는 파일이 413 | 정상 · 멀티파트 상한이 먼저 · 테스트는 4바이트 상한으로 확인함 |

---

&copy; SUPERB DEVOPS. All rights reserved.
