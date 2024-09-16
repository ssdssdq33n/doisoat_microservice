package com.devteria.profile.controller;

import java.util.List;

import com.devteria.profile.dto.identity.IntrospectRequest;
import com.devteria.profile.dto.identity.IntrospectResponse;
import com.devteria.profile.dto.identity.TokenExchangeUserResponse;
import com.devteria.profile.dto.identity.ValidResponse;
import com.devteria.profile.dto.request.RoleAddRequest;
import com.devteria.profile.dto.request.RoleRequest;
import com.devteria.profile.dto.response.RoleReponse;
import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.devteria.profile.dto.ApiResponse;
import com.devteria.profile.dto.request.RegistrationRequest;
import com.devteria.profile.dto.response.ProfileResponse;
import com.devteria.profile.service.ProfileService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProfileController {
    ProfileService profileService;

    @PostMapping("/register")
    ApiResponse<ProfileResponse> register(@RequestBody @Valid RegistrationRequest request) {
        return ApiResponse.<ProfileResponse>builder()
                .result(profileService.register(request))
                .build();
    }

    @PostMapping("/login")
    ApiResponse<TokenExchangeUserResponse> loginUser(@RequestParam("username") String username, @RequestParam("password") String password) {
        return ApiResponse.<TokenExchangeUserResponse>builder()
                .result(profileService.loginUser(username,password))
                .build();
    }

    @PostMapping("/introspect")
    ApiResponse<ValidResponse> introspect(@RequestBody IntrospectRequest request) {
        return ApiResponse.<ValidResponse>builder()
                .result(profileService.introspect(request))
                .build();
    }

    @PostMapping("/resetPassword")
    ApiResponse<String> resetPassword(@RequestParam("username") String username, @RequestParam("password") String password) {
        return ApiResponse.<String>builder()
                .result(profileService.resetPassword(username,password))
                .build();
    }

    @PostMapping("/role")
    ApiResponse<RoleReponse> createRole(@RequestBody RoleRequest request) {
        return ApiResponse.<RoleReponse>builder()
                .result(profileService.createRole(request))
                .build();
    }

    @PostMapping("/addToRole")
    ApiResponse<RoleReponse> addToRole(@RequestBody RoleAddRequest request) {
        return ApiResponse.<RoleReponse>builder()
                .result(profileService.addRoletoRole(request))
                .build();
    }

    @PostMapping("/addRoleToUser")
    ApiResponse<ProfileResponse> addToRole(@RequestParam("user-id") String userId, @RequestBody List<String> listRole) {
        System.out.println(listRole);
        return ApiResponse.<ProfileResponse>builder()
                .result(profileService.addRoleToUser(userId,listRole))
                .build();
    }

    @GetMapping("/roles")
    ApiResponse<List<RoleReponse>> getAllRoles() {
        return ApiResponse.<List<RoleReponse>>builder()
                .result(profileService.getAllRoles())
                .build();
    }

    @GetMapping("/profiles")
    ApiResponse<List<ProfileResponse>> getAllProfiles() {
        return ApiResponse.<List<ProfileResponse>>builder()
                .result(profileService.getAllProfiles())
                .build();
    }

    @GetMapping("/my-profile")
    ApiResponse<ProfileResponse> getMyProfiles() {
        return ApiResponse.<ProfileResponse>builder()
                .result(profileService.getMyProfile())
                .build();
    }
}
