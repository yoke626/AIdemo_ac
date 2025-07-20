package com.xiaomi.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Handles the storage of uploaded files.
 */
@Service
public class StorageService {

    // Defines the root directory for all uploaded files.
    // This creates an "uploads" folder in your project's root directory.
    private final Path rootLocation = Paths.get("uploads");

    /**
     * The constructor initializes the storage service.
     * It creates the root upload directory if it doesn't already exist.
     */
    public StorageService() {
        try {
            Files.createDirectories(rootLocation);
            System.out.println("Uploads directory created at: " + rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory!", e);
        }
    }

    /**
     * Stores an uploaded file.
     *
     * @param file The MultipartFile to store.
     * @return The URL path to access the stored file.
     */
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Cannot store an empty file.");
        }

        try {
            // Generate a unique filename to prevent overwrites
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + fileExtension;

            Path destinationFile = this.rootLocation.resolve(Paths.get(newFileName)).normalize().toAbsolutePath();

            // Copy the file's input stream to the destination path, replacing any existing file.
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Return the web-accessible path for the file.
            // This path needs to be mapped as a static resource.
            return "/uploads/" + newFileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }
}