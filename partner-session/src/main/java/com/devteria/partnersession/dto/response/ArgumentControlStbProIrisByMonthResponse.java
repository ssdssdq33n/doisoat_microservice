package com.devteria.partnersession.dto.response;

public class ArgumentControlStbProIrisByMonthResponse {
    private Long total;
    private Long totalSuccess;
    private Long totalFail;
    private Long totalNotMatch;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getTotalSuccess() {
        return totalSuccess;
    }

    public void setTotalSuccess(Long totalSuccess) {
        this.totalSuccess = totalSuccess;
    }

    public Long getTotalFail() {
        return totalFail;
    }

    public void setTotalFail(Long totalFail) {
        this.totalFail = totalFail;
    }

    public Long getTotalNotMatch() {
        return totalNotMatch;
    }

    public void setTotalNotMatch(Long totalNotMatch) {
        this.totalNotMatch = totalNotMatch;
    }
}
