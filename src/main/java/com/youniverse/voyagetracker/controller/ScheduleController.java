package com.youniverse.voyagetracker.controller;

import com.youniverse.voyagetracker.dto.ApiResponse;
import com.youniverse.voyagetracker.dto.QueryRequest;
import com.youniverse.voyagetracker.dto.VoyageSchedule;
import com.youniverse.voyagetracker.service.ScheduleService;
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
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VoyageSchedule>>> getSchedule(
            @RequestParam("billNo") String billNo,
            @RequestParam(value = "carrier", defaultValue = "") String carrier) {
        List<VoyageSchedule> results = scheduleService.query(billNo, carrier);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<VoyageSchedule>>> postSchedule(
            @Valid @RequestBody QueryRequest request,
            @RequestParam(value = "carrier", defaultValue = "") String carrier) {
        List<VoyageSchedule> results = scheduleService.query(request.getBillNo(), carrier);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}
