package com.devteria.partnersession.dto.response;

public class ArgumentControlProIrisByMonthResponse {
    private Long proIrisCount;
    private Long proSucIrisSucCount;
    private Long proErrorIrisFailCount;
    private Long notMatchCount;

    public Long getProIrisCount() {
        return proIrisCount;
    }

    public void setProIrisCount(Long proIrisCount) {
        this.proIrisCount = proIrisCount;
    }

    public Long getProSucIrisSucCount() {
        return proSucIrisSucCount;
    }

    public void setProSucIrisSucCount(Long proSucIrisSucCount) {
        this.proSucIrisSucCount = proSucIrisSucCount;
    }

    public Long getProErrorIrisFailCount() {
        return proErrorIrisFailCount;
    }

    public void setProErrorIrisFailCount(Long proErrorIrisFailCount) {
        this.proErrorIrisFailCount = proErrorIrisFailCount;
    }

    public Long getNotMatchCount() {
        return notMatchCount;
    }

    public void setNotMatchCount(Long notMatchCount) {
        this.notMatchCount = notMatchCount;
    }
}
