package com.example.file.service;

import com.example.file.storage.StoredFile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 파일 목록(메타데이터). 이제 데이터베이스에 둠.
 *
 * 이전 단계까지는 메모리 Map 이라 인스턴스마다 목록이 달랐음.
 * 같은 메서드 이름을 유지했으므로 FileService 는 한 줄도 바뀌지 않음.
 *   local → H2 (메모리 DB, 설치 불필요)   prod → RDS MySQL
 */
@Component
@Transactional
public class FileRegistry {

    private final FileRecordRepository repository;

    public FileRegistry(FileRecordRepository repository) {
        this.repository = repository;
    }

    public void put(StoredFile file) {
        repository.save(FileRecord.from(file, FileRecord.Status.STORED));
    }

    @Transactional(readOnly = true)
    public Optional<StoredFile> find(String id) {
        return repository.findByIdAndStatus(id, FileRecord.Status.STORED).map(FileRecord::toStoredFile);
    }

    /** 최근 업로드가 위로 오게 정렬함 */
    @Transactional(readOnly = true)
    public List<StoredFile> findAll() {
        return repository.findAllByStatusOrderByUploadedAtDesc(FileRecord.Status.STORED).stream()
                .map(FileRecord::toStoredFile).toList();
    }

    public void remove(String id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public int size() {
        return (int) repository.countByStatus(FileRecord.Status.STORED);
    }

    /** 직접 업로드 대기. 인스턴스가 바뀌어도 complete 를 받을 수 있게 DB 에 둠 */
    public void putPending(StoredFile file) {
        repository.save(FileRecord.from(file, FileRecord.Status.PENDING));
    }

    /** 대기 행을 꺼내며 지움. 두 번 complete 하면 두 번째는 비어 있음 */
    public Optional<StoredFile> takePending(String id) {
        // TODO 4 · 대기 행 꺼내기 (DB 버전)
        //
        // 기능        status 가 PENDING 인 행을 id 로 찾아 지우고, StoredFile 로 바꿔 돌려줌 · 없으면 Optional.empty()
        //             두 번 호출하면 두 번째는 비어 있어야 함 (complete 를 두 번 받아도 목록에 한 번만)
        // 활용        repository.findByIdAndStatus(id, FileRecord.Status.PENDING)   → Optional<FileRecord>
        //             repository.delete(record)                                     → 행 삭제 · Optional.ifPresent 와 함께
        //             FileRecord::toStoredFile                                      → Optional.map 으로 변환
        // 동작결과    FileRegistryTest 의 "대기_행은_한_번만_꺼내짐" 주석을 풀어 통과
        //             서버에서는 직접 업로드 3단계가 인스턴스 2대에서도 완료됨 (대기 행이 DB 에 있으므로)
        return Optional.empty();
    }
}
