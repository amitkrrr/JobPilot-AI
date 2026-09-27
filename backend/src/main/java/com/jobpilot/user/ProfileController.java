package com.jobpilot.user;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.jobpilot.dto.profile.ProfileRequest;
import com.jobpilot.dto.profile.ProfileResponse;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final CandidateProfileService profileService;

    public ProfileController(CandidateProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse getProfile(Authentication authentication) {
        return profileService.getProfile(authentication.getName());
    }

    @PostMapping
    public ProfileResponse createProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileRequest request) {
        return profileService.createProfile(authentication.getName(), request);
    }

    @PutMapping
    public ProfileResponse updateProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileRequest request) {
        return profileService.updateProfile(authentication.getName(), request);
    }
}