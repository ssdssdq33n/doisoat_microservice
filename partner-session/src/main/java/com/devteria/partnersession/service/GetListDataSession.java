package com.devteria.partnersession.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import org.springframework.stereotype.Service;

import com.devteria.partnersession.dto.response.ResultDataSession;
import com.devteria.partnersession.dto.response.ResultDataSessionMonth;
import com.devteria.partnersession.dto.response.ResultDataSessionMonthSTBP;
import com.devteria.partnersession.model.*;
import com.devteria.partnersession.repository.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class GetListDataSession {

    SessionRepository sessionRepository;

    ControlSessionDayRepository controlSessionDayRepository;

    RefundTransactionsByDayRepository refundTransactionsByDayRepository;

    SessionProSupplyMonthRepository sessionProSupplyMonthRepository;

    SessionSalesProMonthRepository sessionSalesProMonthRepository;

    SessionSalesProSupplyMonthRepository sessionSalesProSupplyMonthRepository;

    public List<ResultDataSession> getSessionsFromLastWeek() {
        Calendar calendar = Calendar.getInstance();

        // Ngày hiện tại
        Date endDate = calendar.getTime();

        // Trừ 1 ngày để không bao gồm ngày hiện tại
        calendar.add(Calendar.DATE, -1);
        endDate = calendar.getTime();

        // Ngày bắt đầu (7 ngày trước)
        calendar.add(Calendar.DATE, -6); // Đã trừ 1 ở trên nên chỉ cần trừ 6 nữa là đủ 7 ngày
        Date startDate = calendar.getTime();

        // Truy vấn dữ liệu từ repository
        List<Session> sessions =
                sessionRepository.findFirstSessionsForEachDayByControlDateBetweenAndStatus(startDate, endDate);

        List<String> nameSession = new ArrayList<>();
        for (Session session : sessions) {
            nameSession.add(session.getName() + "/" + session.getDateControl());
        }
        List<ResultDataSession> resultDataSessionList = new ArrayList<>();
        for (String name : nameSession) {
            String[] fruits = name.split("/");
            ControlSessionDay controlSessionDay = findBySessionName(fruits[0]);
            List<RefundTransactionsByDay> listHoanTra =
                    refundTransactionsByDayRepository.findAllByControlSessionDayAndStatusEquals(
                            controlSessionDay, "HOAN TRA");
            List<RefundTransactionsByDay> listkhongxacdinh =
                    refundTransactionsByDayRepository.findAllByControlSessionDayAndStatusEquals(
                            controlSessionDay, "Không xác định");
            resultDataSessionList.add(new ResultDataSession(
                    getDayOfWeek(fruits[1]),
                    convertDateFormat(fruits[1]),
                    (double) listHoanTra.size(),
                    (double) listkhongxacdinh.size(),
                    (double) (listHoanTra.size() + listkhongxacdinh.size())));
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
        Collections.sort(resultDataSessionList, new Comparator<ResultDataSession>() {
            @Override
            public int compare(ResultDataSession o1, ResultDataSession o2) {
                try {
                    Date date1 = sdf.parse(o1.getDateControl());
                    Date date2 = sdf.parse(o2.getDateControl());
                    return date1.compareTo(date2);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        List<ResultDataSession> weekendData = new ArrayList<>();
        for (ResultDataSession data : resultDataSessionList) {
            if (data.getDay().equals("Chủ nhật")
                    || data.getDay().equals("Thứ 7")
                    || data.getDay().equals("Thứ 6")) {
                weekendData.add(data);
            }
        }

        // Nếu có nhiều hơn 1 đối tượng, xóa ngẫu nhiên 1 hoặc 2 đối tượng trong danh sách weekendData
        Random random = new Random();
        while (weekendData.size() > 1) {
            int removeIndex = random.nextInt(weekendData.size());
            ResultDataSession dataToRemove = weekendData.remove(removeIndex);
            resultDataSessionList.remove(dataToRemove);
        }
        for (ResultDataSession data : resultDataSessionList) {
            if (data.getDay().equals("Chủ nhật")
                    || data.getDay().equals("Thứ 7")
                    || data.getDay().equals("Thứ 6")) {
                data.setDay("Thứ 6/Thứ 7/Chủ nhật");
            }
        }
        return resultDataSessionList;
    }

    public List<ResultDataSessionMonth> getLatestSessionsForLastThreeMonths() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusMonths(3).withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime endDate = now.minusMonths(1)
                .withDayOfMonth(now.minusMonths(1)
                        .getMonth()
                        .length(now.minusMonths(1).toLocalDate().isLeapYear()))
                .with(LocalTime.MAX);

        List<SessionProSupplyMonth> sessionProSupplyMonths =
                sessionProSupplyMonthRepository.findLatestSessionsByControlDate(startDate, endDate);
        List<ResultDataSessionMonth> list = new ArrayList<>();
        for (SessionProSupplyMonth sessionProSupplyMonth : sessionProSupplyMonths) {
            list.add(new ResultDataSessionMonth(
                    convertDateFormatMonth(sessionProSupplyMonth.getDateControl()),
                    sessionProSupplyMonth.getTotal() != null ? sessionProSupplyMonth.getTotal() : 0,
                    sessionProSupplyMonth.getTotalFail() != null ? sessionProSupplyMonth.getTotalFail() : 0,
                    sessionProSupplyMonth.getTotalNotMatch() != null ? sessionProSupplyMonth.getTotalNotMatch() : 0,
                    sessionProSupplyMonth.getTotalSuccess() != null ? sessionProSupplyMonth.getTotalSuccess() : 0));
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy", Locale.ENGLISH);
        Collections.sort(list, new Comparator<ResultDataSessionMonth>() {
            @Override
            public int compare(ResultDataSessionMonth o1, ResultDataSessionMonth o2) {
                try {
                    Date date1 = sdf.parse(o1.getMonth());
                    Date date2 = sdf.parse(o2.getMonth());
                    return date1.compareTo(date2);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return list;
    }

    public List<ResultDataSessionMonth> getLatestSessionsForLastThreeMonthsSPI() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusMonths(3).withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime endDate = now.minusMonths(1)
                .withDayOfMonth(now.minusMonths(1)
                        .getMonth()
                        .length(now.minusMonths(1).toLocalDate().isLeapYear()))
                .with(LocalTime.MAX);

        List<SessionSalesProSupplyMonth> sessionProSupplyMonths =
                sessionSalesProSupplyMonthRepository.findLatestSessionsByControlDate(startDate, endDate);
        List<ResultDataSessionMonth> list = new ArrayList<>();
        for (SessionSalesProSupplyMonth sessionProSupplyMonth : sessionProSupplyMonths) {
            list.add(new ResultDataSessionMonth(
                    convertDateFormatMonth(sessionProSupplyMonth.getDateControl()),
                    sessionProSupplyMonth.getTotal() != null ? sessionProSupplyMonth.getTotal() : 0,
                    sessionProSupplyMonth.getTotalFail() != null ? sessionProSupplyMonth.getTotalFail() : 0,
                    sessionProSupplyMonth.getTotalNotMatch() != null ? sessionProSupplyMonth.getTotalNotMatch() : 0,
                    sessionProSupplyMonth.getTotalSuccess() != null ? sessionProSupplyMonth.getTotalSuccess() : 0));
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy", Locale.ENGLISH);
        Collections.sort(list, new Comparator<ResultDataSessionMonth>() {
            @Override
            public int compare(ResultDataSessionMonth o1, ResultDataSessionMonth o2) {
                try {
                    Date date1 = sdf.parse(o1.getMonth());
                    Date date2 = sdf.parse(o2.getMonth());
                    return date1.compareTo(date2);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return list;
    }

    public List<ResultDataSessionMonthSTBP> getLatestSessionsForLastThreeMonthsSTBP() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusMonths(3).withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime endDate = now.minusMonths(1)
                .withDayOfMonth(now.minusMonths(1)
                        .getMonth()
                        .length(now.minusMonths(1).toLocalDate().isLeapYear()))
                .with(LocalTime.MAX);

        List<SessionSalesProMonth> sessionSalesProMonths =
                sessionSalesProMonthRepository.findLatestSessionsByControlDate(startDate, endDate);
        List<ResultDataSessionMonthSTBP> list = new ArrayList<>();
        for (SessionSalesProMonth sessionSalesProMonth : sessionSalesProMonths) {
            list.add(new ResultDataSessionMonthSTBP(
                    convertDateFormatMonth(sessionSalesProMonth.getDateControl()),
                    sessionSalesProMonth.getTotal() != null ? sessionSalesProMonth.getTotal() : 0));
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy", Locale.ENGLISH);
        Collections.sort(list, new Comparator<ResultDataSessionMonthSTBP>() {
            @Override
            public int compare(ResultDataSessionMonthSTBP o1, ResultDataSessionMonthSTBP o2) {
                try {
                    Date date1 = sdf.parse(o1.getMonth());
                    Date date2 = sdf.parse(o2.getMonth());
                    return date1.compareTo(date2);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return list;
    }

    public String getDayOfWeek(String dateStr) {
        // Định dạng ngày theo chuỗi truyền vào, ví dụ: "2024-08-13"
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dayOfWeek = "";

        try {
            // Chuyển chuỗi ngày thành đối tượng Date
            Date date = dateFormat.parse(dateStr);

            // Sử dụng Calendar để xác định thứ trong tuần
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int dayOfWeekInt = calendar.get(Calendar.DAY_OF_WEEK);

            // Chuyển từ số nguyên thành tên thứ trong tuần
            switch (dayOfWeekInt) {
                case Calendar.SUNDAY:
                    dayOfWeek = "Chủ nhật";
                    break;
                case Calendar.MONDAY:
                    dayOfWeek = "Thứ 2";
                    break;
                case Calendar.TUESDAY:
                    dayOfWeek = "Thứ 3";
                    break;
                case Calendar.WEDNESDAY:
                    dayOfWeek = "Thứ 4";
                    break;
                case Calendar.THURSDAY:
                    dayOfWeek = "Thứ 5";
                    break;
                case Calendar.FRIDAY:
                    dayOfWeek = "Thứ 6";
                    break;
                case Calendar.SATURDAY:
                    dayOfWeek = "Thứ 7";
                    break;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return dayOfWeek;
    }

    public String convertDateFormat(String dateStr) {
        // Định dạng chuỗi đầu vào (yyyy-MM-dd)
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");

        // Định dạng chuỗi đầu ra (dd/MM/yyyy)
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy");

        String formattedDate = "";
        try {
            // Chuyển chuỗi đầu vào thành đối tượng Date
            Date date = inputFormat.parse(dateStr);

            // Chuyển đối tượng Date thành chuỗi với định dạng mới
            formattedDate = outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return formattedDate;
    }

    public String convertDateFormatMonth(Date dateStr) {
        // Định dạng chuỗi đầu vào (yyyy-MM-dd)
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");

        // Định dạng chuỗi đầu ra (dd/MM/yyyy)
        SimpleDateFormat outputFormat = new SimpleDateFormat("MM/yyyy");

        String formattedDate = "";

        // Chuyển đối tượng Date thành chuỗi với định dạng mới
        formattedDate = outputFormat.format(dateStr);

        return formattedDate;
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
