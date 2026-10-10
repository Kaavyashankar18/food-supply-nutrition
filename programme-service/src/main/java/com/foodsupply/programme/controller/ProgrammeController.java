package com.foodsupply.programme.controller;

import com.foodsupply.programme.dto.PageResponse;
import com.foodsupply.programme.dto.ParticipantRequest;
import com.foodsupply.programme.dto.ParticipationResponse;
import com.foodsupply.programme.dto.ProgrammeRequest;
import com.foodsupply.programme.dto.ProgrammeResponse;
import com.foodsupply.programme.dto.ProgrammeSummary;
import com.foodsupply.programme.dto.RequirementsResponse;
import com.foodsupply.programme.entity.BeneficiaryCategory;
import com.foodsupply.programme.entity.ProgrammeStatus;
import com.foodsupply.programme.service.ProgrammeService;
import com.foodsupply.programme.service.RequirementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Nutrition Programmes")
@RestController
@RequestMapping("/api/v1/programmes")
@RequiredArgsConstructor
public class ProgrammeController {

    private final ProgrammeService programmeService;
    private final RequirementService requirementService;

    @GetMapping
    public PageResponse<ProgrammeResponse> list(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) ProgrammeStatus status,
                                                @RequestParam(required = false) BeneficiaryCategory targetCategory,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "id") String sort,
                                                @RequestParam(defaultValue = "asc") String dir) {
        return programmeService.list(q, status, targetCategory, page, size, sort, dir);
    }

    @GetMapping("/summary")
    public ProgrammeSummary summary() {
        return programmeService.summary();
    }

    @GetMapping("/{id}")
    public ProgrammeResponse get(@PathVariable Long id) {
        return programmeService.get(id);
    }

    @PostMapping
    public ResponseEntity<ProgrammeResponse> create(@Valid @RequestBody ProgrammeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programmeService.create(request));
    }

    @PutMapping("/{id}")
    public ProgrammeResponse update(@PathVariable Long id, @Valid @RequestBody ProgrammeRequest request) {
        return programmeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        programmeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ProgrammeResponse changeStatus(@PathVariable Long id, @RequestParam ProgrammeStatus value) {
        return programmeService.changeStatus(id, value);
    }

    @GetMapping("/{id}/participants")
    public List<ParticipationResponse> participants(@PathVariable Long id) {
        return programmeService.participants(id);
    }

    @PostMapping("/{id}/participants")
    public ResponseEntity<ParticipationResponse> addParticipant(@PathVariable Long id,
                                                                @Valid @RequestBody ParticipantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programmeService.addParticipant(id, request.getBeneficiaryId()));
    }

    @PatchMapping("/{id}/participants/{participationId}/withdraw")
    public ParticipationResponse withdraw(@PathVariable Long id, @PathVariable Long participationId) {
        return programmeService.withdraw(id, participationId);
    }

    @DeleteMapping("/{id}/participants/{participationId}")
    public ResponseEntity<Void> removeParticipant(@PathVariable Long id, @PathVariable Long participationId) {
        programmeService.withdraw(id, participationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/requirements")
    public RequirementsResponse requirements(@PathVariable Long id,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return requirementService.calculate(id, from, to);
    }
}
