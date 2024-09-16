package com.devteria.partnersession.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.devteria.partnersession.dto.ApiResponse;
import com.devteria.partnersession.dto.request.SupplyPartnerRequest;
import com.devteria.partnersession.dto.response.SupplyPartnerResponse;
import com.devteria.partnersession.service.SupplyPartnerService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/transaction/supply")
@CrossOrigin(value = "*")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupplyPartnerController {

    SupplyPartnerService supplyPartnerService;

    @PostMapping
    ApiResponse<SupplyPartnerResponse> addSupplyPartner(@RequestBody SupplyPartnerRequest request) {
        return ApiResponse.<SupplyPartnerResponse>builder()
                .code(1000)
                .result(supplyPartnerService.addSupplyPartner(request))
                .build();
    }

    @GetMapping("/one/{id}")
    ApiResponse<SupplyPartnerResponse> getSupplyPartnerById(@PathVariable("id") Long id) {
        return ApiResponse.<SupplyPartnerResponse>builder()
                .code(1000)
                .result(supplyPartnerService.getSupplyPartnerById(id))
                .build();
    }

    @GetMapping("/all")
    ApiResponse<List<SupplyPartnerResponse>> getAllSupplyPartner() {
        return ApiResponse.<List<SupplyPartnerResponse>>builder()
                .code(1000)
                .result(supplyPartnerService.getAllSupplyPartner())
                .build();
    }

    @GetMapping("/allPending")
    ApiResponse<List<SupplyPartnerResponse>> getAllSupplyPartnerPending() {
        return ApiResponse.<List<SupplyPartnerResponse>>builder()
                .code(1000)
                .result(supplyPartnerService.getAllSupplyPartnerPending())
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<SupplyPartnerResponse> editSupplyPartner(
            @RequestBody SupplyPartnerRequest request, @PathVariable("id") Long id) {
        return ApiResponse.<SupplyPartnerResponse>builder()
                .code(1000)
                .result(supplyPartnerService.editSupplyPartner(request, id))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> deleteSupplyPartner(@PathVariable("id") Long id) {
        return ApiResponse.<String>builder()
                .code(1000)
                .result(supplyPartnerService.deleteSupplyPartner(id))
                .build();
    }
}
