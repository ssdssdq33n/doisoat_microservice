package com.devteria.partnersession.dto.response;

import java.util.List;
import java.util.Set;

import com.devteria.partnersession.model.IrisReport;
import com.devteria.partnersession.model.ProtonReport;
import com.devteria.partnersession.model.StbReport;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArgumentControlByDayResponse {

    // ArgumentControl Iris vs Proton

    private Long argumentByDayId;

    // cac giao dich can kiem tra lai tren web
    private List<IrisReport> proNotExistIrisSuc;

    // cac giao dich can kiem tra lai tren web
    private List<IrisReport> irisUnknown;
    // cac giao dich can kiem tra lai tren web
    private List<ProtonReport> protonTimeoutIrisNotExist;
    // cac giao dich can kiem tra lai tren web
    private List<IrisReport> irisFailProtonNotExist;
    // End Iris vs Proton

    // ArgumentControl STB vs Proton

    // danh sach gd can hoan tra
    private List<StbReport> listTranNeedRefunded;
    // danh sach hoan tra cho tai khoan khong phai tai khoan thanh toan
    private List<StbReport> transHaveAccVND;
    // danh sach hoan tra cho tai khoan là tai khoan thanh toan
    private List<StbReport> transHavePayAcc;

    private Set<StbReport> allRefundTransactions;

    private Set<StbReport> allUnknownTransactions;
}
