package com.youniverse.voyagetracker.controller;

import com.youniverse.voyagetracker.dto.ApiResponse;
import com.youniverse.voyagetracker.dto.QueryRequest;
import com.youniverse.voyagetracker.model.emc.ContainerMoveResult;
import com.youniverse.voyagetracker.model.emc.LoadedResult;
import com.youniverse.voyagetracker.service.EmcService;
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
@RequestMapping("/api/emc")
public class EmcController {

    private final EmcService emcService;

    public EmcController(EmcService emcService) {
        this.emcService = emcService;
    }

    @GetMapping("/container-moves")
    public ResponseEntity<ApiResponse<List<ContainerMoveResult>>> getContainerMoves(
            @RequestParam("blNo") String blNo) {
        List<ContainerMoveResult> results = emcService.queryContainerMoveDates(blNo);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @PostMapping("/container-moves")
    public ResponseEntity<ApiResponse<List<ContainerMoveResult>>> postContainerMoves(
            @Valid @RequestBody QueryRequest request) {
        List<ContainerMoveResult> results = emcService.queryContainerMoveDates(request.getBillNo());
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/loaded-on-vessel")
    public ResponseEntity<ApiResponse<List<LoadedResult>>> getLoadedOnVessel(
            @RequestParam("blNo") String blNo) {
        List<LoadedResult> results = emcService.queryLoadedOnVesselDates(blNo);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @PostMapping("/loaded-on-vessel")
    public ResponseEntity<ApiResponse<List<LoadedResult>>> postLoadedOnVessel(
            @Valid @RequestBody QueryRequest request) {
        List<LoadedResult> results = emcService.queryLoadedOnVesselDates(request.getBillNo());
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}
