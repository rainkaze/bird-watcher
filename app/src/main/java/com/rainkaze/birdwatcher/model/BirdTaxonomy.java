package com.rainkaze.birdwatcher.model;

// 鸟类分类模型
public class BirdTaxonomy {
    private String kingdom;
    private String phylum;
    private String clazz;
    private String order;
    private String family;
    private String genus;
    private String species;

    // 构造函数和Getter/Setter方法
    public BirdTaxonomy() {}

    // Getters and Setters
    public String getKingdom() { return kingdom; }
    public void setKingdom(String kingdom) { this.kingdom = kingdom; }
    public String getPhylum() { return phylum; }
    public void setPhylum(String phylum) { this.phylum = phylum; }
    public String getClazz() { return clazz; }
    public void setClazz(String clazz) { this.clazz = clazz; }
    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }
    public String getFamily() { return family; }
    public void setFamily(String family) { this.family = family; }
    public String getGenus() { return genus; }
    public void setGenus(String genus) { this.genus = genus; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
}
