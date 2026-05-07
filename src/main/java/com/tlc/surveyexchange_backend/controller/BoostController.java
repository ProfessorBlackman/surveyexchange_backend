package com.tlc.surveyexchange_backend.controller;

import com.tlc.surveyexchange_backend.dto.response.ApiErrorResponse;
import com.tlc.surveyexchange_backend.dto.response.BoostResponseDto;
import com.tlc.surveyexchange_backend.security.UserPrincipal;
import com.tlc.surveyexchange_backend.service.BoostResult;
import com.tlc.surveyexchange_backend.service.BoostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Boost", description = "Promote a survey to the top of the feed for 24 hours")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class BoostController {

    private final BoostService boostService;

    @Operation(
            summary = "Boost a survey",
            description = "Deducts 5 credits and promotes the survey to the top of the feed for 24 hours. "
                    + "Only the survey owner may boost. A survey with an active boost cannot be double-boosted."
    )
    @ApiResponse(responseCode = "200", description = "Boost applied",
            content = @Content(schema = @Schema(implementation = BoostResponseDto.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @ApiResponse(responseCode = "402", description = "Insufficient credits",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Requester does not own the survey",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Survey not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Survey already has an active boost",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping("/{surveyId}/boost")
    public ResponseEntity<BoostResponseDto> boost(
            @Parameter(description = "Survey UUID") @PathVariable UUID surveyId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        BoostResult result;
        try {
            result = boostService.boost(currentUser.getId(), surveyId);
        } catch (ObjectOptimisticLockingFailureException e) {
            result = boostService.boost(currentUser.getId(), surveyId);
        }
        return ResponseEntity.ok(BoostResponseDto.from(result));
    }
}
