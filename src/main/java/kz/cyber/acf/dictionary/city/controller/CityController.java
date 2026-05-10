package kz.cyber.acf.dictionary.city.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.dictionary.city.dto.CityDto;
import kz.cyber.acf.dictionary.city.dto.CityRequest;
import kz.cyber.acf.dictionary.city.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Dictionary — Cities", description = "Lookup table for cities of Kazakhstan.")
@RestController
@RequestMapping("/api/dictionary/cities")
@RequiredArgsConstructor
public class CityController {
    private final CityService cityService;

    @Operation(summary = "List all cities")
    @GetMapping
    public List<CityDto> findAll() {
        return cityService.findAll();
    }

    @Operation(
            summary = "Get city by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "City found"),
                    @ApiResponse(responseCode = "404", description = "City not found")
            }
    )
    @GetMapping("/{id}")
    public CityDto findById(@Parameter(description = "City ID") @PathVariable Long id) {
        return cityService.findById(id);
    }

    @Operation(
            summary = "Create city",
            responses = {
                    @ApiResponse(responseCode = "201", description = "City created")
            }
    )
    @PostMapping
    public ResponseEntity<CityDto> create(@RequestBody CityRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.create(req));
    }

    @Operation(
            summary = "Update city",
            responses = {
                    @ApiResponse(responseCode = "200", description = "City updated"),
                    @ApiResponse(responseCode = "404", description = "City not found")
            }
    )
    @PutMapping("/{id}")
    public CityDto update(
            @Parameter(description = "City ID") @PathVariable Long id,
            @RequestBody CityRequest req) {
        return cityService.update(id, req);
    }

    @Operation(
            summary = "Delete city",
            responses = {
                    @ApiResponse(responseCode = "204", description = "City deleted"),
                    @ApiResponse(responseCode = "404", description = "City not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "City ID") @PathVariable Long id) {
        cityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
