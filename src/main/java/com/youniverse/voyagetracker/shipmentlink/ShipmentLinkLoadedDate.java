package com.youniverse.voyagetracker.shipmentlink;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShipmentLinkLoadedDate {
    private static final String TRACKING_URL = "https://ct.shipmentlink.com/servlet/TDB1_CargoTracking.do";
    private static final String LOADED_ON_VESSEL = "Loaded (FCL) on vessel";
    private static final String DISCHARGED_FCL = "Discharged (FCL)";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    public static void main(String[] args) throws Exception {

        List<ContainerMoveResult> results = queryContainerMoveDates("143661469311");
        if (results.isEmpty()) {
            System.out.println("No container movement result found.");
            return;
        }

        for (ContainerMoveResult result : results) {
            System.out.println("Container: " + result.containerNo);
            printEvent(LOADED_ON_VESSEL, result.loadedOnVessel);
            printEvent(DISCHARGED_FCL, result.dischargedFcl);
        }
    }

    public static List<ContainerMoveResult> queryContainerMoveDates(String blNo) throws IOException {
        String normalizedBlNo = blNo.trim().toUpperCase(Locale.US);
        Document trackingDoc = post(trackingParams(normalizedBlNo));

        Map<String, String> cntrMoveForm = formInputs(trackingDoc, "frmCntrMove");
        if (cntrMoveForm.isEmpty()) {
            throw new IOException("Cannot find frmCntrMove in tracking page. B/L may be invalid or page structure changed.");
        }

        List<String> containerNos = containerNos(trackingDoc);
        if (containerNos.isEmpty()) {
            throw new IOException("Cannot find container link in tracking page. B/L may have no container detail.");
        }

        List<ContainerMoveResult> results = new ArrayList<ContainerMoveResult>();
        for (String containerNo : containerNos) {
            Map<String, String> detailParams = new LinkedHashMap<String, String>();
            detailParams.put("bl_no", valueOr(cntrMoveForm.get("bl_no"), normalizedBlNo));
            detailParams.put("cntr_no", containerNo);
            detailParams.put("onboard_date", valueOr(cntrMoveForm.get("onboard_date"), ""));
            detailParams.put("pol", valueOr(cntrMoveForm.get("pol"), ""));
            detailParams.put("pod", valueOr(cntrMoveForm.get("pod"), ""));
            detailParams.put("podctry", valueOr(cntrMoveForm.get("podctry"), ""));
            detailParams.put("TYPE", "CntrMove");

            Document detailDoc = post(detailParams);
            results.add(new ContainerMoveResult(
                    containerNo,
                    movementEvent(detailDoc, LOADED_ON_VESSEL),
                    movementEvent(detailDoc, DISCHARGED_FCL)));
        }

        return results;
    }

    public static List<LoadedResult> queryLoadedOnVesselDates(String blNo) throws IOException {
        List<LoadedResult> loadedResults = new ArrayList<LoadedResult>();
        for (ContainerMoveResult result : queryContainerMoveDates(blNo)) {
            if (result.loadedOnVessel != null) {
                loadedResults.add(new LoadedResult(
                        result.containerNo,
                        result.loadedOnVessel.date,
                        result.loadedOnVessel.location,
                        result.loadedOnVessel.vesselVoyage));
            }
        }
        return loadedResults;
    }

    private static void printEvent(String label, MovementEvent event) {
        if (event == null) {
            System.out.println(label + " date: Not found");
            return;
        }

        System.out.println(label + " date: " + event.date);
        if (event.location.length() > 0) {
            System.out.println(label + " location: " + event.location);
        }
        if (event.vesselVoyage.length() > 0) {
            System.out.println(label + " vessel/voyage: " + event.vesselVoyage);
        }
    }

    private static Document post(Map<String, String> params) throws IOException {
        Connection.Response response = Jsoup.connect(TRACKING_URL)
                .userAgent(USER_AGENT)
                .header("Origin", "https://ct.shipmentlink.com")
                .referrer(TRACKING_URL)
                .timeout(30000)
                .ignoreHttpErrors(true)
                .data(params)
                .method(Connection.Method.POST)
                .execute();

        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode()
                    + " from ShipmentLink: " + truncate(response.body(), 500));
        }

        return response.parse();
    }

    private static Map<String, String> trackingParams(String blNo) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("TYPE", "BL");
        params.put("BL", blNo);
        params.put("CNTR", "");
        params.put("bkno", "");
        params.put("query_bkno", "");
        params.put("query_rvs", "");
        params.put("query_docno", "");
        params.put("query_seq", "");
        params.put("PRINT", "");
        params.put("SEL", "s_bl");
        params.put("NO", blNo);
        return params;
    }

    private static Map<String, String> formInputs(Document doc, String formName) {
        Map<String, String> inputs = new LinkedHashMap<String, String>();
        Element form = doc.selectFirst("form[name=" + formName + "]");
        if (form == null) {
            return inputs;
        }

        for (Element input : form.select("input[name]")) {
            inputs.put(input.attr("name"), input.attr("value"));
        }
        if (!inputs.isEmpty()) {
            return inputs;
        }

        // ShipmentLink has old table/form markup. Jsoup correctly fixes the DOM by
        // closing the form early, so the hidden fields become following siblings.
        for (Element input = form.nextElementSibling();
             input != null && "input".equalsIgnoreCase(input.tagName());
             input = input.nextElementSibling()) {
            if (input.hasAttr("name")) {
                inputs.put(input.attr("name"), input.attr("value"));
            }
        }
        return inputs;
    }

    private static List<String> containerNos(Document doc) {
        Set<String> containerNos = new LinkedHashSet<String>();
        for (Element link : doc.select("a[href^=javascript:frmCntrMoveDetail]")) {
            String containerNo = parseContainerNo(link.attr("href"));
            if (containerNo.length() > 0) {
                containerNos.add(containerNo);
            }
        }
        return new ArrayList<String>(containerNos);
    }

    private static String parseContainerNo(String href) {
        Matcher matcher = Pattern.compile("frmCntrMoveDetail\\('([^']+)'\\)").matcher(href);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static MovementEvent movementEvent(Document detailDoc, String movementName) {
        for (Element row : detailDoc.select("tr")) {
            List<String> cells = directCellTexts(row);
            if (cells.size() >= 2 && sameMovement(cells.get(1), movementName)) {
                String date = cells.get(0);
                String location = cells.size() >= 3 ? cells.get(2) : "";
                String vesselVoyage = cells.size() >= 4 ? cells.get(3) : "";
                return new MovementEvent(date, location, vesselVoyage);
            }
        }
        return null;
    }

    private static boolean sameMovement(String actual, String expected) {
        return cleanText(actual).equalsIgnoreCase(expected);
    }

    private static List<String> directCellTexts(Element row) {
        List<String> values = new ArrayList<String>();
        Elements children = row.children();
        for (Element child : children) {
            if ("td".equalsIgnoreCase(child.tagName()) || "th".equalsIgnoreCase(child.tagName())) {
                values.add(cleanText(child.text()));
            }
        }
        return values;
    }

    private static String cleanText(String text) {
        return text == null ? "" : text.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String valueOr(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static String truncate(String s, int maxLength) {
        if (s == null || s.length() <= maxLength) {
            return s;
        }
        return s.substring(0, maxLength) + "...";
    }

    public static class ContainerMoveResult {
        public final String containerNo;
        public final MovementEvent loadedOnVessel;
        public final MovementEvent dischargedFcl;

        public ContainerMoveResult(String containerNo, MovementEvent loadedOnVessel, MovementEvent dischargedFcl) {
            this.containerNo = containerNo;
            this.loadedOnVessel = loadedOnVessel;
            this.dischargedFcl = dischargedFcl;
        }
    }

    public static class MovementEvent {
        public final String date;
        public final String location;
        public final String vesselVoyage;

        public MovementEvent(String date, String location, String vesselVoyage) {
            this.date = date;
            this.location = location;
            this.vesselVoyage = vesselVoyage;
        }
    }

    public static class LoadedResult {
        public final String containerNo;
        public final String date;
        public final String location;
        public final String vesselVoyage;

        public LoadedResult(String containerNo, String date, String location, String vesselVoyage) {
            this.containerNo = containerNo;
            this.date = date;
            this.location = location;
            this.vesselVoyage = vesselVoyage;
        }
    }
}
