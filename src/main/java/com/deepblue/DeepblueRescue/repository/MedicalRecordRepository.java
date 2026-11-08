package com.deepblue.DeepblueRescue.repository;

import com.deepblue.rescue.domain.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
}
