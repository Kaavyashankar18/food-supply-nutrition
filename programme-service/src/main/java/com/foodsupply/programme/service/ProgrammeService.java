package com.foodsupply.programme.service;

import com.foodsupply.programme.dto.PageResponse;
import com.foodsupply.programme.dto.ParticipationResponse;
import com.foodsupply.programme.dto.ProgrammeRequest;
import com.foodsupply.programme.dto.ProgrammeResponse;
import com.foodsupply.programme.dto.ProgrammeSummary;
import com.foodsupply.programme.entity.BeneficiaryCategory;
import com.foodsupply.programme.entity.ProgrammeStatus;

import java.util.List;

public interface ProgrammeService {

    PageResponse<ProgrammeResponse> list(String q, ProgrammeStatus status, BeneficiaryCategory category,
                                         int page, int size, String sort, String dir);

    ProgrammeResponse get(Long id);

    ProgrammeResponse create(ProgrammeRequest request);

    ProgrammeResponse update(Long id, ProgrammeRequest request);

    void delete(Long id);

    ProgrammeResponse changeStatus(Long id, ProgrammeStatus value);

    List<ParticipationResponse> participants(Long programmeId);

    ParticipationResponse addParticipant(Long programmeId, Long beneficiaryId);

    ParticipationResponse withdraw(Long programmeId, Long participationId);

    ProgrammeSummary summary();
}
