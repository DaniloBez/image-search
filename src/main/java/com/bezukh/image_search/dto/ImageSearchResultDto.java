package com.bezukh.image_search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Об'єкт результату пошуку зображення з оцінкою релевантності")
public class ImageSearchResultDto {

    @Schema(description = "Унікальний ідентифікатор зображення в базі", example = "10")
    private Long id;

    @Schema(description = "Оригінальна назва завантаженого файлу", example = "summer_vacation.jpg")
    private String fileName;

    @Schema(description = "URL для перегляду повнорозмірного зображення", example = "/api/images/display/10")
    private String displayUrl;

    @Schema(description = "URL для отримання мініатюри (thumbnail) для сітки", example = "/api/images/display/10/thumb")
    private String thumbnailUrl;

    @Schema(description = "Опис вмісту, згенерований моделлю LLaVA", example = "A beautiful beach with golden sand and blue waves.")
    private String aiDescription;

    @Schema(description = "Вага релевантності TF-IDF (чим вище, тим краща відповідність запиту)", example = "0.8542")
    private double score;
}