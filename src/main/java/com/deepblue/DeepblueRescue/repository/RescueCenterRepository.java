package com.deepblue.DeepblueRescue.repository;

import com.deepblue.DeepblueRescue.domain.RescueCenter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RescueCenterRepository extends JpaRepository<RescueCenter, Long> {

    Optional<RescueCenter> findByCode(String code);
}
