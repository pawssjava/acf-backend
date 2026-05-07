package kz.cyber.acf.core.news.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.news.dto.NewsDto;
import kz.cyber.acf.core.news.dto.NewsRequest;
import kz.cyber.acf.core.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "News", description = "News articles shown on the platform. Ordered by newest first.")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "List all news", description = "Returns all news articles sorted by creation date descending.")
    @GetMapping
    public List<NewsDto> findAll() {
        return newsService.findAll();
    }

    @Operation(
            summary = "Get news article by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Article found"),
                    @ApiResponse(responseCode = "404", description = "Article not found")
            }
    )
    @GetMapping("/{id}")
    public NewsDto findById(@Parameter(description = "News article ID") @PathVariable Long id) {
        return newsService.findById(id);
    }

    @Operation(
            summary = "Create news article",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Article created")
            }
    )
    @PostMapping
    public ResponseEntity<NewsDto> create(@RequestBody NewsRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(newsService.create(req));
    }

    @Operation(
            summary = "Update news article",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Article updated"),
                    @ApiResponse(responseCode = "404", description = "Article not found")
            }
    )
    @PutMapping("/{id}")
    public NewsDto update(
            @Parameter(description = "News article ID") @PathVariable Long id,
            @RequestBody NewsRequest req) {
        return newsService.update(id, req);
    }

    @Operation(
            summary = "Upload news image",
            description = "Uploads an image to MinIO and attaches it to the news article.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Image uploaded"),
                    @ApiResponse(responseCode = "404", description = "Article not found")
            }
    )
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public NewsDto uploadImage(
            @Parameter(description = "News article ID") @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return newsService.uploadImage(id, file);
    }

    @Operation(
            summary = "Delete news article",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Article deleted"),
                    @ApiResponse(responseCode = "404", description = "Article not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "News article ID") @PathVariable Long id) {
        newsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
