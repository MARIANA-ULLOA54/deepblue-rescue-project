package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository repository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    private Animal animal(String code, RescueStatus status) {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-001");
        rescueCase.setStatus(status);

        Animal animal = new Animal();
        animal.setAnimalCode(code);
        animal.setRescueCase(rescueCase);
        return animal;
    }

    private AnimalResponse response(String code, RescueStatus status) {
        return new AnimalResponse(
                1L, code, "Green Sea Turtle", "Chelonia mydas",
                AnimalSex.FEMALE, "RES-001", status);
    }

    @Test
    void shouldFindAnimalByCode() {
        Animal animal = animal("AN-001", RescueStatus.IN_REHABILITATION);
        AnimalResponse response = response("AN-001", RescueStatus.IN_REHABILITATION);

        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(mapper.toResponse(animal)).thenReturn(response);

        assertThat(service.findByCode("AN-001")).isEqualTo(response);
    }

    @Test
    void shouldThrowWhenAnimalDoesNotExist() {
        when(repository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-999");

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldFindAnimalsInRehabilitation() {
        Animal first = animal("AN-001", RescueStatus.IN_REHABILITATION);
        Animal second = animal("AN-002", RescueStatus.IN_REHABILITATION);
        AnimalResponse firstResponse = response("AN-001", RescueStatus.IN_REHABILITATION);
        AnimalResponse secondResponse = response("AN-002", RescueStatus.IN_REHABILITATION);

        when(repository.findByRescueCaseStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(first, second));
        when(mapper.toResponse(first)).thenReturn(firstResponse);
        when(mapper.toResponse(second)).thenReturn(secondResponse);

        assertThat(service.findAnimalsInRehabilitation())
                .containsExactly(firstResponse, secondResponse);
    }

    @ParameterizedTest
    @EnumSource(value = RescueStatus.class, names = {"UNDER_EVALUATION", "IN_REHABILITATION"})
    void shouldAllowTreatmentWhenUnderEvaluationOrInRehabilitation(RescueStatus status) {
        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal("AN-001", status)));

        assertThat(service.canReceiveTreatment("AN-001")).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = RescueStatus.class,
            names = {"ADMITTED", "READY_FOR_RELEASE", "RELEASED", "CLOSED"})
    void shouldNotAllowTreatmentInAnyOtherStatus(RescueStatus status) {
        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal("AN-001", status)));

        assertThat(service.canReceiveTreatment("AN-001")).isFalse();
    }

    @Test
    void shouldThrowWhenCheckingTreatmentForMissingAnimal() {
        when(repository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.canReceiveTreatment("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
