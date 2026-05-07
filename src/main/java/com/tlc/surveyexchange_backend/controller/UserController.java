package com.tlc.surveyexchange_backend.controller;

import com.tlc.surveyexchange_backend.dto.response.UserSummaryResponse;
import com.tlc.surveyexchange_backend.security.UserPrincipal;
import com.tlc.surveyexchange_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "Authenticated user profile")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get current user profile",
            description = "Returns the authenticated user's name, email, and current credit balance."
    )
    @ApiResponse(responseCode = "200", description = "Profile returned",
            content = @Content(schema = @Schema(implementation = UserSummaryResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    @GetMapping("/me")
    public ResponseEntity<UserSummaryResponse> getMe(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(userService.getMe(currentUser.getId()));
    }
}
