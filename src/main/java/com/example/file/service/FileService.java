package com.example.file.service;

import com.example.file.common.FileValidator;
import com.example.file.common.NotFoundException;
import com.example.file.storage.FileStorage;
import com.example.file.storage.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
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

    public FileService(FileStorage storage, FileRegistry registry, FileValidator validator) {
        this.storage = storage;
        this.registry = registry;
        this.validator = validator;
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

    public void delete(String id) throws IOException {
        StoredFile file = get(id);
        storage.delete(file.key());
        registry.remove(id);
        log.info("삭제 : {} [{}]", file.originalName(), storage.kind());
    }
}
