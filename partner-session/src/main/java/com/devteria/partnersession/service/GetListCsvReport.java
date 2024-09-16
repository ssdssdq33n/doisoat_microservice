package com.devteria.partnersession.service;

import static java.awt.SystemColor.control;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devteria.partnersession.Constants.Constant;
import com.devteria.partnersession.model.ProtonReport;
import com.devteria.partnersession.model.StbReport;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class GetListCsvReport {
    private static final Logger log = LoggerFactory.getLogger(GetListCsvReport.class);

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // function overloading
    public void getListCsvReport(
            String stbReportFilePath,
            List<StbReport> stbReports,
            String protonReportFilePath,
            List<ProtonReport> protonReports)
            throws IOException {
        getListProCsvReport(protonReportFilePath, protonReports, Constant.DS_TYPE.MONTH);
        getListStbCsvReport(stbReportFilePath, stbReports, Constant.DS_TYPE.MONTH);
    }

    public void getListStbCsvReport(String stbReportFilePath, List<StbReport> stbReports, int type) throws IOException {
        log.info("doc file csv: {}", stbReportFilePath);
        List<String> line;
        // String[] dateControl = control.split("/");
        File f = new File(stbReportFilePath);
        line = FileUtils.readLines(f, StandardCharsets.UTF_8);
        if (line.isEmpty()) {
            return;
        }
        for (String l : line) {
            String[] split = l.split(",");
            if (split.length < 12) {
                log.warn("Dinh dang dong khong hop le : {}", line);
                continue;
            }
            StbReport stbReport = new StbReport();
            //
            //            if (type == Constant.DS_TYPE.MONTH) {
            //                String dateStr = split[2].substring(1);
            //                LocalDateTime stbTransDate = LocalDateTime.parse(dateStr, dateTimeFormatter);
            //
            //                int transactionMonth = stbTransDate.getMonthValue();
            //                int transactionYear = stbTransDate.getYear();
            //
            //                if (transactionMonth != Integer.parseInt(dateControl[0])) {
            //                    continue;
            //                }
            //            }
            // stbReport.setSTT(split[0]);
            stbReport.setTransactionNumberId(split[1]);
            stbReport.setCreateTime(split[2]);
            stbReport.setEffectiveDate(split[3]);
            stbReport.setSystemTraceId(split[4]);
            stbReport.setExplain(split[5]);
            stbReport.setAmount(NumberUtils.toLong(split[6]));
            stbReport.setRecordedAccountNumber(split[7]);
            stbReport.setDebitAccountNumber(split[8]);
            stbReport.setCustomAccountCode(split[9]);
            stbReport.setTransactionChannel(split[10]);
            stbReport.setStatus(split[11]);

            stbReports.add(stbReport);
        }
    }

    public void getListStbCsvReportMonth(String stbReportFilePath, List<StbReport> stbReports, String control, int type)
            throws IOException {
        log.info("doc file csv: {}", stbReportFilePath);
        List<String> line;
        String[] dateControl = control.split("/");
        File f = new File(stbReportFilePath);
        line = FileUtils.readLines(f, StandardCharsets.UTF_8);
        if (line.isEmpty()) {
            return;
        }
        for (String l : line) {
            String[] split = l.split(",");
            if (split.length < 12) {
                log.warn("Dinh dang dong khong hop le : {}", line);
                continue;
            }
            StbReport stbReport = new StbReport();

            if (type == Constant.DS_TYPE.MONTH) {
                String dateStr = split[2].substring(1);
                LocalDateTime stbTransDate = LocalDateTime.parse(dateStr, dateTimeFormatter);

                int transactionMonth = stbTransDate.getMonthValue();
                int transactionYear = stbTransDate.getYear();

                if (transactionMonth != Integer.parseInt(dateControl[0])) {
                    continue;
                }
            }
            stbReport.setTransactionNumberId(split[1]);
            stbReport.setCreateTime(split[2]);
            stbReport.setEffectiveDate(split[3]);
            stbReport.setSystemTraceId(split[4]);
            stbReport.setExplain(split[5]);
            stbReport.setAmount(NumberUtils.toLong(split[6]));
            stbReport.setRecordedAccountNumber(split[7]);
            stbReport.setDebitAccountNumber(split[8]);
            stbReport.setCustomAccountCode(split[9]);
            stbReport.setTransactionChannel(split[10]);
            stbReport.setStatus(split[11]);

            stbReports.add(stbReport);
        }
    }

    public void getListProCsvReport(String protonReportFilePath, List<ProtonReport> protonReports, int type)
            throws IOException {
        log.info("doc file csv: {} ", protonReportFilePath);
        List<String> line;
        File folder = new File(protonReportFilePath);
        File[] files = folder.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile()) {
                    line = FileUtils.readLines(new File(file.getAbsolutePath()), StandardCharsets.UTF_8);
                    if (line.isEmpty()) {
                        return;
                    }
                    for (String l : line) {
                        String[] split = l.split("\t");
                        if ("Tên dịch vụ".equals(split[0])) {
                            continue;
                        }
                        ProtonReport protonReport = new ProtonReport();

                        protonReport.setServiceCode(split[1]);
                        protonReport.setTimeCreated(split[2]);
                        protonReport.setSupplyPartner(split[3]);
                        protonReport.setAmount(NumberUtils.toLong(split[4].replace(",", ""), 0));
                        protonReport.setSl(split[5]);
                        protonReport.setTraceId(split[6]);
                        protonReport.setSystemTraceId(split[7]);
                        protonReport.setRequestIdToPartner(split[8]);
                        protonReport.setStatus(split[9]);

                        protonReports.add(protonReport);
                    }
                }
            }
        } else {
            //                throw new AppException(ErrorCode.PROTON_FILE_NOT_FOUND);
        }
    }

    public void getListCsvReportMonth(
            String stbReportFilePath,
            List<StbReport> stbReports,
            String protonReportFilePath,
            List<ProtonReport> protonReports,
            int type,
            String control)
            throws IOException {
        log.info("doc file csv: {}", stbReportFilePath);
        List<String> line;
        try {
            String[] dateControl = control.split("/");
            File f = new File(stbReportFilePath);
            line = FileUtils.readLines(f, StandardCharsets.UTF_8);
            if (line.isEmpty()) {
                return;
            }
            for (String l : line) {

                try {
                    String[] split = l.split(",");
                    if (split.length < 12) {
                        log.warn("Dinh dang dong khong hop le : {}", line);
                        continue;
                    }
                    StbReport stbReport = new StbReport();

                    if (type == Constant.DS_TYPE.MONTH) {
                        String dateStr = split[2].substring(1);
                        LocalDateTime stbTransDate = LocalDateTime.parse(dateStr, dateTimeFormatter);

                        int transactionMonth = stbTransDate.getMonthValue();
                        int transactionYear = stbTransDate.getYear();

                        if (transactionMonth != Integer.parseInt(dateControl[0])) {
                            continue;
                        }
                    }

                    // stbReport.setSTT(split[0]);
                    stbReport.setTransactionNumberId(split[1]);
                    stbReport.setCreateTime(split[2]);
                    stbReport.setEffectiveDate(split[3]);
                    stbReport.setSystemTraceId(split[4]);
                    stbReport.setExplain(split[5]);
                    stbReport.setAmount(NumberUtils.toLong(split[6]));
                    stbReport.setRecordedAccountNumber(split[7]);
                    stbReport.setDebitAccountNumber(split[8]);
                    stbReport.setCustomAccountCode(split[9]);
                    stbReport.setTransactionChannel(split[10]);
                    stbReport.setStatus(split[11]);

                    stbReports.add(stbReport);
                } catch (DateTimeParseException e) {
                    log.error("Dinh dang ngay khong hop le: {}", line, e);
                } catch (Exception e) {
                    log.error("Loi phan tich dong : {}", line, e);
                }
            }
        } catch (FileNotFoundException e) {
            //            throw new AppException(ErrorCode.SACOMBANK_FILE_NOT_FOUND);
            e.printStackTrace();
        }
        log.info("doc file csv: {} ", protonReportFilePath);
        File folder = new File(protonReportFilePath);
        File[] files = folder.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile()) {
                    line = FileUtils.readLines(new File(file.getAbsolutePath()), StandardCharsets.UTF_8);
                    if (line.isEmpty()) {
                        return;
                    }
                    for (String l : line) {
                        String[] split = l.split("\t");
                        if ("Tên dịch vụ".equals(split[0])) {
                            continue;
                        }
                        ProtonReport protonReport = new ProtonReport();

                        protonReport.setServiceCode(split[1]);
                        protonReport.setTimeCreated(split[2]);
                        protonReport.setAmount(NumberUtils.toLong(split[4].replace(",", ""), 0));
                        protonReport.setSl(split[5]);
                        protonReport.setTraceId(split[6]);
                        protonReport.setSystemTraceId(split[7]);
                        protonReport.setRequestIdToPartner(split[8]);
                        protonReport.setStatus(split[9]);

                        protonReports.add(protonReport);
                    }
                }
            }
        } else {
            //                throw new AppException(ErrorCode.PROTON_FILE_NOT_FOUND);
        }
    }
}
