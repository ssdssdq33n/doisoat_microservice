package com.devteria.profile.mapper;

import com.devteria.profile.dto.request.RegistrationRequest;
import com.devteria.profile.dto.request.RoleRequest;
import com.devteria.profile.dto.response.ProfileResponse;
import com.devteria.profile.dto.response.RoleReponse;
import com.devteria.profile.entity.Profile;
import com.devteria.profile.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    Role toRole(RoleRequest request);

    RoleReponse toRoleReponse(Role role);
}
