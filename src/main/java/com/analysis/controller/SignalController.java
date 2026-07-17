package com.analysis.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.analysis.documents.SectorIndices;
import com.analysis.dto.BullishSignalDTO;
import com.analysis.dto.SectorIndicatorDTO;
import com.analysis.service.LiveDataService;
import com.analysis.service.SectorProcessingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class SignalController {

	
	private final SectorProcessingService sectorProcessingService;
	private final LiveDataService liveDataService;
	private final MongoTemplate mongoTemplate;

	@GetMapping("/sectors")
	public ResponseEntity<List<SectorIndicatorDTO>> getTopSectors() {
		try {
			List<SectorIndices> sectorIndices = mongoTemplate.findAll(SectorIndices.class);
			List<SectorIndicatorDTO> processedSectors = sectorProcessingService.processSectorsInBatch(sectorIndices);
        	return ResponseEntity.ok(processedSectors);
		} catch (Exception e) {
			log.error("Error in getTopSectors", e);
			return ResponseEntity.ok(new ArrayList<>());
		}
	}

	@GetMapping("/bullish")
	public ResponseEntity<List<BullishSignalDTO>> getBullishSignals() {
		try {
			List<BullishSignalDTO> response = liveDataService.getBullishSignals();
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Error in getBullishSignals", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
		}
	}

}
