package com.deepblue.DeepblueRescue.repository;

import com.deepblue.DeepblueRescue.domain.Animal;
import com.deepblue.DeepblueRescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    Optional<Animal> findByAnimalCode(String animalCode);

    List<Animal> findByCommonNameContainingIgnoreCase(String text);

    List<Animal> findByRescueCaseStatus(RescueStatus status);

    List<Animal> findByRescueCaseRescueCenterCode(String centerCode);

    /**
     * Animales con un caso en el estado dado que hayan recibido al menos un
     * tratamiento de un especialista con la experiencia indicada.
     * Varias asociaciones + joins: Query Method no es legible, se usa JPQL.
     */
    @Query(value = """
            select distinct a
            from Animal a
            join a.rescueCase rc
            join a.treatments t
            join t.specialist s
            join s.expertiseAreas e
            where rc.status = :status
              and lower(e.name) = lower(:expertiseName)
            order by a.animalCode asc
            """)
    List<Animal> findByStatusAndTreatedBySpecialistWithExpertise(
            @Param("status") RescueStatus status,
            @Param("expertiseName") String expertiseName);
}
