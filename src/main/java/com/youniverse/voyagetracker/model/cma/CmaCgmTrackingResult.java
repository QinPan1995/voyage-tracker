package com.youniverse.voyagetracker.model.cma;

import java.util.List;

public class CmaCgmTrackingResult {

    private String containerNo;
    private String bookingRef;
    private String pol;
    private String pod;
    private List<CmaCgmMovementEvent> movements;

    public String getContainerNo() {
        return containerNo;
    }

    public void setContainerNo(String containerNo) {
        this.containerNo = containerNo;
    }

    public String getBookingRef() {
        return bookingRef;
    }

    public void setBookingRef(String bookingRef) {
        this.bookingRef = bookingRef;
    }

    public String getPol() {
        return pol;
    }

    public void setPol(String pol) {
        this.pol = pol;
    }

    public String getPod() {
        return pod;
    }

    public void setPod(String pod) {
        this.pod = pod;
    }

    public List<CmaCgmMovementEvent> getMovements() {
        return movements;
    }

    public void setMovements(List<CmaCgmMovementEvent> movements) {
        this.movements = movements;
    }
}
