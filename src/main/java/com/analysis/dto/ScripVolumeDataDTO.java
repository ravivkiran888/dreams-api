package com.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScripVolumeDataDTO {

    @JsonProperty("_id")
    private String id;

    @JsonProperty("ScripCode")
    private String scripCode;

    @JsonProperty("Symbol")
    private String symbol;

    @JsonProperty("Sector")
    private String sector;

    private CandleResponseDTO candleData;
    private String errorMessage;
}
