package com.devteria.partnersession.service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.devteria.partnersession.Constants.Constant;
import com.devteria.partnersession.common.utils.TransactionControlUtils;
import com.devteria.partnersession.dto.request.*;
import com.devteria.partnersession.dto.response.ArgumentControlByMonthResponse;
import com.devteria.partnersession.dto.response.ArgumentControlProIrisByMonth;
import com.devteria.partnersession.dto.response.ArgumentControlProIrisByMonthResponse;
import com.devteria.partnersession.dto.response.ArgumentControlStbProIrisByMonthResponse;
import com.devteria.partnersession.exception.AppException;
import com.devteria.partnersession.exception.ErrorCode;
import com.devteria.partnersession.mapper.TongThangReportMapper;
import com.devteria.partnersession.model.*;
import com.devteria.partnersession.repository.*;
import com.rabbitmq.client.Channel;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class ArgumentControlByMonthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentControlByMonthService.class);

    GetListCsvReport getListCsvReport;

    @Value("${proton.report.save.path}")
    @NonFinal
    private String reportSavePath;

    @Value("${proton.refund-transaction.save.path}")
    @NonFinal
    private String refundTransactionSavePath;

    TongThangReportMapper tongThangReportMapper;

    ControlSessionMonthRepository controlSessionMonthRepository;

    SessionSalesProMonthRepository sessionSalesProMonthRepository;

    SessionSalesProSupplyMonthRepository sessionSalesProSupplyMonthRepository;

    SessionProSupplyMonthRepository sessionProSupplyMonthRepository;

    SaveFileMonthRepository saveFileMonthRepository;

    @Value("${external.api.downloadfile-Proton}")
    @NonFinal
    private String downloadApiUrl;

    private final Queue<ArgumentControlByMonthRequest> requestQueue = new LinkedList<>();

    //    @Scheduled(fixedRate = 5000) // Kiểm tra hàng đợi mỗi giây
    public void processQueue() throws IOException {
        ArgumentControlByMonthRequest request = requestQueue.peek();
        if (request != null) {
            if (request.getSalesPartner() != null && request.getSupplyPartner() != null) {
                Optional<SessionSalesProSupplyMonth> session =
                        sessionSalesProSupplyMonthRepository.findSessionSalesProSupplyMonthByNameContaining(
                                request.getName());
                if (session.isPresent()
                        && (session.get().getStatus().equals("Đã hủy")
                                || session.get().getStatus().equals("Deleted"))) {
                    requestQueue.remove(request);
                } else if (session.isPresent() && session.get().getStatus().equals("Đang xử lý")) {
                    createArgumentControlByMonthToSPI(Objects.requireNonNull(requestQueue.poll()));
                }
            } else if (request.getSupplyPartner() == null) {
                Optional<SessionSalesProMonth> session =
                        sessionSalesProMonthRepository.findSessionMonthByNameContaining(request.getName());
                if (session.isPresent()
                        && (session.get().getStatus().equals("Đã hủy")
                                || session.get().getStatus().equals("Deleted"))) {
                    requestQueue.remove(request);
                } else if (session.isPresent() && session.get().getStatus().equals("Đang xử lý")) {
                    createArgumentControlByMonth(Objects.requireNonNull(requestQueue.poll()));
                }
            } else if (request.getSalesPartner() == null) {
                Optional<SessionProSupplyMonth> session =
                        sessionProSupplyMonthRepository.findSessionProSupplyMonthsByNameContaining(request.getName());
                if (session.isPresent()
                        && (session.get().getStatus().equals("Đã hủy")
                                || session.get().getStatus().equals("Deleted"))) {
                    requestQueue.remove(request);
                } else if (session.isPresent() && session.get().getStatus().equals("Đang xử lý")) {
                    createArgumentControlByMonthToIris(Objects.requireNonNull(requestQueue.poll()));
                }
            }
        }
    }

    @RabbitListener(queues = {"${rabbitmq.queue.json.month}"})
    public void handleMessage(
            ArgumentControlByMonthRequest argumentControlByMonthRequest,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag)
            throws Exception {
        try {
            // Xử lý message ở đây
            LOGGER.info(String.format("Received JSON message -> %s", argumentControlByMonthRequest.toString()));

            // Kiểm tra session và xử lý

            // Sau khi xử lý thành công, gửi ack để RabbitMQ gửi tiếp tin nhắn khác
            channel.basicAck(tag, false);
        } catch (Exception e) {
            // Nếu có lỗi, có thể gửi nack hoặc reject
            channel.basicNack(tag, false, true); // Hoặc có thể dùng basicReject
        }
    }

    public String createArgumentControlByMonth(ArgumentControlByMonthRequest request) throws IOException {
        Optional<SessionSalesProMonth> session =
                sessionSalesProMonthRepository.findSessionMonthByNameContaining(request.getName());
        if (session.get().getStatus().equals("Chờ xử lý")) {
            requestQueue.add(request);
            return null;
        }
        //        System.out.println(request.getValidFromDate());
        //        System.out.println(request.getValidToDate());
        try {
            DownloadRequest downloadRequestFile =
                    new DownloadRequest(request.getValidFromDate(), request.getValidToDate(), "month");
            downloadFileProtonTwoday(downloadRequestFile);
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Lỗi tải file xuống");
            sessionSalesProMonthRepository.save(session.get());
            Optional<SessionSalesProMonth> sessionNext =
                    sessionSalesProMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.PAYMENT_ERROR);
        }
        if (!session.get().getStatus().equals("Đang xử lý")) throw new AppException(ErrorCode.ERROR_STATUS);
        if (session.isEmpty()) {
            Optional<SessionSalesProMonth> sessionNext =
                    sessionSalesProMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.SESSION_NOT_EXISTED);
        }

        if (!"Sacombank".equalsIgnoreCase(request.getSalesPartner())) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Chưa có file của đối tác bán hàng");
            sessionSalesProMonthRepository.save(session.get());
            Optional<SessionSalesProMonth> sessionNext =
                    sessionSalesProMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.SALE_SUPPLIER_PARTNER_NOT_MATCH);
        }

        List<TongThangReport> tongThangReports = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Mồi M/yyyy");
        YearMonth requestMonthYear = YearMonth.parse(request.getDateControl(), formatter);
        YearMonth currentMonthYear = YearMonth.now();

        if (requestMonthYear.isAfter(currentMonthYear)) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Tháng yêu cầu không hợp lệ");

            throw new AppException(ErrorCode.MONTH_ERROR);
        }

        PathFileByDayRequest pathFileByDayRequest;
        try {
            pathFileByDayRequest = getPathfileRequest(request);
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Chưa có file");
            sessionSalesProMonthRepository.save(session.get());
            Optional<SessionSalesProMonth> sessionNext =
                    sessionSalesProMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.MONTH_ERROR);
        }

        FileResult fileResult = getAllFileNameInFolder(
                pathFileByDayRequest.getStbReportFilePath(), request.getYear(), request.getMonth());
        List<String> missDay = fileResult.getMissDay();
        List<String> listFileName = fileResult.getListFileName();
        if (!missDay.isEmpty()) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Lỗi chưa có file của các ngày: " + String.join(",", missDay));
            sessionSalesProMonthRepository.save(session.get());
            Optional<SessionSalesProMonth> sessionNext =
                    sessionSalesProMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.MISSING_DATE);
        }

        // Kiểm tra nếu tháng nhập sau tháng hiện tại
        if (requestMonthYear.isAfter(currentMonthYear)) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Chưa có file của đối tác bán hàng");
            sessionSalesProMonthRepository.save(session.get());
            Optional<SessionSalesProMonth> sessionNext =
                    sessionSalesProMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.MONTH_ERROR);
        }
        ProcessFolderRequest pathFileByMonthRequest;
        try {
            pathFileByMonthRequest = getPathfileByMonthRequest(request.getDateControl());
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Không có file ");
            sessionSalesProMonthRepository.save(session.get());
            Optional<SessionSalesProMonth> sessionNext =
                    sessionSalesProMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.MONTH_ERROR);
        }

        String[] dateComponents = request.getDateControl().split("/");
        String year = dateComponents[1];
        String month = dateComponents[0];

        ControlSessionMonth controlSessionMonthSave = new ControlSessionMonth();
        controlSessionMonthSave.setSessionName(request.getName());
        controlSessionMonthSave = controlSessionMonthRepository.save(controlSessionMonthSave);
        List<StbReport> stbReports = new ArrayList<>();
        List<ProtonReport> protonReports = new ArrayList<>();
        for (String filePath : listFileName) {
            try {
                File file = new File(filePath);
                if (file.isDirectory()) {
                    continue;
                }
                getListCsvReport.getListStbCsvReportMonth(
                        filePath, stbReports, request.getDateControl(), Constant.DS_TYPE.MONTH);
            } catch (IOException e) {
                session.get().setStatus("Thất bại");
                session.get().setMessage("Chưa có file của đối tác bán hàng");
                sessionSalesProMonthRepository.save(session.get());
                Optional<SessionSalesProMonth> sessionNext =
                        sessionSalesProMonthRepository.findTopByStatusEquals("Chờ xử lý");
                if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                    sessionNext.get().setStatus("Đang xử lý");
                    sessionSalesProMonthRepository.save(sessionNext.get());
                }
                throw new AppException(ErrorCode.MONTH_ERROR);
            }
        }
        getListCsvReport.getListProCsvReport(
                pathFileByMonthRequest.getProtonReportFilePath(), protonReports, Constant.DS_TYPE.MONTH);
        processTongThangProtonStb(stbReports, protonReports, tongThangReports, year, month, request.getName());

        Path directoryPath = Paths.get(refundTransactionSavePath, year, "Data-Month", "DS-T" + month);
        Files.createDirectories(directoryPath);

        String filePath = directoryPath + "\\" + request.getName() + ".xlsx";
        try {
            ExcelExporter.exportTongThangReportsToExcel(tongThangReports, filePath, "TongThang");
            session.get().setStatus("Thành công");
            session.get().setTotal((long) tongThangReports.size());
            sessionSalesProMonthRepository.save(session.get());
            Optional<SessionSalesProMonth> sessionNext =
                    sessionSalesProMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProMonthRepository.save(sessionNext.get());
            }
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Lưu file thất bại");
            sessionSalesProMonthRepository.save(session.get());
            Optional<SessionSalesProMonth> sessionNext =
                    sessionSalesProMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.CONTROL_SESSION_MONTH_EXISTED);
        }

        SaveFileMonth saveFileMonth = new SaveFileMonth(request.getName(), filePath);
        saveFileMonthRepository.save(saveFileMonth);

        return "Add success";
    }

    private void downloadFileProtonTwoday(DownloadRequest downloadRequest) {
        // Tạo RestTemplate để gọi API bên ngoài
        RestTemplate restTemplate = new RestTemplate();

        // Thiết lập headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Tạo HttpEntity với request body và headers
        HttpEntity<DownloadRequest> entity = new HttpEntity<>(downloadRequest, headers);

        // Gọi API bên ngoài
        restTemplate.exchange(downloadApiUrl, HttpMethod.POST, entity, byte[].class);
    }

    public ArgumentControlProIrisByMonthResponse createArgumentControlByMonthToIris(
            ArgumentControlByMonthRequest request) throws IOException {
        Optional<SessionProSupplyMonth> session =
                sessionProSupplyMonthRepository.findSessionProSupplyMonthsByNameContaining(request.getName());
        if (session.get().getStatus().equals("Chờ xử lý")) {
            requestQueue.add(request);
            return null;
        }
        try {
            DownloadRequest downloadRequestFile =
                    new DownloadRequest(request.getValidFromDate(), request.getValidToDate(), "month");
            downloadFileProtonTwoday(downloadRequestFile);
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Lỗi tải file xuống");
            sessionProSupplyMonthRepository.save(session.get());
            Optional<SessionProSupplyMonth> sessionNext =
                    sessionProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.PAYMENT_ERROR);
        }
        if (session.isEmpty()) {
            Optional<SessionProSupplyMonth> sessionNext =
                    sessionProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.SESSION_NOT_EXISTED);
        }

        if (!"Iris".equalsIgnoreCase(request.getSupplyPartner())) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Chưa có file của đối tác cung cap");
            sessionProSupplyMonthRepository.save(session.get());
            Optional<SessionProSupplyMonth> sessionNext =
                    sessionProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.SALE_SUPPLIER_PARTNER_NOT_MATCH);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
        YearMonth requestMonthYear = YearMonth.parse(request.getDateControl(), formatter);
        YearMonth currentMonthYear = YearMonth.now();
        // Kiểm tra nếu tháng nhập sau tháng hiện tại
        if (requestMonthYear.isAfter(currentMonthYear)) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Tháng đối soát không hợp lệ");
            sessionProSupplyMonthRepository.save(session.get());
            Optional<SessionProSupplyMonth> sessionNext =
                    sessionProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.MONTH_ERROR);
        }

        String[] dateComponents = request.getDateControl().split("/");
        String year = dateComponents[1];
        String month = dateComponents[0];

        ProcessFolderRequest pathFileByMonthRequest = null;
        List<IrisMonthReport> irisMonthReports = new ArrayList<>();
        List<ProtonReport> proMonthReports = new ArrayList<>();
        ArgumentControlProIrisByMonthResponse argumentControlProIrisByMonthResponse;
        List<TongThangProIrisReport> tongThangProIrisReport = new ArrayList<>();

        List<IrisMonthReport> irisMonReports = new ArrayList<>();
        List<ProtonReport> protonReports = new ArrayList<>();
        try {
            pathFileByMonthRequest = getPathfileByMonthRequest(request.getDateControl());
            getListIrisMonthReport(pathFileByMonthRequest.getIrisReportFilePath(), irisMonReports);
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Không có file của đối tác cung cấp");
            sessionProSupplyMonthRepository.save(session.get());
            Optional<SessionProSupplyMonth> sessionNext =
                    sessionProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.IRIS_FILE_NOT_FOUND);
        }
        try {
            getListCsvReport.getListProCsvReport(pathFileByMonthRequest.getProtonReportFilePath(), protonReports, 1);
            Map<String, ProtonReport> latestProtonReportsMap =
                    TransactionControlUtils.getLatestProtonReports(protonReports);

            protonReports = new ArrayList<>(latestProtonReportsMap.values());

            argumentControlProIrisByMonthResponse = new ArgumentControlProIrisByMonthResponse();

            processTongThangProtonIris(
                    tongThangProIrisReport,
                    argumentControlProIrisByMonthResponse,
                    irisMonReports,
                    protonReports,
                    irisMonthReports,
                    proMonthReports,
                    year,
                    month,
                    request.getName());
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Lỗi đọc file");
            sessionProSupplyMonthRepository.save(session.get());
            Optional<SessionProSupplyMonth> sessionNext =
                    sessionProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.MONTH_ERROR);
        }
        Path directoryPath = Paths.get(refundTransactionSavePath, year, "Data-Month", "DS-T" + month);
        Files.createDirectories(directoryPath);

        String filePath = directoryPath + "\\" + request.getName() + ".xlsx";
        try {
            String[] headers = {
                "STT",
                "Thời gian giao dịch",
                "Số tiền",
                "Số thuê bao",
                "Đối tác cung cấp",
                "Mã giao dịch - Iris",
                "Mã trace Proton",
                "Trạng thái",
                "Lý do không khớp"
            };
            ExcelExporter.writeToExcel(tongThangProIrisReport, headers, filePath, "TongThangProIrisReport");
            session.get().setStatus("Thành công");
            session.get().setTotal(argumentControlProIrisByMonthResponse.getProIrisCount());
            session.get().setTotalSuccess(argumentControlProIrisByMonthResponse.getProSucIrisSucCount());
            session.get().setTotalFail(argumentControlProIrisByMonthResponse.getProErrorIrisFailCount());
            session.get().setTotalNotMatch(argumentControlProIrisByMonthResponse.getNotMatchCount());
            sessionProSupplyMonthRepository.save(session.get());
            Optional<SessionProSupplyMonth> sessionNext =
                    sessionProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionProSupplyMonthRepository.save(sessionNext.get());
            }
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Lưu file thất bại");
            sessionProSupplyMonthRepository.save(session.get());
            Optional<SessionProSupplyMonth> sessionNext =
                    sessionProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.CONTROL_SESSION_MONTH_EXISTED);
        }

        return argumentControlProIrisByMonthResponse;
    }

    public ArgumentControlStbProIrisByMonthResponse createArgumentControlByMonthToSPI(
            ArgumentControlByMonthRequest request) throws IOException {
        Optional<SessionSalesProSupplyMonth> session =
                sessionSalesProSupplyMonthRepository.findSessionSalesProSupplyMonthByNameContaining(request.getName());
        if (session.get().getStatus().equals("Chờ xử lý")) {
            requestQueue.add(request);
            return null;
        }
        System.out.println(request.getValidFromDate());
        System.out.println(request.getValidToDate());
        try {
            DownloadRequest downloadRequestFile =
                    new DownloadRequest(request.getValidFromDate(), request.getValidToDate(), "month");
            downloadFileProtonTwoday(downloadRequestFile);
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Lỗi tải file xuống");
            sessionSalesProSupplyMonthRepository.save(session.get());
            Optional<SessionSalesProSupplyMonth> sessionNext =
                    sessionSalesProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.PAYMENT_ERROR);
        }
        System.out.println("done load");
        if (session.isEmpty()) {
            Optional<SessionSalesProSupplyMonth> sessionNext =
                    sessionSalesProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.SESSION_NOT_EXISTED);
        }

        if (!"Sacombank".equalsIgnoreCase(request.getSalesPartner())) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Chưa có file của đối tác bán hàng");
            sessionSalesProSupplyMonthRepository.save(session.get());
            Optional<SessionSalesProSupplyMonth> sessionNext =
                    sessionSalesProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.SALE_SUPPLIER_PARTNER_NOT_MATCH);
        }

        if (!"Iris".equalsIgnoreCase(request.getSupplyPartner())) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Chưa có file của đối tác cung cap");
            sessionSalesProSupplyMonthRepository.save(session.get());
            Optional<SessionSalesProSupplyMonth> sessionNext =
                    sessionSalesProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.SALE_SUPPLIER_PARTNER_NOT_MATCH);
        }

        List<TongThangStbProtonIris> tongThangStbProtonIrises = new ArrayList<>();
        PathFileByDayRequest pathFileByDayRequest;
        try {
            pathFileByDayRequest = getPathfileRequest(request);
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Chưa có file");
            sessionSalesProSupplyMonthRepository.save(session.get());
            Optional<SessionSalesProSupplyMonth> sessionNext =
                    sessionSalesProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.MONTH_ERROR);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
        YearMonth requestMonthYear = YearMonth.parse(request.getDateControl(), formatter);
        YearMonth currentMonthYear = YearMonth.now();
        // Kiểm tra nếu tháng nhập sau tháng hiện tại
        if (requestMonthYear.isAfter(currentMonthYear)) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Chưa có file của đối tác bán hàng");
            sessionSalesProSupplyMonthRepository.save(session.get());
            Optional<SessionSalesProSupplyMonth> sessionNext =
                    sessionSalesProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.MONTH_ERROR);
        }
        ProcessFolderRequest pathFileByMonthRequest;
        try {
            pathFileByMonthRequest = getPathfileByMonthRequest(request.getDateControl());
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Không có file ");
            sessionSalesProSupplyMonthRepository.save(session.get());
            Optional<SessionSalesProSupplyMonth> sessionNext =
                    sessionSalesProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.MONTH_ERROR);
        }
        FileResult fileResult = getAllFileNameInFolder(
                pathFileByDayRequest.getStbReportFilePath(), request.getYear(), request.getMonth());
        List<String> missDay = fileResult.getMissDay();
        List<String> listFileName = fileResult.getListFileName();

        String[] dateComponents = request.getDateControl().split("/");
        String year = dateComponents[1];
        String month = dateComponents[0];

        if (!missDay.isEmpty()) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Lỗi chưa có file của các ngày: " + String.join(",", missDay));
            sessionSalesProSupplyMonthRepository.save(session.get());
            Optional<SessionSalesProSupplyMonth> sessionNext =
                    sessionSalesProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.MISSING_DATE);
        }

        ArgumentControlStbProIrisByMonthResponse argumentControlStbProIrisByMonthResponse =
                new ArgumentControlStbProIrisByMonthResponse();
        ControlSessionMonth controlSessionMonthSave = new ControlSessionMonth();
        controlSessionMonthSave.setSessionName(request.getName());
        controlSessionMonthSave = controlSessionMonthRepository.save(controlSessionMonthSave);

        List<IrisMonthReport> irisMonthReports = new ArrayList<>();
        List<ProtonReport> proMonthReports = new ArrayList<>();
        List<IrisMonthReport> irisMonReports = new ArrayList<>();
        List<ProtonReport> protonReports = new ArrayList<>();

        try {
            pathFileByMonthRequest = getPathfileByMonthRequest(request.getDateControl());
            getListIrisMonthReport(pathFileByMonthRequest.getIrisReportFilePath(), irisMonReports);
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Không có file của đối tác cung cấp");
            sessionSalesProSupplyMonthRepository.save(session.get());
            Optional<SessionSalesProSupplyMonth> sessionNext =
                    sessionSalesProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.IRIS_FILE_NOT_FOUND);
        }
        List<StbReport> stbReports = new ArrayList<>();
        for (String filePath : listFileName) {
            File file = new File(filePath);
            if (file.isDirectory()) {
                continue;
            }
            getListCsvReport.getListStbCsvReportMonth(
                    filePath, stbReports, request.getDateControl(), Constant.DS_TYPE.MONTH);
        }
        getListCsvReport.getListProCsvReport(
                pathFileByMonthRequest.getProtonReportFilePath(), protonReports, Constant.DS_TYPE.MONTH);
        Map<String, ProtonReport> latestProtonReports = TransactionControlUtils.getLatestProtonReports(protonReports);
        Map<String, ProtonReport> getLatestIrisReports = TransactionControlUtils.getLatestIrisReports(protonReports);
        protonReports = new ArrayList<>(latestProtonReports.values());
        try {

            processDoiSoatProtonStbIris(
                    tongThangStbProtonIrises,
                    argumentControlStbProIrisByMonthResponse,
                    stbReports,
                    irisMonReports.reversed(),
                    protonReports,
                    irisMonthReports,
                    proMonthReports,
                    year,
                    month,
                    request.getName());

        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Chưa có file của đối tác bán hàng");
            sessionSalesProSupplyMonthRepository.save(session.get());
            Optional<SessionSalesProSupplyMonth> sessionNext =
                    sessionSalesProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.MONTH_ERROR);
        }
        Path directoryPath = Paths.get(refundTransactionSavePath, year, "Data-Month", "DS-T" + month);
        Files.createDirectories(directoryPath);

        List<TongThangStbProtonIris> tongThangList = new ArrayList<>();
        Set<String> irisSuc = new HashSet<>();
        Set<String> irisFail = new HashSet<>();
        Set<String> stbSuc = new HashSet<>();
        for (IrisMonthReport irisMonthReport : irisMonReports) {
            if (irisMonthReport.getTopupStatus().equals("Thành công")) {
                irisSuc.add(irisMonthReport.getTraceCode());
            }
            if (irisMonthReport.getTopupStatus().equals("Thất bại")) {
                irisFail.add(irisMonthReport.getTraceCode());
            }
        }
        for (StbReport stbReport : stbReports) {
            if (stbReport.getStatus().equals("THANH CONG")) {
                stbSuc.add(stbReport.getSystemTraceId());
            }
        }

        for (StbReport stbReport : stbReports) {
            String stbTraceCode = stbReport.getSystemTraceId().trim();
            ProtonReport protonReport = latestProtonReports.get(stbTraceCode);

            if (protonReport != null) {
                TongThangStbProtonIris tongThang =
                        convertToTongThangStbProtonIrisList(stbReport, protonReport, irisSuc, irisFail, stbSuc);
                tongThangList.add(tongThang);
            } else {
                TongThangStbProtonIris tongThang = convertToTongThangStbIrisList(stbReport, irisSuc, irisFail);
                tongThangList.add(tongThang);
            }
        }

        for (IrisMonthReport irisMonthReport : irisMonReports) {
            String irisTraceCode = irisMonthReport.getTraceCode();
            ProtonReport protonReport1 = getLatestIrisReports.get(irisTraceCode);
            if (protonReport1 == null) {
                TongThangStbProtonIris tongThang = convertToTongThangProtonIrisList(irisMonthReport, irisSuc, irisFail);
                tongThangList.add(tongThang);
            }
        }

        System.out.println("done");
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        tongThangList = tongThangList.stream()
                .sorted((report1, report2) -> {
                    try {
                        String timeCreated1 =
                                report1.getTimeCreated().replace("'", "").trim();
                        String timeCreated2 =
                                report2.getTimeCreated().replace("'", "").trim();
                        Date date1 = inputDateFormat.parse(timeCreated1);
                        Date date2 = inputDateFormat.parse(timeCreated2);
                        return date2.compareTo(date1); // Sắp xếp giảm dần
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return 0; // Nếu có lỗi phân tích ngày, giữ nguyên thứ tự
                    }
                })
                .collect(Collectors.toList());
        long i = 0;
        for (TongThangStbProtonIris tongThang : tongThangList) {
            tongThang.setStt(++i);
        }

        String filePath = directoryPath + "\\" + request.getName() + ".xlsx";
        try {
            String[] headers = {
                "STT",
                "Thời gian giao dịch",
                "Số tiền",
                "Mã tài khoản KH",
                "Đối tác bán hàng",
                "Đối tác cung cấp",
                "Mã giao dịch",
                "Mã trace",
                "Trạng thái",
                "Lý do không khớp"
            };
            ExcelExporter.writeToExcel(tongThangList, headers, filePath, "KetQua");
            session.get().setTotal(argumentControlStbProIrisByMonthResponse.getTotal());
            session.get().setTotalSuccess(argumentControlStbProIrisByMonthResponse.getTotalSuccess());
            session.get().setTotalFail(argumentControlStbProIrisByMonthResponse.getTotalFail());
            session.get().setTotalNotMatch(argumentControlStbProIrisByMonthResponse.getTotalNotMatch());
            session.get().setStatus("Thành công");
            sessionSalesProSupplyMonthRepository.save(session.get());
            Optional<SessionSalesProSupplyMonth> sessionNext =
                    sessionSalesProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProSupplyMonthRepository.save(sessionNext.get());
            }
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Chưa có file của đối tác bán hàng");
            sessionSalesProSupplyMonthRepository.save(session.get());
            Optional<SessionSalesProSupplyMonth> sessionNext =
                    sessionSalesProSupplyMonthRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionSalesProSupplyMonthRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.CONTROL_SESSION_MONTH_EXISTED);
        }

        SaveFileMonth saveFileMonth = new SaveFileMonth(request.getName(), filePath);
        saveFileMonthRepository.save(saveFileMonth);

        return argumentControlStbProIrisByMonthResponse;
    }

    private void processTongThangProtonIris(
            List<TongThangProIrisReport> tongThangProIrisReport,
            ArgumentControlProIrisByMonthResponse argumentControlProIrisByMonthResponse,
            List<IrisMonthReport> irisMonReports,
            List<ProtonReport> protonReports,
            List<IrisMonthReport> irisMonthReports,
            List<ProtonReport> proMonthReports,
            String year,
            String month,
            String sessionName) {

        Map<String, IrisMonthReport> irisMonthReportMap = new HashMap<>();
        for (IrisMonthReport irisMonthReport : irisMonReports) {
            irisMonthReportMap.put(irisMonthReport.getTraceCode(), irisMonthReport);
        }

        long irisSucTotalCount = irisMonReports.stream()
                .filter(report -> "Thành công".equalsIgnoreCase(report.getTopupStatus()))
                .count();

        long irisFailTotalCount = irisMonReports.stream()
                .filter(report -> "Thất bại".equalsIgnoreCase(report.getTopupStatus()))
                .count();
        long proSucTotalCount = protonReports.stream()
                .filter(report -> "Thành công".equalsIgnoreCase(report.getStatus()))
                .count();
        long proErrorTotalCount = protonReports.stream()
                .filter(report -> "Lỗi".equalsIgnoreCase(report.getStatus()))
                .count();
        long proTimeoutTotalCount = protonReports.stream()
                .filter(report -> "Timeout".equalsIgnoreCase(report.getStatus()))
                .count();

        // Bước 1: Tạo Map từ irisMonReports
        Map<String, IrisMonthReport> irisReportMap =
                irisMonReports.stream().collect(Collectors.toMap(IrisMonthReport::getTraceCode, Function.identity()));

        // Bước 2: Duyệt qua protonReports, tìm kiếm trong irisReportMap
        List<Pair<IrisMonthReport, ProtonReport>> mismatchedTransactions = new ArrayList<>();
        List<Pair<IrisMonthReport, ProtonReport>> matchedTransactions = new ArrayList<>();
        for (ProtonReport protonReport : protonReports) {
            IrisMonthReport irisReport = irisReportMap.get(protonReport.getRequestIdToPartner());
            if (irisReport != null) {
                // Kiểm tra trạng thái giao dịch
                if (!(irisReport.getTopupStatus().equals(protonReport.getStatus())
                        || "Thất bại".equalsIgnoreCase(irisReport.getTopupStatus())
                        || "Lỗi".equalsIgnoreCase(protonReport.getStatus()))) {
                    mismatchedTransactions.add(Pair.of(irisReport, protonReport));
                }
                matchedTransactions.add(Pair.of(irisReport, protonReport));
                // Đánh dấu giao dịch đã được đối soát
                irisReportMap.remove(protonReport.getRequestIdToPartner());
            } else {
                mismatchedTransactions.add(Pair.of(null, protonReport));
            }
        }

        // Bước 3: Duyệt qua các giao dịch còn lại trong irisReportMap
        for (IrisMonthReport irisReport : irisReportMap.values()) {
            mismatchedTransactions.add(Pair.of(irisReport, null));
        }

        // Danh sách các giao dịch lệch nhau
        long i = 1;
        long j = 1;
        long k = 1;
        List<IrisMonthReport> irisFailProtonSuc = new ArrayList<>();
        List<ProtonReport> proErrorIrisSuc = new ArrayList<>();
        for (Pair<IrisMonthReport, ProtonReport> pair : mismatchedTransactions) {
            IrisMonthReport irisReport = pair.getLeft();
            ProtonReport protonReport = pair.getRight();

            if (irisReport == null) {
                protonReport.setStt(i++);
                proMonthReports.add(protonReport);
                System.out.println("Giao dịch trong protonReports , không có trong irisMonReports: "
                        + protonReport.getRequestIdToPartner());
            }
            if (protonReport == null) {
                irisReport.setStt(j++);
                irisMonthReports.add(irisReport);
                System.out.println(
                        "Giao dịch trong irisMonReports, không có trong protonReports: " + irisReport.getTraceCode());
            } else if (irisReport != null) {
                irisReport.setStt(k);
                irisReport.setReasonsNotMatch("Proton thành công, Iris thất bại");
                protonReport.setStt(k++);
                protonReport.setReasonsNotMatch("Proton thất bại , Iris thành công");
                irisFailProtonSuc.add(irisReport);
                proErrorIrisSuc.add(protonReport);
                System.out.println("Giao dịch lệch nhau: " + irisReport.getTraceCode() + " - Trạng thái: "
                        + irisReport.getTopupStatus() + " (irisMonReports) vs. " + protonReport.getStatus()
                        + " (protonReports)");
            }
        }
        long proIrisCount = 0;
        for (Pair<IrisMonthReport, ProtonReport> pair : matchedTransactions) {
            String matched = null;
            tongThangProIrisReport.add(createTongThangProIrisReport(pair, ++proIrisCount, matched));
        }
        for (Pair<IrisMonthReport, ProtonReport> pair : mismatchedTransactions) {
            String matched = "Không khớp";
            tongThangProIrisReport.add(createTongThangProIrisReport(pair, ++proIrisCount, matched));
        }
        ArgumentControlProIrisByMonth argumentControlProIrisByMonth = new ArgumentControlProIrisByMonth();
        argumentControlProIrisByMonth.setProSucIrisSuc(matchedTransactions.stream()
                .map(Pair::getLeft)
                .filter(report -> "Thành công".equalsIgnoreCase(report.getTopupStatus()))
                .collect(Collectors.toList()));
        argumentControlProIrisByMonth.setProErrorIrisFail(matchedTransactions.stream()
                .map(Pair::getLeft)
                .filter(report -> "Thất bại".equalsIgnoreCase(report.getTopupStatus()))
                .collect(Collectors.toList()));
        argumentControlProIrisByMonth.setProNotExistIrisSuc(irisMonthReports.stream()
                .filter(report -> "Thành công".equalsIgnoreCase(report.getTopupStatus()))
                .peek(report -> report.setReasonsNotMatch("Proton không, Iris có"))
                .collect(Collectors.toList()));
        argumentControlProIrisByMonth.setProNotExistIrisFail(irisMonthReports.stream()
                .filter(report -> "Thất bại".equalsIgnoreCase(report.getTopupStatus()))
                .peek(report -> report.setReasonsNotMatch("Proton không, Iris có"))
                .collect(Collectors.toList()));
        argumentControlProIrisByMonth.setIrisNotExistProSuc(proMonthReports.stream()
                .filter(report -> "Thành công".equalsIgnoreCase(report.getStatus()))
                .peek(report -> report.setReasonsNotMatch("Proton có, Iris không"))
                .collect(Collectors.toList()));
        argumentControlProIrisByMonth.setIrisNotExistProError(proMonthReports.stream()
                .filter(report -> "Lỗi".equalsIgnoreCase(report.getStatus()))
                .peek(report -> report.setReasonsNotMatch("Proton có, Iris không"))
                .collect(Collectors.toList()));
        argumentControlProIrisByMonthResponse.setProIrisCount(proIrisCount);
        argumentControlProIrisByMonth.setIrisFailProtonSuc(irisFailProtonSuc);
        argumentControlProIrisByMonth.setProErrorIrisSuc(proErrorIrisSuc);
        argumentControlProIrisByMonthResponse.setProErrorIrisFailCount(
                (long) argumentControlProIrisByMonth.getProErrorIrisFail().size());
        argumentControlProIrisByMonthResponse.setProSucIrisSucCount(
                (long) argumentControlProIrisByMonth.getProSucIrisSuc().size());
        argumentControlProIrisByMonthResponse.setNotMatchCount((long) mismatchedTransactions.size());
    }

    public ArgumentControlByMonthResponse getArgumentControlByMonthStatus() {
        return new ArgumentControlByMonthResponse();
    }

    public ProcessFolderRequest getPathfileByMonthRequest(String dateString) throws IOException {
        ProcessFolderRequest pathFileByMonthRequest = new ProcessFolderRequest();
        String[] dateComponents = dateString.split("/");
        String year = dateComponents[1];
        String month = dateComponents[0];
        String folderPathPro = reportSavePath + "\\" + year + "\\Proton" + "\\DS-T" + month;
        String folderPathIris = reportSavePath + "\\" + year + "\\Iris" + "\\DS-T" + month;

        File folder = new File(folderPathPro);
        File[] files = folder.listFiles();
        File folderIris = new File(folderPathIris);
        File[] filesIris = folderIris.listFiles();

        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();
                if (fileName.startsWith("report_detail")) {
                    pathFileByMonthRequest.setProtonReportFilePath(folderPathPro);
                }
            }
        }
        for (File file : filesIris) {
            if (file.isFile()) {
                String fileName = file.getName();
                if (Character.isDigit(fileName.charAt(0))) {
                    pathFileByMonthRequest.setIrisReportFilePath(folderPathIris);
                }
            }
        }
        return pathFileByMonthRequest;
    }

    public ControlSessionMonth findOrCreateControlSessionMonth(String sessionName, String year, String month) {

        ControlSessionMonth controlSessionMonth = controlSessionMonthRepository.findBySessionName(sessionName);
        if (controlSessionMonth == null) {
            controlSessionMonth = new ControlSessionMonth();
            controlSessionMonth.setSessionName(sessionName);
            controlSessionMonth = controlSessionMonthRepository.save(controlSessionMonth);
        }
        return controlSessionMonth;
    }

    public void processDoiSoatProtonStbIris(
            List<TongThangStbProtonIris> tongThangStbProtonIrises,
            ArgumentControlStbProIrisByMonthResponse argumentControlStbProIrisByMonthResponse,
            List<StbReport> stbReports,
            List<IrisMonthReport> irisMonReports,
            List<ProtonReport> protonReports,
            List<IrisMonthReport> irisMonthReports,
            List<ProtonReport> proMonthReports,
            String year,
            String month,
            String sessionName) {

        Map<String, ProtonReport> latestProtonReports = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        for (ProtonReport protonReport : protonReports) {
            String traceId = protonReport.getSystemTraceId();
            try {
                Date protonTimestamp = dateFormat.parse(protonReport.getTimeCreated());
                ProtonReport existingReport = latestProtonReports.get(traceId);
                if (existingReport == null
                        || protonTimestamp.after(dateFormat.parse(existingReport.getTimeCreated()))) {
                    latestProtonReports.put(traceId, protonReport);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        List<StbReport> successfulStbProtonAndIrisSuccess = new ArrayList<>();
        List<StbReport> successfulStbProtonErrorIrisFail = new ArrayList<>();
        List<StbReport> successfulStbReportIrisFail = new ArrayList<>();
        List<StbReport> successfulStbReportProtonFailIrisSuccess = new ArrayList<>();
        List<StbReport> successfulStbReportsProtonExistIrisNotExist = new ArrayList<>();
        List<StbReport> successfulStbProtonNotExist = new ArrayList<>();

        Set<String> latestIrisReportsSus = new HashSet<>();
        Set<String> latestIrisReportsFail = new HashSet<>();
        Set<String> latestIrisNotExist = new HashSet<>();
        for (IrisMonthReport irisMonthReport : irisMonReports) {
            latestIrisNotExist.add(irisMonthReport.getTraceCode());
        }
        for (IrisMonthReport irisMonthReport : irisMonReports) {
            if (irisMonthReport.getTopupStatus().equals("Thành công"))
                latestIrisReportsSus.add(irisMonthReport.getTraceCode());
        }
        for (IrisMonthReport irisMonthReport : irisMonReports) {
            if (irisMonthReport.getTopupStatus().equals("Thất bại"))
                latestIrisReportsFail.add(irisMonthReport.getTraceCode());
        }
        for (StbReport stbReport : stbReports) {
            String stbTraceCode = stbReport.getSystemTraceId();
            ProtonReport protonReport = latestProtonReports.get(stbTraceCode);
            if (protonReport == null && stbReport.getStatus().equals("THANH CONG")) {
                successfulStbProtonNotExist.add(stbReport);
            } else if (protonReport != null
                    && protonReport.getStatus().equals("Thành công")
                    && stbReport.getStatus().equals("THANH CONG")) {
                if (latestIrisReportsSus.contains(protonReport.getRequestIdToPartner())) {
                    successfulStbProtonAndIrisSuccess.add(stbReport);
                } else if (latestIrisReportsFail.contains(protonReport.getRequestIdToPartner())) {
                    successfulStbReportIrisFail.add(stbReport);
                } else System.out.println("111");
            } else if (protonReport != null
                    && protonReport.getStatus().equals("Lỗi")
                    && stbReport.getStatus().equals("THANH CONG")) {
                if (latestIrisReportsFail.contains(protonReport.getRequestIdToPartner())) {
                    successfulStbProtonErrorIrisFail.add(stbReport);
                } else if (latestIrisReportsSus.contains(protonReport.getRequestIdToPartner())) {
                    successfulStbReportProtonFailIrisSuccess.add(stbReport);
                } else successfulStbReportsProtonExistIrisNotExist.add(stbReport);

            } else if (protonReport != null && stbReport.getStatus().equals("THANH CONG")) {
                if (!latestIrisNotExist.contains(protonReport.getRequestIdToPartner())) {
                    successfulStbReportsProtonExistIrisNotExist.add(stbReport);
                } else System.out.println("timeout123");
            } else {
                System.out.println("nooon");
            }
        }
        int count = 0;
        System.out.println("Giao dịch thành công bên STB, Proton và Iris: ");
        if (!successfulStbProtonAndIrisSuccess.isEmpty()) {
            for (StbReport report : successfulStbProtonAndIrisSuccess) {
                count++;
            }
            System.out.println("tong so luong : " + count);
        } else {
            System.out.println("Không có giao dịch nào");
        }
        argumentControlStbProIrisByMonthResponse.setTotal((long) stbReports.size());
        argumentControlStbProIrisByMonthResponse.setTotalSuccess((long) count);

        for (StbReport stbReport : stbReports) {}

        int count1 = 0;
        System.out.println("Giao dịch thành công bên STB, lỗi bên Proton và thất bại bên Iris: ");
        if (!successfulStbProtonErrorIrisFail.isEmpty()) {
            for (StbReport report : successfulStbProtonErrorIrisFail) {
                count1++;
            }
            System.out.println("tong so luong: " + count1);
        } else {
            System.out.println("Không có giao dịch nào");
        }
        argumentControlStbProIrisByMonthResponse.setTotalFail((long) count1);

        for (IrisMonthReport irisMonthReport : irisMonReports) {
            if (irisMonthReport.getTopupStatus().equals("Thất bại"))
                latestIrisReportsFail.add(irisMonthReport.getTraceCode());
        }
        for (StbReport stbReport : stbReports) {}

        int count2 = 0;
        System.out.println("Giao dịch thành công bên Stb và Proton, thất bại bên Iris:  ");
        if (!successfulStbReportIrisFail.isEmpty()) {
            for (StbReport report : successfulStbReportIrisFail) {
                count2++;
            }
            System.out.println("Tong so luong : " + count2);
        } else {
            System.out.println("Không có giao dịch nào ");
        }

        for (IrisMonthReport irisMonthReport : irisMonReports) {
            if (irisMonthReport.getTopupStatus().equals("Thành công"))
                latestIrisReportsSus.add(irisMonthReport.getTraceCode());
        }
        for (StbReport stbReport : stbReports) {}

        int count3 = 0;
        System.out.println("Giao dịch thành công bên Stb và Iris, Lỗi bên Proton:  ");
        if (!successfulStbReportProtonFailIrisSuccess.isEmpty()) {
            for (StbReport report : successfulStbReportProtonFailIrisSuccess) {
                count3++;
            }
            System.out.println("Tong so luong: " + count3);
        } else {
            System.out.println("Không có giao dịch nào ");
        }

        for (StbReport stbReport : stbReports) {}

        int count4 = 0;
        System.out.println("Giao dịch thành công bên STB, Proton có giao dịch và Iris không có: ");
        if (!successfulStbReportsProtonExistIrisNotExist.isEmpty()) {
            for (StbReport report : successfulStbReportsProtonExistIrisNotExist) {
                count4++;
            }
            System.out.println("Tong so luong: " + count4);
        } else {
            System.out.println("Không có giao dịch nào");
        }

        for (StbReport stbReport : stbReports) {}

        int count5 = 0;
        System.out.println("giao dịch thành công bên STB, Proton không có giao dịch: ");
        if (!successfulStbProtonNotExist.isEmpty()) {
            for (StbReport report : successfulStbProtonNotExist) {
                count5++;
            }
            System.out.println("tong so luong: " + count5);
        } else {
            System.out.println("Khong co giao dich nao");
        }
        argumentControlStbProIrisByMonthResponse.setTotalNotMatch((long) count3 + count4 + count5);

        List<StbReport> allStbProtonIris = new ArrayList<>();
        allStbProtonIris.addAll(successfulStbProtonAndIrisSuccess);
        allStbProtonIris.addAll(successfulStbProtonErrorIrisFail);
        allStbProtonIris.addAll(successfulStbReportIrisFail);
        allStbProtonIris.addAll(successfulStbReportProtonFailIrisSuccess);
        allStbProtonIris.addAll(successfulStbReportsProtonExistIrisNotExist);
        allStbProtonIris.addAll(successfulStbProtonNotExist);
        int count6 = 0;
        for (StbReport stbReport : allStbProtonIris) {
            count6++;
        }
        System.out.println("tong so luong: " + count6);
    }

    private static TongThangStbProtonIris convertToTongThangStbProtonIrisList(
            StbReport stbReport,
            ProtonReport protonReport,
            Set<String> irisSuc,
            Set<String> irisFail,
            Set<String> stbSuc) {
        TongThangStbProtonIris tongThangStbProtonIris = new TongThangStbProtonIris();
        tongThangStbProtonIris.setTimeCreated(stbReport.getCreateTime());
        tongThangStbProtonIris.setAmount(protonReport != null ? protonReport.getAmount() : stbReport.getAmount());
        tongThangStbProtonIris.setCustomAccountCode(stbReport.getCustomAccountCode());
        tongThangStbProtonIris.setSalesPartner("Sacombank");
        tongThangStbProtonIris.setSupplyPartner("Iris");
        tongThangStbProtonIris.setRequestIdToPartner(protonReport != null ? protonReport.getRequestIdToPartner() : "");
        tongThangStbProtonIris.setSystemTraceId(stbReport.getSystemTraceId());

        String status = "Không khớp";
        String reason = "";
        boolean protonSuccess = protonReport != null && protonReport.getStatus().equals("Thành công");
        boolean protonTime = protonReport != null && protonReport.getStatus().equals("Timeout");
        boolean protonFail = protonReport != null && protonReport.getStatus().equals("Lỗi");
        boolean stbSuccess = stbSuc.contains(protonReport.getSystemTraceId());
        boolean irisSuccess = irisSuc.contains(protonReport.getRequestIdToPartner());
        boolean irisFail1 = irisFail.contains(protonReport.getRequestIdToPartner());

        if ((protonSuccess || protonTime) && stbSuccess && irisSuccess) {
            status = "Thành công";
        } else if ((protonFail || protonTime) && irisFail1 && stbSuccess) {
            status = "Thất bại";
        } else {
            if (stbSuccess) {
                if (!irisSuccess && !irisFail1) {
                    reason = "STB thành công, Proton có, Iris không có.";
                } else if (protonFail && irisSuccess) {
                    reason = "STB thành công, Proton thất bại và Iris thành công.";
                } else if (protonSuccess && irisFail1) {
                    reason = "STB thành công, Proton thành công, Iris thất bại.";
                } else {
                    reason = "khong roi vao bat ki truong hop nao";
                }
            }
        }
        tongThangStbProtonIris.setStatus(status);
        tongThangStbProtonIris.setReasons(reason);

        return tongThangStbProtonIris;
    }

    private TongThangStbProtonIris convertToTongThangStbIrisList(
            StbReport stbReport, Set<String> irisSuc, Set<String> irisFail) {
        TongThangStbProtonIris tongThangStbProtonIris = new TongThangStbProtonIris();

        tongThangStbProtonIris.setTimeCreated(stbReport.getCreateTime());
        tongThangStbProtonIris.setAmount(stbReport.getAmount());
        tongThangStbProtonIris.setCustomAccountCode(stbReport.getCustomAccountCode());
        tongThangStbProtonIris.setSalesPartner("Sacombank");
        tongThangStbProtonIris.setSupplyPartner("Iris");
        tongThangStbProtonIris.setRequestIdToPartner("");
        tongThangStbProtonIris.setSystemTraceId(stbReport.getSystemTraceId());

        String status = "Không khớp";
        String reason = "";

        boolean stbSuccess = stbReport.getStatus().equals("THANH CONG");
        if (stbSuccess) {
            reason = "STB thành công, Proton không có.";
        }
        tongThangStbProtonIris.setStatus(status);
        tongThangStbProtonIris.setReasons(reason);

        return tongThangStbProtonIris;
    }

    private static TongThangStbProtonIris convertToTongThangProtonIrisList(
            IrisMonthReport irisMonthReport, Set<String> irisSuc, Set<String> irisFail) {
        TongThangStbProtonIris tongThangStbProtonIris = new TongThangStbProtonIris();
        tongThangStbProtonIris.setTimeCreated(irisMonthReport != null ? irisMonthReport.getTimeCreated() : "");
        tongThangStbProtonIris.setAmount(
                irisMonthReport != null ? irisMonthReport.getAmount() : irisMonthReport.getAmount());
        tongThangStbProtonIris.setCustomAccountCode(irisMonthReport.getPhoneNumber());
        tongThangStbProtonIris.setSalesPartner("Sacombank");
        tongThangStbProtonIris.setSupplyPartner("Iris");
        tongThangStbProtonIris.setRequestIdToPartner(irisMonthReport != null ? irisMonthReport.getTraceCode() : "");
        tongThangStbProtonIris.setSystemTraceId("");

        String status = "Không khớp";
        String reason = "";
        boolean irisSuccess = irisMonthReport.getTopupStatus().equals("Thành công");
        boolean irisFail1 = irisMonthReport.getTopupStatus().equals("Thất bại");

        if (irisSuccess) {
            reason = "Proton không có, Iris thành công";
        } else if (irisFail1) {
            reason = "Proton không có, Iris thất bại";
        }
        tongThangStbProtonIris.setStatus(status);
        tongThangStbProtonIris.setReasons(reason);

        return tongThangStbProtonIris;
    }

    public void processTongThangProtonStb(
            List<StbReport> stbReports,
            List<ProtonReport> protonReports,
            List<TongThangReport> tongThangReports,
            String year,
            String month,
            String sessionName) {

        Map<String, StbReport> stbReportMap = new HashMap<>();
        for (StbReport stbReport : stbReports) {
            stbReportMap.put(stbReport.getSystemTraceId(), stbReport);
        }

        ControlSessionMonth controlSessionMonth = findOrCreateControlSessionMonth(sessionName, year, month);
        List<RefundTransactionsByMonth> refundTransactionsList = new ArrayList<>();
        for (ProtonReport protonReport : protonReports) {
            if (protonReport.getStatus().equalsIgnoreCase("Thành công")) {
                StbReport stbReport = stbReportMap.get(protonReport.getSystemTraceId());
                if (stbReport != null) {
                    TongThangReport tongThangReport = createTongThangReport(protonReport, stbReport);
                    tongThangReports.add(tongThangReport);

                    RefundTransactionsByMonth refundTransaction =
                            tongThangReportMapper.toRefundTransactionByMonth(tongThangReport, controlSessionMonth);
                    refundTransactionsList.add(refundTransaction);
                }
            }
        }
    }

    private SessionSalesProMonth findOrCreateSessionSalesProMonth(String sessionName, String year, String month) {
        SessionSalesProMonth sessionSalesProMonth = new SessionSalesProMonth();
        sessionSalesProMonth.setName(sessionName);
        sessionSalesProMonth.setDateControl(new Date());
        sessionSalesProMonth.setDateCreated(new Date());
        sessionSalesProMonth.setDateUpdated(new Date());
        sessionSalesProMonth.setStatus("Processing");

        return sessionSalesProMonth;
    }

    private static TongThangReport createTongThangReport(ProtonReport protonReport, StbReport stbReport) {
        TongThangReport tongThangReport = new TongThangReport();
        DateTimeFormatter stbDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        tongThangReport.setTransactionNumberId(stbReport.getTransactionNumberId());
        try {
            String stbCreateTimeStr = stbReport.getCreateTime().trim();
            if (stbCreateTimeStr.startsWith("'")) {
                stbCreateTimeStr = stbCreateTimeStr.replaceFirst("^'", "");
            }
            LocalDateTime stbCreateTime = LocalDateTime.parse(stbCreateTimeStr, stbDateTimeFormatter);
            tongThangReport.setTimeCreated(stbCreateTime.format(stbDateTimeFormatter));
            tongThangReport.setEducationalTime(stbCreateTime.format(stbDateTimeFormatter));
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Unparseable date from stbReport : " + stbReport.getCreateTime(), e);
        }
        tongThangReport.setAmount(protonReport.getAmount());
        tongThangReport.setCustomAccountCode(stbReport.getCustomAccountCode());
        tongThangReport.setRecordedAccountNumber(stbReport.getRecordedAccountNumber());
        tongThangReport.setExplain(stbReport.getExplain());
        tongThangReport.setTraceNo(stbReport.getExplain());
        tongThangReport.setServiceProvider("TOPUP");
        tongThangReport.setPaymentService("VNP");
        tongThangReport.setStatus(protonReport.getStatus());
        tongThangReport.setSystemTraceId(protonReport.getSystemTraceId());
        tongThangReport.setTraceCode(protonReport.getRequestIdToPartner());
        return tongThangReport;
    }

    private static TongThangProIrisReport createTongThangProIrisReport(
            Pair<IrisMonthReport, ProtonReport> pair, long i, String status) {
        IrisMonthReport irisMonthReport = pair.getLeft();
        ProtonReport protonReport = pair.getRight();

        TongThangProIrisReport proIrisReport = new TongThangProIrisReport();
        proIrisReport.setStt(i);
        proIrisReport.setTimeCreated(
                irisMonthReport != null ? irisMonthReport.getTimeCreated() : protonReport.getTimeCreated());
        proIrisReport.setAmount(irisMonthReport != null ? irisMonthReport.getAmount() : protonReport.getAmount());
        proIrisReport.setPhoneNumber(irisMonthReport != null ? irisMonthReport.getPhoneNumber() : "");
        proIrisReport.setSupplyPartner(protonReport != null ? protonReport.getSupplyPartner() : "");
        proIrisReport.setIrisTraceCode(
                irisMonthReport != null ? irisMonthReport.getTraceCode() : protonReport.getRequestIdToPartner());
        proIrisReport.setStbSystemTraceId(protonReport != null ? protonReport.getSystemTraceId() : "");
        proIrisReport.setStatus(
                status == null ? Objects.requireNonNull(irisMonthReport).getTopupStatus() : status);
        proIrisReport.setReasonsNotMatch(
                status != null
                        ? ("Proton " + (protonReport != null ? protonReport.getStatus() : "Không có") + ",Iris "
                                + (irisMonthReport != null ? irisMonthReport.getTopupStatus() : "Không có"))
                        : "");
        return proIrisReport;
    }

    public FileResult getAllFileNameInFolder(String basePath, int year, int month) throws IOException {
        List<String> listFileName = new ArrayList<>();
        Set<String> fileDates = new HashSet<>();
        YearMonth yearMonth = YearMonth.of(year, month);

        File file = new File(basePath);
        if (!file.isDirectory()) {
            System.out.println("Đường dẫn không phải là folder!");
            return new FileResult(new ArrayList<>(), listFileName);
        }
        File[] files = file.listFiles();
        if (files == null) {
            System.out.println("Không thể truy cập vào thư mục!");
            return new FileResult(new ArrayList<>(), listFileName);
        }
        List<String> fileDateRange = null;
        for (File f : files) {
            if (f.isDirectory()) {
                continue;
            }
            try {
                String fileName = f.getName();
                String fpath = f.getAbsolutePath();
                listFileName.add(fpath);
                fileDateRange = getDateFromFileName(fileName);
                if (fileDateRange != null) {
                    fileDates.addAll(fileDateRange);
                }
            } catch (Exception e) {
                System.out.println("Không thể đọc file : " + f.getAbsolutePath());
                e.printStackTrace();
            }
        }
        Collections.sort(fileDateRange);
        List<String> allDatesInMonth = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            allDatesInMonth.add(date.format(formatter));
        }

        List<String> missDay = new ArrayList<>();
        for (String date : allDatesInMonth) {
            if (!fileDates.contains(date)) {
                String day = date.substring(0, 2);
                missDay.add(day);
            }
        }
        if (!missDay.isEmpty()) {
            for (String date : missDay) {
                System.out.println(date);
            }
        }
        return new FileResult(missDay, listFileName);
    }

    public class FileResult {
        private List<String> missDay;
        private List<String> listFileName;

        public FileResult(List<String> missDay, List<String> listFileName) {
            this.missDay = missDay;
            this.listFileName = listFileName;
        }

        public List<String> getMissDay() {
            return missDay;
        }

        public List<String> getListFileName() {
            return listFileName;
        }
    }

    private static List<String> getDateFromFileName(String fileName) {
        try {
            String startDateString = fileName.substring(14, 22);
            String endDateString = fileName.substring(23, 31);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
            LocalDate startDate = LocalDate.parse(startDateString, formatter);
            LocalDate endDate = LocalDate.parse(endDateString, formatter);

            List<String> dateRange = new ArrayList<>();
            while (!startDate.isAfter(endDate)) {
                dateRange.add(startDate.format(formatter));
                startDate = startDate.plusDays(1);
            }
            return dateRange;
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            System.out.println("Tên file không hợp lệ: " + fileName);
            return null;
        }
    }

    public PathFileByDayRequest getPathfileRequest(ArgumentControlByMonthRequest request) {
        PathFileByDayRequest filePaths = new PathFileByDayRequest();
        String[] dateComponents = request.getDateControl().split("/");
        int year = Integer.parseInt(dateComponents[1]);
        int month = Integer.parseInt(dateComponents[0]);
        String monthStr = String.format("DS-T%02d", month);
        filePaths.setStbReportFilePath(
                String.format(reportSavePath + "\\%d\\" + request.getSalesPartner() + "\\%s", year, monthStr));
        return filePaths;
    }

    public void getListIrisMonthReport(String irisReportFilePath, List<IrisMonthReport> irisMonthReports)
            throws IOException {
        LOGGER.info("doc file xlsx: {} ", irisReportFilePath);
        var ref = new Object() {
            long i = 0;
        };
        File folder = new File(irisReportFilePath);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {

                    try (InputStream is = new FileInputStream(file.getAbsoluteFile());
                            ReadableWorkbook wb = new ReadableWorkbook(is)) {

                        StopWatch watch = new StopWatch();
                        watch.start();
                        // Lấy sheet 2
                        Optional<Sheet> sheet2 = wb.getSheet(1);

                        try (Stream<Row> rows = sheet2.get().openStream()) {
                            rows.skip(1).forEach(r -> {
                                // Tạo một đối tượng IrisReport
                                IrisMonthReport irisMonthReport = new IrisMonthReport();
                                if (r.getCellAsNumber(4).isEmpty()) {
                                    // Nếu bằng null, thoát khỏi vòng lặp
                                    return;
                                }
                                // Lấy giá trị các ô và gán vào đối tượng IrisReport
                                irisMonthReport.setTimeCreated(
                                        r.getCellAsString(0).orElse(null));

                                irisMonthReport.setPartnerName(
                                        r.getCellAsString(1).orElse(null));

                                irisMonthReport.setTraceCode(
                                        r.getCellAsString(2).orElse(null));

                                irisMonthReport.setPhoneNumber(
                                        r.getCellAsString(3).orElse(null));
                                if (r.getCellAsNumber(4).isEmpty())
                                    System.out.println("NULLPoint" + r.getCellAsString(2));

                                irisMonthReport.setAmount(Objects.requireNonNull(
                                                r.getCellAsNumber(4).orElse(BigDecimal.ZERO))
                                        .longValue());

                                irisMonthReport.setTelco(r.getCellAsString(5).orElse(null));

                                irisMonthReport.setTopupStatus(
                                        r.getCellAsString(6).orElse(null));

                                irisMonthReport.setTopupStatusCode(
                                        r.getCellAsString(7).orElse(null));
                                // Thêm IrisReport vào danh sách
                                irisMonthReports.add(irisMonthReport);
                                ref.i = ref.i + 1;
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        watch.stop();
                        //                        LOGGER.info("Processing time :: " + ref.i + "kkkk" +
                        // watch.getTime(TimeUnit.MILLISECONDS));
                    }
                }
            }
        }
    }
}
