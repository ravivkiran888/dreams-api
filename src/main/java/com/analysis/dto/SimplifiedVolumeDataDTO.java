package com.analysis.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimplifiedVolumeDataDTO {

    @JsonProperty("Symbol")
    private String symbol;

    @JsonProperty("Sector")
    private String sector;

    @JsonProperty("LatestVolume")
    private Long latestVolume;

    @JsonProperty("LatestTimestamp")
    private LocalDateTime latestTimestamp;
}
