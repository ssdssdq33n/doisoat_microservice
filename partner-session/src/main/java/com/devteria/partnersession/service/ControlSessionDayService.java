package com.devteria.partnersession.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.devteria.partnersession.model.ControlSessionDay;
import com.devteria.partnersession.repository.ControlSessionDayRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class ControlSessionDayService {

    ControlSessionDayRepository controlSessionDayRepository;

    public List<ControlSessionDay> getAllControlSessionDays() {
        return controlSessionDayRepository.findAll();
    }

    public Optional<ControlSessionDay> getControlSessionDayById(Long id) {
        return controlSessionDayRepository.findById(id);
    }

    public ControlSessionDay createControlSessionDay(ControlSessionDay controlSessionDay) {
        return controlSessionDayRepository.save(controlSessionDay);
    }

    public ControlSessionDay updateControlSessionDay(Long id, ControlSessionDay controlSessionDay) {
        Optional<ControlSessionDay> existingControlSessionDay = controlSessionDayRepository.findById(id);
        if (existingControlSessionDay.isPresent()) {
            ControlSessionDay updatedControlSessionDay = existingControlSessionDay.get();
            updatedControlSessionDay.setSessionName(controlSessionDay.getSessionName());
            updatedControlSessionDay.setRefundTransactionsByDays(controlSessionDay.getRefundTransactionsByDays());
            return controlSessionDayRepository.save(updatedControlSessionDay);
        } else {
            return null;
        }
    }

    public void deleteControlSessionDay(Long id) {
        controlSessionDayRepository.deleteById(id);
    }

    public Optional<ControlSessionDay> findBySessionName(String name) {
        return controlSessionDayRepository.findBySessionNameEquals(name);
    }
}
