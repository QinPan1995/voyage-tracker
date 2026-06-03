package com.youniverse.voyagetracker.model.emc;

public class MovementEvent {

    private String date;
    private String location;
    private String vesselVoyage;

    public MovementEvent() {
    }

    public MovementEvent(String date, String location, String vesselVoyage) {
        this.date = date;
        this.location = location;
        this.vesselVoyage = vesselVoyage;
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
