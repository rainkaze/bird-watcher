package com.rainkaze.birdwatcher.model;

import lombok.Data;

@Data
public class BirdLocation {
    private String name;
    private String description;
    private double latitude;
    private double longitude;
    private int popularity;

    public BirdLocation(String name, String description, double latitude, double longitude, int popularity) {
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.popularity = popularity;
    }
}
