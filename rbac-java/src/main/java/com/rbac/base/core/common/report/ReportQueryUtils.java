package com.rbac.base.core.common.report;

import com.rbac.base.core.common.vo.TrendPointVO;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ReportQueryUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ReportQueryUtils() {
    }

    public static ReportRange resolveRange(String startDate, String endDate, Integer days, int defaultDays) {
        int safeDays = normalizeDays(days, defaultDays);
        if (StringUtils.hasText(startDate) && StringUtils.hasText(endDate)) {
            LocalDateTime start = parseStart(startDate);
            LocalDateTime end = parseEnd(endDate);
            if (start.isAfter(end)) {
                LocalDateTime temp = start;
                start = end;
                end = temp;
            }
            return new ReportRange(start, end, start.toLocalDate(), end.toLocalDate());
        }

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(safeDays - 1L);
        return new ReportRange(start.atStartOfDay(), end.atTime(LocalTime.MAX), start, end);
    }

    public static List<TrendPointVO> fillTrend(LocalDate startDate, LocalDate endDate, Map<LocalDate, Long> valueMap) {
        List<TrendPointVO> points = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            points.add(new TrendPointVO(cursor.toString(), valueMap.getOrDefault(cursor, 0L)));
            cursor = cursor.plusDays(1);
        }
        return points;
    }

    private static int normalizeDays(Integer days, int defaultDays) {
        if (days == null) {
            return defaultDays;
        }
        List<Integer> allowed = List.of(7, 15, 30, 90);
        return allowed.contains(days) ? days : defaultDays;
    }

    private static LocalDateTime parseStart(String value) {
        return parseDateTime(value, false);
    }

    private static LocalDateTime parseEnd(String value) {
        return parseDateTime(value, true);
    }

    private static LocalDateTime parseDateTime(String value, boolean endOfDay) {
        try {
            if (value.length() == 10) {
                LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
                return endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
            }
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("时间格式错误，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }
}
