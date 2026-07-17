package com.analysis.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.analysis.documents.BullishSignalSnapshot;
import com.analysis.dto.CandleDTO;
import com.analysis.dto.SimplifiedVolumeDataDTO;
import com.analysis.helper.ScripVolumeDataHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScripVolumeService {

    private final MongoTemplate mongoTemplate;
    private final ScripVolumeDataHelper scripVolumeDataHelper;

    public List<SimplifiedVolumeDataDTO> getAllScripVolumes() {
        log.info("Starting getAllScripVolumes processing from Mongo snapshots");
        long processStartTime = System.currentTimeMillis();

        Query query = new Query().with(Sort.by(Sort.Direction.ASC, "symbol"));
        List<BullishSignalSnapshot> snapshots = mongoTemplate.find(query, BullishSignalSnapshot.class);

        List<SimplifiedVolumeDataDTO> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        int filteredCount = 0;

        for (BullishSignalSnapshot snapshot : snapshots) {
            try {
                SimplifiedVolumeDataDTO volumeData = buildVolumeData(snapshot);
                if (volumeData != null) {
                    results.add(volumeData);
                    successCount++;
                } else {
                    filteredCount++;
                }
            } catch (Exception e) {
                failureCount++;
                log.warn("Failed to build volume data for symbol {}: {}", snapshot.getSymbol(), e.getMessage());
            }
        }

        long totalDuration = System.currentTimeMillis() - processStartTime;
        log.info("Completed getAllScripVolumes from Mongo. totalSnapshots: {}, totalDurationMs: {}, totalResults: {}, success: {}, filtered: {}, failure: {}",
                snapshots.size(),
                totalDuration,
                results.size(),
                successCount,
                filteredCount,
                failureCount);

        return results;
    }

    private SimplifiedVolumeDataDTO buildVolumeData(BullishSignalSnapshot snapshot) {
        if (snapshot == null || !Objects.equals("SUCCESS", snapshot.getScanStatus())) {
            return null;
        }

        if (snapshot.getCandleResponse() == null) {
            return null;
        }

        if (!scripVolumeDataHelper.hasSignificantCurrentVolume(snapshot.getCandleResponse())) {
            return null;
        }

        CandleDTO latestCandle = getLatestCandle(snapshot);
        if (latestCandle == null) {
            return null;
        }

        double avgPreviousFiveVolume = scripVolumeDataHelper.getAveragePreviousFiveVolume(snapshot.getCandleResponse());

        return new SimplifiedVolumeDataDTO(
                snapshot.getSymbol(),
                snapshot.getSector(),
                latestCandle.getVolume(),
                avgPreviousFiveVolume,
                latestCandle.getTimestamp(),
                latestCandle.getOpenPrice(),
                latestCandle.getHighPrice(),
                latestCandle.getLowPrice(),
                latestCandle.getClosePrice()
        );
    }

    private CandleDTO getLatestCandle(BullishSignalSnapshot snapshot) {
        if (snapshot == null || snapshot.getCandleResponse() == null || snapshot.getCandleResponse().getPayload() == null) {
            return null;
        }

        List<CandleDTO> candles = snapshot.getCandleResponse().getPayload().getCandles();
        if (candles == null || candles.isEmpty()) {
            return null;
        }

        return candles.get(candles.size() - 1);
    }
}
