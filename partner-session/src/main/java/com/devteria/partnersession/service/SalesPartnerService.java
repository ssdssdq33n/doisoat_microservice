package com.devteria.partnersession.service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.devteria.partnersession.dto.request.SalesPartnerRequest;
import com.devteria.partnersession.dto.response.SalesPartnerResponse;
import com.devteria.partnersession.exception.AppException;
import com.devteria.partnersession.exception.ErrorCode;
import com.devteria.partnersession.mapper.SalesPartnerMapper;
import com.devteria.partnersession.model.SalesPartner;
import com.devteria.partnersession.repository.SalesPartnerRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class SalesPartnerService {

    SalesPartnerRepository salesPartnerRepository;

    SalesPartnerMapper salesPartnerMapper;

    public static String formatDate(Date date) {
        SimpleDateFormat desiredFormat = new SimpleDateFormat("dd/MM/yyyy");
        String formattedDateTime = desiredFormat.format(date);
        return formattedDateTime;
    }

    @PreAuthorize("hasRole('USER')")
    public SalesPartnerResponse addSalesPartner(SalesPartnerRequest request) {
        if (request.getName().isEmpty()
                || request.getDescription().isEmpty()
                || request.getStatus().isEmpty()) throw new AppException(ErrorCode.INVALID_SALEPARTNER);
        if (request.getName().length() > 100) throw new AppException(ErrorCode.MAXLENGTH_NAME);
        if (request.getDescription().length() > 255) throw new AppException(ErrorCode.MAXLENGTH_DESCRIPTION);
        Optional<SalesPartner> salesPartner = salesPartnerRepository.findSalesPartnerByNameEquals(
                request.getName().trim());
        if (salesPartner.isPresent() && !salesPartner.get().getStatus().equals("Deleted"))
            throw new AppException(ErrorCode.SALEPARTNER_EXISTED);
        if (salesPartner.isPresent() && salesPartner.get().getStatus().equals("Deleted")) {
            SalesPartner response = salesPartnerMapper.toSalesPartner(request);
            Date currentDateAdd = new Date();
            response.setName(request.getName().trim());
            response.setDescription(request.getDescription().trim());
            response.setStatus(request.getStatus());
            response.setUpdatedDate(currentDateAdd);
            response.setCreatedDate(currentDateAdd);
            response = salesPartnerRepository.save(response);
            salesPartnerRepository.delete(salesPartner.get());
            return salesPartnerMapper.toSalesPartnerResponse(response);
        }
        SalesPartner response = salesPartnerMapper.toSalesPartner(request);
        Date currentDateAdd = new Date();
        response.setName(request.getName().trim());
        response.setDescription(request.getDescription().trim());
        response.setStatus(request.getStatus());
        response.setCreatedDate(currentDateAdd);
        response.setUpdatedDate(currentDateAdd);
        response = salesPartnerRepository.save(response);
        return salesPartnerMapper.toSalesPartnerResponse(response);
    }

    //    @PreAuthorize("hasAuthority('VIEW_PARTNER')")
    public List<SalesPartnerResponse> getAllSalesPartner() {
        List<SalesPartner> salesPartners = salesPartnerRepository.findAllByStatusNotContaining("Deleted");
        return salesPartners.stream()
                .map(item -> {
                    SalesPartnerResponse salesPartnerResponse = salesPartnerMapper.toSalesPartnerResponse(item);
                    salesPartnerResponse.setCreated(formatDate(item.getCreatedDate()));
                    salesPartnerResponse.setUpdated(formatDate(item.getUpdatedDate()));
                    return salesPartnerResponse;
                })
                .collect(Collectors.toList());
    }

    //    @PreAuthorize("hasAuthority('VIEW_PARTNER')")
    public List<SalesPartnerResponse> getAllSalesPartnerPending() {
        List<SalesPartner> salesPartners = salesPartnerRepository.findAllByStatusEquals("Hoạt động");
        return salesPartners.stream()
                .map(item -> {
                    SalesPartnerResponse salesPartnerResponse = salesPartnerMapper.toSalesPartnerResponse(item);
                    salesPartnerResponse.setCreated(formatDate(item.getCreatedDate()));
                    salesPartnerResponse.setUpdated(formatDate(item.getUpdatedDate()));
                    return salesPartnerResponse;
                })
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('USER')")
    public SalesPartnerResponse getSalesPartnerById(Long id) {
        SalesPartner salesPartner = salesPartnerRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SALEPARTNER_NOT_EXISTED));
        return salesPartnerMapper.toSalesPartnerResponse(salesPartner);
    }

    @PreAuthorize("hasRole('USER')")
    public SalesPartnerResponse editSalesPartner(SalesPartnerRequest request, Long id) {
        if (request.getName().isEmpty()
                || request.getDescription().isEmpty()
                || request.getStatus().isEmpty()) throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        if (request.getName().length() > 100) throw new AppException(ErrorCode.MAXLENGTH_NAME);
        if (request.getDescription().length() > 255) throw new AppException(ErrorCode.MAXLENGTH_DESCRIPTION);
        SalesPartner salesPartner = salesPartnerRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SALEPARTNER_NOT_EXISTED));
        Optional<SalesPartner> salesPartnerData = salesPartnerRepository.findSalesPartnerByNameEquals(
                request.getName().trim());
        if (salesPartnerData.isPresent()
                && salesPartner.getSALES_PARTNER_ID() != salesPartnerData.get().getSALES_PARTNER_ID()
                && !salesPartnerData.get().getStatus().equals("Deleted"))
            throw new AppException(ErrorCode.SALEPARTNER_EXISTED);
        if (salesPartnerData.isPresent()
                && salesPartner.getSALES_PARTNER_ID() != salesPartnerData.get().getSALES_PARTNER_ID()
                && salesPartnerData.get().getStatus().equals("Deleted")) {
            salesPartnerMapper.updateSalesPartner(salesPartner, request);
            salesPartner.setName(request.getName().trim());
            salesPartner.setDescription(request.getDescription().trim());
            salesPartner.setStatus(request.getStatus());
            Date currentDateEdit = new Date();
            salesPartner.setUpdatedDate(currentDateEdit);
            SalesPartner response = salesPartnerRepository.save(salesPartner);
            salesPartnerRepository.delete(salesPartnerData.get());
            return salesPartnerMapper.toSalesPartnerResponse(response);
        }
        salesPartnerMapper.updateSalesPartner(salesPartner, request);
        salesPartner.setName(request.getName().trim());
        salesPartner.setStatus(request.getStatus());
        salesPartner.setDescription(request.getDescription().trim());
        Date currentDateEdit = new Date();
        salesPartner.setUpdatedDate(currentDateEdit);
        SalesPartner response = salesPartnerRepository.save(salesPartner);
        return salesPartnerMapper.toSalesPartnerResponse(response);
    }

    @PreAuthorize("hasRole('USER')")
    public String deleteSalesPartner(Long id) {
        SalesPartner salesPartner = salesPartnerRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SALEPARTNER_NOT_EXISTED));
        if (salesPartner.getStatus().equals("Deleted")) throw new AppException(ErrorCode.CANT_DELETE);
        salesPartner.setStatus("Deleted");
        salesPartnerRepository.save(salesPartner);
        return "Delete success";
    }
}
