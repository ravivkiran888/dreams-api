package com.analysis.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.analysis.dto.SimplifiedVolumeDataDTO;
import com.analysis.service.ScripVolumeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/scrips")
@RequiredArgsConstructor
public class ScripMasterController {

    private final ScripVolumeService scripVolumeService;

    @GetMapping("/volumes")
    public ResponseEntity<List<SimplifiedVolumeDataDTO>> getAllScripVolumes() {
        log.info("=== Starting getAllScripVolumes endpoint ===");
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Fetching all scrip volumes from service...");
            List<ScripVolumeDataDTO> volumes = scripVolumeService.getAllScripVolumes();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully fetched {} scrip volumes in {} ms", volumes.size(), duration);
            
            return ResponseEntity.ok(volumes);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Error fetching volume data for scrip_symbol_eq after {} ms", duration, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }
}
