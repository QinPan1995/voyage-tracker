package com.youniverse.voyagetracker.controller;

import com.youniverse.voyagetracker.dto.ApiResponse;
import com.youniverse.voyagetracker.dto.QueryRequest;
import com.youniverse.voyagetracker.model.cosco.SailingScheduleResult;
import com.youniverse.voyagetracker.service.CoscoShippingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/cosco")
public class CoscoShipmentController {

    private final CoscoShippingService coscoShippingService;

    public CoscoShipmentController(CoscoShippingService coscoShippingService) {
        this.coscoShippingService = coscoShippingService;
    }

    @GetMapping("/schedule")
    public ResponseEntity<ApiResponse<List<SailingScheduleResult>>> getSchedule(
            @RequestParam("billNo") String billNo) {
        List<SailingScheduleResult> results = coscoShippingService.queryLiveSchedules(billNo);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<List<SailingScheduleResult>>> postSchedule(
            @Valid @RequestBody QueryRequest request) {
        List<SailingScheduleResult> results = coscoShippingService.queryLiveSchedules(request.getBillNo());
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}
