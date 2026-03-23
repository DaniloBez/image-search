package com.bezukh.image_search.controller;

import com.bezukh.image_search.dto.ImageSearchResultDto;
import com.bezukh.image_search.exception.ErrorResponse;
import com.bezukh.image_search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search Controller", description = "Smart search using TF-IDF and AI descriptions")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Search images", description = "Performs ranked search based on filename and AI-generated description")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ImageSearchResultDto.class)))),
            @ApiResponse(responseCode = "500", description = "Internal Error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ImageSearchResultDto>> search(@RequestParam String q) {
        return ResponseEntity.ok(searchService.search(q));
    }

    @PostMapping("/rebuild")
    @Operation(summary = "Rebuild index", description = "Manually triggers inverted index reconstruction from database")
    @ApiResponse(responseCode = "200", description = "Index updated")
    public ResponseEntity<String> rebuild() {
        searchService.rebuildIndex();
        return ResponseEntity.ok("Index successfully rebuilt");
    }
}