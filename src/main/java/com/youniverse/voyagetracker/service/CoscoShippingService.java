package com.youniverse.voyagetracker.service;

import com.youniverse.voyagetracker.model.cosco.SailingScheduleResult;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CoscoShippingService {

    private static final String BILL_TRACKING_URL = "https://elines.coscoshipping.com/ebtracking/public/bill/";
    private static final String CARGO_TRACKING_URL = "https://elines.coscoshipping.com/ebusiness/cargoTracking";
    private static final String TRACKING_TYPE_BILL_OF_LADING = "BILLOFLADING";
    private static final String ACTUAL_SHIPMENT = "actualShipment";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    public List<SailingScheduleResult> queryLiveSchedules(String billNo) throws IOException {
        String normalizedBillNo = billNo.trim().toUpperCase(Locale.US);
        if (normalizedBillNo.isEmpty()) {
            throw new IOException("B/L number is required.");
        }

        String responseBody = get(BILL_TRACKING_URL + encode(normalizedBillNo), referrer(normalizedBillNo));
        validateResponse(responseBody);
        return parseLiveSchedules(responseBody);
    }

    List<SailingScheduleResult> parseLiveSchedules(String responseBody) throws IOException {
        String actualShipmentArray = jsonArray(responseBody, ACTUAL_SHIPMENT);
        List<SailingScheduleResult> results = new ArrayList<>();
        if (actualShipmentArray == null || actualShipmentArray.isEmpty()) {
            return results;
        }

        for (String shipmentJson : splitObjects(actualShipmentArray)) {
            results.add(new SailingScheduleResult(
                    stringField(shipmentJson, "rownum"),
                    stringField(shipmentJson, "sequenceNumber"),
                    stringField(shipmentJson, "vesselName"),
                    stringField(shipmentJson, "voyageNo"),
                    stringField(shipmentJson, "service"),
                    stringField(shipmentJson, "portOfLoading"),
                    stringField(shipmentJson, "expectedDateOfDeparture"),
                    stringField(shipmentJson, "actualDepartureDate"),
                    stringField(shipmentJson, "portOfDischarge"),
                    stringField(shipmentJson, "estimatedDateOfArrival"),
                    stringField(shipmentJson, "actualArrivalDate"),
                    stringField(shipmentJson, "actualDischargeDate"),
                    stringField(shipmentJson, "transType")));
        }
        return results;
    }

    private String get(String url, String referrer) throws IOException {
        Connection.Response response = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json;charset=utf-8")
                .header("language", "zh_CN")
                .header("sys", "EB")
                .header("X-Client-Timestamp", String.valueOf(System.currentTimeMillis()))
                .referrer(referrer)
                .timeout(30000)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .method(Connection.Method.GET)
                .execute();

        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode()
                    + " from COSCO Shipping: " + truncate(response.body(), 500));
        }
        return response.body();
    }

    private void validateResponse(String body) throws IOException {
        String code = stringField(body, "code");
        if (code == null || code.isEmpty() || "200".equals(code)) {
            return;
        }

        String message = stringField(body, "message");
        throw new IOException("COSCO Shipping response code " + code + ": " + valueOr(message, ""));
    }

    private String jsonArray(String json, String fieldName) throws IOException {
        int keyIndex = json.indexOf("\"" + fieldName + "\"");
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', keyIndex);
        if (colonIndex < 0) {
            throw new IOException("Invalid JSON: cannot find ':' after " + fieldName + ".");
        }

        int arrayStart = skipWhitespace(json, colonIndex + 1);
        if (startsWith(json, arrayStart, "null")) {
            return null;
        }
        if (arrayStart >= json.length() || json.charAt(arrayStart) != '[') {
            throw new IOException("Invalid JSON: " + fieldName + " is not an array.");
        }

        int arrayEnd = findMatching(json, arrayStart, '[', ']');
        if (arrayEnd < 0) {
            throw new IOException("Invalid JSON: " + fieldName + " array is not closed.");
        }
        return json.substring(arrayStart + 1, arrayEnd);
    }

    private List<String> splitObjects(String arrayBody) throws IOException {
        List<String> objects = new ArrayList<>();
        int index = 0;
        while (index < arrayBody.length()) {
            index = skipWhitespaceAndCommas(arrayBody, index);
            if (index >= arrayBody.length()) {
                break;
            }
            if (arrayBody.charAt(index) != '{') {
                throw new IOException("Invalid JSON: expected object in actualShipment array.");
            }

            int objectEnd = findMatching(arrayBody, index, '{', '}');
            if (objectEnd < 0) {
                throw new IOException("Invalid JSON: actualShipment object is not closed.");
            }
            objects.add(arrayBody.substring(index, objectEnd + 1));
            index = objectEnd + 1;
        }
        return objects;
    }

    private int findMatching(String text, int start, char open, char close) {
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
            } else if (ch == open) {
                depth++;
            } else if (ch == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String stringField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName)
                + "\"\\s*:\\s*(null|\"((?:\\\\.|[^\"\\\\])*)\")");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find() || "null".equals(matcher.group(1))) {
            return null;
        }
        return unescapeJson(matcher.group(2));
    }

    private String unescapeJson(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch != '\\' || i + 1 >= value.length()) {
                result.append(ch);
                continue;
            }

            char escaped = value.charAt(++i);
            if (escaped == '"' || escaped == '\\' || escaped == '/') {
                result.append(escaped);
            } else if (escaped == 'b') {
                result.append('\b');
            } else if (escaped == 'f') {
                result.append('\f');
            } else if (escaped == 'n') {
                result.append('\n');
            } else if (escaped == 'r') {
                result.append('\r');
            } else if (escaped == 't') {
                result.append('\t');
            } else if (escaped == 'u' && i + 4 < value.length()) {
                String hex = value.substring(i + 1, i + 5);
                result.append((char) Integer.parseInt(hex, 16));
                i += 4;
            } else {
                result.append(escaped);
            }
        }
        return result.toString();
    }

    private int skipWhitespace(String value, int index) {
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private int skipWhitespaceAndCommas(String value, int index) {
        while (index < value.length()) {
            char ch = value.charAt(index);
            if (!Character.isWhitespace(ch) && ch != ',') {
                break;
            }
            index++;
        }
        return index;
    }

    private boolean startsWith(String value, int index, String prefix) {
        return index >= 0 && index + prefix.length() <= value.length()
                && value.substring(index, index + prefix.length()).equals(prefix);
    }

    private String referrer(String billNo) throws UnsupportedEncodingException {
        return CARGO_TRACKING_URL
                + "?trackingType=" + TRACKING_TYPE_BILL_OF_LADING
                + "&number=" + encode(billNo);
    }

    private String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8");
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
