
package com.example.application.services;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final String UPLOAD_FOLDER = "uploads";
    
    public FileService() {
        // Ensure the upload directory exists
        createUploadDirectory();
    }
    
    /**
     * Creates the upload directory if it doesn't exist
     */
    private void createUploadDirectory() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_FOLDER));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }
    
    /**
     * Save a file from an input stream
     * 
     * @param fileName The name of the file
     * @param inputStream The input stream containing the file data
     * @return The path where the file was saved
     * @throws IOException If there's an error saving the file
     */
    public Path saveFile(String fileName, InputStream inputStream) throws IOException {
        Path targetPath = Paths.get(UPLOAD_FOLDER, fileName);
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath;
    }
    
    /**
     * Delete a file
     * 
     * @param fileName The name of the file to delete
     * @return true if successful, false otherwise
     */
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(UPLOAD_FOLDER, fileName);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Get a list of all uploaded files
     * 
     * @return List of file names
     */
    public List<String> getAllFiles() {
        try {
            return Files.list(Paths.get(UPLOAD_FOLDER))
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Check if a file exists
     * 
     * @param fileName The name of the file
     * @return true if the file exists, false otherwise
     */
    public boolean fileExists(String fileName) {
        Path filePath = Paths.get(UPLOAD_FOLDER, fileName);
        return Files.exists(filePath);
    }
    
    /**
     * Get the path to a file
     * 
     * @param fileName The name of the file
     * @return The path to the file
     */
    public Path getFilePath(String fileName) {
        return Paths.get(UPLOAD_FOLDER, fileName);
    }
}
