package com.devteria.partnersession.controller;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.devteria.partnersession.dto.request.DownloadRequest;
import com.devteria.partnersession.service.FileDownloadService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/")
@CrossOrigin(value = "*")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileDownloadController {

    FileDownloadService fileDownloadService; // Đường dẫn thư mục để lưu file

    @PostMapping("/downloadFromExternalApi")
    @ResponseBody
    public void downloadFromExternalApi(@RequestBody DownloadRequest downloadRequest, HttpServletResponse response)
            throws IOException {
        try {
            fileDownloadService.downloadFromExternalApi(downloadRequest, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
