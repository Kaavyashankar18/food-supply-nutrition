package com.foodsupply.distribution.controller;

import com.foodsupply.distribution.dto.CentreRequest;
import com.foodsupply.distribution.dto.CentreResponse;
import com.foodsupply.distribution.dto.PageResponse;
import com.foodsupply.distribution.entity.CentreStatus;
import com.foodsupply.distribution.service.CentreService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Distribution Centres")
@RestController
@RequestMapping("/api/v1/distribution-centres")
@RequiredArgsConstructor
public class DistributionCentreController {

    private final CentreService centreService;

    @GetMapping
    public PageResponse<CentreResponse> list(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) CentreStatus status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(defaultValue = "id") String sort,
                                             @RequestParam(defaultValue = "asc") String dir) {
        return centreService.list(q, status, page, size, sort, dir);
    }

    @GetMapping("/{id}")
    public CentreResponse get(@PathVariable Long id) {
        return centreService.get(id);
    }

    @PostMapping
    public ResponseEntity<CentreResponse> create(@Valid @RequestBody CentreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(centreService.create(request));
    }

    @PutMapping("/{id}")
    public CentreResponse update(@PathVariable Long id, @Valid @RequestBody CentreRequest request) {
        return centreService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        centreService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
