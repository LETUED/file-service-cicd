package com.example.file.api;

import com.example.file.api.dto.DownloadUrlResponse;
import com.example.file.api.dto.FileInfo;
import com.example.file.api.dto.StatusResponse;
import com.example.file.service.FileRegistry;
import com.example.file.service.FileService;
import com.example.file.storage.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 파일 요청 수신. REST 규약.
 *
 *   POST   /api/files            업로드 (multipart, 항목 이름 file)   → 201 + Location
 *   GET    /api/files            목록
 *   GET    /api/files/{id}       정보
 *   GET    /api/files/{id}/content   앱을 거쳐 내려받음
 *   GET    /api/files/{id}/url   저장소에서 직접 받는 임시 URL (S3 만)
 *   DELETE /api/files/{id}       삭제                               → 204
 *   GET    /api/status           저장소 종류 · 호스트명 · 파일 수
 */
@RestController
@RequestMapping("/api")
public class FileController {

    private final FileService fileService;
    private final FileRegistry registry;

    public FileController(FileService fileService, FileRegistry registry) {
        this.fileService = fileService;
        this.registry = registry;
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileInfo> upload(@RequestParam("file") MultipartFile file) throws IOException {
        StoredFile stored = fileService.upload(file);
        return ResponseEntity.created(URI.create("/api/files/" + stored.id()))
                .body(FileInfo.from(stored));
    }

    @GetMapping("/files")
    public List<FileInfo> list() {
        return fileService.list().stream().map(FileInfo::from).toList();
    }

    @GetMapping("/files/{id}")
    public FileInfo info(@PathVariable String id) {
        return FileInfo.from(fileService.get(id));
    }

    /** 앱 경유 다운로드. 서버 대역폭과 메모리를 씀 → 07 교안에서 프리사인과 비교함 */
    @GetMapping("/files/{id}/content")
    public ResponseEntity<Resource> download(@PathVariable String id) throws IOException {
        StoredFile file = fileService.get(id);
        Resource body = fileService.download(id);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaTypeOf(file.contentType()))
                .body(body);
    }

    /** 프리사인 GET URL. 디스크 저장소(local)에서는 404 가 아니라 409 로 "지원 안 함"을 알림 */
    @GetMapping("/files/{id}/url")
    public ResponseEntity<DownloadUrlResponse> downloadUrl(@PathVariable String id) {
        return fileService.downloadUrl(id)
                .map(url -> ResponseEntity.ok(
                        new DownloadUrlResponse(id, url, fileService.urlTtl().toSeconds())))
                .orElseGet(() -> ResponseEntity.status(409).build());
    }

    @DeleteMapping("/files/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws IOException {
        fileService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public StatusResponse status() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "unknown";
        }
        return new StatusResponse(fileService.storageKind(), host, registry.size());
    }

    private static MediaType mediaTypeOf(String contentType) {
        try {
            return contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
