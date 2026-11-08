package com.deepblue.DeepblueRescue.domain;

import com.deepblue.rescue.domain.Expertise;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "specialists")
public class Specialist {

@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professional_code", nullable = false, unique = true, length = 50)
    private String professionalCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToMany
    @JoinTable(
        name = "specialist_expertise",
        joinColumns = @JoinColumn(name = "specialist_id"),
        inverseJoinColumns = @JoinColumn(name = "expertise_id")
    )
    private Set<Expertise> expertiseAreas = new HashSet<>();

    @OneToMany(mappedBy = "specialist")
    private List<Treatment> treatments = new ArrayList<>();

    public Specialist() {}

    public void addExpertise(Expertise expertise) {
        expertiseAreas.add(expertise);
        expertise.getSpecialists().add(this);
    }

    public void removeExpertise(Expertise expertise) {
        expertiseAreas.remove(expertise);
        expertise.getSpecialists().remove(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProfessionalCode() { return professionalCode; }
    public void setProfessionalCode(String professionalCode) { this.professionalCode = professionalCode; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Set<Expertise> getExpertiseAreas() { return expertiseAreas; }
    public void setExpertiseAreas(Set<Expertise> expertiseAreas) { this.expertiseAreas = expertiseAreas; }
    public List<Treatment> getTreatments() { return treatments; }
    public void setTreatments(List<Treatment> treatments) { this.treatments = treatments; }    

}
