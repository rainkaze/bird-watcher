package com.rainkaze.birdwatcher.model;

public class BirdLocation {
    private String name;
    private String description;
    private double latitude;
    private double longitude;
    private int popularity; // 人气值

    public BirdLocation(String name, String description, double latitude, double longitude, int popularity) {
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.popularity = popularity;
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getPopularity() { return popularity; }
}
