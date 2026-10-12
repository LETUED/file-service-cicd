package com.example.file.service;

import com.example.file.common.FileValidator;
import com.example.file.common.NotFoundException;
import com.example.file.storage.FileStorage;
import com.example.file.storage.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 파일 업무 흐름. 검증 → 저장 → 목록 등록.
 *
 * 저장소가 디스크인지 S3 인지는 FileStorage 뒤에 숨어 있음.
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileStorage storage;
    private final FileRegistry registry;
    private final FileValidator validator;
    private final Duration urlTtl;

    public FileService(FileStorage storage, FileRegistry registry, FileValidator validator,
                       @Value("${app.presign.ttl-seconds:300}") long urlTtlSeconds) {
        this.storage = storage;
        this.registry = registry;
        this.validator = validator;
        this.urlTtl = Duration.ofSeconds(urlTtlSeconds);
    }

    public String storageKind() {
        return storage.kind();
    }

    public StoredFile upload(MultipartFile file) throws IOException {
        validator.validate(file);

        String id = UUID.randomUUID().toString();
        // 저장 이름 = id_원본명. 같은 이름을 두 번 올려도 덮어쓰지 않음
        String key = storage.save(file, storage.keyFor(id + "_" + file.getOriginalFilename()));

        StoredFile stored = new StoredFile(
                id, file.getOriginalFilename(), key,
                file.getSize(), file.getContentType(), Instant.now());
        registry.put(stored);
        log.info("업로드 : {} → {} [{}]", stored.originalName(), key, storage.kind());
        return stored;
    }

    public List<StoredFile> list() {
        return registry.findAll();
    }

    public StoredFile get(String id) {
        return registry.find(id).orElseThrow(() -> new NotFoundException("파일 없음: " + id));
    }

    /** 앱을 거쳐 내려받음. 디스크 · S3 모두 가능 */
    public Resource download(String id) throws IOException {
        return storage.load(get(id).key());
    }

    /** 저장소에서 직접 내려받는 임시 URL. 디스크 저장소는 비어 있음 */
    public Optional<String> downloadUrl(String id) {
        // TODO 3 · 직접 다운로드 URL
        //
        // 기능        id 로 목록에서 파일을 찾고, 저장소에 프리사인 URL 을 요청해 그대로 돌려줌
        //             없는 id 는 NotFoundException (404) · 디스크 저장소는 Optional.empty() (컨트롤러가 409)
        // 활용        get(id)                                        → StoredFile · 없으면 NotFoundException (위에 있음)
        //             storage.downloadUrl(key, originalName, ttl)    → Optional<String> · 규약 FileStorage
        //             file.key() · file.originalName() · urlTtl      → 인자 세 개
        // 동작결과    FileServiceTest 의 "직접_다운로드_URL_은_저장소가_만든_값을_그대로_돌려줌" 주석을 풀어 통과
        //             서버(S3)에서는 GET /api/files/{id}/url 이 200 {url, expiresIn}
        return Optional.empty();
    }

    public Duration urlTtl() {
        return urlTtl;
    }

    public void delete(String id) throws IOException {
        StoredFile file = get(id);
        storage.delete(file.key());
        registry.remove(id);
        log.info("삭제 : {} [{}]", file.originalName(), storage.kind());
    }
}
