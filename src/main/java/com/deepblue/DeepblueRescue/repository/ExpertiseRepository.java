package com.deepblue.DeepblueRescue.repository;

import com.deepblue.DeepblueRescue.domain.Expertise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpertiseRepository extends JpaRepository<Expertise, Long> {

    Optional<Expertise> findByNameIgnoreCase(String name);
}
