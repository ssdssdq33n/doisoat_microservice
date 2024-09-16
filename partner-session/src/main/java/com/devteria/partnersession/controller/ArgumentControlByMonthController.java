package com.devteria.partnersession.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.devteria.partnersession.dto.ApiResponse;
import com.devteria.partnersession.dto.request.ArgumentControlByMonthRequest;
import com.devteria.partnersession.dto.response.ArgumentControlByMonthResponse;
import com.devteria.partnersession.dto.response.ArgumentControlProIrisByMonthResponse;
import com.devteria.partnersession.dto.response.ArgumentControlStbProIrisByMonthResponse;
import com.devteria.partnersession.exception.AppException;
import com.devteria.partnersession.exception.ErrorCode;
import com.devteria.partnersession.model.ControlSessionMonth;
import com.devteria.partnersession.model.RefundTransactionsByMonth;
import com.devteria.partnersession.model.SaveFileMonth;
import com.devteria.partnersession.repository.ControlSessionMonthRepository;
import com.devteria.partnersession.repository.SaveFileMonthRepository;
import com.devteria.partnersession.service.ArgumentControlByMonthService;
import com.devteria.partnersession.service.FileService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/transaction/argument/control/month")
@CrossOrigin(value = "*")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ArgumentControlByMonthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentControlByMonthController.class);

    @Value("${proton.refund-transaction.save.path}")
    @NonFinal
    private String refundTransactionSavePath;

    ArgumentControlByMonthService argumentControlByMonthService;

    ControlSessionMonthRepository controlSessionMonthRepository;

    SaveFileMonthRepository saveFileMonthRepository;

    FileService fileService;

    @PostMapping()
    public ApiResponse<String> createArgumentControlByMonth(@RequestBody ArgumentControlByMonthRequest request)
            throws IOException {
        try {
            String result = argumentControlByMonthService.createArgumentControlByMonth(request);
            return ApiResponse.<String>builder().result(result).code(1000).build();
        } catch (AppException e) {
            LOGGER.error("Error occurred during argument control creation: " + e.getMessage(), e);
            return ApiResponse.<String>builder()
                    .code(e.getErrorCode().getCode())
                    .message(e.getMessage())
                    .build();
        } catch (IOException e) {
            LOGGER.error("IO Exception occurred: " + e.getMessage(), e);
            return ApiResponse.<String>builder()
                    .code(5000)
                    .message("Internal server error")
                    .build();
        }
    }

    @GetMapping("/statusProgress")
    public ApiResponse<ArgumentControlByMonthResponse> getArgumentControlByMonthStatus() {
        ArgumentControlByMonthResponse response = argumentControlByMonthService.getArgumentControlByMonthStatus();
        return ApiResponse.<ArgumentControlByMonthResponse>builder()
                .result(response)
                .build();
    }

    //    @PostMapping("/process-folder")
    //    public ApiResponse<List<TongThangReport>> processFileInFolder(@RequestBody ProcessFolderRequest request)
    //            throws IOException {
    //        List<TongThangReport> tongThangReports = argumentControlByMonthService.processFilesInFolder(request);
    //        return ApiResponse.<List<TongThangReport>>builder()
    //                .result(tongThangReports)
    //                .build();
    //    }

    @GetMapping("/one/{id}")
    ApiResponse<List<RefundTransactionsByMonth>> getArgumentControlByMonthByName(
            @PathVariable("id") String argumentControlDetail) {
        ControlSessionMonth controlSessionMonth =
                controlSessionMonthRepository.findBySessionName(argumentControlDetail);
        System.out.println(controlSessionMonth.getRefundTransactionsByMonths().size());
        return ApiResponse.<List<RefundTransactionsByMonth>>builder()
                .result(controlSessionMonth.getRefundTransactionsByMonths())
                .code(1000)
                .build();
    }

    @GetMapping("/save-file")
    public ApiResponse<List<SaveFileMonth>> getAllSavedFiles() {
        List<SaveFileMonth> savedFiles = saveFileMonthRepository.findAll();
        return ApiResponse.<List<SaveFileMonth>>builder().result(savedFiles).build();
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @RequestParam("name") String name,
            @RequestParam("fileName") String fileName,
            @RequestParam("year") String year,
            @RequestParam("month") String month)
            throws FileNotFoundException {
        String filePath =
                refundTransactionSavePath + "\\" + year + "\\Data-Month\\DS-T" + month + "\\" + name + ".xlsx";
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

    @PostMapping("/iris")
    public ApiResponse<ArgumentControlProIrisByMonthResponse> createArgumentControlByMonthToIris(
            @RequestBody ArgumentControlByMonthRequest request) throws IOException {
        try {
            ArgumentControlProIrisByMonthResponse result =
                    argumentControlByMonthService.createArgumentControlByMonthToIris(request);
            return ApiResponse.<ArgumentControlProIrisByMonthResponse>builder()
                    .result(result)
                    .code(1000)
                    .build();
        } catch (AppException e) {
            LOGGER.error("Error occurred during argument control creation: " + e.getMessage(), e);
            return ApiResponse.<ArgumentControlProIrisByMonthResponse>builder()
                    .code(e.getErrorCode().getCode())
                    .message(e.getMessage())
                    .build();
        } catch (IOException e) {
            LOGGER.error("IO Exception occurred: " + e.getMessage(), e);
            return ApiResponse.<ArgumentControlProIrisByMonthResponse>builder()
                    .code(5000)
                    .message("Internal server error")
                    .build();
        }
    }

    @PostMapping("/protonIrisStb")
    public ApiResponse<ArgumentControlStbProIrisByMonthResponse> createArgumentControlByMonthToSPI(
            @RequestBody ArgumentControlByMonthRequest request) {
        try {
            ArgumentControlStbProIrisByMonthResponse result =
                    argumentControlByMonthService.createArgumentControlByMonthToSPI(request);
            return ApiResponse.<ArgumentControlStbProIrisByMonthResponse>builder()
                    .result(result)
                    .code(1000)
                    .build();
        } catch (AppException e) {
            LOGGER.error("Error occurred during argument control creation: " + e.getMessage(), e);
            return ApiResponse.<ArgumentControlStbProIrisByMonthResponse>builder()
                    .code(e.getErrorCode().getCode())
                    .message(e.getMessage())
                    .build();
        } catch (IOException e) {
            LOGGER.error("IO Exception occurred: " + e.getMessage(), e);
            return ApiResponse.<ArgumentControlStbProIrisByMonthResponse>builder()
                    .code(5000)
                    .message("Internal server error")
                    .build();
        }
    }
}
