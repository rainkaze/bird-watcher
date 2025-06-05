package com.rainkaze.birdwatcher.model;

// 鸟类分布区域模型
public class BirdDistribution {
    private String regionCode;
    private String regionName;
    private String countryCode;
    private String subnational1Code;
    private String subnational2Code;
    private boolean hotspots;
    private int numObservations;
    private int numSpecies;

    // 构造函数和Getter/Setter方法
    public BirdDistribution() {}

    // Getters and Setters
    public String getRegionCode() { return regionCode; }
    public String getRegionName() { return regionName; }
    public String getCountryCode() { return countryCode; }
    public String getSubnational1Code() { return subnational1Code; }
    public String getSubnational2Code() { return subnational2Code; }
    public boolean isHotspots() { return hotspots; }
    public int getNumObservations() { return numObservations; }
    public int getNumSpecies() { return numSpecies; }
}