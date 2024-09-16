package com.devteria.partnersession.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.devteria.partnersession.dto.request.DetailReportRequest;
import com.devteria.partnersession.dto.request.DownloadRequest;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class FileDownloadService {
    @Value("${external.api.url}")
    @NonFinal
    private String externalApiUrl; // URL của API bên ngoài

    @Value("${proton.report.save.path}")
    @NonFinal
    private String fileSavePath;

    public void downloadFromExternalApi(DownloadRequest downloadRequest, HttpServletResponse response)
            throws IOException {
        LocalDate date = LocalDate.parse(downloadRequest.getValidFromDate(), DateTimeFormatter.ofPattern("d/M/yyyy"));
        LocalDate currentDate = LocalDate.now();
        DayOfWeek currentDateDayOfWeek = currentDate.getDayOfWeek();
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        //        if ((currentDateDayOfWeek == DayOfWeek.SATURDAY || currentDateDayOfWeek == DayOfWeek.SUNDAY) &&
        // currentDate.isBefore(date.plusDays(3))){
        //            if(dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.FRIDAY){
        //                throw new AppException(ErrorCode.DAY_ERROR);
        //            }
        //
        //        }
        //        if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)
        // {
        //
        //            int daysToSunday = DayOfWeek.SUNDAY.getValue() - dayOfWeek.getValue();
        //            int daysFromFriday = dayOfWeek.getValue() - DayOfWeek.FRIDAY.getValue();
        //            LocalDate dateTo = date.plusDays(daysToSunday);
        //            LocalDate dateFrom = date.minusDays(daysFromFriday);
        //            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
        //            downloadRequest.setValidFromDate(dateFrom.format(formatter));
        //            downloadRequest.setValidToDate(dateTo.format(formatter));
        //        }
        // Tạo request body cho API bên ngoài
        DetailReportRequest request = new DetailReportRequest();
        request.setValidFromDate(downloadRequest.getValidFromDate());
        request.setValidToDate(downloadRequest.getValidToDate());
        request.setStatus("-1");
        request.setServiceProvider("");
        request.setService("");
        request.setUsername("");
        request.setTransType(Arrays.asList("2", "3", "4", "5"));
        ResponseEntity<byte[]> responseEntity = null;
        try {
            // Tạo RestTemplate để gọi API bên ngoài
            RestTemplate restTemplate = new RestTemplate();

            // Thiết lập headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Tạo HttpEntity với request body và headers
            HttpEntity<DetailReportRequest> entity = new HttpEntity<>(request, headers);

            // Gọi API bên ngoài và nhận về một ResponseEntity<byte[]>

            responseEntity = restTemplate.exchange(externalApiUrl, HttpMethod.POST, entity, byte[].class);
        } catch (Exception e) {
            e.printStackTrace();
            //            throw new AppException(ErrorCode.PAYMENT_ERROR);
        }

        // Kiểm tra response status từ API bên ngoài
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            // Parse the validToDate to extract day, month, and year
            String validToDate = request.getValidToDate();
            LocalDate date1;
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
                date = LocalDate.parse(validToDate, formatter);
                date1 = LocalDate.parse(request.getValidFromDate(), formatter);
            } catch (DateTimeParseException e) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.getWriter().write("Invalid date format: " + validToDate);
                return;
            }

            String day = String.format("%02d", date.getDayOfMonth());
            String month = String.format("%02d", date.getMonthValue());
            String year = String.valueOf(date.getYear());

            String month1 = String.format("%02d", date1.getMonthValue());
            String year1 = String.valueOf(date1.getYear());
            Path directoryPath = null;
            // Create directories for year, month, and day
            if ("day".equalsIgnoreCase(downloadRequest.getDownloadFileCycle())) {
                directoryPath = Paths.get(fileSavePath, year, "Proton", "DS-T" + month, day + "-" + month);
            } else {
                directoryPath = Paths.get(fileSavePath, year1, "Proton", "DS-T" + month1);
            }

            Files.createDirectories(directoryPath);

            // Tạo tên file ZIP với đường dẫn đầy đủ
            Path zipFilePath = directoryPath.resolve("data.zip");

            // Lưu nội dung của file ZIP vào ổ cứng
            try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(zipFilePath))) {
                out.write(responseEntity.getBody());
                out.flush();
            }
            Path filePath =
                    directoryPath.resolve(directoryPath + "\\report_detail" + year + "-" + month + "-" + day + ".tsv");

            // Giải nén file ZIP
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    // Sanitize the file name to remove illegal characters
                    if (!zipEntry.isDirectory()) {
                        // Extract the file
                        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(filePath))) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = zis.read(buffer)) != -1) {
                                bos.write(buffer, 0, bytesRead);
                            }
                        }
                    } else {
                        // Create directories
                        Files.createDirectories(filePath);
                    }
                    zis.closeEntry();
                }
            }
            // Delete the ZIP file after extraction
            Files.delete(zipFilePath);

            // Thiết lập các thông tin header cho response của API này
            response.setContentType("text/tab-separated-values");
            response.setHeader(
                    "Content-Disposition", String.format("attachment; filename=report_%s_%s_%s.tsv", year, month, day));

            // Ghi nội dung của file TSV vào HttpServletResponse
            try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(filePath));
                    BufferedOutputStream outStream = new BufferedOutputStream(response.getOutputStream())) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
                outStream.flush();
            }
        } else {
            // Xử lý trường hợp lỗi từ API bên ngoài (ví dụ: trả về một lỗi khác nếu cần)
            response.setStatus(responseEntity.getStatusCodeValue());
            response.getWriter().write("Error occurred while fetching data from external API");
        }
    }
}
