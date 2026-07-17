package com.analysis.dto;

import java.math.BigDecimal;
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

    @JsonProperty("AvgPrev5Volume")
    private Double avgPrev5Volume;

    @JsonProperty("LatestTimestamp")
    private LocalDateTime latestTimestamp;

    @JsonProperty("LatestOpenPrice")
    private BigDecimal latestOpenPrice;

    @JsonProperty("LatestHighPrice")
    private BigDecimal latestHighPrice;

    @JsonProperty("LatestLowPrice")
    private BigDecimal latestLowPrice;

    @JsonProperty("LatestClosePrice")
    private BigDecimal latestClosePrice;
}
