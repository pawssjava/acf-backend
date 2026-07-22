package kz.cyber.acf.core.news.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.news.dto.NewsDto;
import kz.cyber.acf.core.news.dto.NewsRequest;
import kz.cyber.acf.core.news.service.NewsService;
import kz.cyber.acf.core.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "News", description = "News articles shown on the platform. Ordered by newest first.")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    private final UserService userService;

    @Operation(
            summary = "List news",
            description = "Returns news articles sorted by creation date descending. " +
                    "By default only active (non-archived) articles are returned; pass archived=true to list the archive (admin only)."
    )
    @GetMapping
    public List<NewsDto> findAll(
            @Parameter(description = "Return archived articles instead of active ones (admin only)") @RequestParam(required = false, defaultValue = "false") boolean archived,
            @AuthenticationPrincipal Jwt jwt) {
        if (archived) {
            userService.requireAdmin(jwt);
        }
        return newsService.findAll(archived);
    }

    @Operation(
            summary = "Get news article by ID",
            description = "Archived articles are only visible to admins; other callers get 404.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Article found"),
                    @ApiResponse(responseCode = "404", description = "Article not found")
            }
    )
    @GetMapping("/{id}")
    public NewsDto findById(@Parameter(description = "News article ID") @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return newsService.findById(id, jwt);
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
            description = "Admin only.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Article updated"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
                    @ApiResponse(responseCode = "404", description = "Article not found")
            }
    )
    @PutMapping("/{id}")
    public NewsDto update(
            @Parameter(description = "News article ID") @PathVariable Long id,
            @RequestBody NewsRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return newsService.update(id, req, jwt);
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
            summary = "Archive news article",
            description = "Admin only. Hides the article from the public site while keeping it in the admin panel, listed under the archive filter. Reversible via restore.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Article archived"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
                    @ApiResponse(responseCode = "404", description = "Article not found")
            }
    )
    @PatchMapping("/{id}/archive")
    public NewsDto archive(@Parameter(description = "News article ID") @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return newsService.archive(id, jwt);
    }

    @Operation(
            summary = "Restore news article",
            description = "Admin only. Restores a previously archived article back to the public site.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Article restored"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
                    @ApiResponse(responseCode = "404", description = "Article not found")
            }
    )
    @PatchMapping("/{id}/restore")
    public NewsDto restore(@Parameter(description = "News article ID") @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return newsService.restore(id, jwt);
    }

    @Operation(
            summary = "Delete news article",
            description = "Admin only. Permanently deletes the article. Cannot be undone.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Article deleted"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
                    @ApiResponse(responseCode = "404", description = "Article not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "News article ID") @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        newsService.delete(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
