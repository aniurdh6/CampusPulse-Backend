package com.campuspulse.model;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A bookable thing: a classroom, a lab or a piece of equipment. */
@Entity
@Table(name = "resources")
public class CampusResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceType type;

    @Column(nullable = false, length = 150)
    private String location;

    @Column(nullable = false)
    private int capacity = 1;

    /** Comma separated, for example "Projector,AC,Whiteboard". */
    @Column(length = 300)
    private String features = "";

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    public List<String> featureList() {
        if (features == null || features.isBlank()) {
            return List.of();
        }
        return Arrays.stream(features.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ResourceType getType() { return type; }
    public void setType(ResourceType type) { this.type = type; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
