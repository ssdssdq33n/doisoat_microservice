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
import com.devteria.partnersession.mapper.SessionSalesProMonthMapper;
import com.devteria.partnersession.model.SalesPartner;
import com.devteria.partnersession.model.SessionSalesProMonth;
import com.devteria.partnersession.repository.SalesPartnerRepository;
import com.devteria.partnersession.repository.SessionSalesProMonthRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class SessionSalesProMonthService {

    SessionSalesProMonthRepository sessionSalesProMonthRepository;

    SessionSalesProMonthMapper sessionSalesProMonthMapper;

    SalesPartnerRepository salesPartnerRepository;

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
        List<SessionSalesProMonth> sessions = sessionSalesProMonthRepository.findAllByNameContains(lastTwoDigits);
        String number;
        if (sessions.size() > 0) {
            number = (sessions.size() + 1) > 9 && (sessions.size() + 1) <= 99
                    ? "0" + (sessions.size() + 1)
                    : (sessions.size() + 1) > 99 ? String.valueOf(sessions.size() + 1) : "00" + (sessions.size() + 1);
        } else {
            number = "001";
        }
        return "DSSP" + lastTwoDigits + number;
    }

    @PreAuthorize("hasRole('USER')")
    public SessionMonthResponse createSession(SessionMonthRequest request) throws ParseException {
        Optional<SessionSalesProMonth> session =
                sessionSalesProMonthRepository.findSessionMonthByNameContaining(request.getName());
        if (session.isPresent()) throw new AppException(ErrorCode.SESSION_EXISTED);
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date control = formatter.parse(request.getDateControlRequest());
        if (control.toInstant().isAfter(date.toInstant())) throw new AppException(ErrorCode.SESSION_DAY_ERROR);
        Optional<SessionSalesProMonth> sessionByStatusEquals =
                sessionSalesProMonthRepository.findSessionMonthByStatusEquals("Đang xử lý");
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
        SessionSalesProMonth sessionSave = new SessionSalesProMonth();
        sessionSave.setName(request.getName());
        sessionSave.setDateControl(control);
        sessionSave.setDateCreated(currentDateAdd);
        sessionSave.setDateUpdated(currentDateAdd);
        sessionSave.setStatus(status);
        sessionSave.setSalesPartner(salesPartner);
        sessionSave = sessionSalesProMonthRepository.save(sessionSave);
        SessionMonthResponse sessionResponse = sessionSalesProMonthMapper.toSessionMonthResponse(sessionSave);
        sessionResponse.setName_salesPartner(salesPartner.getName());
        return sessionResponse;
    }

    public String updateStatus(String name, String error) {
        //        if (status.equals("Đang xử lý") || status.equals("Đã hủy"))
        //            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        Optional<SessionSalesProMonth> session = sessionSalesProMonthRepository.findSessionMonthByNameContaining(name);
        if (session.isEmpty()) throw new AppException(ErrorCode.INVALID_SESSION);
        if (session.get().getStatus().equals("Thất bại")) throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        session.get().setStatus("Thất bại");
        session.get().setMessage(error);
        sessionSalesProMonthRepository.save(session.get());
        return "Update Success";
    }

    //    @PreAuthorize("hasAuthority('VIEW_SESSION_STBP')")
    public List<SessionMonthResponse> getAllSession() {
        List<SessionSalesProMonth> sessions = sessionSalesProMonthRepository.findAllByStatusNot("Deleted");
        return sessions.stream()
                .map(sessionSave -> {
                    SessionMonthResponse sessionResponse =
                            sessionSalesProMonthMapper.toSessionMonthResponse(sessionSave);
                    sessionResponse.setName_salesPartner(
                            sessionSave.getSalesPartner().getName());
                    sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
                    sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
                    return sessionResponse;
                })
                .collect(Collectors.toList());
    }

    public List<SessionMonthResponse> getAllSessionReload() {
        List<SessionSalesProMonth> sessions =
                sessionSalesProMonthRepository.findTransactionsWithSuccessOrFailureStatus();
        return sessions.stream()
                .map(sessionSave -> {
                    SessionMonthResponse sessionResponse =
                            sessionSalesProMonthMapper.toSessionMonthResponse(sessionSave);
                    sessionResponse.setName_salesPartner(
                            sessionSave.getSalesPartner().getName());
                    sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
                    sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
                    return sessionResponse;
                })
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('USER')")
    public SessionMonthResponse getSessionById(Long id) {
        SessionSalesProMonth sessionSave = sessionSalesProMonthRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));
        SessionMonthResponse sessionResponse = sessionSalesProMonthMapper.toSessionMonthResponse(sessionSave);
        sessionResponse.setName_salesPartner(sessionSave.getSalesPartner().getName());
        sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
        sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
        return sessionResponse;
    }

    @PreAuthorize("hasRole('USER')")
    public SessionMonthResponse getSessionByName(String name) {
        SessionSalesProMonth sessionSave = sessionSalesProMonthRepository
                .findSessionMonthByNameContaining(name)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));
        SessionMonthResponse sessionResponse = sessionSalesProMonthMapper.toSessionMonthResponse(sessionSave);
        sessionResponse.setName_salesPartner(sessionSave.getSalesPartner().getName());
        sessionResponse.setCreated(formatDate(sessionSave.getDateCreated()));
        sessionResponse.setControl(formatDate(sessionSave.getDateControl()));
        return sessionResponse;
    }

    @PreAuthorize("hasRole('USER')")
    public String deleteSession(Long id) {
        SessionSalesProMonth session = sessionSalesProMonthRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));
        if (session.getStatus().equals("Đang xử lý")) throw new AppException(ErrorCode.DELETE_ERROR);
        if (session.getStatus().equals("Chờ xử lý")) {
            session.setStatus("Đã hủy");
            sessionSalesProMonthRepository.save(session);
        } else {
            session.setStatus("Deleted");
            sessionSalesProMonthRepository.save(session);
        }
        return "delete success";
    }
}
