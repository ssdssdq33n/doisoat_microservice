package com.devteria.partnersession.dto.response;

import java.util.ArrayList;
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
public class ArgumentControlByDay {

    // ArgumentControl Iris vs Proton

    private Long argumentByDayId;
    // tong giao dich iris
    private Long irisTotalCount;
    // tong giao dich proton
    private Long protonTotalCount;
    // tong giao dich irisSuccess
    private Long irisSuccessCount;
    // tong giao dich protonSuccess
    private Long protonSuccessCount;
    // danh sach gd thanh cong ở proton nhưng không tìm thấy trong iris
    private List<ProtonReport> sucProIrisNotExist;
    // danh sach gd timeout ở proton nhưng trong irisSuccess
    private List<ProtonReport> timeoutProIrisSuc;
    // danh sach gd timeout ở proton nhưng trong irisSuccess
    private List<IrisReport> irisSucTimeoutPro;
    // cac giao dich can kiem tra lai tren web
    private List<IrisReport> proNotExistIrisSuc;
    // tổng giao dich that bai ben iris
    private Long irisFailCount;
    // tổng giao dich loi ben proton
    private Long proErrorCount;
    // giao dich proton lỗi khong tìm thay trong iris la data test
    private List<ProtonReport> proErrorIsDataTest;
    // cac giao dich can kiem tra lai tren web
    private List<IrisReport> irisUnknown;
    // cac giao dich can kiem tra lai tren web
    private List<ProtonReport> protonTimeoutIrisNotExist;
    // cac giao dich can kiem tra lai tren web
    private List<IrisReport> irisFailProtonNotExist;
    // cac giao dich can kiem tra lai tren web
    private List<IrisReport> irisUnknownProtonNotExist = new ArrayList<>();

    private List<ProtonReport> protonTimeout = new ArrayList<>();

    private List<StbReport> protonNotExist = new ArrayList<>();
    // End Iris vs Proton

    // ArgumentControl STB vs Proton
    // tong giao dich stb
    private Long stbTotalCount;
    // chenh lech StbSuc vs ProtonSuc
    private Long subStbSucProtonSuc;
    // danh sach gd can hoan tra
    private List<StbReport> listTranNeedRefunded;
    // tong so gd can hoan
    private Long tranNeedRefundedTotalCount;
    // danh sach hoan tra cho tai khoan khong phai tai khoan thanh toan
    private List<StbReport> transHaveAccVND;
    // danh sach hoan tra cho tai khoan là tai khoan thanh toan
    private List<StbReport> transHavePayAcc;

    private Set<StbReport> allRefundTransactions;

    private Set<StbReport> allUnknownTransactions;
}
