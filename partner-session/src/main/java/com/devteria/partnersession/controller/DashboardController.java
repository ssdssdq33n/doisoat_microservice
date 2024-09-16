package com.devteria.partnersession.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.devteria.partnersession.dto.ApiResponse;
import com.devteria.partnersession.dto.response.ResultDataSession;
import com.devteria.partnersession.dto.response.ResultDataSessionMonth;
import com.devteria.partnersession.dto.response.ResultDataSessionMonthSTBP;
import com.devteria.partnersession.service.GetListDataSession;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(value = "*")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardController {

    GetListDataSession getListDataSession;

    @GetMapping("sessionDay")
    ApiResponse<List<ResultDataSession>> getDataListSession() {
        return ApiResponse.<List<ResultDataSession>>builder()
                .code(1000)
                .result(getListDataSession.getSessionsFromLastWeek())
                .build();
    }

    @GetMapping("sessionPI")
    ApiResponse<List<ResultDataSessionMonth>> getDataListSessionPI() {
        return ApiResponse.<List<ResultDataSessionMonth>>builder()
                .code(1000)
                .result(getListDataSession.getLatestSessionsForLastThreeMonths())
                .build();
    }

    @GetMapping("sessionSPI")
    ApiResponse<List<ResultDataSessionMonth>> getDataListSessionSPI() {
        return ApiResponse.<List<ResultDataSessionMonth>>builder()
                .code(1000)
                .result(getListDataSession.getLatestSessionsForLastThreeMonthsSPI())
                .build();
    }

    @GetMapping("sessionSTBP")
    ApiResponse<List<ResultDataSessionMonthSTBP>> getDataListSessionSTBP() {
        return ApiResponse.<List<ResultDataSessionMonthSTBP>>builder()
                .code(1000)
                .result(getListDataSession.getLatestSessionsForLastThreeMonthsSTBP())
                .build();
    }
}
