package com.deepblue.DeepblueRescue.repository;

import com.deepblue.DeepblueRescue.domain.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    List<Treatment> findByAnimalIdOrderByPerformedAtAsc(Long animalId);

    List<Treatment> findByAnimalAnimalCodeOrderByPerformedAtAsc(String animalCode);

    @Query("""
            select t
            from Treatment t
            where t.performedAt between :start and :end
            order by t.performedAt asc
            """)
    List<Treatment> findPerformedBetween(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query("""
            select t
            from Treatment t
            join t.animal a
            join a.rescueCase rc
            join rc.rescueCenter c
            where c.code = :centerCode
            order by t.performedAt asc
            """)
    List<Treatment> findByCenterCode(@Param("centerCode") String centerCode);

    @Query("""
            select distinct t
            from Treatment t
            join t.specialist s
            join s.expertiseAreas e
            where lower(e.name) = lower(:expertiseName)
            order by t.performedAt asc
            """)
    List<Treatment> findBySpecialistExpertise(@Param("expertiseName") String expertiseName);
}
