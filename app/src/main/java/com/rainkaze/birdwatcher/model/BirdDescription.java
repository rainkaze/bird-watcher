package com.rainkaze.birdwatcher.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

// 鸟类描述模型
public class BirdDescription {
    @SerializedName("speciesCode")
    private String speciesCode;

    @SerializedName("descriptions")
    private List<DescriptionItem> descriptions;

    public static class DescriptionItem {
        @SerializedName("description")
        private String description;

        @SerializedName("source")
        private String source;

        @SerializedName("date")
        private String date;

        // Getters
        public String getDescription() { return description; }
        public String getSource() { return source; }
        public String getDate() { return date; }
    }

    // Getters
    public String getSpeciesCode() { return speciesCode; }
    public List<DescriptionItem> getDescriptions() { return descriptions; }
}