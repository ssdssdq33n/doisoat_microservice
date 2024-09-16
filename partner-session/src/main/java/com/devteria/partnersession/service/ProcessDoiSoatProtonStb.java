package com.devteria.partnersession.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.devteria.partnersession.common.utils.TransactionControlUtils;
import com.devteria.partnersession.dto.response.ArgumentControlByDay;
import com.devteria.partnersession.model.IrisReport;
import com.devteria.partnersession.model.ProtonReport;
import com.devteria.partnersession.model.StbReport;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class ProcessDoiSoatProtonStb {
    @Value("${proton.refund-transaction.save.path}")
    @NonFinal
    private String refundTransactionSavePath;

    @Value("${proton.report.save.path}")
    @NonFinal
    private String basePath;

    private static final Logger log = LoggerFactory.getLogger(ProcessDoiSoatProtonStb.class);

    public void processDoiSoatProtonStb(
            List<StbReport> stbReports,
            List<ProtonReport> protonReports,
            List<IrisReport> irisReports,
            ArgumentControlByDay argumentControlByDayResponse,
            String protonReportFilePath)
            throws IOException {

        String[] folderParts = protonReportFilePath.split("\\\\");
        String folderPathFile = "\\" + folderParts[3] + "\\" + folderParts[5] + "\\" + folderParts[6];
        Path directoryPath = Paths.get(refundTransactionSavePath, folderParts[3], folderParts[5], folderParts[6]);

        Files.createDirectories(directoryPath);
        long stbCount = stbReports.size();
        Map<String, ProtonReport> latestProtonReportsMap =
                TransactionControlUtils.getLatestProtonReports(protonReports);
        List<ProtonReport> latestProtonReports = new ArrayList<>(latestProtonReportsMap.values());

        long protonCount = 0;
        List<String> protonSuc = new ArrayList<>();
        for (ProtonReport protonReport : latestProtonReports) {
            if (protonReport.getStatus().equals("Thành công")) {
                protonCount++;
                protonSuc.add(protonReport.getSystemTraceId());
            }
        }
        //
        List<String> protonSucStb = new ArrayList<>();
        for (StbReport stbReport : stbReports) {
            if (!protonSuc.contains(stbReport.getSystemTraceId())) {
                protonSucStb.add(stbReport.getSystemTraceId());
            }
        }
        //
        log.debug("Tổng số mã trace của STB: " + stbCount + " | Tổng số mã trace của Proton: " + protonCount);
        argumentControlByDayResponse.setStbTotalCount(stbCount);
        long differenceCount = Math.abs(stbCount - protonCount);
        argumentControlByDayResponse.setSubStbSucProtonSuc(differenceCount);
        log.debug("Tổng số lượng giao dịch không khớp nhau : " + differenceCount);

        List<StbReport> unmatchedStbReports = new ArrayList<>();
        Set<Integer> indexesToRemoveStb = new HashSet<>();
        Set<Integer> indexesToRemoveProton = new HashSet<>();
        Set<StbReport> errorProtonMatchedStbReports = new HashSet<>();
        List<StbReport> transactionsToReturn = new ArrayList<>();
        List<StbReport> IrisTransactionUnknown = new ArrayList<>();

        Set<StbReport> allRefundTransactions = new HashSet<>();
        Set<StbReport> allUnknownTransactions = new HashSet<>();

        for (int i = 0; i < stbReports.size(); i++) {
            StbReport stbReport = stbReports.get(i);
            String stbTraceCode = stbReport.getSystemTraceId();
            boolean foundMatch = false;

            ProtonReport protonReport = latestProtonReportsMap.get(stbTraceCode);
            if (protonReport != null) {

                foundMatch = true;
                if (protonReport.getStatus().equals("Lỗi")
                        && stbReport.getStatus().equals("THANH CONG")) {
                    stbReport.setStatus("HOAN TRA");
                    errorProtonMatchedStbReports.add(stbReport);
                    allRefundTransactions.add(stbReport);
                } else if (!protonReport.getStatus().equals("Lỗi")
                        || !stbReport.getStatus().equals("THANH CONG")) {
                    indexesToRemoveStb.add(i);
                }
            }

            if (!foundMatch) {
                if (argumentControlByDayResponse.getProNotExistIrisSuc().isEmpty()
                        && argumentControlByDayResponse
                                .getIrisUnknownProtonNotExist()
                                .isEmpty()) {
                    stbReport.setStatus("HOAN TRA");
                    allRefundTransactions.add(stbReport);
                } else {
                    stbReport.setStatus("Không xác định");
                    allUnknownTransactions.add(stbReport);
                }
                unmatchedStbReports.add(stbReport);
            }
        }
        if (!argumentControlByDayResponse.getProNotExistIrisSuc().isEmpty()) {

            log.info("cac giao dich can kiem tra lai tren web Giao dịch Proton không có, bên Iris Thành công ");
            for (IrisReport irisReport : argumentControlByDayResponse.getProNotExistIrisSuc()) {
                log.info(String.valueOf(irisReport));
            }
        }

        for (ProtonReport protonReport : latestProtonReportsMap.values()) {

            if (protonReport.getStatus().equals("Timeout")) {
                String protonTraceCode = protonReport.getSystemTraceId();
                boolean irisErrorFound = false;
                boolean irisIsUnknown = false;
                for (IrisReport irisReport : irisReports) {
                    if (irisReport.getTraceCode().equals(protonReport.getRequestIdToPartner())
                            && irisReport.getTopupStatus().equals("Thất bại")) {
                        irisErrorFound = true;
                        break;
                    }
                    if (irisReport.getTraceCode().equals(protonReport.getRequestIdToPartner())
                            && irisReport.getTopupStatus().equals("Không xác định")) {
                        irisIsUnknown = true;
                        break;
                    }
                }

                if (irisErrorFound) {
                    for (StbReport stbReport : stbReports) {
                        if (stbReport.getSystemTraceId().equals(protonTraceCode)
                                && stbReport.getStatus().equals("THANH CONG")) {
                            stbReport.setStatus("HOAN TRA");
                            transactionsToReturn.add(stbReport);
                            allRefundTransactions.add(stbReport);
                            break;
                        }
                    }
                }
                if (irisIsUnknown) {
                    for (StbReport stbReport : stbReports) {
                        if (stbReport.getSystemTraceId().equals(protonTraceCode)
                                && stbReport.getStatus().equals("THANH CONG")) {
                            stbReport.setStatus("Không xác định");
                            IrisTransactionUnknown.add(stbReport);
                            allUnknownTransactions.add(stbReport);
                            break;
                        }
                    }
                }
            }
        }
        for (ProtonReport protonReport : argumentControlByDayResponse.getProtonTimeoutIrisNotExist()) {
            for (StbReport stbReport : stbReports) {
                if (stbReport.getSystemTraceId().equals(protonReport.getSystemTraceId())
                        && stbReport.getStatus().equals("THANH CONG")) {
                    stbReport.setStatus("Không xác định");
                    transactionsToReturn.add(stbReport);
                    allUnknownTransactions.add(stbReport);
                    break;
                }
            }
        }

        List<Integer> sortedIndexesToRemoveStb = new ArrayList<>(indexesToRemoveStb);
        sortedIndexesToRemoveStb.sort(Collections.reverseOrder());
        for (int index : sortedIndexesToRemoveStb) {
            stbReports.remove(index);
        }

        List<Integer> sortedIndexesToRemoveProton = new ArrayList<>(indexesToRemoveProton);
        sortedIndexesToRemoveProton.sort(Collections.reverseOrder());
        for (int index : sortedIndexesToRemoveProton) {
            protonReports.remove(index);
        }

        List<StbReport> allTransactions = new ArrayList<>();

        allTransactions.addAll(errorProtonMatchedStbReports);

        allTransactions.addAll(unmatchedStbReports);

        allTransactions.addAll(transactionsToReturn);
        allTransactions.addAll(IrisTransactionUnknown);
        log.debug("Danh sách giao dịch cần hoàn trả: ");
        long i = 0;
        for (StbReport transaction : allTransactions) {
            log.debug(String.valueOf(transaction));
            transaction.setStt(++i);
        }
        argumentControlByDayResponse.setListTranNeedRefunded(allTransactions);

        long totalTransaction = allTransactions.size();
        log.debug("Tổng số giao dịch cần hoàn trả mong muốn: " + totalTransaction);
        argumentControlByDayResponse.setTranNeedRefundedTotalCount(totalTransaction);
        ExcelExporter.exportListToExcel(
                allTransactions,
                refundTransactionSavePath + "\\" + folderPathFile + "\\HoanTraATOMI.xlsx",
                "HoanTraATOMI_filetong");
        ExcelExporter.exportListToExcelTestTimeout(
                allTransactions,
                refundTransactionSavePath + "\\" + folderPathFile + "\\HoanTraATOMIchecklaiweb.xlsx",
                "HoanTraATOMI_filetong");
        List<StbReport> transHavePayAcc = new ArrayList<>();
        List<StbReport> transHaveAccVND = new ArrayList<>();

        long j = 0, k = 0;
        for (StbReport transaction : allTransactions) {
            String accountNumber = transaction.getRecordedAccountNumber();

            if (accountNumber.startsWith("'01")) {
                transaction.setStt(++k);
                transHaveAccVND.add(transaction);
                continue;
            }
            if (accountNumber.startsWith("'VND")) {
                transaction.setStt(++k);
                transHaveAccVND.add(transaction);
                continue;
            }
            transaction.setStt(++j);
            transHavePayAcc.add(transaction);
        }
        argumentControlByDayResponse.setTransHaveAccVND(transHaveAccVND);
        argumentControlByDayResponse.setTransHavePayAcc(transHavePayAcc);
        long VNDCount = 0;

        if (!transHaveAccVND.isEmpty()) {
            log.debug("Danh sách giao dịch có tài khoản VND : ");
            for (StbReport transaction : transHaveAccVND) {
                log.debug(String.valueOf(transaction));
                VNDCount++;
            }
            log.debug("Tổng số lượng : " + VNDCount);
            ExcelExporter.exportListToExcel(
                    transHaveAccVND,
                    refundTransactionSavePath + "\\" + folderPathFile + "\\HoanTraATOMI_VND.xlsx",
                    "HoanTraAtomi_VND");
        }
        long paymentAccountCount = 0;
        if (!transHavePayAcc.isEmpty()) {
            log.debug("Danh sách có tài khoản thanh toán: ");
            for (StbReport transaction : transHavePayAcc) {
                log.debug(String.valueOf(transaction));
                paymentAccountCount++;
            }
            log.debug("Tổng số lượng : " + paymentAccountCount);
            ExcelExporter.exportListToExcel(
                    transHavePayAcc,
                    refundTransactionSavePath + "\\" + folderPathFile + "\\HoanTraATOMI_0.xlsx",
                    "HoanTraAtomi_0");
        }
        argumentControlByDayResponse.setAllRefundTransactions(allRefundTransactions);
        argumentControlByDayResponse.setAllUnknownTransactions(allUnknownTransactions);
    }
}
