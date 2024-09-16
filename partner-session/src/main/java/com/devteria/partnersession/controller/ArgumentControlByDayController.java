package com.devteria.partnersession.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.devteria.partnersession.dto.ApiResponse;
import com.devteria.partnersession.dto.request.ArgumentControlByDayRequest;
import com.devteria.partnersession.dto.response.ArgumentControlByDay;
import com.devteria.partnersession.dto.response.ArgumentControlByDayResponse;
import com.devteria.partnersession.exception.AppException;
import com.devteria.partnersession.exception.ErrorCode;
import com.devteria.partnersession.model.ControlSessionDay;
import com.devteria.partnersession.model.RefundTransactionsByDay;
import com.devteria.partnersession.repository.RefundTransactionsByDayRepository;
import com.devteria.partnersession.repository.SessionRepository;
import com.devteria.partnersession.service.ArgumentControlByDayService;
import com.devteria.partnersession.service.ControlSessionDayService;
import com.devteria.partnersession.service.RefundTransactionComparator;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/transaction/argument/control/day")
@CrossOrigin(value = "*")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ArgumentControlByDayController {

    @Value("${proton.refund-transaction.save.path}")
    @NonFinal
    private String refundTransactionSavePath;

    ArgumentControlByDayService argumentControlByDayService;

    ControlSessionDayService controlSessionDayService;

    RefundTransactionsByDayRepository refundTransactionsByDayRepository;

    SessionRepository sessionRepository;

    @PostMapping
    //    @TimeLimiter(name = "argumentControlByDayService")
    ApiResponse<ArgumentControlByDayResponse> createArgumentControlByDay(
            @RequestBody ArgumentControlByDayRequest request) throws IOException {
        return ApiResponse.<ArgumentControlByDayResponse>builder()
                .result(argumentControlByDayService.createArgumentControlByDay(request))
                .code(1000)
                .build();
    }

    @GetMapping("/statusProgress/")
    ApiResponse<ArgumentControlByDay> getArgumentControlByDayById() {
        return ApiResponse.<ArgumentControlByDay>builder()
                .result(argumentControlByDayService.getArgumentControlByDayById())
                .code(1000)
                .build();
    }

    @GetMapping("/one/{id}")
    ApiResponse<List<RefundTransactionsByDay>> getArgumentControlByDayByName(
            @PathVariable("id") String argumentControlDetail) {
        ControlSessionDay controlSessionDay = argumentControlByDayService.findBySessionName(argumentControlDetail);
        List<RefundTransactionsByDay> list =
                refundTransactionsByDayRepository.findAllByControlSessionDay(controlSessionDay);
        Collections.sort(list, new RefundTransactionComparator());
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRefundTransactionsByDay_ID((long) (i + 1));
        }
        return ApiResponse.<List<RefundTransactionsByDay>>builder()
                .result(list)
                .code(1000)
                .build();
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @RequestParam("name") String name,
            @RequestParam("fileName") String fileName,
            @RequestParam("year") String year,
            @RequestParam("month") String month,
            @RequestParam("day") String day)
            throws FileNotFoundException {
        String filePath = refundTransactionSavePath + "\\" + year + "\\DS-T" + month + "\\" + day + "\\"
                + "HoanTraATOMIchecklaiweb" + ".xlsx";
        InputStreamResource resource = null;
        File file = null;
        try {
            file = new File(filePath);
            resource = new InputStreamResource(new FileInputStream(file));
        } catch (AppException e) {
            throw new AppException(ErrorCode.NOT_FILE);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);
    }
}
