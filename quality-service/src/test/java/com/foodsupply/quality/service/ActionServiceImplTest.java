package com.foodsupply.quality.service;

import com.foodsupply.quality.dto.ActionRequest;
import com.foodsupply.quality.dto.ActionResponse;
import com.foodsupply.quality.entity.ActionStatus;
import com.foodsupply.quality.entity.CorrectiveAction;
import com.foodsupply.quality.exception.InvalidRequestException;
import com.foodsupply.quality.exception.ResourceNotFoundException;
import com.foodsupply.quality.repository.CorrectiveActionRepository;
import com.foodsupply.quality.repository.QualityInspectionRepository;
import com.foodsupply.quality.repository.WastageRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionServiceImplTest {

    @Mock
    private CorrectiveActionRepository repository;

    @Mock
    private QualityInspectionRepository inspectionRepository;

    @Mock
    private WastageRecordRepository wastageRepository;

    @InjectMocks
    private ActionServiceImpl service;

    @Test
    void createRequiresInspectionOrWastage() {
        ActionRequest request = ActionRequest.builder()
                .description("Fix the cold room")
                .assignedTo("Maintenance")
                .dueDate(LocalDate.now().plusDays(3))
                .build();

        assertThrows(InvalidRequestException.class, () -> service.create(request));
    }

    @Test
    void createFailsWhenInspectionDoesNotExist() {
        ActionRequest request = ActionRequest.builder()
                .inspectionId(99L)
                .description("Re-test")
                .assignedTo("Meena")
                .dueDate(LocalDate.now().plusDays(3))
                .build();
        when(inspectionRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.create(request));
    }

    @Test
    void closingAnActionStampsCompletionDate() {
        CorrectiveAction action = CorrectiveAction.builder()
                .id(4L)
                .wastageId(1L)
                .description("Install alarm")
                .assignedTo("Maintenance")
                .dueDate(LocalDate.now().minusDays(1))
                .status(ActionStatus.IN_PROGRESS)
                .build();
        when(repository.findById(4L)).thenReturn(Optional.of(action));
        when(repository.save(any(CorrectiveAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ActionResponse response = service.changeStatus(4L, ActionStatus.CLOSED);

        assertEquals(ActionStatus.CLOSED, response.status());
        assertNotNull(response.completedOn());
        assertEquals(false, response.overdue());
    }
}
