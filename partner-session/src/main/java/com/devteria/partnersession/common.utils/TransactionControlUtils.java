package com.devteria.partnersession.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devteria.partnersession.model.ProtonReport;
import com.devteria.partnersession.service.ProcessDoiSoatProtonIris;

public class TransactionControlUtils {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(ProcessDoiSoatProtonIris.class);

    public static Map<String, ProtonReport> getLatestProtonReports(List<ProtonReport> protonReports) {
        Map<String, ProtonReport> latestProtonReportsMap = new HashMap<>();

        for (ProtonReport protonReport : protonReports) {
            String traceId = protonReport.getSystemTraceId();
            try {
                Date protonTimestamp = dateFormat.parse(protonReport.getTimeCreated());
                ProtonReport existingReport = latestProtonReportsMap.get(traceId);
                if (existingReport == null
                        || protonTimestamp.after(dateFormat.parse(existingReport.getTimeCreated()))) {
                    latestProtonReportsMap.put(traceId, protonReport);
                }
            } catch (ParseException e) {
                e.printStackTrace();
                log.error("Error parsing Proton report timestamp: {}", protonReport.getTimeCreated(), e);
            }
        }

        return latestProtonReportsMap;
    }

    public static Map<String, ProtonReport> getLatestIrisReports(List<ProtonReport> protonReports) {
        Map<String, ProtonReport> latestProtonReportsMap = new HashMap<>();

        for (ProtonReport protonReport : protonReports) {
            String traceId = protonReport.getRequestIdToPartner();
            try {
                Date protonTimestamp = dateFormat.parse(protonReport.getTimeCreated());
                ProtonReport existingReport = latestProtonReportsMap.get(traceId);
                if (existingReport == null
                        || protonTimestamp.after(dateFormat.parse(existingReport.getTimeCreated()))) {
                    latestProtonReportsMap.put(traceId, protonReport);
                }
            } catch (ParseException e) {
                e.printStackTrace();
                log.error("Error parsing Proton report timestamp: {}", protonReport.getTimeCreated(), e);
            }
        }

        return latestProtonReportsMap;
    }
}
