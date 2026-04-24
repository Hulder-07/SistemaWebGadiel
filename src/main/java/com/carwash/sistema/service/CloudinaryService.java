package com.carwash.sistema.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadFile(MultipartFile multipartFile, String folderName) throws IOException {
        File file = convertMultiPartToFile(multipartFile);
        try {
            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap("folder", folderName));
            return uploadResult.get("url").toString();
        } catch (Exception e) {
            log.error("Error uploading file to Cloudinary", e);
            throw new RuntimeException("Error uploading image");
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}
