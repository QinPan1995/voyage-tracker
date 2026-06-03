package com.youniverse.voyagetracker.dto;

import javax.validation.constraints.NotBlank;

public class QueryRequest {

    @NotBlank(message = "Bill of lading number is required")
    private String billNo;

    public QueryRequest() {
    }

    public QueryRequest(String billNo) {
        this.billNo = billNo;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }
}
