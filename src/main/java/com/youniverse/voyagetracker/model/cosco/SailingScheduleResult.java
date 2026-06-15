package com.youniverse.voyagetracker.model.cosco;

public class SailingScheduleResult {

    private String rowNumber;
    private String sequenceNumber;
    private String vesselName;
    private String voyageNo;
    private String service;
    private String portOfLoading;
    private String portOfDischarge;
    private String expectedDateOfDeparture;
    private String actualDepartureDate;
    private String estimatedDateOfArrival;
    private String actualArrivalDate;
    private String transType;

    public SailingScheduleResult() {
    }

    public SailingScheduleResult(String rowNumber, String sequenceNumber, String vesselName,
                                  String voyageNo, String service, String portOfLoading,
                                  String expectedDateOfDeparture, String actualDepartureDate,
                                  String portOfDischarge, String estimatedDateOfArrival,
                                  String actualArrivalDate,
                                  String transType) {
        this.rowNumber = rowNumber;
        this.sequenceNumber = sequenceNumber;
        this.vesselName = vesselName;
        this.voyageNo = voyageNo;
        this.service = service;
        this.portOfLoading = portOfLoading;
        this.expectedDateOfDeparture = expectedDateOfDeparture;
        this.actualDepartureDate = actualDepartureDate;
        this.portOfDischarge = portOfDischarge;
        this.estimatedDateOfArrival = estimatedDateOfArrival;
        this.actualArrivalDate = actualArrivalDate;
        this.transType = transType;
    }

    public String getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(String rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(String sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getVesselName() {
        return vesselName;
    }

    public void setVesselName(String vesselName) {
        this.vesselName = vesselName;
    }

    public String getVoyageNo() {
        return voyageNo;
    }

    public void setVoyageNo(String voyageNo) {
        this.voyageNo = voyageNo;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getPortOfLoading() {
        return portOfLoading;
    }

    public void setPortOfLoading(String portOfLoading) {
        this.portOfLoading = portOfLoading;
    }

    public String getExpectedDateOfDeparture() {
        return expectedDateOfDeparture;
    }

    public void setExpectedDateOfDeparture(String expectedDateOfDeparture) {
        this.expectedDateOfDeparture = expectedDateOfDeparture;
    }

    public String getActualDepartureDate() {
        return actualDepartureDate;
    }

    public void setActualDepartureDate(String actualDepartureDate) {
        this.actualDepartureDate = actualDepartureDate;
    }

    public String getPortOfDischarge() {
        return portOfDischarge;
    }

    public void setPortOfDischarge(String portOfDischarge) {
        this.portOfDischarge = portOfDischarge;
    }

    public String getEstimatedDateOfArrival() {
        return estimatedDateOfArrival;
    }

    public void setEstimatedDateOfArrival(String estimatedDateOfArrival) {
        this.estimatedDateOfArrival = estimatedDateOfArrival;
    }

    public String getActualArrivalDate() {
        return actualArrivalDate;
    }

    public void setActualArrivalDate(String actualArrivalDate) {
        this.actualArrivalDate = actualArrivalDate;
    }

    public String getTransType() {
        return transType;
    }

    public void setTransType(String transType) {
        this.transType = transType;
    }
}
