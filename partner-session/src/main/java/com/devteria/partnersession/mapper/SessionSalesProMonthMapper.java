package com.devteria.partnersession.mapper;

import org.mapstruct.Mapper;

import com.devteria.partnersession.dto.request.SessionMonthRequest;
import com.devteria.partnersession.dto.response.SessionMonthResponse;
import com.devteria.partnersession.model.SessionSalesProMonth;

@Mapper(componentModel = "spring")
public interface SessionSalesProMonthMapper {
    SessionSalesProMonth toSessionMonth(SessionMonthRequest request);

    SessionMonthResponse toSessionMonthResponse(SessionSalesProMonth sessionSalesProMonth);
}
