package com.example.file.service;

import com.example.file.storage.StoredFile;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 파일 목록(메타데이터). 메모리에만 둠.
 *
 * 앱을 재시작하면 목록이 비고, 인스턴스가 둘이면 서로 목록이 다름.
 * 이것이 "상태를 인스턴스에 두면 안 된다"의 가장 작은 예임.
 * 파일 본체는 S3 에 남아 있으므로, 목록만 DB(RDS)로 옮기면 해결됨 → 다음 과정의 주제.
 */
@Component
public class FileRegistry {

    private final Map<String, StoredFile> files = new ConcurrentHashMap<>();

    /** 직접 업로드 대기 목록. PUT 이 끝나고 complete 가 오기 전까지만 있음 */
    private final Map<String, StoredFile> pending = new ConcurrentHashMap<>();

    public void put(StoredFile file) {
        files.put(file.id(), file);
    }

    public Optional<StoredFile> find(String id) {
        return Optional.ofNullable(files.get(id));
    }

    /** 최근 업로드가 위로 오게 정렬함 */
    public List<StoredFile> findAll() {
        return files.values().stream()
                .sorted(Comparator.comparing(StoredFile::uploadedAt).reversed())
                .toList();
    }

    public void remove(String id) {
        files.remove(id);
    }

    public void putPending(StoredFile file) {
        pending.put(file.id(), file);
    }

    /** 대기 목록에서 꺼내며 지움. 두 번 complete 하면 두 번째는 비어 있음 */
    public Optional<StoredFile> takePending(String id) {
        return Optional.ofNullable(pending.remove(id));
    }

    public int size() {
        return files.size();
    }
}
