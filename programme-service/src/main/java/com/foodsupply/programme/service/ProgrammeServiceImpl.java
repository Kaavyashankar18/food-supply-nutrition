package com.foodsupply.programme.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.foodsupply.programme.client.BeneficiaryClient;
import com.foodsupply.programme.dto.PageResponse;
import com.foodsupply.programme.dto.ParticipationResponse;
import com.foodsupply.programme.dto.ProgrammeRequest;
import com.foodsupply.programme.dto.ProgrammeResponse;
import com.foodsupply.programme.dto.ProgrammeSummary;
import com.foodsupply.programme.entity.BeneficiaryCategory;
import com.foodsupply.programme.entity.NutritionProgramme;
import com.foodsupply.programme.entity.Participation;
import com.foodsupply.programme.entity.ParticipationStatus;
import com.foodsupply.programme.entity.ProgrammeStatus;
import com.foodsupply.programme.exception.BusinessException;
import com.foodsupply.programme.exception.InvalidRequestException;
import com.foodsupply.programme.exception.ResourceNotFoundException;
import com.foodsupply.programme.mapper.ProgrammeMapper;
import com.foodsupply.programme.repository.ParticipationRepository;
import com.foodsupply.programme.repository.ProgrammeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgrammeServiceImpl implements ProgrammeService {

    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "startDate", "endDate", "status");

    private final ProgrammeRepository programmeRepository;
    private final ParticipationRepository participationRepository;
    private final BeneficiaryClient beneficiaryClient;

    @Override
    public PageResponse<ProgrammeResponse> list(String q, ProgrammeStatus status, BeneficiaryCategory category,
                                                int page, int size, String sort, String dir) {
        Specification<NutritionProgramme> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), like));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (category != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("targetCategory"), category));
        }
        String field = SORT_FIELDS.contains(sort) ? sort : "id";
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 1000), Sort.by(direction, field));
        Page<NutritionProgramme> result = programmeRepository.findAll(spec, pageable);
        return PageResponse.of(result.map(ProgrammeMapper::toResponse));
    }

    @Override
    public ProgrammeResponse get(Long id) {
        return ProgrammeMapper.toResponse(find(id));
    }

    @Override
    @Transactional
    public ProgrammeResponse create(ProgrammeRequest r) {
        validateDates(r);
        NutritionProgramme p = NutritionProgramme.builder()
                .name(r.getName().trim()).type(r.getType()).objectives(r.getObjectives())
                .startDate(r.getStartDate()).endDate(r.getEndDate()).targetCategory(r.getTargetCategory())
                .status(r.getStatus() != null ? r.getStatus() : ProgrammeStatus.DRAFT)
                .build();
        NutritionProgramme saved = programmeRepository.save(p);
        log.info("Created programme {} ({})", saved.getId(), saved.getName());
        return ProgrammeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProgrammeResponse update(Long id, ProgrammeRequest r) {
        validateDates(r);
        NutritionProgramme p = find(id);
        p.setName(r.getName().trim());
        p.setType(r.getType());
        p.setObjectives(r.getObjectives());
        p.setStartDate(r.getStartDate());
        p.setEndDate(r.getEndDate());
        p.setTargetCategory(r.getTargetCategory());
        if (r.getStatus() != null) {
            p.setStatus(r.getStatus());
        }
        return ProgrammeMapper.toResponse(programmeRepository.save(p));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        NutritionProgramme p = find(id);
        programmeRepository.delete(p);
        log.info("Deleted programme {}", id);
    }

    @Override
    @Transactional
    public ProgrammeResponse changeStatus(Long id, ProgrammeStatus value) {
        NutritionProgramme p = find(id);
        if (p.getStatus() == ProgrammeStatus.COMPLETED || p.getStatus() == ProgrammeStatus.CANCELLED) {
            throw new BusinessException("Programme is " + p.getStatus() + " and its status cannot be changed");
        }
        p.setStatus(value);
        return ProgrammeMapper.toResponse(programmeRepository.save(p));
    }

    @Override
    public List<ParticipationResponse> participants(Long programmeId) {
        find(programmeId);
        return participationRepository.findByProgrammeIdOrderByIdAsc(programmeId).stream()
                .map(ProgrammeMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ParticipationResponse addParticipant(Long programmeId, Long beneficiaryId) {
        NutritionProgramme p = find(programmeId);
        if (p.getStatus() == ProgrammeStatus.COMPLETED || p.getStatus() == ProgrammeStatus.CANCELLED) {
            throw new BusinessException("Cannot enrol participants: programme is " + p.getStatus());
        }
        JsonNode b = beneficiaryClient.getById(beneficiaryId);
        String name = b.path("fullName").asText("Beneficiary " + beneficiaryId);
        if (!"ACTIVE".equals(b.path("status").asText())) {
            throw new BusinessException("Beneficiary '" + name + "' is not ACTIVE");
        }
        if (!b.path("eligible").asBoolean(false)) {
            throw new BusinessException("Beneficiary '" + name + "' is not eligible");
        }
        String category = b.path("category").asText(null);
        if (p.getTargetCategory() != null && !p.getTargetCategory().name().equals(category)) {
            throw new BusinessException("Beneficiary category " + category + " does not match programme target "
                    + p.getTargetCategory());
        }
        int people = Math.max(1, b.path("peopleServed").asInt(1));

        Optional<Participation> existing = participationRepository.findByProgrammeIdAndBeneficiaryId(programmeId, beneficiaryId);
        Participation part;
        if (existing.isPresent()) {
            part = existing.get();
            if (part.getStatus() == ParticipationStatus.ENROLLED) {
                throw new BusinessException("Beneficiary is already enrolled in this programme");
            }
            part.setStatus(ParticipationStatus.ENROLLED);
            part.setPeopleCovered(people);
            part.setBeneficiaryName(name);
            part.setEnrolledOn(LocalDate.now());
        } else {
            part = Participation.builder().programme(p).beneficiaryId(beneficiaryId).beneficiaryName(name)
                    .peopleCovered(people).enrolledOn(LocalDate.now()).status(ParticipationStatus.ENROLLED).build();
        }
        Participation saved = participationRepository.save(part);
        log.info("Beneficiary {} enrolled in programme {} covering {} people", beneficiaryId, programmeId, people);
        return ProgrammeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ParticipationResponse withdraw(Long programmeId, Long participationId) {
        Participation part = participationRepository.findByIdAndProgrammeId(participationId, programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("Participation not found: " + participationId));
        part.setStatus(ParticipationStatus.WITHDRAWN);
        return ProgrammeMapper.toResponse(participationRepository.save(part));
    }

    @Override
    public ProgrammeSummary summary() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        long total = 0;
        for (ProgrammeStatus s : ProgrammeStatus.values()) {
            long c = programmeRepository.countByStatus(s);
            byStatus.put(s.name(), c);
            total += c;
        }
        long participants = participationRepository.countByStatusInProgrammes(ParticipationStatus.ENROLLED, ProgrammeStatus.ACTIVE);
        long people = participationRepository.sumPeopleByStatusInProgrammes(ParticipationStatus.ENROLLED, ProgrammeStatus.ACTIVE).longValue();
        return new ProgrammeSummary(total, byStatus.get(ProgrammeStatus.ACTIVE.name()), byStatus, participants, people);
    }

    private NutritionProgramme find(Long id) {
        return programmeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + id));
    }

    private void validateDates(ProgrammeRequest r) {
        if (r.getEndDate().isBefore(r.getStartDate())) {
            throw new InvalidRequestException("endDate must not be before startDate");
        }
    }
}
