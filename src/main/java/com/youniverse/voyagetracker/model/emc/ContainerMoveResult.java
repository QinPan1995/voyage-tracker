package com.youniverse.voyagetracker.model.emc;

public class ContainerMoveResult {

    private String containerNo;
    private MovementEvent loadedOnVessel;
    private MovementEvent dischargedFcl;

    public ContainerMoveResult() {
    }

    public ContainerMoveResult(String containerNo, MovementEvent loadedOnVessel,
                                MovementEvent dischargedFcl) {
        this.containerNo = containerNo;
        this.loadedOnVessel = loadedOnVessel;
        this.dischargedFcl = dischargedFcl;
    }

    public String getContainerNo() {
        return containerNo;
    }

    public void setContainerNo(String containerNo) {
        this.containerNo = containerNo;
    }

    public MovementEvent getLoadedOnVessel() {
        return loadedOnVessel;
    }

    public void setLoadedOnVessel(MovementEvent loadedOnVessel) {
        this.loadedOnVessel = loadedOnVessel;
    }

    public MovementEvent getDischargedFcl() {
        return dischargedFcl;
    }

    public void setDischargedFcl(MovementEvent dischargedFcl) {
        this.dischargedFcl = dischargedFcl;
    }
}
