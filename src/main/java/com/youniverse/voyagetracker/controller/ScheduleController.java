package com.youniverse.voyagetracker.controller;

import com.youniverse.voyagetracker.dto.ApiResponse;
import com.youniverse.voyagetracker.dto.QueryRequest;
import com.youniverse.voyagetracker.model.cosco.SailingScheduleResult;
import com.youniverse.voyagetracker.model.emc.ContainerMoveResult;
import com.youniverse.voyagetracker.service.CoscoShippingService;
import com.youniverse.voyagetracker.service.EmcService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final CoscoShippingService coscoShippingService;
    private final EmcService emcService;

    public ScheduleController(CoscoShippingService coscoShippingService, EmcService emcService) {
        this.coscoShippingService = coscoShippingService;
        this.emcService = emcService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSchedule(
            @RequestParam("billNo") String billNo,
            @RequestParam(value = "carrier", defaultValue = "") String carrier) {
        return query(billNo, carrier);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> postSchedule(
            @Valid @RequestBody QueryRequest request,
            @RequestParam(value = "carrier", defaultValue = "") String carrier) {
        return query(request.getBillNo(), carrier);
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> query(String billNo, String carrier) {
        try {
            String resolvedCarrier = resolveCarrier(billNo, carrier);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("carrier", resolvedCarrier);

            if ("cosco".equalsIgnoreCase(resolvedCarrier)) {
                List<SailingScheduleResult> data = coscoShippingService.queryLiveSchedules(billNo);
                result.put("date", data);
            } else if ("emc".equalsIgnoreCase(resolvedCarrier)) {
                List<ContainerMoveResult> data = emcService.queryContainerMoveDates(billNo);
                result.put("date", data);
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Unsupported carrier: " + resolvedCarrier));
            }

            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        }
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
        }
        return "cosco";
    }
}
