package com.analysis.documents;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.analysis.constants.Constants;
import com.analysis.dto.BullishSignalDTO;
import com.analysis.dto.CandleResponseDTO;
import com.analysis.dto.LiveDataResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = Constants.BULLISH_SIGNALS_COLLECTION)
public class BullishSignalSnapshot {

    @Id
    private String id;
    private String symbol;
    private String sector;
    private LiveDataResponseDTO liveDataResponse;
    private CandleResponseDTO candleResponse;
    private BullishSignalDTO bullishSignal;
    private String scanStatus;
    private String scanError;
    private Instant refreshedAt;
}