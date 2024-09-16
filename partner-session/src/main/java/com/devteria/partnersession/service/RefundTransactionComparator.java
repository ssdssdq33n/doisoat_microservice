package com.devteria.partnersession.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

import com.devteria.partnersession.model.RefundTransactionsByDay;

public class RefundTransactionComparator implements Comparator<RefundTransactionsByDay> {
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    @Override
    public int compare(RefundTransactionsByDay rt1, RefundTransactionsByDay rt2) {
        try {
            Date date1 = dateFormat.parse(rt1.getCreateTime().replace("'", ""));
            Date date2 = dateFormat.parse(rt2.getCreateTime().replace("'", ""));
            return date2.compareTo(date1);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
