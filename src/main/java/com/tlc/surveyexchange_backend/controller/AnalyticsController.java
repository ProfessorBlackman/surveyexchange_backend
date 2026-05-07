package com.tlc.surveyexchange_backend.controller;

import com.tlc.surveyexchange_backend.dto.response.PageResponse;
import com.tlc.surveyexchange_backend.dto.response.ResponseRowDTO;
import com.tlc.surveyexchange_backend.dto.response.SurveyAnalyticsResponse;
import com.tlc.surveyexchange_backend.dto.response.ApiErrorResponse;
import com.tlc.surveyexchange_backend.security.UserPrincipal;
import com.tlc.surveyexchange_backend.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Analytics", description = "Aggregated survey insights and raw response data — owner only")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/surveys/{id}/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(
            summary = "Get survey analytics",
            description = "Returns aggregated metrics and per-question distributions for the survey. "
                    + "Choice-type questions (MULTIPLE_CHOICE, CHECKBOX, RATING, YES_NO) return option counts "
                    + "and percentages; text questions (SHORT_TEXT, LONG_TEXT) return the raw response list. "
                    + "Only the survey owner may call this endpoint."
    )
    @ApiResponse(responseCode = "200", description = "Analytics returned",
            content = @Content(schema = @Schema(implementation = SurveyAnalyticsResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Not the survey owner",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Survey not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping
    public ResponseEntity<SurveyAnalyticsResponse> getAnalytics(
            @Parameter(description = "Survey UUID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(analyticsService.getAnalytics(id, currentUser.getId()));
    }

    @Operation(
            summary = "Get raw paginated responses",
            description = "Returns paginated response rows showing each respondent's answers per question. "
                    + "User identity is never exposed. Only the survey owner may call this endpoint."
    )
    @ApiResponse(responseCode = "200", description = "Responses returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Not the survey owner",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Survey not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/responses")
    public ResponseEntity<PageResponse<ResponseRowDTO>> getRawResponses(
            @Parameter(description = "Survey UUID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(analyticsService.getRawResponses(id, currentUser.getId(), page, size));
    }
}
