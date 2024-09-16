package com.devteria.partnersession.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.devteria.partnersession.dto.request.ArgumentControlByMonthRequest;
import com.devteria.partnersession.dto.request.SessionMonthRequest;
import com.devteria.partnersession.dto.response.SessionMonthResponse;
import com.devteria.partnersession.exception.AppException;
import com.devteria.partnersession.exception.ErrorCode;
import com.devteria.partnersession.mapper.SessionProSupplyMonthMapper;
import com.devteria.partnersession.model.SessionProSupplyMonth;
import com.devteria.partnersession.model.SupplyPartner;
import com.devteria.partnersession.repository.SessionProSupplyMonthRepository;
import com.devteria.partnersession.repository.SupplyPartnerRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class SessionProSupplyMonthService {

    SessionProSupplyMonthRepository sessionProSupplyMonthRepository;

    SessionProSupplyMonthMapper sessionProSupplyMonthMapper;

    SupplyPartnerRepository supplyPartnerRepository;

    RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    @NonFinal
    private String exchange;

    @Value("${rabbitmq.routing.json.month}")
    @NonFinal
    private String routingJsonKey;

    public static String formatDate(Date date) {
        SimpleDateFormat desiredFormat = new SimpleDateFormat("dd/MM/yyyy");
        String formattedDateTime = desiredFormat.format(date);
        return formattedDateTime;
    }

    public static String formatMonth(Date date) {
        SimpleDateFormat desiredFormat = new SimpleDateFormat("MM/yyyy");
        String formattedDateTime = desiredFormat.format(date);
        return formattedDateTime;
    }

    @PreAuthorize("hasRole('USER')")
    public String createNameSession() {
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        String day = date.getDate() > 9 ? date.getDate() + "" : "0" + date.getDate();
        String month = date.getMonth() + 1 > 9 ? date.getMonth() + "" : "0" + (date.getMonth() + 1);
        String yearString = String.valueOf(year);
        String lastTwoDigits = month + yearString;
        List<SessionProSupplyMonth> sessions = sessionProSupplyMonthRepository.findAllByNameContains(lastTwoDigits);
        String number;
        if (sessions.size() > 0) {
            number = (sessions.size() + 1) > 9 && (sessions.size() + 1) <= 99
                    ? "0" + (sessions.size() + 1)
                    : (sessions.size() + 1) > 99 ? String.valueOf(sessions.size() + 1) : "00" + (sessions.size() + 1);
        } else {
            number = "001";
        }
        return "DSPI" + lastTwoDigits + number;
    }

    @PreAuthorize("hasRole('USER')")
    public SessionMonthResponse createSession(SessionMonthRequest request) throws ParseException {
        Optional<SessionProSupplyMonth> session =
                sessionProSupplyMonthRepository.findSessionProSupplyMonthsByNameContaining(request.getName());
        if (session.isPresent()) throw new AppException(ErrorCode.SESSION_EXISTED);
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date control = formatter.parse(request.getDateControlRequest());
        if (control.toInstant().isAfter(date.toInstant())) throw new AppException(ErrorCode.SESSION_DAY_ERROR);
        Optional<SessionProSupplyMonth> sessionByStatusEquals =
                sessionProSupplyMonthRepository.findSessionProSupplyMonthsByStatusEquals("Đang xử lý");
        Random random = new Random();
        int randomNumber = random.nextInt(3) + 1;
        String status;
        if (sessionByStatusEquals.isPresent()) {
            status = "Chờ xử lý";
        } else {
            status = "Đang xử lý";
        }
        List<SupplyPartner> supplyPartners = supplyPartnerRepository.findAllById(request.getSupplyPartnerId());
        if (supplyPartners.size() != request.getSupplyPartnerId().size()) {
            throw new AppException(ErrorCode.SUPPLYPARTNER_NOT_EXISTED);
        }
        Date currentDateAdd = new Date();
        SessionProSupplyMonth sessionSave = new SessionProSupplyMonth();
        sessionSave.setDateControl(control);
        sessionSave.setName(request.getName());
        sessionSave.setDateCreated(currentDateAdd);
        sessionSave.setDateUpdated(currentDateAdd);
        sessionSave.setStatus(status);
        Set<SupplyPartner> supplyPartnerSet = new HashSet<>(supplyPartners);
        sessionSave.setSupplyPartner(supplyPartnerSet);
        sessionSave = sessionProSupplyMonthRepository.save(sessionSave);
        SessionMonthResponse sessionResponse = sessionProSupplyMonthMapper.toSessionMonthResponse(sessionSave);
        sessionResponse.setName_supplyPartner(
                supplyPartners.stream().map(SupplyPartner::getName).collect(Collectors.toList()));

        ArgumentControlByMonthRequest argumentControlByMonthRequest = new ArgumentControlByMonthRequest();
        argumentControlByMonthRequest.setName(request.getName());
        argumentControlByMonthRequest.setSalesPartner(sessionResponse.getName_salesPartner());
        argumentControlByMonthRequest.setSupplyPartner(
                sessionResponse.getName_supplyPartner().get(0));
        argumentControlByMonthRequest.setDateControl(request.getDateControlRequest());
        rabbitTemplate.convertAndSend(exchange, routingJsonKey, argumentControlByMonthRequest);

        return sessionResponse;
    }

    public String updateStatus(String name, String error) {
        //        if (status.equals("Đang xử lý") || status.equals("Đã hủy"))
        //            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        Optional<SessionProSupplyMonth> session =
                sessionProSupplyMonthRepository.findSessionProSupplyMonthsByNameContaining(name);
        if (session.isEmpty()) throw new AppException(ErrorCode.INVALID_SESSION);
        if (session.get().getStatus().equals("Thất bại")) throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        session.get().setStatus("Thất bại");
        session.get().setMessage(error);
        sessionProSupplyMonthRepository.save(session.get());
        return "Update Success";
    }

    //    @PreAuthorize("hasAuthority('VIEW_SESSION_IRISP')")
    public List<SessionMonthResponse> getAllSession() {
        List<SessionProSupplyMonth> sessions = sessionProSupplyMonthRepository.findAllByStatusNot("Deleted");
        return sessions.stream()
                .map(sessionSave -> {
                    SessionMonthResponse sessionResponse =
                            sessionProSupplyMonthMapper.toSessionMonthResponse(sessionSave);
                    sessionResponse.setName_supplyPartner(sessionSave.getSupplyPartner().stream()
                            .map(SupplyPartner::getName)
                            .collect(Collectors.toList()));
                    sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
                    sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
                    return sessionResponse;
                })
                .collect(Collectors.toList());
    }

    public List<SessionMonthResponse> getAllSessionReload() {
        List<SessionProSupplyMonth> sessions =
                sessionProSupplyMonthRepository.findTransactionsWithSuccessOrFailureStatus();
        return sessions.stream()
                .map(sessionSave -> {
                    SessionMonthResponse sessionResponse =
                            sessionProSupplyMonthMapper.toSessionMonthResponse(sessionSave);
                    sessionResponse.setName_supplyPartner(sessionSave.getSupplyPartner().stream()
                            .map(SupplyPartner::getName)
                            .collect(Collectors.toList()));
                    sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
                    sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
                    return sessionResponse;
                })
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('USER')")
    public SessionMonthResponse getSessionById(Long id) {
        SessionProSupplyMonth sessionSave = sessionProSupplyMonthRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));
        SessionMonthResponse sessionResponse = sessionProSupplyMonthMapper.toSessionMonthResponse(sessionSave);
        sessionResponse.setName_supplyPartner(sessionSave.getSupplyPartner().stream()
                .map(SupplyPartner::getName)
                .collect(Collectors.toList()));
        sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
        sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
        return sessionResponse;
    }

    @PreAuthorize("hasRole('USER')")
    public SessionMonthResponse getSessionByName(String name) {
        SessionProSupplyMonth sessionSave = sessionProSupplyMonthRepository
                .findSessionProSupplyMonthsByNameContaining(name)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));
        SessionMonthResponse sessionResponse = sessionProSupplyMonthMapper.toSessionMonthResponse(sessionSave);
        sessionResponse.setName_supplyPartner(sessionSave.getSupplyPartner().stream()
                .map(SupplyPartner::getName)
                .collect(Collectors.toList()));
        sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
        sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
        return sessionResponse;
    }

    @PreAuthorize("hasRole('USER')")
    public String deleteSession(Long id) {
        SessionProSupplyMonth session = sessionProSupplyMonthRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));
        if (session.getStatus().equals("Đang xử lý")) throw new AppException(ErrorCode.DELETE_ERROR);
        if (session.getStatus().equals("Chờ xử lý")) {
            session.setStatus("Đã hủy");
            sessionProSupplyMonthRepository.save(session);
        } else {
            session.setStatus("Deleted");
            sessionProSupplyMonthRepository.save(session);
        }
        return "delete success";
    }
}
