package com.youniverse.voyagetracker.service;

import com.youniverse.voyagetracker.model.emc.ContainerMoveResult;
import com.youniverse.voyagetracker.model.emc.LoadedResult;
import com.youniverse.voyagetracker.model.emc.MovementEvent;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

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

@Service
public class EmcService {

    private static final String TRACKING_URL = "https://ct.shipmentlink.com/servlet/TDB1_CargoTracking.do";
    private static final String LOADED_ON_VESSEL = "Loaded (FCL) on vessel";
    private static final String DISCHARGED_FCL = "Discharged (FCL)";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    public List<ContainerMoveResult> queryContainerMoveDates(String blNo) throws IOException {
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

        List<ContainerMoveResult> results = new ArrayList<>();
        for (String containerNo : containerNos) {
            Map<String, String> detailParams = new LinkedHashMap<>();
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

    public List<LoadedResult> queryLoadedOnVesselDates(String blNo) throws IOException {
        List<LoadedResult> loadedResults = new ArrayList<>();
        for (ContainerMoveResult result : queryContainerMoveDates(blNo)) {
            if (result.getLoadedOnVessel() != null) {
                loadedResults.add(new LoadedResult(
                        result.getContainerNo(),
                        result.getLoadedOnVessel().getDate(),
                        result.getLoadedOnVessel().getLocation(),
                        result.getLoadedOnVessel().getVesselVoyage()));
            }
        }
        return loadedResults;
    }

    private Document post(Map<String, String> params) throws IOException {
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

    private Map<String, String> trackingParams(String blNo) {
        Map<String, String> params = new LinkedHashMap<>();
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

    private Map<String, String> formInputs(Document doc, String formName) {
        Map<String, String> inputs = new LinkedHashMap<>();
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

        for (Element input = form.nextElementSibling();
             input != null && "input".equalsIgnoreCase(input.tagName());
             input = input.nextElementSibling()) {
            if (input.hasAttr("name")) {
                inputs.put(input.attr("name"), input.attr("value"));
            }
        }
        return inputs;
    }

    private List<String> containerNos(Document doc) {
        Set<String> containerNos = new LinkedHashSet<>();
        for (Element link : doc.select("a[href^=javascript:frmCntrMoveDetail]")) {
            String containerNo = parseContainerNo(link.attr("href"));
            if (!containerNo.isEmpty()) {
                containerNos.add(containerNo);
            }
        }
        return new ArrayList<>(containerNos);
    }

    private String parseContainerNo(String href) {
        Matcher matcher = Pattern.compile("frmCntrMoveDetail\\('([^']+)'\\)").matcher(href);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private MovementEvent movementEvent(Document detailDoc, String movementName) {
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

    private boolean sameMovement(String actual, String expected) {
        return cleanText(actual).equalsIgnoreCase(expected);
    }

    private List<String> directCellTexts(Element row) {
        List<String> values = new ArrayList<>();
        Elements children = row.children();
        for (Element child : children) {
            if ("td".equalsIgnoreCase(child.tagName()) || "th".equalsIgnoreCase(child.tagName())) {
                values.add(cleanText(child.text()));
            }
        }
        return values;
    }

    private String cleanText(String text) {
        return text == null ? "" : text.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private String valueOr(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private String truncate(String s, int maxLength) {
        if (s == null || s.length() <= maxLength) {
            return s;
        }
        return s.substring(0, maxLength) + "...";
    }
}
