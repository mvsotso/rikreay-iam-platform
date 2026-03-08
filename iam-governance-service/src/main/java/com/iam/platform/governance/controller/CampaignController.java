package com.iam.platform.governance.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.governance.dto.CampaignRequest;
import com.iam.platform.governance.dto.CampaignResponse;
import com.iam.platform.governance.dto.ReviewRequest;
import com.iam.platform.governance.dto.ReviewResponse;
import com.iam.platform.governance.enums.CampaignStatus;
import com.iam.platform.governance.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/governance/campaigns")
@RequiredArgsConstructor
@Tag(name = "Certification Campaigns", description = "Access review campaigns and certifications")
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    @Operation(summary = "Create a new certification campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestBody CampaignRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(ApiResponse.ok(
                campaignService.createCampaign(request, username), "Campaign created"));
    }

    @GetMapping
    @Operation(summary = "List certification campaigns")
    public ResponseEntity<ApiResponse<Page<CampaignResponse>>> listCampaigns(
            @RequestParam(required = false) CampaignStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(campaignService.listCampaigns(status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get campaign by ID")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaign(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(campaignService.getCampaign(id)));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate a campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> activateCampaign(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(ApiResponse.ok(
                campaignService.activateCampaign(id, username), "Campaign activated"));
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "Get reviews for a campaign")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviews(
            @PathVariable UUID id,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(campaignService.getReviews(id, pageable)));
    }

    @PostMapping("/{campaignId}/reviews/{reviewId}")
    @Operation(summary = "Submit a review decision")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @PathVariable UUID campaignId,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String reviewerId = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(ApiResponse.ok(
                campaignService.submitReview(campaignId, reviewId, request, reviewerId), "Review submitted"));
    }
}
