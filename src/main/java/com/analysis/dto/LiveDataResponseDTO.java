package com.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveDataResponseDTO {

    private String status;
    private LiveDataPayloadDTO payload;
}
