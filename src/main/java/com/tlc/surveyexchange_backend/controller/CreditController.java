package com.tlc.surveyexchange_backend.controller;

import com.tlc.surveyexchange_backend.dto.request.PurchaseRequest;
import com.tlc.surveyexchange_backend.dto.response.*;
import com.tlc.surveyexchange_backend.security.UserPrincipal;
import com.tlc.surveyexchange_backend.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Credits", description = "Credit balance, transaction history, and purchases")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    @Operation(
            summary = "Get credit balance",
            description = "Returns the authenticated user's current credit balance."
    )
    @ApiResponse(responseCode = "200", description = "Balance returned",
            content = @Content(schema = @Schema(implementation = CreditBalanceResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @GetMapping("/balance")
    public ResponseEntity<CreditBalanceResponse> getCreditBalance(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(creditService.getCreditBalance(currentUser.getId()));
    }

    @Operation(
            summary = "Get credit transaction history",
            description = "Returns the authenticated user's paginated credit ledger, sorted by most recent first."
    )
    @ApiResponse(responseCode = "200", description = "History returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @GetMapping("/history")
    public ResponseEntity<PageResponse<CreditTransactionResponse>> getCreditHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(creditService.getCreditHistory(currentUser.getId(), page));
    }

    @Operation(
            summary = "Purchase credits",
            description = "Simulates a credit purchase (mock payment — always succeeds in v1). "
                    + "Valid package sizes: 10, 25, 50, 100."
    )
    @ApiResponse(responseCode = "200", description = "Credits added",
            content = @Content(schema = @Schema(implementation = PurchaseResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid package size",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @PostMapping("/purchase")
    public ResponseEntity<PurchaseResponse> purchaseCredits(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.ok(creditService.purchaseCredits(currentUser.getId(), request));
    }
}
