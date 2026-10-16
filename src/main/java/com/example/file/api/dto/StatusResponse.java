package com.example.file.api.dto;

/**
 * 서비스 상태. 어느 저장소 · 어느 목록 DB · 어느 인스턴스인지 화면에서 바로 봄.
 *
 * @param storage   local 또는 s3
 * @param registry  h2 또는 mysql (파일 목록이 있는 곳)
 * @param host      응답한 서버의 호스트명
 * @param files     목록에 있는 파일 수 (DB 이므로 모든 인스턴스가 같음)
 */
public record StatusResponse(String storage, String registry, String host, int files) {
}
