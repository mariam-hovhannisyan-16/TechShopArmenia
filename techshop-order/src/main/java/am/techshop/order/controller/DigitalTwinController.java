package am.techshop.order.controller;

import am.techshop.common.dto.request.AddRepairEntryRequest;
import am.techshop.common.dto.request.UpdateNotesRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.DigitalTwinDetailResponse;
import am.techshop.common.dto.response.DigitalTwinSummaryResponse;
import am.techshop.common.security.CurrentUser;
import am.techshop.order.service.DigitalTwinService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders/my-products")
@RequiredArgsConstructor
@Validated
public class DigitalTwinController {

    private final DigitalTwinService digitalTwinService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DigitalTwinSummaryResponse>>> getMyDigitalTwins(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(digitalTwinService.getMyDigitalTwins(CurrentUser.id(authentication))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DigitalTwinDetailResponse>> getDigitalTwinDetail(
            @PathVariable @Positive Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(digitalTwinService.getDigitalTwinDetail(CurrentUser.id(authentication), id)));
    }

    @PostMapping("/{id}/repairs")
    public ResponseEntity<ApiResponse<DigitalTwinDetailResponse>> addRepairEntry(
            @PathVariable @Positive Long id,
            @RequestBody @Valid AddRepairEntryRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Repair entry added",
                digitalTwinService.addRepairEntry(CurrentUser.id(authentication), id, request)));
    }

    @PutMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<DigitalTwinDetailResponse>> updateNotes(
            @PathVariable @Positive Long id,
            @RequestBody @Valid UpdateNotesRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Notes updated",
                digitalTwinService.updateNotes(CurrentUser.id(authentication), id, request)));
    }
}
