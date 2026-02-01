package com.br.elohostel.controller;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.br.elohostel.service.FileStorageService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/files")
public class FileDownloadController {

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) throws IOException {
        Resource resource = fileStorageService.loadFileAsResource(fileName);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/stream/{fileName}")
    public ResponseEntity<StreamingResponseBody> streamFile(
            @PathVariable String fileName, 
            HttpServletResponse response) throws IOException {
        
        Resource resource = fileStorageService.loadFileAsResource(fileName);
        
        StreamingResponseBody stream = outputStream -> {
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        };
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(stream);
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFileWithExtension(
            @PathVariable String fileName,
            @RequestParam(required = false) String contentType) throws IOException {
        
        Resource resource = fileStorageService.loadFileAsResource(fileName);
        String fileContentType = contentType != null ? 
                contentType : 
                fileStorageService.getContentType(fileName);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileContentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

}