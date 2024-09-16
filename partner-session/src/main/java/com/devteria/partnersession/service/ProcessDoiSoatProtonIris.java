package com.devteria.partnersession.service;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devteria.partnersession.common.utils.TransactionControlUtils;
import com.devteria.partnersession.dto.response.ArgumentControlByDay;
import com.devteria.partnersession.model.IrisReport;
import com.devteria.partnersession.model.ProtonReport;
import com.devteria.partnersession.model.StbReport;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class ProcessDoiSoatProtonIris {
    private static final Logger log = LoggerFactory.getLogger(ProcessDoiSoatProtonIris.class);

    public void processDoiSoatProtonIris(
            List<StbReport> stbReports,
            List<IrisReport> irisReports,
            List<ProtonReport> protonReports,
            ArgumentControlByDay argumentControlByDayResponse) {

        List<ProtonReport> latestProtonReports = new ArrayList<>(
                TransactionControlUtils.getLatestProtonReports(protonReports).values());

        long irisCount = irisReports.size();
        long protonCount = latestProtonReports.size();

        // IN1 Tổng giao dịch Iris, Proton
        log.debug("Iris reports count: {}", irisCount);
        log.debug("Proton reports count: {}", protonCount);

        argumentControlByDayResponse.setIrisTotalCount(irisCount);
        argumentControlByDayResponse.setProtonTotalCount(protonCount);

        long irisSuccessCount = 0;
        long protonSuccessCount = 0;
        long irisFailCount = 0;
        long proErrorCount = 0;
        Set<String> proCodes = new HashSet<>();
        Set<String> irisSuccessCodes = new HashSet<>();
        List<IrisReport> proNotExistIrisSuc = new ArrayList<>();
        List<IrisReport> irisUnknownProtonNotExist = new ArrayList<>();
        Set<String> irisErrorCodes = new HashSet<>();
        List<ProtonReport> protonTimeoutIrisNotExist = new ArrayList<>();
        List<IrisReport> irisUnknown = new ArrayList<>();
        Set<String> irisCodes = new HashSet<>();

        Map<String, ProtonReport> latestProtonReportsMap =
                TransactionControlUtils.getLatestProtonReports(protonReports);
        List<StbReport> stbProtonNotExist = new ArrayList<>();
        for (int i = 0; i < stbReports.size(); i++) {
            StbReport stbReport = stbReports.get(i);
            String stbTraceCode = stbReport.getSystemTraceId();
            boolean foundMatch = false;

            ProtonReport protonReport = latestProtonReportsMap.get(stbTraceCode);
            if (protonReport != null) {
                foundMatch = true;
            }

            if (!foundMatch) {
                stbProtonNotExist.add(stbReport);
            }
        }
        argumentControlByDayResponse.setProtonNotExist(stbProtonNotExist);
        for (IrisReport irisReport : irisReports) {
            irisCodes.add(irisReport.getTraceCode());

            if (irisReport.getTopupStatus().equals("Thành công")) {
                irisSuccessCodes.add(irisReport.getTraceCode());
                irisSuccessCount++;
                // Giao dịch Proton không có, bên Iris Thành công
                boolean found = false;
                for (ProtonReport protonReport : latestProtonReports) {
                    if (irisReport.getTraceCode().equals(protonReport.getRequestIdToPartner())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {

                    proNotExistIrisSuc.add(irisReport);
                }
            }
            if (irisReport.getTopupStatus().equals("Thất bại")) {
                irisFailCount++;
                irisErrorCodes.add(irisReport.getTraceCode());
            }
            if (irisReport.getTopupStatus().equals("Không xác định")) {
                irisUnknown.add(irisReport);

                boolean found = false;
                for (ProtonReport protonReport : latestProtonReports) {
                    if (irisReport.getTraceCode().equals(protonReport.getRequestIdToPartner())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {

                    irisUnknownProtonNotExist.add(irisReport);
                }
            }
        }

        argumentControlByDayResponse.setIrisUnknownProtonNotExist(irisUnknownProtonNotExist);
        // IN2 Tổng giao dịch Iris thành công
        log.debug("Giao dịch thành công của Iris: " + irisSuccessCount);
        argumentControlByDayResponse.setIrisSuccessCount(irisSuccessCount);

        List<ProtonReport> sucProIrisNotExistCount = new ArrayList<>();
        List<ProtonReport> timeoutProIrisSuc = new ArrayList<>();
        List<IrisReport> irisSuctimeoutPro = new ArrayList<>();
        List<ProtonReport> proErrorIsDataTest = new ArrayList<>();

        long protonErrorCount = 0;
        long j = 0;
        List<ProtonReport> protonTimeoutReport = new ArrayList<>();
        for (ProtonReport protonReport : latestProtonReports) {
            proCodes.add(protonReport.getRequestIdToPartner());
            if (protonReport.getStatus().equals("Thành công")) {
                protonSuccessCount++;
                if (!irisSuccessCodes.contains(protonReport.getRequestIdToPartner())) {
                    sucProIrisNotExistCount.add(protonReport);
                }
            }
            if (protonReport.getStatus().equals("Timeout")) {
                protonTimeoutReport.add(protonReport);

                for (IrisReport irisReport : irisReports) {
                    if (protonReport.getRequestIdToPartner().equals(irisReport.getTraceCode())
                            && irisReport.getTopupStatus().equals("Thành công")) {
                        timeoutProIrisSuc.add(protonReport);
                        irisSuctimeoutPro.add(irisReport);
                    }
                }
            }
            if (protonReport.getStatus().equals("Lỗi")) {
                proErrorCount++;
            }
            if (protonReport.getStatus().equals("Lỗi")
                    && !irisErrorCodes.contains(protonReport.getRequestIdToPartner())) {
                proErrorIsDataTest.add(protonReport);

                protonErrorCount++;
            }
            if (protonReport.getStatus().equals("Timeout")
                    && !irisCodes.contains(protonReport.getRequestIdToPartner())) {
                protonReport.setStt(++j);
                protonTimeoutIrisNotExist.add(protonReport);
            }
        }
        argumentControlByDayResponse.setProtonTimeout(protonTimeoutReport);
        List<IrisReport> irisFailProtonNotExist = new ArrayList<>();
        for (IrisReport irisReport : irisReports) {
            if (irisReport.getTopupStatus().equals("Thất bại") && !proCodes.contains(irisReport.getTraceCode())) {
                irisFailProtonNotExist.add(irisReport);
            }
        }
        argumentControlByDayResponse.setIrisFailProtonNotExist(irisFailProtonNotExist);
        // IN Số giao dịch iris that bai nhung proton khong co
        if (!irisFailProtonNotExist.isEmpty()) {
            log.warn("cac giao dich can kiem tra lai tren web giao dich iris that bai nhung proton khong co");
            for (IrisReport irisReport : irisFailProtonNotExist) {
                log.info(String.valueOf(irisReport));
            }
        }
        // IN3 Tổng giao dịch Proton thành công
        log.debug("Giao dịch thành công của Proton: " + protonSuccessCount);
        argumentControlByDayResponse.setProtonSuccessCount(protonSuccessCount);

        /**5*/
        // IN4 giao dịch Proton thành công, Iris không có
        log.debug("Danh sách Giao dịch thành công bị lệnh : giao dịch Proton thành công, Iris không có : ");
        if (sucProIrisNotExistCount.isEmpty()) {
            log.debug("không có giao dịch Proton thành công, Iris không có");
        }
        for (ProtonReport protonReport : sucProIrisNotExistCount) {
            log.debug(String.valueOf(protonReport));
        }

        log.debug("Tổng số lượng giao dịch Proton thành công, Iris không có : " + sucProIrisNotExistCount.size());
        argumentControlByDayResponse.setSucProIrisNotExist(sucProIrisNotExistCount);
        /***/

        // IN5 Giao dịch Proton bị Timeout, bên Iris Thành công
        log.debug("Danh sách Giao dịch bị Timeout bên Proton và Thành công bên Iris: ");
        long i = 0;
        for (ProtonReport protonReport : timeoutProIrisSuc) {
            log.debug("Proton Report : " + protonReport);
            protonReport.setStt(++i);
        }
        for (IrisReport irisReport : irisSuctimeoutPro) log.debug("Iris Report : " + irisReport);

        argumentControlByDayResponse.setTimeoutProIrisSuc(timeoutProIrisSuc);
        argumentControlByDayResponse.setIrisSucTimeoutPro(irisSuctimeoutPro);
        /**1*/
        // IN6 Giao dịch Proton không có, bên Iris Thành công
        log.debug("Giao dịch có bên Iris, không có bên Proton: ");
        for (IrisReport irisReport : proNotExistIrisSuc) log.debug(String.valueOf(irisReport));

        argumentControlByDayResponse.setProNotExistIrisSuc(proNotExistIrisSuc);

        /***/

        // IN7 Giao dịch bên Iris Thất bại

        log.debug("Giao dịch thất bại của Iris: " + irisFailCount);
        argumentControlByDayResponse.setIrisFailCount(irisFailCount);

        // IN8 Giao dịch bên Proton Lỗi
        log.debug("Giao dịch lỗi của Proton: " + proErrorCount);
        argumentControlByDayResponse.setProErrorCount(proErrorCount);

        // IN9 Giao dịch bên Proton Lỗi nhưng iris không thất bại

        /**2*/
        // data test
        log.debug("Giao dịch lỗi bị lệnh la data test: ");
        i = 0;
        for (ProtonReport protonReport : proErrorIsDataTest) {
            log.debug(String.valueOf(protonReport));
            protonReport.setStt(++i);
        }
        log.debug("Tổng số giao dịch la data test: " + protonErrorCount);
        argumentControlByDayResponse.setProErrorIsDataTest(proErrorIsDataTest);
        /***/
        /**4*/
        // IN10 Giao dịch bên iris Không xác định
        log.info("Các giao dịch bị không xác định bên Iris : ");
        argumentControlByDayResponse.setIrisUnknown(irisUnknown);
        if (!irisUnknown.isEmpty()) {
            log.info("cac giao dich can kiem tra lai tren web Giao dịch bên Iris : ");
            for (IrisReport irisReport : irisUnknown) {
                log.info(String.valueOf(irisReport));
            }
            log.info("Tổng số lượng giao dịch không xác định : " + irisUnknown.size());
        } else {
            log.info("Không có giao dịch không xác định bên Iris.");
        }
        /***/

        /**3*/
        // IN11 Giao dịch bên proton Timeout

        log.info("Các giao dịch bị time out : ");
        if (!protonTimeoutIrisNotExist.isEmpty()) {

            log.info("cac giao dich can kiem tra lai tren web Giao dịch bên Proton : ");
            for (ProtonReport protonReport : protonTimeoutIrisNotExist) {
                log.info(String.valueOf(protonReport));
            }
            log.info("Tổng số lượng giao dịch timeout : " + protonTimeoutIrisNotExist.size());
        } else {
            log.info("không có giao dịch time out bên proton.");
        }
        argumentControlByDayResponse.setProtonTimeoutIrisNotExist(protonTimeoutIrisNotExist);
        /***/
    }
}
