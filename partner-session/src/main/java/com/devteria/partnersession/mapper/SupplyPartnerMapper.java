package com.devteria.partnersession.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.devteria.partnersession.dto.request.SupplyPartnerRequest;
import com.devteria.partnersession.dto.response.SupplyPartnerResponse;
import com.devteria.partnersession.model.ServerConfig;
import com.devteria.partnersession.model.SupplyPartner;

@Mapper(componentModel = "spring")
public interface SupplyPartnerMapper {
    SupplyPartner toSupplyPartner(SupplyPartnerRequest request);

    List<ServerConfig> toServerConfig(List<SupplyPartnerResponse> supplyPartner);

    SupplyPartnerResponse toSupplyPartnerResponse(SupplyPartner response);

    void updateSupplyPartner(@MappingTarget SupplyPartner response, SupplyPartnerRequest request);
}
