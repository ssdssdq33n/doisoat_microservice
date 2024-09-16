package com.devteria.partnersession.mapper;

import org.mapstruct.Mapper;

import com.devteria.partnersession.dto.request.SessionMonthRequest;
import com.devteria.partnersession.dto.response.SessionMonthResponse;
import com.devteria.partnersession.model.SessionSalesProSupplyMonth;

@Mapper(componentModel = "spring")
public interface SessionSalesProSupplyMonthMapper {
    SessionSalesProSupplyMonth toSessionMonth(SessionMonthRequest request);

    SessionMonthResponse toSessionMonthResponse(SessionSalesProSupplyMonth sessionSalesProMonth);
}
