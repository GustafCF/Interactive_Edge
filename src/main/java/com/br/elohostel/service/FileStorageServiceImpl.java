package com.br.elohostel.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageServiceImpl(@Value("${file.upload-dir:uploads}") String uploadDir) throws IOException {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.fileStorageLocation);
    }

    @Override
    public String storeFile(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        String fileName = UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = this.fileStorageLocation.resolve(fileName);
        
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        return fileName;
    }

    @Override
    public Resource loadFileAsResource(String fileName) throws IOException {
        try {
            Path filePath = getFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IOException("Arquivo não encontrado ou não pode ser lido: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new IOException("Erro ao carregar o arquivo: " + fileName, ex);
        }
    }

    @Override
    public String getContentType(String fileName) throws IOException {
        Path filePath = getFilePath(fileName);
        return Files.probeContentType(filePath);
    }

    @Override
    public Path getFilePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName).normalize();
    }

    @Override
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = getFilePath(fileName);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
}