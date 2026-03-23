package com.bezukh.image_search.service;

import com.bezukh.image_search.exception.AiCaptioningException;
import com.bezukh.image_search.exception.ImageNotFoundException;
import com.bezukh.image_search.model.ImageDocument;
import com.bezukh.image_search.repository.ImageDocumentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {

    private final ImageDocumentRepository repository;

    private final AiCaptionService aiCaptionService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(uploadDir);
            Files.createDirectories(rootLocation);
            log.info("The Downloads folder has been initialized: {}", rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Unable to create a downloads folder!", e);
        }
    }

    public ImageDocument storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        Path destinationPath = null;
        Path thumbPath = null;

        try {
            String uniqueId = UUID.randomUUID().toString();
            String storedFileName = uniqueId + getFileExtension(originalFileName);
            destinationPath = this.rootLocation.resolve(storedFileName);

            Files.copy(file.getInputStream(), destinationPath);

            String thumbFileName = uniqueId + "_thumb.jpg";
            thumbPath = this.rootLocation.resolve(thumbFileName);
            createAndSaveThumbnail(destinationPath.toFile(), thumbPath.toFile());

            String description = aiCaptionService.generateDescription(destinationPath);

            ImageDocument document = ImageDocument.builder()
                    .originalFileName(originalFileName)
                    .storagePath(destinationPath.toString())
                    .thumbnailPath(thumbPath.toString())
                    .aiDescription(description)
                    .build();

            return repository.save(document);

        } catch (Exception e) {
            if (destinationPath != null)
                try { Files.deleteIfExists(destinationPath); } catch (IOException ignored) {}

            log.error("Failed to store file and process AI: {}", e.getMessage());
            if (e instanceof AiCaptioningException) throw (AiCaptioningException) e;
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    private void createAndSaveThumbnail(File original, File thumb) throws IOException {
        BufferedImage originalImage = ImageIO.read(original);
        if (originalImage == null) throw new IOException("Cannot read uploaded image format for thumbnail");

        int targetWidth = 400;
        int targetHeight = (int) (originalImage.getHeight() * ((double) targetWidth / originalImage.getWidth()));

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        ImageIO.write(resizedImage, "jpg", thumb);
    }

    private String getFileExtension(String originalFileName) {
        String fileExtension = "";
        if (originalFileName.contains("."))
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));

        return fileExtension;
    }

    public List<ImageDocument> getAllImages() {
        return repository.findAll();
    }

    public void deleteFile(Long id) {
        ImageDocument document = repository.findById(id)
                .orElseThrow(() -> new ImageNotFoundException("Image not found with ID: " + id));

        try {
            if (!Files.deleteIfExists(Paths.get(document.getStoragePath())))
                log.warn("File {} was already missing from disk", document.getStoragePath());

            if (!Files.deleteIfExists(Paths.get(document.getThumbnailPath())))
                log.warn("Thumbnail file {} was already missing from disk", document.getStoragePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not delete physical file due to system error", e);
        }

        repository.deleteById(id);
        log.info("Database record with ID {} deleted successfully", id);
    }

    public void updateDescription(Long id, String newDescription) {
        ImageDocument document = getImageById(id);
        document.setAiDescription(newDescription);
        repository.save(document);
        log.info("Description updated for image ID: {}", id);
    }

    public ImageDocument getImageById(Long id) {
        log.info("Fetching image document metadata for ID: {}", id);
        return repository.findById(id)
                .orElseThrow(() -> new ImageNotFoundException("Image with ID " + id + " not found"));
    }
}
