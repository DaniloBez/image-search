package com.bezukh.image_search.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Стандартна обгортка для відповідей про помилки")
public class ErrorResponse {

    @Schema(description = "HTTP статус код", example = "404")
    private int status;

    @Schema(description = "Текстове повідомлення про помилку", example = "Зображення з ID 1 не знайдено")
    private String message;

    @Schema(description = "Час виникнення помилки")
    private LocalDateTime timestamp;
}