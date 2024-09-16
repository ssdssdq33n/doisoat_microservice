package com.devteria.partnersession.service;

import static com.devteria.partnersession.Constants.Constant.DS_TYPE.DAY;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.support.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.devteria.partnersession.common.utils.TransactionControlUtils;
import com.devteria.partnersession.dto.request.ArgumentControlByDayRequest;
import com.devteria.partnersession.dto.request.DownloadRequest;
import com.devteria.partnersession.dto.request.PathFileByDayRequest;
import com.devteria.partnersession.dto.response.ArgumentControlByDay;
import com.devteria.partnersession.dto.response.ArgumentControlByDayResponse;
import com.devteria.partnersession.exception.AppException;
import com.devteria.partnersession.exception.ErrorCode;
import com.devteria.partnersession.mapper.ArgumentControlByDayMapper;
import com.devteria.partnersession.model.*;
import com.devteria.partnersession.repository.ControlSessionDayRepository;
import com.devteria.partnersession.repository.RefundTransactionsByDayRepository;
import com.devteria.partnersession.repository.SessionRepository;
import com.rabbitmq.client.Channel;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class ArgumentControlByDayService {
    private static final Logger log = LoggerFactory.getLogger(ArgumentControlByDayService.class);

    GetListCsvReport getListCsvReport;

    GetListXlsxReport getListXlsxReport;

    ProcessDoiSoatProtonIris processDoiSoatProtonIris;

    ProcessDoiSoatProtonStb processDoiSoatProtonStb;

    ArgumentControlByDayMapper argumentControlByDayMapper;

    ControlSessionDayRepository controlSessionDayRepository;

    RefundTransactionsByDayRepository refundTransactionsByDayRepository;

    SessionRepository sessionRepository;

    @Value("${proton.report.save.path}")
    @NonFinal
    private String fileSavePath;

    @Value("${external.api.downloadfile-Proton}")
    @NonFinal
    private String downloadApiUrl;

    private final Queue<ArgumentControlByDayRequest> requestQueue = new LinkedList<>();

    //    @Scheduled(fixedRate = 1000) // Kiểm tra hàng đợi mỗi giây
    public void processQueue() throws IOException {
        ArgumentControlByDayRequest request = requestQueue.peek();
        if (request != null) {
            Optional<Session> session = sessionRepository.findSessionByNameContaining(request.getName());
            if (session.isPresent() && session.get().getStatus().equals("Đang xử lý")) {
                createArgumentControlByDay(Objects.requireNonNull(requestQueue.poll()));
            } else if (session.isPresent()
                    && (session.get().getStatus().equals("Đã hủy")
                            || session.get().getStatus().equals("Deleted"))) {
                requestQueue.remove(request);
            }
        }
    }

    //    @RabbitListener(queues = {"${rabbitmq.queue.json.name}"})
    //    public void consumerJsonMessage(ArgumentControlByDayRequest argumentControlByDayRequest){
    //        log.info(String.format("Received JSON message -> %s", argumentControlByDayRequest.toString()));
    //
    //        // Kiểm tra session và xử lý
    //        Optional<Session> session =
    // sessionRepository.findSessionByNameContaining(argumentControlByDayRequest.getName());
    //        if (session.isPresent() && session.get().getStatus().equals("Đang xử lý")) {
    //            try {
    //                createArgumentControlByDay(argumentControlByDayRequest);
    //            } catch (IOException e) {
    //                throw new RuntimeException("Lỗi : "+e);
    //
    //            }
    //        }
    //    }

    @RabbitListener(queues = {"${rabbitmq.queue.json.name}"})
    public void handleMessage(
            ArgumentControlByDayRequest argumentControlByDayRequest,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag)
            throws Exception {
        try {
            // Xử lý message ở đây
            log.info(String.format("Received JSON message -> %s", argumentControlByDayRequest.toString()));

            // Kiểm tra session và xử lý
            Optional<Session> session =
                    sessionRepository.findSessionByNameContaining(argumentControlByDayRequest.getName());
            if (session.isPresent() && session.get().getStatus().equals("Đang xử lý")) {
                try {
                    createArgumentControlByDay(argumentControlByDayRequest);
                } catch (IOException e) {
                    throw new RuntimeException("Lỗi : " + e);
                }
            }

            // Sau khi xử lý thành công, gửi ack để RabbitMQ gửi tiếp tin nhắn khác
            channel.basicAck(tag, false);
        } catch (Exception e) {
            // Nếu có lỗi, có thể gửi nack hoặc reject
            channel.basicNack(tag, false, true); // Hoặc có thể dùng basicReject
        }
    }

    //    @RabbitListener(queues = {"${rabbitmq.queue.json.name}"}, containerFactory = "myFactory")
    //    public void consumerJsonMessage(@Payload ArgumentControlByDayRequest argumentControlByDayRequest, Message
    // message, Channel channel) {
    //        try {
    //            // Xử lý message
    //            log.info(String.format("Received JSON message -> %s", argumentControlByDayRequest.toString()));
    //
    //            // Kiểm tra session và xử lý
    //            Optional<Session> session =
    // sessionRepository.findSessionByNameContaining(argumentControlByDayRequest.getName());
    //            if (session.isPresent() && session.get().getStatus().equals("Đang xử lý")) {
    //                createArgumentControlByDay(argumentControlByDayRequest);
    //            }
    //
    //            // Lấy deliveryTag từ Message object để xác nhận rằng message đã được xử lý thành công
    //            long deliveryTag = message.getMessageProperties().getDeliveryTag();
    //            channel.basicAck(deliveryTag, false);  // false = chỉ acknowledge cho message này
    //        } catch (Exception e) {
    //            // Xử lý khi gặp lỗi
    //            System.err.println("Error processing message: " + e.getMessage());
    //
    //            // Tùy thuộc vào loại lỗi, bạn có thể quyết định reject hoặc nack
    //            try {
    //                long deliveryTag = message.getMessageProperties().getDeliveryTag();
    //                channel.basicNack(deliveryTag, false, true); // Nack và requeue lại message
    //            } catch (Exception nackEx) {
    //                nackEx.printStackTrace();
    //            }
    //        }
    //    }

    //    @PreAuthorize("hasAuthority('CREATE_SESSION_DAY')")
    public ArgumentControlByDayResponse createArgumentControlByDay(ArgumentControlByDayRequest request)
            throws IOException {
        Optional<Session> session = sessionRepository.findSessionByNameContaining(request.getName());
        //        if (session.get().getStatus().equals("Chờ xử lý")) {
        //            requestQueue.add(request);
        //            return null;
        //        }
        //        try {
        //            DownloadRequest downloadRequestFile =
        //                    new DownloadRequest(request.getValidFromDate(), request.getValidToDate(), "day");
        //            downloadFileProtonTwoday(downloadRequestFile);
        //        } catch (Exception e) {
        //            session.get().setStatus("Thất bại");
        //            session.get().setMessage("Lỗi tải file xuống");
        //            sessionRepository.save(session.get());
        //            Optional<Session> sessionNext = sessionRepository.findTopByStatusEquals("Chờ xử lý");
        //            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
        //                sessionNext.get().setStatus("Đang xử lý");
        //                sessionRepository.save(sessionNext.get());
        //            }
        //            throw new AppException(ErrorCode.PAYMENT_ERROR);
        //        }

        LocalDate date = LocalDate.parse(request.getDateControl(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        LocalDate currentDate = LocalDate.now();
        DayOfWeek currentDateDayOfWeek = currentDate.getDayOfWeek();
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if ((currentDateDayOfWeek == DayOfWeek.SATURDAY || currentDateDayOfWeek == DayOfWeek.SUNDAY)
                && currentDate.isBefore(date.plusDays(3))) {
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.FRIDAY) {
                session.get().setStatus("Thất bại");
                session.get().setMessage("Chưa có file của ngày này");
                sessionRepository.save(session.get());
                Optional<Session> sessionNext = sessionRepository.findTopByStatusEquals("Chờ xử lý");
                if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                    sessionNext.get().setStatus("Đang xử lý");
                    sessionRepository.save(sessionNext.get());
                }
                throw new AppException(ErrorCode.DAY_ERROR);
            }
        }
        // **
        if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            int daysToSunday = DayOfWeek.SUNDAY.getValue() - dayOfWeek.getValue();
            date = date.plusDays(daysToSunday);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            request.setDateControl(date.format(formatter));
        }

        if (session.isEmpty()) {
            Optional<Session> sessionNext = sessionRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.SESSION_NOT_EXISTED);
        }
        if (session.get().getStatus().equals("Thành công")
                || session.get().getStatus().equals("Thất bại")
                || session.get().getStatus().equals("Đã hủy")) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Không thể đối soát");
            sessionRepository.save(session.get());
            Optional<Session> sessionNext = sessionRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.ERROR_STATUS);
        }
        if (!"Sacombank".equalsIgnoreCase(request.getSalesPartner())
                || !"Iris".equalsIgnoreCase(request.getSupplyPartner())) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Chưa có file của đối tác bán hàng hoặc cung cấp");
            sessionRepository.save(session.get());
            Optional<Session> sessionNext = sessionRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.SALE_SUPPLIER_PARTNER_NOT_MATCH);
        }
        PathFileByDayRequest pathFileByDayRequest = new PathFileByDayRequest();
        try {
            pathFileByDayRequest = getPathfileByDayRequest(request);
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Chưa có file của ngày này");
            sessionRepository.save(session.get());
            Optional<Session> sessionNext = sessionRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.DAY_ERROR);
        }

        ArgumentControlByDay argumentControlByDay = new ArgumentControlByDay();
        List<StbReport> stbReports = new ArrayList<>();
        List<ProtonReport> protonReports = new ArrayList<>();
        List<IrisReport> irisReports = new ArrayList<>();
        System.out.println("done pathFileByDayRequest");
        try {
            getListCsvReport.getListStbCsvReport(pathFileByDayRequest.getStbReportFilePath(), stbReports, DAY);
            getListCsvReport.getListProCsvReport(pathFileByDayRequest.getProtonReportFilePath(), protonReports, DAY);
            getListXlsxReport.getListIrisReport(pathFileByDayRequest.getIrisReportFilePath(), irisReports);
        } catch (Exception e) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Chưa có file của ngày này");
            sessionRepository.save(session.get());
            Optional<Session> sessionNext = sessionRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.DAY_ERROR);
        }
        System.out.println("done iris");
        // bat dau chạy đối soát lưu vào controlSessionDay
        Optional<ControlSessionDay> controlSessionDay =
                controlSessionDayRepository.findBySessionNameEquals(request.getName());
        if (controlSessionDay.isPresent()) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Control session không tồn tại");
            sessionRepository.save(session.get());
            Optional<Session> sessionNext = sessionRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.CONTROL_SESSION_DAY_EXISTED);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate requestDate = LocalDate.parse(request.getDateControl(), formatter);
        if (requestDate.isAfter(currentDate)) {
            session.get().setStatus("Thất bại");
            session.get().setMessage("Ngày đối soát không hợp lệ");
            sessionRepository.save(session.get());
            Optional<Session> sessionNext = sessionRepository.findTopByStatusEquals("Chờ xử lý");
            if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                sessionNext.get().setStatus("Đang xử lý");
                sessionRepository.save(sessionNext.get());
            }
            throw new AppException(ErrorCode.DAY_ERROR);
        }
        ControlSessionDay controlSessionDaySave = new ControlSessionDay();
        controlSessionDaySave.setSessionName(request.getName());
        controlSessionDaySave = controlSessionDayRepository.save(controlSessionDaySave);

        // kết thúc lưu vào controlSessionDay

        processDoiSoatProtonIris.processDoiSoatProtonIris(stbReports, irisReports, protonReports, argumentControlByDay);

        List<ProtonReport> protonReports1 = new ArrayList<>();

        // neu co giao dich Iris Success mà pro khong co thi get 2 ngay ben proton
        if (!argumentControlByDay.getProNotExistIrisSuc().isEmpty()
                || !argumentControlByDay.getIrisUnknownProtonNotExist().isEmpty()
                || !argumentControlByDay.getIrisUnknown().isEmpty()
                || !argumentControlByDay.getProtonTimeoutIrisNotExist().isEmpty()
                || !argumentControlByDay.getProtonTimeout().isEmpty()
                || !argumentControlByDay.getProtonNotExist().isEmpty()) {

            DownloadRequest downloadRequest = new DownloadRequest();
            date = LocalDate.parse(request.getDateControl(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            LocalDate dateTo = date.plusDays(1);
            // Định dạng lại chuỗi ngày tháng
            String dayFrom = dateTo.format(DateTimeFormatter.ofPattern("d/M/yyyy"));
            String dayTo = date.plusDays(1).format(DateTimeFormatter.ofPattern("d/M/yyyy"));
            downloadRequest.setValidFromDate(dayTo);
            downloadRequest.setValidToDate(dayTo);
            downloadRequest.setDownloadFileCycle("day");
            try {
                String protonReportFilePathAfter = fileSavePath + "\\" + dateTo.getYear() + "\\Proton\\DS-T"
                        + String.format("%02d", dateTo.getMonthValue()) + "\\"
                        + String.format("%02d", dateTo.getDayOfMonth()) + "-"
                        + String.format("%02d", dateTo.getMonthValue());
                if (!Files.exists(Paths.get(protonReportFilePathAfter))) {
                    downloadFileProtonTwoday(downloadRequest);
                }
                List<StbReport> stbReports1 = new ArrayList<>();
                getListCsvReport.getListProCsvReport(protonReportFilePathAfter, protonReports1, DAY);
                Map<String, ProtonReport> latestProtonReportsMap =
                        TransactionControlUtils.getLatestProtonReports(protonReports1);

                protonReports1 = new ArrayList<>(latestProtonReportsMap.values());
                processIrisAndProtonReports(protonReports1, argumentControlByDay, protonReports);

            } catch (Exception e) {
                session.get().setStatus("Thất bại");
                session.get().setMessage("Không tải được file");
                sessionRepository.save(session.get());
                Optional<Session> sessionNext = sessionRepository.findTopByStatusEquals("Chờ xử lý");
                if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
                    sessionNext.get().setStatus("Đang xử lý");
                    sessionRepository.save(sessionNext.get());
                }
                throw new AppException(ErrorCode.PROTON_FILE_NOT_FOUND);
            }
        }
        argumentControlByDay = new ArgumentControlByDay();
        processDoiSoatProtonIris.processDoiSoatProtonIris(stbReports, irisReports, protonReports, argumentControlByDay);

        // ket thuc neu co giao dich Iris Success mà pro khong co
        processDoiSoatProtonStb.processDoiSoatProtonStb(
                stbReports,
                protonReports,
                irisReports,
                argumentControlByDay,
                pathFileByDayRequest.getProtonReportFilePath());
        ArgumentControlByDayResponse argumentControlByDayMapper1 =
                argumentControlByDayMapper.toArgumentControlByDayResponse(argumentControlByDay);

        List<RefundTransactionsByDay> refundTransactionsByDay = new ArrayList<>();
        List<StbReport> listRefundTransactionsByDay = argumentControlByDayMapper1.getListTranNeedRefunded();
        for (StbReport stbReport : listRefundTransactionsByDay) {
            RefundTransactionsByDay refundByDay = getRefundTransactionsByDay(stbReport, controlSessionDaySave);
            refundTransactionsByDay.add(refundByDay);
        }

        session.get().setStatus("Thành công");
        sessionRepository.save(session.get());

        Optional<Session> sessionNext = sessionRepository.findTopByStatusEquals("Chờ xử lý");
        if (sessionNext.isPresent() && sessionNext.get().getStatus().equals("Chờ xử lý")) {
            sessionNext.get().setStatus("Đang xử lý");
            sessionRepository.save(sessionNext.get());
        }

        refundTransactionsByDayRepository.saveAll(refundTransactionsByDay);

        log.info("Status session: Success");

        return argumentControlByDayMapper1;
    }

    public void processIrisAndProtonReports(
            List<ProtonReport> protonReports1,
            ArgumentControlByDay argumentControlByDay,
            List<ProtonReport> protonReports) {
        List<IrisReport> proNotExistIrisSuc = new ArrayList<>(argumentControlByDay.getProNotExistIrisSuc());
        List<IrisReport> irisUnknownProtonNotExist =
                new ArrayList<>(argumentControlByDay.getIrisUnknownProtonNotExist());
        List<IrisReport> irisUnknown = new ArrayList<>(argumentControlByDay.getIrisUnknown());
        List<ProtonReport> protonTimeoutIrisNotExist =
                new ArrayList<>(argumentControlByDay.getProtonTimeoutIrisNotExist());
        List<ProtonReport> protonTimeout = new ArrayList<>(argumentControlByDay.getProtonTimeout());
        List<StbReport> protonNotExist = new ArrayList<>(argumentControlByDay.getProtonNotExist());
        List<ProtonReport> proton1 = new ArrayList<>();
        List<ProtonReport> proton2 = new ArrayList<>();

        // Duyệt qua các danh sách Iris Report
        processIrisReports(
                protonReports1,
                proNotExistIrisSuc,
                irisUnknownProtonNotExist,
                protonTimeoutIrisNotExist,
                protonTimeout,
                protonNotExist,
                irisUnknown,
                protonReports,
                proton1,
                proton2);

        // Cập nhật lại các danh sách Iris Report trong ArgumentControlByDay
        argumentControlByDay.setProNotExistIrisSuc(proNotExistIrisSuc);
        argumentControlByDay.setIrisUnknownProtonNotExist(irisUnknownProtonNotExist);
        argumentControlByDay.setIrisUnknown(irisUnknown);
        argumentControlByDay.setProtonTimeoutIrisNotExist(protonTimeoutIrisNotExist);
        argumentControlByDay.getProtonTimeoutIrisNotExist().addAll(proton1);
        argumentControlByDay.setProtonTimeout(protonTimeout);
        argumentControlByDay.getProtonTimeout().addAll(proton2);

        argumentControlByDay.setProtonNotExist(protonNotExist);
    }

    private void processIrisReports(
            List<ProtonReport> protonReports1,
            List<IrisReport> proNotExistIrisSuc,
            List<IrisReport> irisUnknownProtonNotExist,
            List<ProtonReport> protonTimeoutIrisNotExist,
            List<ProtonReport> protonTimeout,
            List<StbReport> proNotExist,
            List<IrisReport> irisUnknown,
            List<ProtonReport> protonReports,
            List<ProtonReport> proton1,
            List<ProtonReport> proton2) {
        // Duyệt qua danh sách ProNotExistIrisSuc
        Iterator<IrisReport> proNotExistIrisSucIterator = proNotExistIrisSuc.iterator();
        processIrisReportList(protonReports1, proNotExistIrisSucIterator, protonReports);

        // Duyệt qua danh sách IrisUnknownProtonNotExist
        Iterator<IrisReport> irisUnknownProtonNotExistIterator = irisUnknownProtonNotExist.iterator();
        processIrisReportList(protonReports1, irisUnknownProtonNotExistIterator, protonReports);

        // Duyệt qua danh sách IrisUnknown
        Iterator<IrisReport> irisUnknownIterator = irisUnknown.iterator();
        processIrisReportList(protonReports1, irisUnknownIterator, protonReports);

        // Duyệt qua danh sách ProtonTimeoutIrisNotExist
        Iterator<ProtonReport> protonTimeoutIrisNotExistIterator = protonTimeoutIrisNotExist.iterator();
        processProtonReportList(protonReports1, protonTimeoutIrisNotExistIterator, protonReports, proton1);

        // Duyệt qua danh sách ProtonTimeout
        Iterator<ProtonReport> protonTimeoutIterator = protonTimeout.iterator();
        processProtonReportList(protonReports1, protonTimeoutIterator, protonReports, proton2);

        // Duyệt qua danh sách proNotExistIrisExist
        Iterator<StbReport> proNotExistIterator = proNotExist.iterator();
        processStbReportList(protonReports1, proNotExistIterator, protonReports);
    }

    private void processIrisReportList(
            List<ProtonReport> protonReports1,
            Iterator<IrisReport> irisReportIterator,
            List<ProtonReport> protonReports) {
        while (irisReportIterator.hasNext()) {
            IrisReport irisReport = irisReportIterator.next();
            for (ProtonReport protonReport : protonReports1) {
                if (irisReport.getTraceCode().equals(protonReport.getRequestIdToPartner())) {
                    irisReportIterator.remove();
                    protonReports.add(protonReport);
                    break;
                }
            }
        }
    }

    private void processProtonReportList(
            List<ProtonReport> protonReports1,
            Iterator<ProtonReport> protonReportIterator,
            List<ProtonReport> protonReports,
            List<ProtonReport> proton) {
        while (protonReportIterator.hasNext()) {
            ProtonReport protonReport = protonReportIterator.next();
            for (ProtonReport proReport : protonReports1) {
                if (proReport.getRequestIdToPartner().equals(protonReport.getRequestIdToPartner())) {
                    if (!proReport.getStatus().equalsIgnoreCase("Timeout")) {
                        protonReportIterator.remove();
                    } else {
                        protonReportIterator.remove();
                        proton.add(proReport);
                    }

                    protonReports.add(proReport);
                    break;
                }
            }
        }
    }

    private void processStbReportList(
            List<ProtonReport> protonReports1,
            Iterator<StbReport> stbReportIteratorIterator,
            List<ProtonReport> protonReports) {
        while (stbReportIteratorIterator.hasNext()) {
            StbReport stbReport = stbReportIteratorIterator.next();
            for (ProtonReport protonReport : protonReports1) {
                if (stbReport.getSystemTraceId().equals(protonReport.getSystemTraceId())) {
                    stbReportIteratorIterator.remove();
                    protonReports.add(protonReport);
                    break;
                }
            }
        }
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

    private static RefundTransactionsByDay getRefundTransactionsByDay(
            StbReport stbReport, ControlSessionDay controlSessionDaySave) {
        RefundTransactionsByDay refundByDay = new RefundTransactionsByDay();
        refundByDay.setTransactionNumber_ID(stbReport.getTransactionNumberId());
        refundByDay.setCreateTime(stbReport.getCreateTime());
        refundByDay.setEffectiveDate(stbReport.getEffectiveDate());
        refundByDay.setSystemTraceId(stbReport.getSystemTraceId());
        refundByDay.setExplanation(stbReport.getExplain());
        refundByDay.setAmount(stbReport.getAmount());
        refundByDay.setRecordedAccountNumber(stbReport.getRecordedAccountNumber());
        refundByDay.setDebitAccountNumber(stbReport.getDebitAccountNumber());
        refundByDay.setCustomAccountCode(stbReport.getCustomAccountCode());
        refundByDay.setTransactionChannel(stbReport.getTransactionChannel());
        refundByDay.setStatus("THANH CONG".equals(stbReport.getStatus()) ? "HOAN TRA" : stbReport.getStatus());
        refundByDay.setControlSessionDay(controlSessionDaySave);
        return refundByDay;
    }

    public PathFileByDayRequest getPathfileByDayRequest(ArgumentControlByDayRequest request) {
        PathFileByDayRequest filePaths = new PathFileByDayRequest();

        LocalDate date = LocalDate.parse(request.getDateControl(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        String monthStr = String.format("DS-T%02d", month);
        String dayMonthStr = String.format("%02d-%02d", day, month);
        String dayMonthYearStr = String.format("%02d%02d%02d", day, month, year);
        filePaths.setIrisReportFilePath(String.format(
                fileSavePath + "\\%d\\" + request.getSupplyPartner() + "\\%s\\%s", year, monthStr, dayMonthStr));
        filePaths.setProtonReportFilePath(
                String.format(fileSavePath + "\\%d\\Proton\\%s\\%s", year, monthStr, dayMonthStr));

        // Kiểm tra xem ngày đó có phải thứ 6, thứ 7 hoặc chủ nhật không

        if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            LocalDate dateFrom = date.minusDays((dayOfWeek.getValue() - DayOfWeek.FRIDAY.getValue()));
            LocalDate dateTo = date.plusDays((DayOfWeek.SUNDAY.getValue() - dayOfWeek.getValue()));
            String dayFromMonthYearStr = String.format(
                    "%02d%02d%02d", dateFrom.getDayOfMonth(), dateFrom.getMonthValue(), dateFrom.getYear());
            String dayToMonthYearStr =
                    String.format("%02d%02d%02d", dateTo.getDayOfMonth(), dateTo.getMonthValue(), dateTo.getYear());
            filePaths.setStbReportFilePath(String.format(
                    fileSavePath + "\\%d\\" + request.getSalesPartner() + "\\%s\\%s",
                    year,
                    monthStr,
                    "DoiSoatPROTON_" + dayFromMonthYearStr + "_" + dayToMonthYearStr + ".csv"));
        } else {
            filePaths.setStbReportFilePath(String.format(
                    fileSavePath + "\\%d\\" + request.getSalesPartner() + "\\%s\\%s",
                    year,
                    monthStr,
                    "DoiSoatPROTON_" + dayMonthYearStr + "_" + dayMonthYearStr + ".csv"));
        }
        return filePaths;
    }

    public ArgumentControlByDay getArgumentControlByDayById() {
        System.out.println();
        return null;
    }

    public ControlSessionDay findBySessionName(String argumentControlDetail) {

        ControlSessionDay controlSessionDay = controlSessionDayRepository.findBySessionName(argumentControlDetail);
        List<RefundTransactionsByDay> list = controlSessionDay.getRefundTransactionsByDays();

        Collections.sort(list, Comparator.comparing(RefundTransactionsByDay::getCreateTime));
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRefundTransactionsByDay_ID((long) (i + 1));
        }
        return controlSessionDay;
    }
}
