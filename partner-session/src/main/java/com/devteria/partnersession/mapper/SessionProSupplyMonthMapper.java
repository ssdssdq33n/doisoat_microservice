package com.devteria.partnersession.mapper;

import org.mapstruct.Mapper;

import com.devteria.partnersession.dto.request.SessionMonthRequest;
import com.devteria.partnersession.dto.response.SessionMonthResponse;
import com.devteria.partnersession.model.SessionProSupplyMonth;

@Mapper(componentModel = "spring")
public interface SessionProSupplyMonthMapper {
    SessionProSupplyMonth toSessionMonth(SessionMonthRequest request);

    SessionMonthResponse toSessionMonthResponse(SessionProSupplyMonth sessionSalesProMonth);
}
