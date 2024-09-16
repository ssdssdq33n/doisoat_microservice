package com.devteria.profile.repository;

import com.devteria.profile.dto.identity.*;
import com.devteria.profile.dto.request.PasswordRequest;
import com.devteria.profile.dto.request.RoleRequest;
import com.devteria.profile.dto.response.RoleIdReponse;
import com.devteria.profile.dto.response.RoleReponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import feign.QueryMap;

import java.util.List;

@FeignClient(name = "identity-client", url = "${idp.url}")
public interface IdentityClient {
    @PostMapping(
            value = "/realms/profile/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    TokenExchangeResponse exchangeToken(@QueryMap TokenExchangeParam param);

    @PostMapping(
            value = "/realms/profile/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    TokenExchangeUserResponse loginUser(@QueryMap TokenExchangeUser param);

    @PostMapping(
            value = "/realms/profile/protocol/openid-connect/token/introspect",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    IntrospectResponse introspect(@QueryMap IntrospectParam param);

    @PostMapping(
            value = "/admin/realms/profile/roles",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> createRole(@RequestHeader("authorization") String token,@RequestBody RoleRequest param);

    @GetMapping(
            value = "/admin/realms/profile/roles/{name}")
    RoleIdReponse getIdRole(@RequestHeader("authorization") String token, @PathVariable("name") String name);

    @PostMapping(
            value = "/admin/realms/profile/roles-by-id/{role-id}/composites")
    ResponseEntity<?> addRoletoRole(@RequestHeader("authorization") String token, @PathVariable("role-id") String roleId, @RequestBody List<RoleIdReponse> requests);

    @PutMapping(
            value = "/admin/realms/profile/users/{user-id}/reset-password",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> resetPassword(@RequestHeader("authorization") String token, @PathVariable("user-id") String userId, @RequestBody PasswordRequest request);

    @PostMapping(
            value = "/admin/realms/profile/users/{user-id}/role-mappings/realm",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> addRoleToUser(@RequestHeader("authorization") String token, @PathVariable("user-id") String userId, @RequestBody List<RoleIdReponse> requests);

    @PostMapping(value = "/admin/realms/profile/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> createUser(@RequestHeader("authorization") String token, @RequestBody UserCreationParam param);
}
