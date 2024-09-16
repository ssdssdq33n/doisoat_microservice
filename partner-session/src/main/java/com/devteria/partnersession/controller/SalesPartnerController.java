package com.devteria.partnersession.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.devteria.partnersession.dto.ApiResponse;
import com.devteria.partnersession.dto.request.SalesPartnerRequest;
import com.devteria.partnersession.dto.response.SalesPartnerResponse;
import com.devteria.partnersession.service.SalesPartnerService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/transaction/sales")
@CrossOrigin(value = "*")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SalesPartnerController {

    SalesPartnerService salesPartnerService;

    @PostMapping
    ApiResponse<SalesPartnerResponse> addSalesPartner(@RequestBody SalesPartnerRequest request) {
        return ApiResponse.<SalesPartnerResponse>builder()
                .code(1000)
                .result(salesPartnerService.addSalesPartner(request))
                .build();
    }

    @GetMapping("/one/{id}")
    ApiResponse<SalesPartnerResponse> getSalesPartnerById(@PathVariable("id") Long id) {
        return ApiResponse.<SalesPartnerResponse>builder()
                .code(1000)
                .result(salesPartnerService.getSalesPartnerById(id))
                .build();
    }

    @GetMapping("/all")
    ApiResponse<List<SalesPartnerResponse>> getAllSalesPartner() {
        return ApiResponse.<List<SalesPartnerResponse>>builder()
                .code(1000)
                .result(salesPartnerService.getAllSalesPartner())
                .build();
    }

    @GetMapping("/allPending")
    ApiResponse<List<SalesPartnerResponse>> getAllSalesPartnerPending() {
        return ApiResponse.<List<SalesPartnerResponse>>builder()
                .code(1000)
                .result(salesPartnerService.getAllSalesPartnerPending())
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<SalesPartnerResponse> editSalesPartner(
            @RequestBody SalesPartnerRequest request, @PathVariable("id") Long id) {
        return ApiResponse.<SalesPartnerResponse>builder()
                .code(1000)
                .result(salesPartnerService.editSalesPartner(request, id))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> deleteSalesPartner(@PathVariable("id") Long id) {
        return ApiResponse.<String>builder()
                .code(1000)
                .result(salesPartnerService.deleteSalesPartner(id))
                .build();
    }
}
