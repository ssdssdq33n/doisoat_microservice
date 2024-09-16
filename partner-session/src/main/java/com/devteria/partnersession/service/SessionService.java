package com.devteria.partnersession.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.devteria.partnersession.dto.request.ArgumentControlByDayRequest;
import com.devteria.partnersession.dto.request.SessionRequest;
import com.devteria.partnersession.dto.response.SessionResponse;
import com.devteria.partnersession.exception.AppException;
import com.devteria.partnersession.exception.ErrorCode;
import com.devteria.partnersession.mapper.SessionMapper;
import com.devteria.partnersession.model.SalesPartner;
import com.devteria.partnersession.model.Session;
import com.devteria.partnersession.model.SupplyPartner;
import com.devteria.partnersession.repository.SalesPartnerRepository;
import com.devteria.partnersession.repository.SessionRepository;
import com.devteria.partnersession.repository.SupplyPartnerRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class SessionService {

    SessionRepository sessionRepository;

    SupplyPartnerRepository supplyPartnerRepository;

    SalesPartnerRepository salesPartnerRepository;

    SessionMapper sessionMapper;

    RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    @NonFinal
    private String exchange;

    @Value("${rabbitmq.routing.json.key}")
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
        int year = date.getYear();
        String day = date.getDate() > 9 ? date.getDate() + "" : "0" + date.getDate();
        String month = date.getMonth() + 1 > 9 ? date.getMonth() + "" : "0" + (date.getMonth() + 1);
        String yearString = String.valueOf(year);
        String lastTwoDigits = day + month + yearString.substring(yearString.length() - 2);
        List<Session> sessions = sessionRepository.findAllByNameContains(lastTwoDigits);
        String number;
        if (sessions.size() > 0) {
            number = (sessions.size() + 1) > 9 && (sessions.size() + 1) <= 99
                    ? "0" + (sessions.size() + 1)
                    : (sessions.size() + 1) > 99 ? String.valueOf(sessions.size() + 1) : "00" + (sessions.size() + 1);
        } else {
            number = "001";
        }
        return "DS" + lastTwoDigits + number;
    }

    @PreAuthorize("hasRole('USER')")
    public SessionResponse createSession(SessionRequest request) throws ParseException {
        Optional<Session> session = sessionRepository.findSessionByNameContaining(request.getName());
        if (session.isPresent()) throw new AppException(ErrorCode.SESSION_EXISTED);
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date control = formatter.parse(request.getDateControlRequest());
        if (control.toInstant().isAfter(date.toInstant())) throw new AppException(ErrorCode.SESSION_DAY_ERROR);
        Optional<Session> sessionByStatusEquals = sessionRepository.findSessionByStatusEquals("Đang xử lý");
        Random random = new Random();
        int randomNumber = random.nextInt(3) + 1;
        String status;
        if (sessionByStatusEquals.isPresent()) {
            status = "Chờ xử lý";
        } else {
            status = "Đang xử lý";
        }
        SalesPartner salesPartner = salesPartnerRepository
                .findById(request.getSalesPartnerId())
                .orElseThrow(() -> new AppException(ErrorCode.SALEPARTNER_NOT_EXISTED));
        Date currentDateAdd = new Date();
        Session sessionSave = new Session();
        sessionSave.setName(request.getName());
        sessionSave.setDateControl(control);
        sessionSave.setDateCreated(currentDateAdd);
        sessionSave.setDateUpdated(currentDateAdd);
        sessionSave.setStatus(status);
        sessionSave.setSalesPartner(salesPartner);
        Set<SupplyPartner> list = new HashSet<>();
        for (Long id : request.getSupplyPartnerId()) {
            SupplyPartner supplyPartner = supplyPartnerRepository
                    .findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.SUPPLYPARTNER_NOT_EXISTED));
            list.add(supplyPartner);
        }
        sessionSave.setSupplyPartner(list);
        sessionSave = sessionRepository.save(sessionSave);
        SessionResponse sessionResponse = sessionMapper.toSessionResponse(sessionSave);
        List<String> listNameSupply = new ArrayList<>();
        for (SupplyPartner supplyPartner : sessionSave.getSupplyPartner()) {
            listNameSupply.add(supplyPartner.getName());
        }
        Collections.sort(listNameSupply);
        sessionResponse.setName_supplyPartner(listNameSupply);
        sessionResponse.setName_salesPartner(salesPartner.getName());

        ArgumentControlByDayRequest argumentControlByDayRequest = new ArgumentControlByDayRequest();
        argumentControlByDayRequest.setName(request.getName());
        argumentControlByDayRequest.setDateControl(request.getDateControlRequest());
        argumentControlByDayRequest.setSalesPartner(salesPartner.getName());
        argumentControlByDayRequest.setSupplyPartner(listNameSupply.get(0));
        rabbitTemplate.convertAndSend(exchange, routingJsonKey, argumentControlByDayRequest);

        return sessionResponse;
    }

    @PreAuthorize("hasRole('USER')")
    public String updateStatus(String name, String error) {
        //        if (status.equals("Đang xử lý") || status.equals("Đã hủy"))
        //            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        Optional<Session> session = sessionRepository.findSessionByNameContaining(name);
        if (session.isEmpty()) throw new AppException(ErrorCode.INVALID_SESSION);
        if (session.get().getStatus().equals("Thất bại")) throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        session.get().setStatus("Thất bại");
        session.get().setMessage(error);
        sessionRepository.save(session.get());
        return "Update Success";
    }

    //    @PreAuthorize("hasAuthority('VIEW_SESSION_DAY')")
    public List<SessionResponse> getAllSession() {
        List<Session> sessions = sessionRepository.findAllByStatusNot("Deleted");
        return sessions.stream()
                .map(sessionSave -> {
                    SessionResponse sessionResponse = sessionMapper.toSessionResponse(sessionSave);
                    sessionResponse.setName_salesPartner(
                            sessionSave.getSalesPartner().getName());
                    List<String> listNameSupply = new ArrayList<>();
                    for (SupplyPartner supplyPartner : sessionSave.getSupplyPartner()) {
                        listNameSupply.add(supplyPartner.getName());
                    }
                    Collections.sort(listNameSupply);
                    sessionResponse.setName_supplyPartner(listNameSupply);
                    sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
                    sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
                    return sessionResponse;
                })
                .collect(Collectors.toList());
    }

    public List<SessionResponse> getAllSessionReload() {
        List<Session> sessions = sessionRepository.findTransactionsWithSuccessOrFailureStatus();
        return sessions.stream()
                .map(sessionSave -> {
                    SessionResponse sessionResponse = sessionMapper.toSessionResponse(sessionSave);
                    sessionResponse.setName_salesPartner(
                            sessionSave.getSalesPartner().getName());
                    List<String> listNameSupply = new ArrayList<>();
                    for (SupplyPartner supplyPartner : sessionSave.getSupplyPartner()) {
                        listNameSupply.add(supplyPartner.getName());
                    }
                    Collections.sort(listNameSupply);
                    sessionResponse.setName_supplyPartner(listNameSupply);
                    sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
                    sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
                    return sessionResponse;
                })
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('USER')")
    public SessionResponse getSessionById(Long id) {
        Session sessionSave =
                sessionRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));
        SessionResponse sessionResponse = sessionMapper.toSessionResponse(sessionSave);
        sessionResponse.setName_salesPartner(sessionSave.getSalesPartner().getName());
        List<String> listNameSupply = new ArrayList<>();
        for (SupplyPartner supplyPartner : sessionSave.getSupplyPartner()) {
            listNameSupply.add(supplyPartner.getName());
        }
        sessionResponse.setName_supplyPartner(listNameSupply);
        sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
        sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
        return sessionResponse;
    }

    @PreAuthorize("hasRole('USER')")
    public SessionResponse getSessionByName(String name) {
        Session sessionSave = sessionRepository
                .findSessionByNameContaining(name)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));
        SessionResponse sessionResponse = sessionMapper.toSessionResponse(sessionSave);
        sessionResponse.setName_salesPartner(sessionSave.getSalesPartner().getName());
        List<String> listNameSupply = new ArrayList<>();
        for (SupplyPartner supplyPartner : sessionSave.getSupplyPartner()) {
            listNameSupply.add(supplyPartner.getName());
        }
        sessionResponse.setName_supplyPartner(listNameSupply);
        sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
        sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
        return sessionResponse;
    }

    @PreAuthorize("hasRole('USER')")
    public String deleteSession(Long id) {
        Session session =
                sessionRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));
        if (session.getStatus().equals("Đang xử lý")) throw new AppException(ErrorCode.DELETE_ERROR);
        if (session.getStatus().equals("Chờ xử lý")) {
            session.setStatus("Đã hủy");
            sessionRepository.save(session);
        } else {
            session.setStatus("Deleted");
            sessionRepository.save(session);
        }
        return "delete success";
    }
}
