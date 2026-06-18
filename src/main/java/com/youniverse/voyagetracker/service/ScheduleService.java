package com.youniverse.voyagetracker.service;

import com.youniverse.voyagetracker.dto.VoyageSchedule;
import com.youniverse.voyagetracker.exception.TrackingException;
import com.youniverse.voyagetracker.model.cma.CmaCgmMovementEvent;
import com.youniverse.voyagetracker.model.cma.CmaCgmTrackingResult;
import com.youniverse.voyagetracker.model.cosco.SailingScheduleResult;
import com.youniverse.voyagetracker.model.emc.ContainerMoveResult;
import com.youniverse.voyagetracker.model.emc.MovementEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ScheduleService {

    private static final Pattern VESSEL_VOYAGE_PATTERN = Pattern.compile("^(.+?)\\s+(\\S+)$");
    private static final Pattern EMC_DATE_PATTERN = Pattern.compile("^(\\w{3})-(\\d{2})-(\\d{4})$");

    private static final Map<String, String> MONTH_MAP = new HashMap<>();

    static {
        MONTH_MAP.put("JAN", "01");
        MONTH_MAP.put("FEB", "02");
        MONTH_MAP.put("MAR", "03");
        MONTH_MAP.put("APR", "04");
        MONTH_MAP.put("MAY", "05");
        MONTH_MAP.put("JUN", "06");
        MONTH_MAP.put("JUL", "07");
        MONTH_MAP.put("AUG", "08");
        MONTH_MAP.put("SEP", "09");
        MONTH_MAP.put("OCT", "10");
        MONTH_MAP.put("NOV", "11");
        MONTH_MAP.put("DEC", "12");
    }

    private final CoscoShippingService coscoShippingService;
    private final EmcService emcService;
    private final CmaCgmService cmaCgmService;

    public ScheduleService(CoscoShippingService coscoShippingService, EmcService emcService,
                           CmaCgmService cmaCgmService) {
        this.coscoShippingService = coscoShippingService;
        this.emcService = emcService;
        this.cmaCgmService = cmaCgmService;
    }

    public List<VoyageSchedule> query(String billNo, String carrier) {
        String resolvedCarrier = resolveCarrier(billNo, carrier);

        if ("cosco".equals(resolvedCarrier)) {
            return toVoyageSchedulesFromCosco(billNo);
        }

        if ("emc".equals(resolvedCarrier)) {
            List<ContainerMoveResult> results = emcService.queryContainerMoveDates(billNo);
            return toVoyageSchedulesFromEmc(results);
        }

        if ("cma".equals(resolvedCarrier)) {
            CmaCgmTrackingResult result = cmaCgmService.queryTracking(billNo);
            return toVoyageSchedulesFromCmaCgm(result);
        }

        throw new TrackingException("Unsupported carrier: " + resolvedCarrier);
    }

    private List<VoyageSchedule> toVoyageSchedulesFromCosco(String billNo) {
        SailingScheduleResult r = coscoShippingService.queryFinalSchedule(billNo);
        VoyageSchedule s = new VoyageSchedule();
        s.setCarrier("cosco");
        s.setVesselName(r.getVesselName());
        s.setVoyageNo(r.getVoyageNo());
        s.setService(r.getService());
        s.setPortOfLoading(r.getPortOfLoading());
        s.setPortOfDischarge(r.getPortOfDischarge());
        s.setEtd(truncateDate(r.getExpectedDateOfDeparture()));
        s.setAtd(truncateDate(r.getActualDepartureDate()));
        s.setEta(truncateDate(r.getEstimatedDateOfArrival()));
        s.setAta(truncateDate(r.getActualArrivalDate()));
        List<VoyageSchedule> list = new ArrayList<>();
        list.add(s);
        return list;
    }

    private List<VoyageSchedule> toVoyageSchedulesFromEmc(List<ContainerMoveResult> results) {
        List<VoyageSchedule> list = new ArrayList<>();
        for (ContainerMoveResult r : results) {
            VoyageSchedule s = new VoyageSchedule();
            s.setCarrier("emc");
            s.setContainerNo(r.getContainerNo());

            MovementEvent loaded = r.getLoadedOnVessel();
            MovementEvent discharged = r.getDischargedFcl();

            if (loaded != null) {
                String[] parts = parseVesselVoyage(loaded.getVesselVoyage());
                s.setVesselName(parts[0]);
                s.setVoyageNo(parts[1]);
                s.setPortOfLoading(loaded.getLocation());
                s.setAtd(convertEmcDate(loaded.getDate()));
            }
            if (discharged != null) {
                s.setPortOfDischarge(discharged.getLocation());
                s.setAta(convertEmcDate(discharged.getDate()));
            }

            list.add(s);
        }
        return list;
    }

    private List<VoyageSchedule> toVoyageSchedulesFromCmaCgm(CmaCgmTrackingResult result) {
        List<VoyageSchedule> list = new ArrayList<>();
        VoyageSchedule s = new VoyageSchedule();
        s.setCarrier("cmacgm");
        s.setContainerNo(result.getContainerNo());
        s.setBookingRef(result.getBookingRef());
        s.setPortOfLoading(result.getPol());
        s.setPortOfDischarge(result.getPod());

        for (CmaCgmMovementEvent m : result.getMovements()) {
            String moveType = m.getMove();
            String fullDate = m.getDate() + (m.getTime().isEmpty() ? "" : " " + m.getTime());
            String formattedDate = cmaCgmService.extractDateFromDateTime(fullDate);

            if ("LOADED ON BOARD".equalsIgnoreCase(moveType)) {
                s.setVesselName(parseCmaVesselName(m.getVessel()));
                s.setVoyageNo(parseCmaVoyageNo(m.getVessel()));
                s.setAtd(formattedDate);
                s.setPortOfLoading(m.getLocation());
                continue;
            }
            if ("VESSEL DEPARTURE".equalsIgnoreCase(moveType) && s.getAtd() == null) {
                s.setVesselName(parseCmaVesselName(m.getVessel()));
                s.setVoyageNo(parseCmaVoyageNo(m.getVessel()));
                s.setAtd(formattedDate);
                s.setPortOfLoading(m.getLocation());
                continue;
            }
            if ("DISCHARGED".equalsIgnoreCase(moveType)) {
                s.setAta(formattedDate);
                s.setPortOfDischarge(m.getLocation());
                continue;
            }
            if ("VESSEL ARRIVAL".equalsIgnoreCase(moveType) && s.getAta() == null) {
                s.setAta(formattedDate);
                s.setPortOfDischarge(m.getLocation());
            }
        }

        list.add(s);
        return list;
    }

    private String parseCmaVesselName(String vesselStr) {
        if (vesselStr == null || vesselStr.isEmpty()) return "";
        int parenIdx = vesselStr.indexOf('(');
        return parenIdx > 0 ? vesselStr.substring(0, parenIdx).trim() : vesselStr.trim();
    }

    private String parseCmaVoyageNo(String vesselStr) {
        if (vesselStr == null || vesselStr.isEmpty()) return "";
        int parenStart = vesselStr.indexOf('(');
        int parenEnd = vesselStr.indexOf(')');
        if (parenStart >= 0 && parenEnd > parenStart) {
            return vesselStr.substring(parenStart + 1, parenEnd).trim();
        }
        return "";
    }

    private String[] parseVesselVoyage(String vesselVoyage) {
        if (vesselVoyage == null || vesselVoyage.isEmpty()) {
            return new String[]{"", ""};
        }
        Matcher matcher = VESSEL_VOYAGE_PATTERN.matcher(vesselVoyage.trim());
        if (matcher.matches()) {
            return new String[]{matcher.group(1), matcher.group(2)};
        }
        return new String[]{vesselVoyage, ""};
    }

    private String truncateDate(String dateTime) {
        if (dateTime == null) {
            return null;
        }
        int spaceIndex = dateTime.indexOf(' ');
        return spaceIndex > 0 ? dateTime.substring(0, spaceIndex) : dateTime;
    }

    private String convertEmcDate(String emcDate) {
        if (emcDate == null || emcDate.isEmpty()) {
            return null;
        }
        Matcher matcher = EMC_DATE_PATTERN.matcher(emcDate.trim().toUpperCase());
        if (!matcher.matches()) {
            return emcDate;
        }
        String month = MONTH_MAP.get(matcher.group(1));
        if (month == null) {
            return emcDate;
        }
        return matcher.group(3) + "-" + month + "-" + matcher.group(2);
    }

    private String resolveCarrier(String billNo, String carrier) {
        if (carrier != null && !carrier.isEmpty()) {
            return carrier.toLowerCase();
        }
        if (billNo != null) {
            String upper = billNo.toUpperCase().trim();
            if (upper.startsWith("COSU") || upper.matches("\\d+")) {
                return "cosco";
            }
            if (upper.startsWith("EGLV")) {
                return "emc";
            }
            if (upper.startsWith("CMAU") || upper.startsWith("CMDU")) {
                return "cmacgm";
            }
        }
        return "cosco";
    }
}
