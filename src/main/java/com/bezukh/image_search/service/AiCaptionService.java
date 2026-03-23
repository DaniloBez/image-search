package com.bezukh.image_search.service;

import com.bezukh.image_search.exception.AiCaptioningException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class AiCaptionService {

    @Value("${app.ai.model-url}")
    private String modelUrl;

    @Value("${app.ai.model-name}")
    private String modelName;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateDescription(Path imagePath) {
        try {
            log.info("Starting local AI analysis for: {}", imagePath.getFileName());

            byte[] imageBytes = Files.readAllBytes(imagePath);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String prompt = "Describe this image in detail. " +
                    "Then, provide a comma-separated list of 10-15 broad categorical tags, synonyms, and related concepts. " +
                    "For example, if you see a car, add tags like: vehicle, transport, transportation, travel, road.";

            Map<String, Object> request = Map.of(
                    "model", modelName,
                    "prompt", prompt,
                    "stream", false,
                    "images", new String[]{base64Image}
            );

            String response = restTemplate.postForObject(modelUrl, request, String.class);
            JsonNode root = objectMapper.readTree(response);

            String description = root.get("response").asText().trim();
            log.info("AI description generated: {}", description);
            return description;

        } catch (Exception e) {
            log.error("Local AI engine failure: {}", e.getMessage());
            throw new AiCaptioningException("Local AI processing failed: " + e.getMessage());
        }
    }
}