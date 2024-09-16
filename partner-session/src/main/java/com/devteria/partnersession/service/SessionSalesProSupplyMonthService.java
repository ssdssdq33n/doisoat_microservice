package com.devteria.partnersession.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.devteria.partnersession.dto.request.SessionMonthRequest;
import com.devteria.partnersession.dto.response.SessionMonthResponse;
import com.devteria.partnersession.exception.AppException;
import com.devteria.partnersession.exception.ErrorCode;
import com.devteria.partnersession.mapper.SessionSalesProSupplyMonthMapper;
import com.devteria.partnersession.model.SalesPartner;
import com.devteria.partnersession.model.SessionSalesProSupplyMonth;
import com.devteria.partnersession.model.SupplyPartner;
import com.devteria.partnersession.repository.SalesPartnerRepository;
import com.devteria.partnersession.repository.SessionSalesProSupplyMonthRepository;
import com.devteria.partnersession.repository.SupplyPartnerRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class SessionSalesProSupplyMonthService {

    SessionSalesProSupplyMonthRepository sessionSalesProSupplyMonthRepository;

    SessionSalesProSupplyMonthMapper sessionSalesProSupplyMonthMapper;

    SalesPartnerRepository salesPartnerRepository;

    SupplyPartnerRepository supplyPartnerRepository;

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
        List<SessionSalesProSupplyMonth> sessions =
                sessionSalesProSupplyMonthRepository.findAllByNameContains(lastTwoDigits);
        String number;
        if (sessions.size() > 0) {
            number = (sessions.size() + 1) > 9 && (sessions.size() + 1) <= 99
                    ? "0" + (sessions.size() + 1)
                    : (sessions.size() + 1) > 99 ? String.valueOf(sessions.size() + 1) : "00" + (sessions.size() + 1);
        } else {
            number = "001";
        }
        return "DSSPI" + lastTwoDigits + number;
    }

    @PreAuthorize("hasRole('USER')")
    public SessionMonthResponse createSession(SessionMonthRequest request) throws ParseException {
        Optional<SessionSalesProSupplyMonth> session =
                sessionSalesProSupplyMonthRepository.findSessionSalesProSupplyMonthByNameContaining(request.getName());
        if (session.isPresent()) throw new AppException(ErrorCode.SESSION_EXISTED);
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date control = formatter.parse(request.getDateControlRequest());
        if (control.toInstant().isAfter(date.toInstant())) throw new AppException(ErrorCode.SESSION_DAY_ERROR);
        Optional<SessionSalesProSupplyMonth> sessionByStatusEquals =
                sessionSalesProSupplyMonthRepository.findSessionSalesProSupplyMonthByStatusEquals("Đang xử lý");
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
        SalesPartner salesPartner = salesPartnerRepository
                .findById(request.getSalesPartnerId())
                .orElseThrow(() -> new AppException(ErrorCode.SALEPARTNER_NOT_EXISTED));
        Date currentDateAdd = new Date();
        SessionSalesProSupplyMonth sessionSave = new SessionSalesProSupplyMonth();
        sessionSave.setName(request.getName());
        sessionSave.setDateControl(control);
        sessionSave.setDateCreated(currentDateAdd);
        sessionSave.setDateUpdated(currentDateAdd);
        sessionSave.setStatus(status);
        sessionSave.setSalesPartner(salesPartner);
        Set<SupplyPartner> supplyPartnerSet = new HashSet<>(supplyPartners);
        sessionSave.setSupplyPartner(supplyPartnerSet);
        sessionSave = sessionSalesProSupplyMonthRepository.save(sessionSave);
        SessionMonthResponse sessionResponse = sessionSalesProSupplyMonthMapper.toSessionMonthResponse(sessionSave);
        sessionResponse.setName_salesPartner(salesPartner.getName());
        sessionResponse.setName_supplyPartner(
                supplyPartners.stream().map(SupplyPartner::getName).sorted().collect(Collectors.toList()));
        return sessionResponse;
    }

    public String updateStatus(String name, String error) {
        //        if (status.equals("Đang xử lý") || status.equals("Đã hủy"))
        //            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        Optional<SessionSalesProSupplyMonth> session =
                sessionSalesProSupplyMonthRepository.findSessionSalesProSupplyMonthByNameContaining(name);
        if (session.isEmpty()) throw new AppException(ErrorCode.INVALID_SESSION);
        if (session.get().getStatus().equals("Thất bại")) throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        session.get().setStatus("Thất bại");
        session.get().setMessage(error);
        sessionSalesProSupplyMonthRepository.save(session.get());
        return "Update Success";
    }

    //    @PreAuthorize("hasAuthority('VIEW_SESSION_SIP')")
    public List<SessionMonthResponse> getAllSession() {
        List<SessionSalesProSupplyMonth> sessions = sessionSalesProSupplyMonthRepository.findAllByStatusNot("Deleted");
        return sessions.stream()
                .map(sessionSave -> {
                    SessionMonthResponse sessionResponse =
                            sessionSalesProSupplyMonthMapper.toSessionMonthResponse(sessionSave);
                    sessionResponse.setName_salesPartner(
                            sessionSave.getSalesPartner().getName());
                    sessionResponse.setName_supplyPartner(sessionSave.getSupplyPartner().stream()
                            .map(SupplyPartner::getName)
                            .sorted()
                            .collect(Collectors.toList()));
                    sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
                    sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
                    return sessionResponse;
                })
                .collect(Collectors.toList());
    }

    public List<SessionMonthResponse> getAllSessionReload() {
        List<SessionSalesProSupplyMonth> sessions =
                sessionSalesProSupplyMonthRepository.findTransactionsWithSuccessOrFailureStatus();
        return sessions.stream()
                .map(sessionSave -> {
                    SessionMonthResponse sessionResponse =
                            sessionSalesProSupplyMonthMapper.toSessionMonthResponse(sessionSave);
                    sessionResponse.setName_salesPartner(
                            sessionSave.getSalesPartner().getName());
                    sessionResponse.setName_supplyPartner(sessionSave.getSupplyPartner().stream()
                            .map(SupplyPartner::getName)
                            .sorted()
                            .collect(Collectors.toList()));
                    sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
                    sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
                    return sessionResponse;
                })
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('USER')")
    public SessionMonthResponse getSessionById(Long id) {
        SessionSalesProSupplyMonth sessionSave = sessionSalesProSupplyMonthRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));
        SessionMonthResponse sessionResponse = sessionSalesProSupplyMonthMapper.toSessionMonthResponse(sessionSave);
        sessionResponse.setName_salesPartner(sessionSave.getSalesPartner().getName());
        sessionResponse.setName_supplyPartner(sessionSave.getSupplyPartner().stream()
                .map(SupplyPartner::getName)
                .sorted()
                .collect(Collectors.toList()));
        sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
        sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
        return sessionResponse;
    }

    @PreAuthorize("hasRole('USER')")
    public SessionMonthResponse getSessionByName(String name) {
        SessionSalesProSupplyMonth sessionSave = sessionSalesProSupplyMonthRepository
                .findSessionSalesProSupplyMonthByNameContaining(name)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));
        SessionMonthResponse sessionResponse = sessionSalesProSupplyMonthMapper.toSessionMonthResponse(sessionSave);
        sessionResponse.setName_salesPartner(sessionSave.getSalesPartner().getName());
        sessionResponse.setName_supplyPartner(sessionSave.getSupplyPartner().stream()
                .map(SupplyPartner::getName)
                .sorted()
                .collect(Collectors.toList()));
        sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
        sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
        return sessionResponse;
    }

    @PreAuthorize("hasRole('USER')")
    public String deleteSession(Long id) {
        SessionSalesProSupplyMonth session = sessionSalesProSupplyMonthRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));
        if (session.getStatus().equals("Đang xử lý")) throw new AppException(ErrorCode.DELETE_ERROR);
        if (session.getStatus().equals("Chờ xử lý")) {
            session.setStatus("Đã hủy");
            sessionSalesProSupplyMonthRepository.save(session);
        } else {
            session.setStatus("Deleted");
            sessionSalesProSupplyMonthRepository.save(session);
        }
        return "delete success";
    }
}
