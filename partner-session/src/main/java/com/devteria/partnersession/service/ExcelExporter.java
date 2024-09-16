package com.devteria.partnersession.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devteria.partnersession.model.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class ExcelExporter {
    private static final Logger log = LoggerFactory.getLogger(ExcelExporter.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void exportToExcel(List<StbReport> stbReports, String filePath1, String filePath2) {
        List<StbReport> filteredReports = new ArrayList<>();
        List<StbReport> remainingReports = new ArrayList<>();

        for (StbReport report : stbReports) {
            String recordedAccountNumber = report.getRecordedAccountNumber();
            if (recordedAccountNumber.startsWith("VND") || recordedAccountNumber.startsWith("01")) {
                filteredReports.add(report);
            } else {
                remainingReports.add(report);
            }
        }
        exportListToExcel(filteredReports, filePath1, "HoanTraAtomi_filtered");
        exportListToExcel(remainingReports, filePath2, "HoanTraAtomi_Remaining");
    }

    public static void exportListToExcel(List<StbReport> stbReports, String filePath, String sheetName) {
        Workbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet(sheetName);

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "STT",
                "Số giao dịch ID",
                "Ngày giao dịch",
                "Ngày hiệu lực",
                "Số REF",
                "Diễn giải",
                "Số tiền giao dịch",
                "Số tài khoản ghi có",
                "Số tài khoản ghi nợ",
                "Mã tài khoản KH",
                "Kênh giao dịch",
                "Trạng thái"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            long sum = 0;
            for (StbReport stbReport : stbReports) {
                Row row = sheet.createRow(rowNum);
                row.createCell(0).setCellValue(rowNum);
                row.createCell(1).setCellValue(stbReport.getTransactionNumberId());
                row.createCell(2).setCellValue(stbReport.getCreateTime());
                row.createCell(3).setCellValue(stbReport.getEffectiveDate());
                row.createCell(4).setCellValue(stbReport.getSystemTraceId());
                row.createCell(5).setCellValue(stbReport.getExplain());
                row.createCell(6).setCellValue(stbReport.getAmount());
                row.createCell(7).setCellValue(stbReport.getRecordedAccountNumber());
                row.createCell(8).setCellValue(stbReport.getDebitAccountNumber());
                row.createCell(9).setCellValue(stbReport.getCustomAccountCode());
                row.createCell(10).setCellValue(stbReport.getTransactionChannel());
                row.createCell(11).setCellValue(stbReport.getStatus());

                String status = stbReport.getStatus();
                if ("THANH CONG".equals(status)) {
                    status = "HOAN TRA";
                }
                row.createCell(11).setCellValue(status);

                sum += stbReport.getAmount();

                rowNum++;
            }
            Row sumRow = sheet.createRow(rowNum);
            sumRow.createCell(0).setCellValue("Tổng cộng : ");
            sumRow.createCell(6).setCellValue(sum);
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            File file = new File(filePath);
            try (FileOutputStream fileOut =
                    !file.exists() ? new FileOutputStream(file) : new FileOutputStream(file, false)) {
                workbook.write(fileOut);
                log.info("xuất file thành công: " + filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void exportListToExcelTestTimeout(List<StbReport> stbReports, String filePath, String sheetName) {
        Workbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet(sheetName);

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "TRANS_ID",
                "CONSUMER_USER",
                "TRANSACTION_DATE",
                "TRANS_NAME",
                "INPUT_VALUE",
                "INPUT_AMOUNT",
                "PAYMENT_REF_NO",
                "PARTNER_CODE"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            long sum = 0;
            if (stbReports.isEmpty()) {
                return;
            }
            LocalDateTime date =
                    LocalDateTime.parse(stbReports.getFirst().getCreateTime().replace("'", ""), dateTimeFormatter);
            for (StbReport stbReport : stbReports) {
                Row row = sheet.createRow(rowNum);
                row.createCell(0).setCellValue("1520520");
                row.createCell(1).setCellValue("stb_pg_prod");

                if (rowNum == 1) {
                    row.createCell(2).setCellValue(date.minusDays(3).format(dateTimeFormatter));
                } else if (rowNum == stbReports.size()) {
                    row.createCell(2).setCellValue(date.plusDays(3).format(dateTimeFormatter));
                } else {
                    LocalTime currentTime = LocalTime.now();

                    // Định dạng thời gian dưới dạng "00:00:00"
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                    String formattedTime = currentTime.format(formatter);
                    row.createCell(2).setCellValue(date.format(dateTimeFormatter));
                }
                row.createCell(3).setCellValue("CC1520520");
                row.createCell(4).setCellValue("20000");
                row.createCell(5).setCellValue("20000");
                row.createCell(6).setCellValue(stbReport.getSystemTraceId());
                row.createCell(7).setCellValue("IRIS");

                String status = stbReport.getStatus();
                if ("THANH CONG".equals(status)) {
                    status = "HOAN TRA";
                }

                sum += stbReport.getAmount();

                rowNum++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            File file = new File(filePath);
            try (FileOutputStream fileOut =
                    !file.exists() ? new FileOutputStream(file) : new FileOutputStream(file, false)) {
                workbook.write(fileOut);
                log.info("xuất file thành công: " + filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String extractEWCode(String input) {
        if (input != null) {
            Pattern pattern = Pattern.compile("\\bEW\\w+");
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return " ";
    }

    public static void exportTongThangReportsToExcel(
            List<TongThangReport> tongThangReports, String filePath, String sheetName) {
        Workbook workbook = new SXSSFWorkbook(50000);
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat outputTimeFormat = new SimpleDateFormat("HH:mm:ss");
        try {
            List<TongThangReport> sortedReports = tongThangReports.stream()
                    .filter(report -> !report.getStatus().equals("Lỗi"))
                    .sorted((report1, report2) -> {
                        try {
                            Date date1 = inputDateFormat.parse(report1.getTimeCreated());
                            Date date2 = inputDateFormat.parse(report2.getTimeCreated());
                            return date2.compareTo(date1); // Sắp xếp giảm dần
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "STT",
                "BÚT TOÁN FT/Số giao dịch ID",
                "NGÀY GD",
                " THỜI GIAN GD ",
                "SỐ TIỀN",
                "Mã tài khoản KH",
                "Số tài khoản ghi có",
                "Diễn giải",
                "TRACENO",
                "SERVICEPROVIDER",
                " PAYMENTSERVICE",
                "Trạng thái",
                "Mã Trace",
                "Mã giao dịch"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            int index = 1;
            for (TongThangReport report : sortedReports) {
                String status = report.getStatus();
                String explain = report.getExplain();
                String ewCode = extractEWCode(explain);

                Date dateTime = inputDateFormat.parse(report.getTimeCreated());
                String dateStr = outputDateFormat.format(dateTime);
                String timeStr = outputTimeFormat.format(dateTime);

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(index);
                row.createCell(1).setCellValue(report.getTransactionNumberId());
                row.createCell(2).setCellValue(dateStr);
                row.createCell(3).setCellValue(timeStr);
                row.createCell(4).setCellValue(report.getAmount());
                row.createCell(5).setCellValue(report.getCustomAccountCode());
                row.createCell(6).setCellValue(report.getRecordedAccountNumber());
                row.createCell(7).setCellValue(report.getExplain());
                row.createCell(8).setCellValue(ewCode);
                row.createCell(9).setCellValue(report.getServiceProvider());
                row.createCell(10).setCellValue(report.getPaymentService());

                if (status.equals("Thành công")) {
                    row.createCell(11).setCellValue(report.getStatus());
                }
                index++;
                row.createCell(12).setCellValue(report.getSystemTraceId());
                row.createCell(13).setCellValue(report.getTraceCode());
            }
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
                log.info("FIle Excel đã được tạo thành công tại : " + filePath);
            } catch (IOException e) {
                e.printStackTrace();
                log.error("Lỗi khi ghi file : " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Lỗi khi tạo file : " + e.getMessage());
            }

        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
                log.error("Lỗi khi đóng workbook : " + e.getMessage());
            }
        }
    }

    public static void exportTongThangStbProtonIris(
            List<TongThangStbProtonIris> reports, String filePath, String sheetName) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet(sheetName);
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat outputTimeFormat = new SimpleDateFormat("HH:mm:ss");

        String[] headers = {
            "STT",
            "Thời gian giao dịch",
            "Số tiền",
            "Mã tài khoản KH",
            "Đối tác bán hàng",
            "Đối tác cung cấp",
            "Mã giao dịch",
            "Mã trace",
            "Trạng thái",
            "Lý do không khớp"
        };
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // Sắp xếp danh sách theo ngày giao dịch từ cuối tháng đến đầu tháng
        List<TongThangStbProtonIris> sortedReports = reports.stream()
                .sorted((report1, report2) -> {
                    try {
                        String timeCreated1 =
                                report1.getTimeCreated().replace("'", "").trim();
                        String timeCreated2 =
                                report2.getTimeCreated().replace("'", "").trim();
                        // System.out.println("Parsing date: " + timeCreated1);
                        Date date1 = inputDateFormat.parse(timeCreated1);
                        Date date2 = inputDateFormat.parse(timeCreated2);
                        return date2.compareTo(date1); // Sắp xếp giảm dần
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return 0; // Nếu có lỗi phân tích ngày, giữ nguyên thứ tự
                    }
                })
                .collect(Collectors.toList());

        int rowNum = 1;
        for (TongThangStbProtonIris report : sortedReports) {
            try {
                String timeCreated = report.getTimeCreated().replace("'", "").trim();
                // System.out.println("Parsing date for row: " + rowNum + " - " + timeCreated);
                Date dateTime = inputDateFormat.parse(timeCreated);
                String dateStr = outputDateFormat.format(dateTime);
                String timeStr = outputTimeFormat.format(dateTime);

                Row row = sheet.createRow(rowNum);
                row.createCell(0).setCellValue(rowNum);
                row.createCell(1).setCellValue(dateStr + " " + timeStr);
                row.createCell(2).setCellValue(report.getAmount());
                row.createCell(3).setCellValue(report.getCustomAccountCode());
                row.createCell(4).setCellValue(report.getSalesPartner());
                row.createCell(5).setCellValue(report.getSupplyPartner());
                row.createCell(6).setCellValue(report.getRequestIdToPartner());
                row.createCell(7).setCellValue(report.getSystemTraceId());
                row.createCell(8).setCellValue(report.getStatus());
                row.createCell(9).setCellValue(report.getReasons());
                rowNum++;
            } catch (ParseException e) {
                e.printStackTrace();
                System.err.println("Lỗi phân tích ngày tháng cho báo cáo: " + report.getTimeCreated());
            }
        }

        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
            log.info("FIle Excel đã được tạo thành công tại : " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void exportTongThangReportsToExcelByBatch(
            List<TongThangReport> tongThangReports, String filePath, String sheetName, boolean createHeader) {
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat outputTimeFormat = new SimpleDateFormat("HH:mm:ss");
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8)) {
            String[] headers = {
                "STT",
                "BÚT TOÁN FT/Số giao dịch ID",
                "NGÀY GD",
                "THỜI GIAN GD",
                "SỐ TIỀN",
                "Mã tài khoản KH",
                "Số tài khoản ghi có",
                "Diễn giải",
                "TRACENO",
                "SERVICEPROVIDER",
                "PAYMENTSERVICE",
                "Trạng thái"
            };
            for (int i = 0; i < headers.length; i++) {
                writer.append(escapeSpecialCharacters(headers[i]));
                if (i < headers.length - 1) {
                    writer.append(",");
                }
            }
            writer.append("\n");
            int index = 1;
            for (TongThangReport report : tongThangReports) {
                String status = report.getStatus();
                if (!status.equals("Lỗi")) {
                    String explain = report.getExplain();
                    String ewCode = extractEWCode(explain);

                    Date dateTime = inputDateFormat.parse(report.getTimeCreated());
                    String dateStr = outputDateFormat.format(dateTime);
                    String timeStr = outputTimeFormat.format(dateTime);

                    writer.append(String.valueOf(index)).append(",");
                    writer.append(escapeSpecialCharacters(report.getTransactionNumberId()))
                            .append(",");
                    writer.append(escapeSpecialCharacters(dateStr)).append(",");
                    writer.append(escapeSpecialCharacters(timeStr)).append(",");
                    writer.append(String.valueOf(report.getAmount())).append(",");
                    writer.append(escapeSpecialCharacters(report.getCustomAccountCode()))
                            .append(",");
                    writer.append(escapeSpecialCharacters(report.getRecordedAccountNumber()))
                            .append(",");
                    writer.append(escapeSpecialCharacters(report.getExplain())).append(",");
                    writer.append(escapeSpecialCharacters(ewCode)).append(",");
                    writer.append(escapeSpecialCharacters(report.getServiceProvider()))
                            .append(",");
                    writer.append(escapeSpecialCharacters(report.getPaymentService()))
                            .append(",");

                    if (status.equals("Thành công")) {
                        writer.append(escapeSpecialCharacters(report.getStatus()));
                    }
                    writer.append("\n");
                    index++;
                }
            }
            System.out.println("File đã được tạo thành công tại: " + filePath);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            System.out.println("Lỗi khi ghi file: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Lỗi khi tạo file: " + e.getMessage());
        }
    }

    // Phương thức để escape các ký tự đặc biệt trong trường dữ liệu
    private static String escapeSpecialCharacters(String data) {
        // Nếu data chứa dấu phẩy, hoặc ký tự "
        if (data.contains(",") || data.contains("\"")) {
            // Đặt data trong dấu nháy kép và thay thế ký tự " bằng ""
            return "\"" + data.replaceAll("\"", "\"\"") + "\"";
        }
        return data;
    }

    public static void exportProMonthReportsToExcel(
            List<ProtonReport> proMonthReports, String filePath, String sheetName) {
        Workbook workbook = new SXSSFWorkbook(50000);
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat outputTimeFormat = new SimpleDateFormat("HH:mm:ss");
        try {
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "STT",
                "BÚT TOÁN FT/Số giao dịch ID",
                "NGÀY GD",
                " THỜI GIAN GD ",
                "SỐ TIỀN",
                "Mã tài khoản KH",
                "Số tài khoản ghi có",
                "Diễn giải",
                "TRACENO",
                "SERVICEPROVIDER",
                " PAYMENTSERVICE",
                "Trạng thái",
                "Mã Trace",
                "Request ID"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            int index = 1;
            for (ProtonReport report : proMonthReports) {
                String status = report.getStatus();
                if (!status.equals("Lỗi")) {}
            }
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
                log.info("FIle Excel đã được tạo thành công tại : " + filePath);
            } catch (IOException e) {
                e.printStackTrace();
                log.error("Lỗi khi ghi file : " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Lỗi khi tạo file : " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
                log.error("Lỗi khi đóng workbook : {}", e.getMessage());
            }
        }
    }

    public static void exportIrisMonthReportsToExcel(
            List<IrisReport> irisMonthReports, String filePath, String proton) {}

    public static <T> void writeToExcel(List<T> objects, String[] headers, String filePath, String sheetName) {
        try {
            // Tạo workbook mới
            Workbook workbook = new SXSSFWorkbook(50000);

            // Tạo sheet mới
            Sheet sheet = workbook.createSheet(sheetName);

            // Ghi header
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            Field[] fields = objects.get(0).getClass().getDeclaredFields();
            // Ghi dữ liệu
            int rowNum = 1;
            for (T obj : objects) {
                if (obj == null) return;
                Row row = sheet.createRow(rowNum++);
                int cellNum = 0;
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value != null) row.createCell(cellNum++).setCellValue(value.toString());
                }
            }

            // Ghi file ra
            FileOutputStream fileOut = new FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
            log.info("xuất file đối soát tháng ProIris thành công tại" + filePath);
            workbook.close();
        } catch (IOException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
