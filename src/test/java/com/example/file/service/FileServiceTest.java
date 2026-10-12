package com.example.file.service;

import com.example.file.common.FileValidator;
import com.example.file.storage.FileStorage;
import com.example.file.storage.StoredFile;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FileService 단위 테스트. 저장소는 메모리 Map 으로 흉내 낸 가짜(FakeStorage)를 씀.
 *
 * 규약(FileStorage)만 지키면 무엇이든 끼울 수 있다는 것을 테스트가 보여 줌 —
 * 디스크도 S3 도 아닌 세 번째 구현이 여기 있음.
 */
class FileServiceTest {

    /** 규약을 최소로 구현한 가짜 저장소. 프리사인 URL 은 고정 문자열로 돌려줌 */
    static class FakeStorage implements FileStorage {
        final Map<String, byte[]> store = new HashMap<>();

        @Override public String kind() { return "fake"; }

        @Override public String save(MultipartFile file, String key) throws IOException {
            store.put(key, file.getBytes());
            return key;
        }

        @Override public Resource load(String key) { return new ByteArrayResource(store.get(key)); }

        @Override public void delete(String key) { store.remove(key); }

        @Override public Optional<String> downloadUrl(String key, String originalName, Duration ttl) {
            return Optional.of("https://fake/" + key + "?expires=" + ttl.toSeconds());
        }
    }

    private final FakeStorage storage = new FakeStorage();
    private final FileRegistry registry = new FileRegistry();
    private final FileService service = new FileService(storage, registry, new FileValidator(1024), 300);

    private StoredFile uploadHello() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());
        return service.upload(file);
    }

    @Test
    void 업로드하면_저장소와_목록에_같이_들어감() throws IOException {
        StoredFile stored = uploadHello();
        assertTrue(storage.store.containsKey(stored.key()));
        assertEquals(1, registry.size());
        assertEquals("hello.txt", stored.originalName());
    }

    @Test
    void 삭제하면_저장소와_목록에서_같이_빠짐() throws IOException {
        StoredFile stored = uploadHello();
        service.delete(stored.id());
        assertTrue(storage.store.isEmpty());
        assertEquals(0, registry.size());
    }

    // TODO 3 · FileService.downloadUrl 을 작성한 뒤 아래 주석을 풀면 통과해야 함
    // @Test
    // void 직접_다운로드_URL_은_저장소가_만든_값을_그대로_돌려줌() throws IOException {
    //     StoredFile stored = uploadHello();
    //     Optional<String> url = service.downloadUrl(stored.id());
    //     assertTrue(url.isPresent());
    //     assertEquals("https://fake/" + stored.key() + "?expires=300", url.get());
    // }
}
