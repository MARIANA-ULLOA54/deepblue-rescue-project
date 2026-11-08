package com.deepblue.DeepblueRescue.domain;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "rescue_cases")
public class RescueCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_code", nullable = false, unique = true, length = 50)
    private String caseCode;

    @Column(name = "rescue_date", nullable = false)
    private LocalDate rescueDate;

    @Column(name = "rescue_location", nullable = false, length = 200)
    private String rescueLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RescueStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_center_id", nullable = false)
    private RescueCenter rescueCenter;

    @OneToOne(mappedBy = "rescueCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Animal animal;

    public RescueCase() {}

    public void assignAnimal(Animal animal) {
        this.animal = animal;
        if (animal != null) {
            animal.setRescueCase(this);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseCode() { return caseCode; }
    public void setCaseCode(String caseCode) { this.caseCode = caseCode; }
    public LocalDate getRescueDate() { return rescueDate; }
    public void setRescueDate(LocalDate rescueDate) { this.rescueDate = rescueDate; }
    public String getRescueLocation() { return rescueLocation; }
    public void setRescueLocation(String rescueLocation) { this.rescueLocation = rescueLocation; }
    public RescueStatus getStatus() { return status; }
    public void setStatus(RescueStatus status) { this.status = status; }
    public RescueCenter getRescueCenter() { return rescueCenter; }
    public void setRescueCenter(RescueCenter rescueCenter) { this.rescueCenter = rescueCenter; }
    public Animal getAnimal() { return animal; }
    public void setAnimal(Animal animal) { this.animal = animal; }

}
