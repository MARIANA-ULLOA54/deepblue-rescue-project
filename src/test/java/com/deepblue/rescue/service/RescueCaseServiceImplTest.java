package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    private RescueCase rescueCase(String code, RescueStatus status) {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setId(1L);
        rescueCase.setCaseCode(code);
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 20));
        rescueCase.setRescueLocation("Taganga");
        rescueCase.setStatus(status);
        return rescueCase;
    }

    private RescueCaseResponse response(String code, RescueStatus status) {
        return new RescueCaseResponse(
                1L, code, LocalDate.of(2026, 8, 20), "Taganga",
                status, "CENTER-001", "AN-001");
    }

    // ------------------------------------------------------------ findByCode

    @Test
    void shouldFindRescueCaseByCode() {
        RescueCase rescueCase = rescueCase("RES-001", RescueStatus.ADMITTED);
        RescueCaseResponse response = response("RES-001", RescueStatus.ADMITTED);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.findByCode("RES-001");

        assertThat(result).isEqualTo(response);
        verify(repository).findByCaseCode("RES-001");
        verify(mapper).toResponse(rescueCase);
    }

    @Test
    void shouldThrowWhenRescueCaseDoesNotExist() {
        when(repository.findByCaseCode("RES-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RES-999");

        verify(mapper, never()).toResponse(any());
    }

    // ----------------------------------------------------------- findByStatus

    @Test
    void shouldFindRescueCasesByStatus() {
        RescueCase first = rescueCase("RES-001", RescueStatus.IN_REHABILITATION);
        RescueCase second = rescueCase("RES-002", RescueStatus.IN_REHABILITATION);
        RescueCaseResponse firstResponse = response("RES-001", RescueStatus.IN_REHABILITATION);
        RescueCaseResponse secondResponse = response("RES-002", RescueStatus.IN_REHABILITATION);

        when(repository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(first, second));
        when(mapper.toResponse(first)).thenReturn(firstResponse);
        when(mapper.toResponse(second)).thenReturn(secondResponse);

        List<RescueCaseResponse> result = service.findByStatus(RescueStatus.IN_REHABILITATION);

        assertThat(result).containsExactly(firstResponse, secondResponse);
    }

    @Test
    void shouldReturnEmptyListWhenNoCaseHasThatStatus() {
        when(repository.findByStatusOrderByRescueDateAsc(RescueStatus.RELEASED))
                .thenReturn(List.of());

        assertThat(service.findByStatus(RescueStatus.RELEASED)).isEmpty();

        verify(mapper, never()).toResponse(any());
    }

    // ----------------------------------------------------------- changeStatus

    @Test
    void shouldChangeStatusWhenTransitionIsValid() {
        RescueCase rescueCase = rescueCase("RES-001", RescueStatus.ADMITTED);
        RescueCaseResponse response = response("RES-001", RescueStatus.UNDER_EVALUATION);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(repository.save(rescueCase)).thenReturn(rescueCase);
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.changeStatus(
                "RES-001", new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION));

        assertThat(result).isEqualTo(response);
        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.UNDER_EVALUATION);
        verify(repository).save(rescueCase);
    }

    @ParameterizedTest
    @CsvSource({
            "ADMITTED,UNDER_EVALUATION",
            "UNDER_EVALUATION,IN_REHABILITATION",
            "IN_REHABILITATION,READY_FOR_RELEASE",
            "READY_FOR_RELEASE,RELEASED"
    })
    void shouldAcceptEveryStepOfTheRescueFlow(RescueStatus current, RescueStatus next) {
        RescueCase rescueCase = rescueCase("RES-001", current);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(repository.save(rescueCase)).thenReturn(rescueCase);
        when(mapper.toResponse(rescueCase)).thenReturn(response("RES-001", next));

        RescueCaseResponse result = service.changeStatus(
                "RES-001", new ChangeRescueStatusRequest(next));

        assertThat(result.status()).isEqualTo(next);
        assertThat(rescueCase.getStatus()).isEqualTo(next);
    }

    @Test
    void shouldRejectInvalidTransitionAndNeverSave() {
        RescueCase rescueCase = rescueCase("RES-001", RescueStatus.ADMITTED);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() -> service.changeStatus(
                "RES-001", new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ADMITTED")
                .hasMessageContaining("READY_FOR_RELEASE");

        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.ADMITTED);
        verify(repository, never()).save(any());
        verify(mapper, never()).toResponse(any());
    }

    @ParameterizedTest
    @CsvSource({
            "ADMITTED,RELEASED",
            "RELEASED,IN_REHABILITATION",
            "IN_REHABILITATION,UNDER_EVALUATION",
            "UNDER_EVALUATION,UNDER_EVALUATION",
            "CLOSED,ADMITTED"
    })
    void shouldRejectEveryForbiddenTransition(RescueStatus current, RescueStatus next) {
        RescueCase rescueCase = rescueCase("RES-001", current);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() -> service.changeStatus(
                "RES-001", new ChangeRescueStatusRequest(next)))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowWhenChangingStatusOfMissingCase() {
        when(repository.findByCaseCode("RES-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(
                "RES-999", new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectMissingNewStatus() {
        RescueCase rescueCase = rescueCase("RES-001", RescueStatus.ADMITTED);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() -> service.changeStatus(
                "RES-001", new ChangeRescueStatusRequest(null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any());
    }
}
