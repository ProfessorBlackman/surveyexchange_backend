package com.tlc.surveyexchange_backend.controller;

import com.tlc.surveyexchange_backend.dto.request.CreateSurveyRequest;
import com.tlc.surveyexchange_backend.dto.request.PatchSurveyRequest;
import com.tlc.surveyexchange_backend.dto.request.SubmitResponseRequest;
import com.tlc.surveyexchange_backend.dto.request.UpdateSurveyRequest;
import com.tlc.surveyexchange_backend.dto.response.*;
import com.tlc.surveyexchange_backend.security.UserPrincipal;
import com.tlc.surveyexchange_backend.service.ResponseService;
import com.tlc.surveyexchange_backend.service.SurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Surveys", description = "Create and browse surveys, submit responses, and boost")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;
    private final ResponseService responseService;

    @Operation(
            summary = "Get survey feed",
            description = "Returns a paginated list of surveys the authenticated user has not created and has not yet answered. "
                          + "Boosted surveys appear first, then most recent."
    )
    @ApiResponse(responseCode = "200", description = "Feed returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @GetMapping("/feed")
    public ResponseEntity<PageResponse<SurveySummaryResponse>> getFeed(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(surveyService.getFeed(currentUser.getId(), page));
    }

    @Operation(
            summary = "Get my surveys",
            description = "Returns all surveys created by the authenticated user, ordered by creation date descending."
    )
    @ApiResponse(responseCode = "200", description = "List returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @GetMapping("/mine")
    public ResponseEntity<List<SurveySummaryResponse>> getMySurveys(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(surveyService.getMySurveys(currentUser.getId()));
    }

    @Operation(
            summary = "Create a survey",
            description = "Creates a new survey with the provided questions. "
                          + "The survey's credit value is automatically calculated from question types."
    )
    @ApiResponse(responseCode = "201", description = "Survey created",
            content = @Content(schema = @Schema(implementation = SurveyResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @PostMapping
    public ResponseEntity<SurveyResponse> createSurvey(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateSurveyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(surveyService.createSurvey(request, currentUser.getId()));
    }

    @Operation(
            summary = "Get survey by ID",
            description = "Returns full survey details including all questions."
    )
    @ApiResponse(responseCode = "200", description = "Survey returned",
            content = @Content(schema = @Schema(implementation = SurveyResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @ApiResponse(responseCode = "404", description = "Survey not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<SurveyResponse> getSurveyById(
            @Parameter(description = "Survey UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(surveyService.getSurveyById(id));
    }

    @Operation(
            summary = "Submit a response to a survey",
            description = "Saves the user's answers and atomically awards credits equal to the survey's credit value."
                          + " A user cannot respond to their own survey or respond more than once."
    )
    @ApiResponse(responseCode = "201", description = "Response submitted and credit awarded",
            content = @Content(schema = @Schema(implementation = SubmitResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Cannot respond to own survey",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Survey not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Already responded to this survey",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping("/{id}/respond")
    public ResponseEntity<SubmitResponseDto> submitResponse(
            @Parameter(description = "Survey UUID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody SubmitResponseRequest request) {
        int newBalance = responseService.submitResponse(id, request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new SubmitResponseDto(newBalance));
    }

    @Operation(
            summary = "Edit a survey",
            description = "Updates a survey's content. Only the owner may call this, and only for DRAFT or ACTIVE surveys. "
                    + "If questions are provided they fully replace the existing list and the credit value is recalculated. "
                    + "Omit questions to leave them unchanged."
    )
    @ApiResponse(responseCode = "200", description = "Survey updated",
            content = @Content(schema = @Schema(implementation = SurveyResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Not the owner or survey is closed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Survey not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PutMapping("/{id}")
    public ResponseEntity<SurveyResponse> updateSurvey(
            @Parameter(description = "Survey UUID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateSurveyRequest request) {
        return ResponseEntity.ok(surveyService.updateSurvey(id, currentUser.getId(), request));
    }

    @Operation(
            summary = "Update survey status",
            description = "Transitions the survey to a new status. Valid moves: DRAFT→ACTIVE (publish), "
                          + "ACTIVE→CLOSED (close), CLOSED→ACTIVE (re-open). Only the owner may call this."
    )
    @ApiResponse(responseCode = "200", description = "Status updated",
            content = @Content(schema = @Schema(implementation = SurveySummaryResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Caller is not the survey owner",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Survey not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Survey is already in the requested status",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PatchMapping("/{id}")
    public ResponseEntity<SurveySummaryResponse> updateStatus(
            @Parameter(description = "Survey UUID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody PatchSurveyRequest request) {
        return ResponseEntity.ok(surveyService.updateStatus(id, currentUser.getId(), request.getStatus()));
    }

}
