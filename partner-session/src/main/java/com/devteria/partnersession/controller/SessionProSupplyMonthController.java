package com.devteria.partnersession.controller;

import java.text.ParseException;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.devteria.partnersession.dto.ApiResponse;
import com.devteria.partnersession.dto.request.SessionMonthRequest;
import com.devteria.partnersession.dto.response.SessionMonthResponse;
import com.devteria.partnersession.service.SessionProSupplyMonthService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/transaction/session/proSupply/month")
@CrossOrigin(value = "*")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SessionProSupplyMonthController {

    SessionProSupplyMonthService sessionProSupplyMonthService;

    @GetMapping("/name")
    ApiResponse<String> createNameSession() {
        return ApiResponse.<String>builder()
                .code(1000)
                .result(sessionProSupplyMonthService.createNameSession())
                .build();
    }

    @PostMapping
    ApiResponse<SessionMonthResponse> createSession(@RequestBody SessionMonthRequest request) throws ParseException {
        return ApiResponse.<SessionMonthResponse>builder()
                .code(1000)
                .result(sessionProSupplyMonthService.createSession(request))
                .build();
    }

    @GetMapping("/all")
    ApiResponse<List<SessionMonthResponse>> getAllSession() {
        return ApiResponse.<List<SessionMonthResponse>>builder()
                .code(1000)
                .result(sessionProSupplyMonthService.getAllSession())
                .build();
    }

    @GetMapping("/reload")
    ApiResponse<List<SessionMonthResponse>> getAllSessionReload() {
        return ApiResponse.<List<SessionMonthResponse>>builder()
                .code(1000)
                .result(sessionProSupplyMonthService.getAllSessionReload())
                .build();
    }

    @GetMapping("/one/{id}")
    ApiResponse<SessionMonthResponse> getSessionById(@PathVariable("id") Long id) {
        return ApiResponse.<SessionMonthResponse>builder()
                .code(1000)
                .result(sessionProSupplyMonthService.getSessionById(id))
                .build();
    }

    @PostMapping("/one/{name}")
    ApiResponse<SessionMonthResponse> getSessionById(@PathVariable("name") String name) {
        return ApiResponse.<SessionMonthResponse>builder()
                .code(1000)
                .result(sessionProSupplyMonthService.getSessionByName(name))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> deleteSession(@PathVariable("id") Long id) {
        return ApiResponse.<String>builder()
                .code(1000)
                .result(sessionProSupplyMonthService.deleteSession(id))
                .build();
    }

    @GetMapping("/updateStatus")
    ApiResponse<String> updateStatus(@RequestParam("name") String name, @RequestParam("error") String error) {
        return ApiResponse.<String>builder()
                .code(1000)
                .result(sessionProSupplyMonthService.updateStatus(name, error))
                .build();
    }
}