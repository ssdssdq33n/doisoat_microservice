package com.devteria.partnersession.controller;

import java.text.ParseException;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.devteria.partnersession.dto.ApiResponse;
import com.devteria.partnersession.dto.request.SessionRequest;
import com.devteria.partnersession.dto.response.SessionResponse;
import com.devteria.partnersession.service.SessionService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/transaction/session")
@CrossOrigin(value = "*")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SessionController {

    SessionService sessionService;

    @GetMapping("/name")
    ApiResponse<String> createNameSession() {
        return ApiResponse.<String>builder()
                .code(1000)
                .result(sessionService.createNameSession())
                .build();
    }

    @PostMapping
    ApiResponse<SessionResponse> createSession(@RequestBody SessionRequest request) throws ParseException {
        return ApiResponse.<SessionResponse>builder()
                .code(1000)
                .result(sessionService.createSession(request))
                .build();
    }

    @GetMapping("/all")
    ApiResponse<List<SessionResponse>> getAllSession() {
        return ApiResponse.<List<SessionResponse>>builder()
                .code(1000)
                .result(sessionService.getAllSession())
                .build();
    }

    @GetMapping("/reload")
    ApiResponse<List<SessionResponse>> getAllSessionReload() {
        return ApiResponse.<List<SessionResponse>>builder()
                .code(1000)
                .result(sessionService.getAllSessionReload())
                .build();
    }

    @GetMapping("/one/{id}")
    ApiResponse<SessionResponse> getSessionById(@PathVariable("id") Long id) {
        return ApiResponse.<SessionResponse>builder()
                .code(1000)
                .result(sessionService.getSessionById(id))
                .build();
    }

    @PostMapping("/one/{name}")
    ApiResponse<SessionResponse> getSessionById(@PathVariable("name") String name) {
        return ApiResponse.<SessionResponse>builder()
                .code(1000)
                .result(sessionService.getSessionByName(name))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> deleteSession(@PathVariable("id") Long id) {
        return ApiResponse.<String>builder()
                .code(1000)
                .result(sessionService.deleteSession(id))
                .build();
    }

    @GetMapping("/updateStatus")
    ApiResponse<String> updateStatus(@RequestParam("name") String name, @RequestParam("error") String status) {
        return ApiResponse.<String>builder()
                .code(1000)
                .result(sessionService.updateStatus(name, status))
                .build();
    }
}
