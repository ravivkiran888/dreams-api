package com.analysis.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveDataDepthDTO {

    private List<LiveDataDepthEntryDTO> buy;
    private List<LiveDataDepthEntryDTO> sell;
}
