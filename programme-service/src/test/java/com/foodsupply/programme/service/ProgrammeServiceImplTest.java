package com.foodsupply.programme.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodsupply.programme.client.BeneficiaryClient;
import com.foodsupply.programme.dto.ParticipationResponse;
import com.foodsupply.programme.entity.BeneficiaryCategory;
import com.foodsupply.programme.entity.NutritionProgramme;
import com.foodsupply.programme.entity.Participation;
import com.foodsupply.programme.entity.ProgrammeStatus;
import com.foodsupply.programme.exception.BusinessException;
import com.foodsupply.programme.repository.ParticipationRepository;
import com.foodsupply.programme.repository.ProgrammeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgrammeServiceImplTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ProgrammeRepository programmeRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private BeneficiaryClient beneficiaryClient;
    @InjectMocks
    private ProgrammeServiceImpl service;

    private NutritionProgramme programme() {
        return NutritionProgramme.builder().id(1L).name("School Mid-Day Meal").startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(6)).targetCategory(BeneficiaryCategory.STUDENT)
                .status(ProgrammeStatus.ACTIVE).build();
    }

    @Test
    void addParticipant_rejectsNonEligibleBeneficiary() throws Exception {
        JsonNode b = MAPPER.readTree("{\"id\":5,\"fullName\":\"School A\",\"status\":\"ACTIVE\",\"eligible\":false,\"category\":\"STUDENT\",\"peopleServed\":500}");
        when(programmeRepository.findById(1L)).thenReturn(Optional.of(programme()));
        when(beneficiaryClient.getById(5L)).thenReturn(b);

        assertThrows(BusinessException.class, () -> service.addParticipant(1L, 5L));
    }

    @Test
    void addParticipant_usesPeopleServedOfBeneficiary() throws Exception {
        JsonNode b = MAPPER.readTree("{\"id\":5,\"fullName\":\"School A\",\"status\":\"ACTIVE\",\"eligible\":true,\"category\":\"STUDENT\",\"peopleServed\":500}");
        when(programmeRepository.findById(1L)).thenReturn(Optional.of(programme()));
        when(beneficiaryClient.getById(5L)).thenReturn(b);
        when(participationRepository.findByProgrammeIdAndBeneficiaryId(1L, 5L)).thenReturn(Optional.empty());
        when(participationRepository.save(any(Participation.class))).thenAnswer(inv -> inv.getArgument(0));

        ParticipationResponse r = service.addParticipant(1L, 5L);

        assertEquals(500, r.peopleCovered());
        assertEquals("School A", r.beneficiaryName());
    }

    @Test
    void addParticipant_rejectsCategoryMismatch() throws Exception {
        JsonNode b = MAPPER.readTree("{\"id\":6,\"fullName\":\"Mr X\",\"status\":\"ACTIVE\",\"eligible\":true,\"category\":\"ELDERLY\",\"peopleServed\":1}");
        when(programmeRepository.findById(1L)).thenReturn(Optional.of(programme()));
        when(beneficiaryClient.getById(6L)).thenReturn(b);

        assertThrows(BusinessException.class, () -> service.addParticipant(1L, 6L));
    }
}
