package com.deepblue.DeepblueRescue.repository;

import com.deepblue.DeepblueRescue.domain.RescueCase;
import com.deepblue.DeepblueRescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RescueCaseRepository extends JpaRepository<RescueCase, Long> {

    Optional<RescueCase> findByCaseCode(String caseCode);

    boolean existsByCaseCode(String caseCode);

    List<RescueCase> findByStatusOrderByRescueDateAsc(RescueStatus status);

    List<RescueCase> findByRescueCenterCode(String centerCode);

    List<RescueCase> findByRescueDateAfterOrderByRescueDateDesc(LocalDate date);
}
