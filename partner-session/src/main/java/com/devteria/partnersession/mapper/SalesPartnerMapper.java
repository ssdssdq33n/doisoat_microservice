package com.devteria.partnersession.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.devteria.partnersession.dto.request.SalesPartnerRequest;
import com.devteria.partnersession.dto.response.SalesPartnerResponse;
import com.devteria.partnersession.model.SalesPartner;
import com.devteria.partnersession.model.ServerConfig;

@Mapper(componentModel = "spring")
public interface SalesPartnerMapper {
    SalesPartner toSalesPartner(SalesPartnerRequest request);

    List<ServerConfig> toServerConfig(List<SalesPartnerResponse> supplyPartner);

    SalesPartnerResponse toSalesPartnerResponse(SalesPartner response);

    void updateSalesPartner(@MappingTarget SalesPartner response, SalesPartnerRequest request);
}
