package com.br.elohostel.model.dtos;

public record FileUploadResponse (
    String fileName,
    String downloadUri,
    long size,
    String message
) {
}