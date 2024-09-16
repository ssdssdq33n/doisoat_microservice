package com.devteria.partnersession.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.devteria.partnersession.model.ControlSessionMonth;
import com.devteria.partnersession.model.RefundTransactionsByMonth;
import com.devteria.partnersession.model.TongThangReport;

@Mapper(componentModel = "spring")
public interface TongThangReportMapper {
    @Mapping(target = "transactionNumber_ID", source = "tongThangReport.transactionNumberId")
    @Mapping(target = "dayTrading", source = "tongThangReport.timeCreated", dateFormat = "dd/MM/yyyy HH:mm:ss")
    @Mapping(target = "educationalTime", source = "tongThangReport.educationalTime", dateFormat = "dd/MM/yyyy HH:mm:ss")
    @Mapping(target = "amount", source = "tongThangReport.amount")
    @Mapping(target = "customAccountCode", source = "tongThangReport.customAccountCode")
    @Mapping(target = "recordedAccountNumber", source = "tongThangReport.recordedAccountNumber")
    @Mapping(target = "explanation", source = "tongThangReport.explain")
    @Mapping(target = "traceNo", source = "tongThangReport.traceNo")
    @Mapping(target = "serviceProvider", source = "tongThangReport.serviceProvider")
    @Mapping(target = "paymentService", source = "tongThangReport.paymentService")
    @Mapping(target = "status", source = "tongThangReport.status")
    RefundTransactionsByMonth toRefundTransactionByMonth(
            TongThangReport tongThangReport, ControlSessionMonth controlSessionMonth);
}
