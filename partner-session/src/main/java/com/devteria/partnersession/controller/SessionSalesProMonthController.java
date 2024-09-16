package com.devteria.partnersession.controller;

import java.text.ParseException;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.devteria.partnersession.dto.ApiResponse;
import com.devteria.partnersession.dto.request.SessionMonthRequest;
import com.devteria.partnersession.dto.response.SessionMonthResponse;
import com.devteria.partnersession.service.SessionSalesProMonthService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/transaction/session/salesPro/month")
@CrossOrigin(value = "*")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SessionSalesProMonthController {

    SessionSalesProMonthService sessionSalesProMonthService;

    @GetMapping("/name")
    ApiResponse<String> createNameSession() {
        return ApiResponse.<String>builder()
                .code(1000)
                .result(sessionSalesProMonthService.createNameSession())
                .build();
    }

    @PostMapping
    ApiResponse<SessionMonthResponse> createSession(@RequestBody SessionMonthRequest request) throws ParseException {
        return ApiResponse.<SessionMonthResponse>builder()
                .code(1000)
                .result(sessionSalesProMonthService.createSession(request))
                .build();
    }

    @GetMapping("/all")
    ApiResponse<List<SessionMonthResponse>> getAllSession() {
        return ApiResponse.<List<SessionMonthResponse>>builder()
                .code(1000)
                .result(sessionSalesProMonthService.getAllSession())
                .build();
    }

    @GetMapping("/reload")
    ApiResponse<List<SessionMonthResponse>> getAllSessionReload() {
        return ApiResponse.<List<SessionMonthResponse>>builder()
                .code(1000)
                .result(sessionSalesProMonthService.getAllSessionReload())
                .build();
    }

    @GetMapping("/one/{id}")
    ApiResponse<SessionMonthResponse> getSessionById(@PathVariable("id") Long id) {
        return ApiResponse.<SessionMonthResponse>builder()
                .code(1000)
                .result(sessionSalesProMonthService.getSessionById(id))
                .build();
    }

    @PostMapping("/one/{name}")
    ApiResponse<SessionMonthResponse> getSessionById(@PathVariable("name") String name) {
        return ApiResponse.<SessionMonthResponse>builder()
                .code(1000)
                .result(sessionSalesProMonthService.getSessionByName(name))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> deleteSession(@PathVariable("id") Long id) {
        return ApiResponse.<String>builder()
                .code(1000)
                .result(sessionSalesProMonthService.deleteSession(id))
                .build();
    }

    @GetMapping("/updateStatus")
    ApiResponse<String> updateStatus(@RequestParam("name") String name, @RequestParam("error") String error) {
        return ApiResponse.<String>builder()
                .code(1000)
                .result(sessionSalesProMonthService.updateStatus(name, error))
                .build();
    }
}
