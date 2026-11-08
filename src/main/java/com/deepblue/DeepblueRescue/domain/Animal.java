package com.deepblue.DeepblueRescue.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "animals")
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "animal_code", nullable = false, unique = true, length = 50)
    private String animalCode;

    @Column(name = "common_name", nullable = false, length = 100)
    private String commonName;

    @Column(name = "scientific_name", nullable = false, length = 100)
    private String scientificName;

    @Column(name = "tracking_device_code", unique = true, length = 50)
    private String trackingDeviceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnimalSex sex;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_case_id", nullable = false, unique = true)
    private RescueCase rescueCase;

    @OneToOne(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private MedicalRecord medicalRecord;

    @OneToMany(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Treatment> treatments = new ArrayList<>();

    public Animal() {}

    public void assignMedicalRecord(MedicalRecord medicalRecord) {
        this.medicalRecord = medicalRecord;
        if (medicalRecord != null) {
            medicalRecord.setAnimal(this);
        }
    }

    public void addTreatment(Treatment treatment) {
        treatments.add(treatment);
        treatment.setAnimal(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAnimalCode() { return animalCode; }
    public void setAnimalCode(String animalCode) { this.animalCode = animalCode; }
    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }
    public String getScientificName() { return scientificName; }
    public void setScientificName(String scientificName) { this.scientificName = scientificName; }
    public AnimalSex getSex() { return sex; }
    public void setSex(AnimalSex sex) { this.sex = sex; }
    public RescueCase getRescueCase() { return rescueCase; }
    public void setRescueCase(RescueCase rescueCase) { this.rescueCase = rescueCase; }
    public MedicalRecord getMedicalRecord() { return medicalRecord; }
    public void setMedicalRecord(MedicalRecord medicalRecord) { this.medicalRecord = medicalRecord; }
    public List<Treatment> getTreatments() { return treatments; }
    public void setTreatments(List<Treatment> treatments) { this.treatments = treatments; }
    public String getTrackingDeviceCode() { return trackingDeviceCode; }
    public void setTrackingDeviceCode(String trackingDeviceCode) { this.trackingDeviceCode = trackingDeviceCode; }

}
