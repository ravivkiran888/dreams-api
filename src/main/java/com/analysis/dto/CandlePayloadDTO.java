package com.analysis.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandlePayloadDTO {

    private List<CandleDTO> candles;

    @JsonProperty("closing_price")
    private BigDecimal closingPrice;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    @JsonProperty("interval_in_minutes")
    private Integer intervalInMinutes;
}
