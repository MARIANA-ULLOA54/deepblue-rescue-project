package com.deepblue.DeepblueRescue.repository;

import com.deepblue.DeepblueRescue.domain.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
}
