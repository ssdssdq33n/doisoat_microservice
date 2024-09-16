package com.devteria.partnersession.service;

import java.io.*;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devteria.partnersession.model.IrisReport;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class GetListXlsxReport {
    private static final Logger log = LoggerFactory.getLogger(ProcessDoiSoatProtonIris.class);

    public void getListIrisReport(String irisReportFilePath, List<IrisReport> irisReports) throws IOException {
        log.info("doc file xlsx: {} ", irisReportFilePath);
        File folder = new File(irisReportFilePath);
        File[] files = folder.listFiles();
        System.out.println(files.length);
        if (files != null && files.length == 3) {
            for (File file : files) {
                DayOfWeek dayOfWeek = toConvertDay(file.getName());
                if (dayOfWeek == DayOfWeek.THURSDAY) {
                    throw new IOException("Sai file, chứa ngày thứ năm");
                }
            }
        }
        if (files != null && files.length > 3) {
            System.out.println("co 4 file");
            for (int i = 0; i < files.length; i++) {
                DayOfWeek dayOfWeek = toConvertDay(files[i].getName());
                if (dayOfWeek == DayOfWeek.THURSDAY) {
                    List<File> fileList = new ArrayList<>(Arrays.asList(files));

                    // Xóa phần tử thứ n (ví dụ phần tử thứ 2, chỉ số 1)
                    fileList.remove(i); // Xóa phần tử có chỉ số 1 (phần tử thứ 2)

                    // Chuyển lại List<File> thành File[] nếu cần
                    files = fileList.toArray(new File[0]);
                }
            }
        }
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {

                    // Tạo FileInputStream để đọc tệp Excel
                    FileInputStream inputStream = new FileInputStream(file.getAbsoluteFile());

                    // Tạo Workbook từ FileInputStream
                    Workbook workbook = new XSSFWorkbook(inputStream);

                    // Lấy Sheet đầu tiên
                    Sheet sheet = workbook.getSheetAt(0);

                    // Bỏ qua dòng tiêu đề
                    Iterator<Row> rowIterator = sheet.iterator();
                    rowIterator.next();

                    // Duyệt qua các hàng trong Sheet
                    while (rowIterator.hasNext()) {
                        Row row = rowIterator.next();

                        // Tạo một đối tượng IrisReport
                        IrisReport irisReport = new IrisReport();

                        // Lấy giá trị các ô và gán vào đối tượng IrisReport
                        Cell cell = row.getCell(0);
                        irisReport.setTimeCreated(cell.getStringCellValue());

                        cell = row.getCell(1);
                        irisReport.setTraceCode(cell.getStringCellValue());

                        cell = row.getCell(5);
                        irisReport.setAmount((long) (cell.getNumericCellValue()));

                        cell = row.getCell(3);
                        irisReport.setTopupStatus(cell.getStringCellValue());

                        // Thêm IrisReport vào danh sách
                        irisReports.add(irisReport);
                    }

                    // Đóng Workbook
                    workbook.close();
                }
            }
        }
    }

    public int toConvertNumber(String fileName) {
        int underscoreIndex = fileName.lastIndexOf('_');

        // Lấy chuỗi số từ vị trí sau dấu gạch dưới đến trước phần mở rộng file
        String numberString = fileName.substring(underscoreIndex - 2, underscoreIndex);

        // Chuyển chuỗi số thành số nguyên (nếu cần)
        int number = Integer.parseInt(numberString);
        return number;
    }

    public DayOfWeek toConvertDay(String fileName) {
        String datePart = fileName.substring(0, 8);

        // Định dạng chuỗi thành LocalDate
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(datePart, formatter);

        // Lấy thứ trong tuần
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek;
    }
}
