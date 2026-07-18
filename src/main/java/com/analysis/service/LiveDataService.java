package com.analysis.service;

import java.util.ArrayList;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.analysis.documents.BullishSignalSnapshot;
import com.analysis.dto.BullishSignalDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveDataService {

    private final MongoTemplate mongoTemplate;

    @Value("${analysis.bullish.min-score:4}")
    private int minBullishScore;

    public List<BullishSignalDTO> getBullishSignals() {
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "bullishSignal.bullishScore")
                .and(Sort.by(Sort.Direction.ASC, "symbol")));
        List<BullishSignalSnapshot> snapshots = mongoTemplate.find(query, BullishSignalSnapshot.class);
        if (snapshots.isEmpty()) {
            log.info("Bullish snapshots not available in Mongo yet. Returning empty list.");
            return new ArrayList<>();
        }

        List<BullishSignalDTO> response = snapshots.stream()
                .map(this::toBullishSignalResponse)
                .filter(java.util.Objects::nonNull)
                .filter(signal -> signal.getBullishScore() != null && signal.getBullishScore() >= minBullishScore)
                .toList();

        log.info("Serving {} bullish signals from Mongo per-symbol snapshots using minScore {}",
                response.size(),
                minBullishScore);
        return response;
    }

    private BullishSignalDTO toBullishSignalResponse(BullishSignalSnapshot snapshot) {
        if (snapshot == null || snapshot.getBullishSignal() == null) {
            return null;
        }

        BullishSignalDTO signal = snapshot.getBullishSignal();
        if (signal.getRefreshedAt() == null) {
            signal.setRefreshedAt(snapshot.getRefreshedAt());
        }
        return signal;
    }

}
