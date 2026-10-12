package com.example.file.api.dto;

/**
 * 서비스 상태. 어느 저장소로 떠 있는지 · 어느 인스턴스인지 화면에서 바로 봄.
 *
 * @param storage  local 또는 s3
 * @param host     응답한 서버의 호스트명 (ALB 뒤에서 어느 인스턴스가 받았는지)
 * @param files    목록에 있는 파일 수 (인스턴스마다 다를 수 있음)
 */
public record StatusResponse(String storage, String host, int files) {
}
