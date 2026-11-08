package com.deepblue.DeepblueRescue.domain;

import com.deepblue.DeepblueRescue.domain.Animal;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false, unique = true)
    private Animal animal;

    @Column(name = "initial_weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal initialWeight;

    @Column(name = "initial_condition", nullable = false, length = 100)
    private String initialCondition;

    @Column(columnDefinition = "TEXT")
    private String injuries;

    @Column(columnDefinition = "TEXT")
    private String observations;

    public MedicalRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Animal getAnimal() { return animal; }
    public BigDecimal getInitialWeight() { return initialWeight; }
    public void setAnimal(Animal animal) { this.animal = animal; }
    public void setInitialWeight(BigDecimal initialWeight) { this.initialWeight = initialWeight; }
    public String getInitialCondition() { return initialCondition; }
    public void setInitialCondition(String initialCondition) { this.initialCondition = initialCondition; }
    public String getInjuries() { return injuries; }
    public void setInjuries(String injuries) { this.injuries = injuries; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

}
