package com.devteria.partnersession.dto.response;

import java.util.List;

import com.devteria.partnersession.model.IrisMonthReport;
import com.devteria.partnersession.model.ProtonReport;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArgumentControlProIrisByMonth {

    // cac giao dich matcher
    private List<IrisMonthReport> proSucIrisSuc;
    private List<IrisMonthReport> proErrorIrisFail;

    // cac giao dich can kiem tra lai tren web
    private List<IrisMonthReport> proNotExistIrisSuc;
    private List<IrisMonthReport> proNotExistIrisFail;

    private List<ProtonReport> irisNotExistProSuc;
    private List<ProtonReport> irisNotExistProError;

    // cac giao dich can kiem tra lai tren web
    private List<IrisMonthReport> irisFailProtonSuc;
    private List<ProtonReport> proErrorIrisSuc;
}
