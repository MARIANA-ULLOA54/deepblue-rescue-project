package com.deepblue.rescue.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rescue_centers")
public class RescueCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @OneToMany(mappedBy = "rescueCenter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RescueCase> cases = new ArrayList<>();

    public RescueCenter() {}

    public RescueCenter(String code, String name, String city) {
        this.code = code;
        this.name = name;
        this.city = city;
    }

    public void addCase(RescueCase rescueCase) {
        cases.add(rescueCase);
        rescueCase.setRescueCenter(this);
    }

    public void removeCase(RescueCase rescueCase) {
        cases.remove(rescueCase);
        rescueCase.setRescueCenter(null);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public List<RescueCase> getCases() { return cases; }
    public void setCases(List<RescueCase> cases) { this.cases = cases; }
}
