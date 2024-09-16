package com.devteria.partnersession.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.devteria.partnersession.dto.request.SupplyPartnerRequest;
import com.devteria.partnersession.dto.response.SupplyPartnerResponse;
import com.devteria.partnersession.exception.AppException;
import com.devteria.partnersession.exception.ErrorCode;
import com.devteria.partnersession.mapper.SupplyPartnerMapper;
import com.devteria.partnersession.model.SupplyPartner;
import com.devteria.partnersession.repository.SupplyPartnerRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class SupplyPartnerService {

    SupplyPartnerRepository supplyPartnerRepository;

    SupplyPartnerMapper supplyPartnerMapper;

    public static String formatDate(Date date) {
        SimpleDateFormat desiredFormat = new SimpleDateFormat("dd/MM/yyyy");
        String formattedDateTime = desiredFormat.format(date);
        return formattedDateTime;
    }

    @PreAuthorize("hasRole('USER')")
    public SupplyPartnerResponse addSupplyPartner(SupplyPartnerRequest request) {
        if (request.getName().isEmpty()
                || request.getDescription().isEmpty()
                || request.getStatus().isEmpty()) throw new AppException(ErrorCode.INVALID_SUPPLYPARTNER);
        if (request.getName().length() > 100) throw new AppException(ErrorCode.MAXLENGTH_NAME);
        if (request.getDescription().length() > 255) throw new AppException(ErrorCode.MAXLENGTH_DESCRIPTION);
        Optional<SupplyPartner> supplyPartner = supplyPartnerRepository.findSupplyPartnerByNameEquals(
                request.getName().trim());
        if (supplyPartner.isPresent() && !supplyPartner.get().getStatus().equals("Deleted"))
            throw new AppException(ErrorCode.SUPPLYPARTNER_EXISTED);
        if (supplyPartner.isPresent() && supplyPartner.get().getStatus().equals("Deleted")) {
            SupplyPartner response = supplyPartnerMapper.toSupplyPartner(request);
            Date currentDateAdd = new Date();
            response.setName(request.getName().trim());
            response.setDescription(request.getDescription().trim());
            response.setStatus(request.getStatus());
            response.setUpdatedDate(currentDateAdd);
            response.setCreatedDate(currentDateAdd);
            response = supplyPartnerRepository.save(response);
            supplyPartnerRepository.delete(supplyPartner.get());
            return supplyPartnerMapper.toSupplyPartnerResponse(response);
        }
        SupplyPartner response = supplyPartnerMapper.toSupplyPartner(request);
        Date currentDateAdd = new Date();
        response.setName(request.getName().trim());
        response.setDescription(request.getDescription().trim());
        response.setStatus(request.getStatus());
        response.setUpdatedDate(currentDateAdd);
        response.setCreatedDate(currentDateAdd);
        response = supplyPartnerRepository.save(response);
        return supplyPartnerMapper.toSupplyPartnerResponse(response);
    }

    //    @PreAuthorize("hasAuthority('VIEW_PARTNER')")
    public List<SupplyPartnerResponse> getAllSupplyPartner() {
        List<SupplyPartner> supplyPartners = supplyPartnerRepository.findAllByStatusNotContaining("Deleted");
        return supplyPartners.stream()
                .map(item -> {
                    SupplyPartnerResponse supplyPartnerResponse = supplyPartnerMapper.toSupplyPartnerResponse(item);
                    supplyPartnerResponse.setCreated(formatDate(item.getCreatedDate()));
                    supplyPartnerResponse.setUpdated(formatDate(item.getUpdatedDate()));
                    return supplyPartnerResponse;
                })
                .collect(Collectors.toList());
    }

    //    @PreAuthorize("hasAuthority('VIEW_PARTNER')")
    public List<SupplyPartnerResponse> getAllSupplyPartnerPending() {
        List<SupplyPartner> supplyPartners = supplyPartnerRepository.findAllByStatusEquals("Hoạt động");
        return supplyPartners.stream()
                .map(item -> {
                    SupplyPartnerResponse supplyPartnerResponse = supplyPartnerMapper.toSupplyPartnerResponse(item);
                    supplyPartnerResponse.setCreated(formatDate(item.getCreatedDate()));
                    supplyPartnerResponse.setUpdated(formatDate(item.getUpdatedDate()));
                    return supplyPartnerResponse;
                })
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('USER')")
    public SupplyPartnerResponse getSupplyPartnerById(Long id) {
        SupplyPartner supplyPartner = supplyPartnerRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLYPARTNER_NOT_EXISTED));
        return supplyPartnerMapper.toSupplyPartnerResponse(supplyPartner);
    }

    @PreAuthorize("hasRole('USER')")
    public SupplyPartnerResponse editSupplyPartner(SupplyPartnerRequest request, Long id) {
        if (request.getName().isEmpty()
                || request.getDescription().isEmpty()
                || request.getStatus().isEmpty()) throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        if (request.getName().length() > 100) throw new AppException(ErrorCode.MAXLENGTH_NAME);
        if (request.getDescription().length() > 255) throw new AppException(ErrorCode.MAXLENGTH_DESCRIPTION);
        SupplyPartner supplyPartner = supplyPartnerRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLYPARTNER_NOT_EXISTED));
        Optional<SupplyPartner> supplyPartnerData = supplyPartnerRepository.findSupplyPartnerByNameEquals(
                request.getName().trim());
        if (supplyPartnerData.isPresent()
                && supplyPartner.getSUPPLY_PARTNER_ID()
                        != supplyPartnerData.get().getSUPPLY_PARTNER_ID()
                && !supplyPartnerData.get().getStatus().equals("Deleted"))
            throw new AppException(ErrorCode.SUPPLYPARTNER_EXISTED);
        if (supplyPartnerData.isPresent()
                && supplyPartner.getSUPPLY_PARTNER_ID()
                        != supplyPartnerData.get().getSUPPLY_PARTNER_ID()
                && supplyPartnerData.get().getStatus().equals("Deleted")) {
            supplyPartnerMapper.updateSupplyPartner(supplyPartner, request);
            supplyPartner.setName(request.getName().trim());
            supplyPartner.setDescription(request.getDescription().trim());
            supplyPartner.setStatus(request.getStatus());
            Date currentDateEdit = new Date();
            supplyPartner.setUpdatedDate(currentDateEdit);
            SupplyPartner response = supplyPartnerRepository.save(supplyPartner);
            supplyPartnerRepository.delete(supplyPartnerData.get());
            return supplyPartnerMapper.toSupplyPartnerResponse(response);
        }
        supplyPartnerMapper.updateSupplyPartner(supplyPartner, request);
        supplyPartner.setName(request.getName().trim());
        supplyPartner.setStatus(request.getStatus());
        supplyPartner.setDescription(request.getDescription().trim());
        Date currentDateEdit = new Date();
        supplyPartner.setUpdatedDate(currentDateEdit);
        SupplyPartner response = supplyPartnerRepository.save(supplyPartner);
        return supplyPartnerMapper.toSupplyPartnerResponse(response);
    }

    @PreAuthorize("hasRole('USER')")
    public String deleteSupplyPartner(Long id) {
        SupplyPartner supplyPartner = supplyPartnerRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLYPARTNER_NOT_EXISTED));
        if (supplyPartner.getStatus().equals("Deleted")) throw new AppException(ErrorCode.CANT_DELETE);
        supplyPartner.setStatus("Deleted");
        supplyPartnerRepository.save(supplyPartner);
        return "Delete success";
    }
}
