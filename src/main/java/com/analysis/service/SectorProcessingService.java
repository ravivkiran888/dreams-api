package com.analysis.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.analysis.apicalls.GrowAPIClient;
import com.analysis.documents.SectorIndices;
import com.analysis.dto.SectorIndicatorDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorProcessingService {

    private final GrowAPIClient growAPIClient;
    private final ObjectMapper objectMapper;

    public List<SectorIndicatorDTO> processSectorsInBatch(List<SectorIndices> sectorIndices) {
        List<SectorIndicatorDTO> results = new ArrayList<>();
        
        if (sectorIndices == null || sectorIndices.isEmpty()) {
            return results;
        }

        for (SectorIndices sector : sectorIndices) {
            try {
                SectorIndicatorDTO dto = processSector(sector);
                if (dto != null) {
                    results.add(dto);
                }
             } catch (Exception e) {
                log.error("Error processing sector {}: {}", sector.getSector(), e.getMessage());
            }
        }
        
        Collections.sort(results);
        return results;
    }

    public SectorIndicatorDTO processSector(SectorIndices sector) throws Exception {
        String responseBody = growAPIClient.getSectorData(sector.getSector());
        
        if (responseBody != null) {
            return parseResponse(sector, responseBody);
        }
        return null;
    }

    private SectorIndicatorDTO parseResponse(SectorIndices sector, String responseBody) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        
        String status = rootNode.path("status").asText();
        if (!"SUCCESS".equals(status)) {
            log.error("API non-success status for {}: {}", sector.getSector(), status);
            return null;
        }

        JsonNode payload = rootNode.path("payload");
        BigDecimal dayChange = extractBigDecimal(payload, "day_change");
        
        if (dayChange == null) {
            return null;
        }

        return new SectorIndicatorDTO(
            sector.getName(),
            sector.getSector(),
            dayChange.setScale(2, RoundingMode.DOWN),
            Instant.now()
        );
    }

    private BigDecimal extractBigDecimal(JsonNode node, String field) {
        if (node.has(field) && !node.path(field).isNull()) {
            return node.path(field).decimalValue();
        }
        return null;
    }
}



