package com.foodsupply.quality.service;

import com.foodsupply.quality.dto.QualitySummaryResponse;
import com.foodsupply.quality.entity.ActionStatus;
import com.foodsupply.quality.entity.InspectionResult;
import com.foodsupply.quality.entity.QualityStatus;
import com.foodsupply.quality.entity.WastageRecord;
import com.foodsupply.quality.repository.CorrectiveActionRepository;
import com.foodsupply.quality.repository.QualityInspectionRepository;
import com.foodsupply.quality.repository.WastageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QualitySummaryServiceImpl implements QualitySummaryService {

    private final QualityInspectionRepository inspectionRepository;
    private final WastageRecordRepository wastageRepository;
    private final CorrectiveActionRepository actionRepository;

    @Override
    public QualitySummaryResponse summary() {
        Map<String, Long> byResult = new LinkedHashMap<>();
        for (InspectionResult r : InspectionResult.values()) {
            byResult.put(r.name(), inspectionRepository.countByResult(r));
        }

        Map<String, Long> byQualityStatus = new LinkedHashMap<>();
        for (QualityStatus s : QualityStatus.values()) {
            byQualityStatus.put(s.name(), inspectionRepository.countByQualityStatus(s));
        }

        Map<String, Long> actionsByStatus = new LinkedHashMap<>();
        for (ActionStatus s : ActionStatus.values()) {
            actionsByStatus.put(s.name(), actionRepository.countByStatus(s));
        }

        long openActions = actionRepository.countByStatusNot(ActionStatus.CLOSED);
        long overdueActions = actionRepository.countByStatusNotAndDueDateBefore(ActionStatus.CLOSED, LocalDate.now());

        List<WastageRecord> wastage = wastageRepository.findAll();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        Map<String, BigDecimal> wastageByUnit = new LinkedHashMap<>();
        for (WastageRecord w : wastage) {
            totalQuantity = totalQuantity.add(w.getQuantity());
            wastageByUnit.merge(w.getUnit().name(), w.getQuantity(), BigDecimal::add);
        }

        return new QualitySummaryResponse(inspectionRepository.count(), byResult, byQualityStatus,
                openActions, overdueActions, actionsByStatus, wastage.size(), totalQuantity, wastageByUnit);
    }
}
