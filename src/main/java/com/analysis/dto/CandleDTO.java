package com.analysis.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CandleDTO {

    private static final DateTimeFormatter SPACE_SEPARATED_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LocalDateTime timestamp;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private Long volume;
    private Long openInterest;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public CandleDTO(List<Object> values) {
        if (values == null || values.size() < 7) {
            throw new IllegalArgumentException("Invalid candle payload");
        }

        this.timestamp = parseTimestamp(values.get(0));
        this.openPrice = toBigDecimal(values.get(1));
        this.highPrice = toBigDecimal(values.get(2));
        this.lowPrice = toBigDecimal(values.get(3));
        this.closePrice = toBigDecimal(values.get(4));
        this.volume = toLong(values.get(5));
        this.openInterest = toLong(values.get(6));
    }

    private static LocalDateTime parseTimestamp(Object value) {
        if (value == null) {
            return null;
        }

        String timestampValue = value.toString();
        try {
            return LocalDateTime.parse(timestampValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(timestampValue, SPACE_SEPARATED_DATE_TIME);
        }
    }

    private static BigDecimal toBigDecimal(Object value) {
        return value == null ? null : new BigDecimal(value.toString());
    }

    private static Long toLong(Object value) {
        return value == null ? null : Long.valueOf(value.toString());
    }
}
