package com.topoom.missingcase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Safe182Response {
    private Safe182Body body;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Safe182Body {
        @JsonProperty("items")
        private List<MissingItem> items;
    }

    @Getter
    @NoArgsConstructor
    public static class MissingItem {
        @JsonProperty("msspsnIdntfccd")
        private String id;

        @JsonProperty("nm")
        private String name;

        @JsonProperty("writingTrgetDscd")
        private String targetType;

        @JsonProperty("sexdstnDscd")
        private String gender;

        @JsonProperty("age")
        private Integer ageAtTime;

        @JsonProperty("ageNow")
        private Integer currentAge;

        @JsonProperty("nltyDscd")
        private String nationality;

        @JsonProperty("occrde")
        private String occurredAt; // ISO 8601 문자열

        @JsonProperty("occrAdres")
        private String occurredLocation;

        @JsonProperty("height")
        private Integer heightCm;

        @JsonProperty("bdwgh")
        private Integer weightKg;

        @JsonProperty("frmDscd")
        private String bodyType;

        @JsonProperty("faceshpeDscd")
        private String faceShape;

        @JsonProperty("haircolrDscd")
        private String hairColor;

        @JsonProperty("hairshpeDscd")
        private String hairStyle;

        @JsonProperty("alldressingDscd")
        private String clothingDesc;
    }
}
