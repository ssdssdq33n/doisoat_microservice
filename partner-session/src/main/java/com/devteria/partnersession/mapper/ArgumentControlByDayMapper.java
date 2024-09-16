package com.devteria.partnersession.mapper;

import org.mapstruct.Mapper;

import com.devteria.partnersession.dto.response.ArgumentControlByDay;
import com.devteria.partnersession.dto.response.ArgumentControlByDayResponse;

@Mapper(componentModel = "spring")
public interface ArgumentControlByDayMapper {
    ArgumentControlByDayResponse toArgumentControlByDayResponse(ArgumentControlByDay response);
}
