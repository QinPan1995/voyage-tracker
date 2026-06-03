package com.youniverse.voyagetracker.model.emc;

public class LoadedResult {

    private String containerNo;
    private String date;
    private String location;
    private String vesselVoyage;

    public LoadedResult() {
    }

    public LoadedResult(String containerNo, String date, String location, String vesselVoyage) {
        this.containerNo = containerNo;
        this.date = date;
        this.location = location;
        this.vesselVoyage = vesselVoyage;
    }

    public String getContainerNo() {
        return containerNo;
    }

    public void setContainerNo(String containerNo) {
        this.containerNo = containerNo;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getVesselVoyage() {
        return vesselVoyage;
    }

    public void setVesselVoyage(String vesselVoyage) {
        this.vesselVoyage = vesselVoyage;
    }
}
