package com.bezukh.image_search.controller;

import com.bezukh.image_search.exception.ErrorResponse;
import com.bezukh.image_search.model.ImageDocument;
import com.bezukh.image_search.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Image Controller", description = "Endpoints for uploading and managing images")
public class ImageController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new image", description = "Saves the image locally and generates an AI description")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image successfully uploaded",
                    content = @Content(schema = @Schema(implementation = ImageDocument.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file provided",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "AI Context creating unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Server error during processing",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ImageDocument> uploadImage(
            @Parameter(description = "Image file (JPG/PNG)", required = true)
            @RequestParam("file") MultipartFile file) {
        log.info("Received upload request for file: {}", file.getOriginalFilename());
        ImageDocument savedDoc = fileStorageService.storeFile(file);
        return ResponseEntity.ok(savedDoc);
    }

    @GetMapping
    @Operation(summary = "Get all images", description = "Returns a list of all images with their AI descriptions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of images retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ImageDocument.class)))),
            @ApiResponse(responseCode = "500", description = "Database error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ImageDocument>> getAllImages() {
        log.info("Fetching all image documents");
        return ResponseEntity.ok(fileStorageService.getAllImages());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete image", description = "Removes the file and its database record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Image ID not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        log.info("Request to delete image ID: {}", id);
        fileStorageService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/description")
    @Operation(summary = "Update AI description", description = "Updates the text description of the image")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Editing successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Image ID not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> updateDescription(@PathVariable Long id, @RequestBody java.util.Map<String, String> payload) {
        fileStorageService.updateDescription(id, payload.get("description"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/display/{id}")
    @Operation(summary = "Get image file", description = "Returns the physical image for display")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image found", content = @Content(mediaType = "image/jpeg")),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Resource> displayImage(@PathVariable Long id) {
        ImageDocument doc = fileStorageService.getImageById(id);
        try {
            Path path = Paths.get(doc.getStoragePath());
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to stream image file for ID: {}", id);
            throw new RuntimeException("Error reading image file");
        }
    }

    @GetMapping("/display/{id}/thumb")
    @Operation(summary = "Get image thumbnail", description = "Returns a scaled-down version of the image for grid view")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image found", content = @Content(mediaType = "image/jpeg")),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Resource> displayThumbnail(@PathVariable Long id) {
        ImageDocument doc = fileStorageService.getImageById(id);
        try {
            Path path = Paths.get(doc.getThumbnailPath());
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to stream image file for ID: {}", id);
            throw new RuntimeException("Error reading image file");
        }
    }
}