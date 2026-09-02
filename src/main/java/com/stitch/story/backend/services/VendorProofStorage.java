package com.stitch.story.backend.services;

import com.stitch.story.backend.exceptions.BadRequestException;
import com.stitch.story.backend.exceptions.ResourceNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class VendorProofStorage {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of(
            "pdf", "jpg", "jpeg", "png", "webp", "gif", "doc", "docx"
    );

    private final Path directory;

    public VendorProofStorage() {
        this.directory = Path.of("uploads", "vendor-proofs").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.directory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create vendor proof upload folder");
        }
    }

    public StoredProof store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A proof document is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Document must be 10 MB or smaller");
        }

        String originalName = safeOriginalName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED.contains(extension)) {
            throw new BadRequestException("Upload a PDF, image, or Word document");
        }

        String storedName = UUID.randomUUID() + "." + extension;
        Path target = directory.resolve(storedName).normalize();
        if (!target.startsWith(directory)) {
            throw new BadRequestException("Invalid file");
        }

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BadRequestException("Could not save document");
        }

        return new StoredProof(storedName, originalName, contentTypeOf(file, extension));
    }

    public Resource load(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            throw new ResourceNotFoundException("Document not found");
        }
        Path path = directory.resolve(storedName).normalize();
        if (!path.startsWith(directory) || !Files.exists(path) || !Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("Document not found");
        }
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Document not found");
            }
            return resource;
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Document not found");
        }
    }

    private static String safeOriginalName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document";
        }
        String name = Path.of(filename).getFileName().toString().replaceAll("[\\\\/]+", "");
        return name.isBlank() ? "document" : name;
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String contentTypeOf(MultipartFile file, String extension) {
        String provided = file.getContentType();
        if (provided != null && !provided.isBlank() && !"application/octet-stream".equals(provided)) {
            return provided;
        }
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    public record StoredProof(String storedName, String originalName, String contentType) {
    }
}
