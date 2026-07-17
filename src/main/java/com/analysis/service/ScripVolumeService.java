package com.analysis.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.analysis.apicalls.GrowAPIClient;
import com.analysis.documents.ScripMaster;
import com.analysis.dto.CandleDTO;
import com.analysis.dto.CandleResponseDTO;
import com.analysis.dto.SimplifiedVolumeDataDTO;
import com.analysis.helper.ScripVolumeDataHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScripVolumeService {

    private static final String DEFAULT_CANDLE_INTERVAL = "5minute";
    private static final int BATCH_SIZE = 180; // Process 180 stocks at a time
    private static final long BATCH_DELAY_MS = 2000; // 2 second delay between batches
    private static final long REQUEST_DELAY_MS = 300; // 300ms delay between individual requests

    private final ScripMasterService scripMasterService;
    private final GrowAPIClient growAPIClient;
    private final ScripVolumeDataHelper scripVolumeDataHelper;

    public List<SimplifiedVolumeDataDTO> getAllScripVolumes() {
        log.info("=============== Starting getAllScripVolumes processing ===============");
        long processStartTime = System.currentTimeMillis();
        
        List<ScripMaster> allScripMasters = scripMasterService.getAllScripMasters();
        
        List<SimplifiedVolumeDataDTO> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        int filteredCount = 0; // Stocks with no significant volume

        // Process in batches
        int batchNumber = 0;
        for (int i = 0; i < allScripMasters.size(); i += BATCH_SIZE) {
            batchNumber++;
            int endIndex = Math.min(i + BATCH_SIZE, allScripMasters.size());
            List<ScripMaster> batch = allScripMasters.subList(i, endIndex);
            
            log.info("\n--- Batch {} of {} (size: {}) ---", batchNumber, 
                    (allScripMasters.size() + BATCH_SIZE - 1) / BATCH_SIZE, batch.size());
            long batchStartTime = System.currentTimeMillis();

            for (ScripMaster scripMaster : batch) {
                try {
                    SimplifiedVolumeDataDTO volumeData = fetchVolumeData(scripMaster);
                    if (volumeData != null) {
                        results.add(volumeData);
                        successCount++;
                    } else {
                        filteredCount++;
                    }
                    // Add delay between individual requests
                    Thread.sleep(REQUEST_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Batch processing interrupted for batch {}", batchNumber);
                    break;
                }
            }
            
            long batchDuration = System.currentTimeMillis() - batchStartTime;
            log.info("Batch {} completed in {} ms | Success: {} | Filtered: {}", 
                    batchNumber, batchDuration, successCount, filteredCount);

            // Add delay between batches (except for the last batch)
            if (endIndex < allScripMasters.size()) {
                try {
                    log.info("Waiting {} ms before next batch...", BATCH_DELAY_MS);
                    Thread.sleep(BATCH_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Batch delay interrupted");
                }
            }
        }

        long totalDuration = System.currentTimeMillis() - processStartTime;
        log.info("=============== getAllScripVolumes COMPLETED ===============");
        log.info("Total Duration: {} ms | Total Results: {} | Success: {} | Filtered: {} | Failure: {}", 
                totalDuration, results.size(), successCount, filteredCount, failureCount);
        
        return results;
    }

    private SimplifiedVolumeDataDTO fetchVolumeData(ScripMaster scripMaster) {
        if (!StringUtils.hasText(scripMaster.getSymbol())) {
            log.warn("Skipping stock with empty symbol");
            return null;
        }
        
        String symbol = scripMaster.getSymbol();
        String growwSymbol = scripVolumeDataHelper.buildGrowwSymbol(symbol);
        
        log.debug("[API REQUEST] Symbol: {} | Groww Symbol: {} | Interval: {}", 
                symbol, growwSymbol, DEFAULT_CANDLE_INTERVAL);
        
        long requestStartTime = System.currentTimeMillis();
        
        try {
            // Call Grow API
            log.debug("Sending API request for: {}", growwSymbol);
            CandleResponseDTO candleResponse = growAPIClient.getCandleData(
                    growwSymbol,
                    DEFAULT_CANDLE_INTERVAL
            );
            
            long apiDuration = System.currentTimeMillis() - requestStartTime;
            log.debug("[API RESPONSE] Symbol: {} | Duration: {} ms | Status: {}", 
                    symbol, apiDuration, candleResponse != null ? candleResponse.getStatus() : "NULL");
            
            // Check if volume is significant
            if (candleResponse == null) {
                log.warn("[NO DATA] Symbol: {} | API returned null", symbol);
                return null;
            }
            
            if (!scripVolumeDataHelper.hasSignificantCurrentVolume(candleResponse)) {
                log.debug("[FILTERED] Symbol: {} | No significant volume detected", symbol);
                return null;
            }
            
            // Volume is significant - extract latest candle data
            CandleDTO latestCandle = getLatestCandle(candleResponse);
            if (latestCandle == null) {
                log.warn("[NO CANDLE] Symbol: {} | No candle data available", symbol);
                return null;
            }
            
            SimplifiedVolumeDataDTO response = new SimplifiedVolumeDataDTO(
                    symbol,
                    scripMaster.getSector(),
                    latestCandle.getVolume(),
                    latestCandle.getTimestamp()
            );
            log.info("[SUCCESS] Symbol: {} | Volume: {} | Timestamp: {}", 
                    symbol, latestCandle.getVolume(), latestCandle.getTimestamp());
            return response;
            
        } catch (Exception e) {
            long errorDuration = System.currentTimeMillis() - requestStartTime;
            log.error("[ERROR] Symbol: {} | Duration: {} ms | Exception: {}", 
                    symbol, errorDuration, e.getMessage(), e);
            return null;
        }
    }
    
    private CandleDTO getLatestCandle(CandleResponseDTO candleResponse) {
        if (candleResponse == null || candleResponse.getPayload() == null) {
            return null;
        }
        
        List<CandleDTO> candles = candleResponse.getPayload().getCandles();
        if (candles == null || candles.isEmpty()) {
            return null;
        }
        
        return candles.get(candles.size() - 1);
    }
}
