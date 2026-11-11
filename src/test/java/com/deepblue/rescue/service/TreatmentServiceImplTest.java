package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    private static final LocalDateTime VALID_DATE = LocalDateTime.of(2026, 8, 21, 9, 0);

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    // ---------------------------------------------------------------- helpers

    private Animal animal(RescueStatus status) {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-001");
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 20));
        rescueCase.setStatus(status);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-001");
        animal.setRescueCase(rescueCase);
        return animal;
    }

    private Specialist specialist(boolean active) {
        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SPEC-001");
        specialist.setActive(active);
        return specialist;
    }

    private CreateTreatmentRequest request(LocalDateTime performedAt) {
        return new CreateTreatmentRequest(
                "AN-001", "SPEC-001", performedAt,
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper injury.");
    }

    private TreatmentResponse response() {
        return new TreatmentResponse(
                1L, "AN-001", "SPEC-001", VALID_DATE,
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper injury.");
    }

    // --------------------------------------------------------------- register

    @Test
    void shouldRegisterTreatment() {
        Animal animal = animal(RescueStatus.IN_REHABILITATION);
        Specialist specialist = specialist(true);
        TreatmentResponse response = response();

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));
        when(treatmentRepository.save(any(Treatment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Treatment.class))).thenReturn(response);

        TreatmentResponse result = service.register(request(VALID_DATE));

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Treatment> captor = ArgumentCaptor.forClass(Treatment.class);
        verify(treatmentRepository).save(captor.capture());

        Treatment saved = captor.getValue();
        assertThat(saved.getAnimal()).isSameAs(animal);
        assertThat(saved.getSpecialist()).isSameAs(specialist);
        assertThat(saved.getPerformedAt()).isEqualTo(VALID_DATE);
        assertThat(saved.getType()).isEqualTo(TreatmentType.WOUND_CARE);
        assertThat(saved.getDescription()).isEqualTo("Cleaning of left front flipper injury.");
    }

    @Test
    void shouldAcceptTreatmentOnTheSameDayAsTheRescue() {
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal(RescueStatus.UNDER_EVALUATION)));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist(true)));
        when(treatmentRepository.save(any(Treatment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Treatment.class))).thenReturn(response());

        service.register(request(LocalDateTime.of(2026, 8, 20, 0, 5)));

        verify(treatmentRepository).save(any(Treatment.class));
    }

    @Test
    void shouldThrowWhenAnimalDoesNotExist() {
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(request(VALID_DATE)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-001");

        verifyNoInteractions(specialistRepository, treatmentRepository, mapper);
    }

    @Test
    void shouldThrowWhenSpecialistDoesNotExist() {
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal(RescueStatus.IN_REHABILITATION)));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(request(VALID_DATE)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SPEC-001");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectInactiveSpecialistAndNeverSave() {
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal(RescueStatus.IN_REHABILITATION)));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist(false)));

        assertThatThrownBy(() -> service.register(request(VALID_DATE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");

        verify(treatmentRepository, never()).save(any());
        verify(mapper, never()).toResponse(any());
    }

    @ParameterizedTest
    @EnumSource(value = RescueStatus.class, names = {"RELEASED", "CLOSED"})
    void shouldRejectTreatmentWhenCaseIsFinished(RescueStatus status) {
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal(status)));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist(true)));

        CreateTreatmentRequest observation = new CreateTreatmentRequest(
                "AN-001", "SPEC-001", VALID_DATE, TreatmentType.OBSERVATION, "Follow-up");

        assertThatThrownBy(() -> service.register(observation))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(status.name());

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectTreatmentBeforeTheRescueDate() {
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal(RescueStatus.IN_REHABILITATION)));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist(true)));

        assertThatThrownBy(() -> service.register(request(LocalDateTime.of(2026, 8, 15, 9, 0))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rescue date");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectMissingTreatmentDate() {
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal(RescueStatus.IN_REHABILITATION)));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist(true)));

        assertThatThrownBy(() -> service.register(request(null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    // ------------------------------------------------------- findByAnimalCode

    @Test
    void shouldFindTreatmentsByAnimalCode() {
        Treatment first = new Treatment(null, null, VALID_DATE, TreatmentType.HYDRATION, "IV fluids");
        Treatment second = new Treatment(null, null, VALID_DATE.plusDays(1), TreatmentType.NUTRITION, "Diet");
        TreatmentResponse firstResponse = new TreatmentResponse(
                1L, "AN-001", "SPEC-001", VALID_DATE, TreatmentType.HYDRATION, "IV fluids");
        TreatmentResponse secondResponse = new TreatmentResponse(
                2L, "AN-001", "SPEC-001", VALID_DATE.plusDays(1), TreatmentType.NUTRITION, "Diet");

        when(treatmentRepository.findByAnimalAnimalCodeOrderByPerformedAtAsc("AN-001"))
                .thenReturn(List.of(first, second));
        when(mapper.toResponse(first)).thenReturn(firstResponse);
        when(mapper.toResponse(second)).thenReturn(secondResponse);

        assertThat(service.findByAnimalCode("AN-001"))
                .containsExactly(firstResponse, secondResponse);
    }
}
