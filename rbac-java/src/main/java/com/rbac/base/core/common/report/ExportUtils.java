package com.rbac.base.core.common.report;

import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ExportUtils {

    private ExportUtils() {
    }

    public static void write(String format, String fileName, List<String> headers, List<? extends List<?>> rows, HttpServletResponse response) throws IOException {
        String safeFormat = "csv".equalsIgnoreCase(format) ? "csv" : "xlsx";
        if ("csv".equals(safeFormat)) {
            writeCsv(fileName, headers, rows, response);
            return;
        }
        writeExcel(fileName, headers, rows, response);
    }

    private static void writeExcel(String fileName, List<String> headers, List<? extends List<?>> rows, HttpServletResponse response) throws IOException {
        response.reset();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", buildDisposition(fileName + ".xlsx"));
        try (ServletOutputStream outputStream = response.getOutputStream();
             ExcelWriter writer = ExcelUtil.getWriter(true)) {
            writer.writeRow(headers);
            for (List<?> row : rows) {
                writer.writeRow(new ArrayList<>(row));
            }
            writer.flush(outputStream, true);
        }
    }

    private static void writeCsv(String fileName, List<String> headers, List<? extends List<?>> rows, HttpServletResponse response) throws IOException {
        response.reset();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", buildDisposition(fileName + ".csv"));
        try (ServletOutputStream outputStream = response.getOutputStream();
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            outputStreamWriter.write('\uFEFF');
            CsvWriter writer = CsvUtil.getWriter(outputStreamWriter);
            writer.write(headers);
            for (List<?> row : rows) {
                writer.write(row.stream().map(item -> item == null ? "" : String.valueOf(item)).toList());
            }
            outputStreamWriter.flush();
        }
    }

    private static String buildDisposition(String fileName) {
        return "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
    }
}
