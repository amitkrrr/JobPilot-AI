package com.jobpilot.user;

import org.springframework.stereotype.Service;

import com.jobpilot.dto.profile.ProfileRequest;
import com.jobpilot.dto.profile.ProfileResponse;

@Service
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepository;
    private final UserRepository userRepository;

    public CandidateProfileService(
            CandidateProfileRepository profileRepository,
            UserRepository userRepository) {

        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    public ProfileResponse createProfile(
            String email,
            ProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        ));

        if (profileRepository.existsByUserId(user.getId())) {
            throw new IllegalArgumentException(
                    "Profile already exists"
            );
        }

        CandidateProfile profile =
                new CandidateProfile(user);

        updateProfileFields(profile, request);

        CandidateProfile savedProfile =
                profileRepository.save(profile);

        return toResponse(savedProfile);
    }

    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CandidateProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for this user"));

        return toResponse(profile);
    }

    public ProfileResponse updateProfile(String email, ProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CandidateProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for this user"));

        updateProfileFields(profile, request);

        CandidateProfile updatedProfile = profileRepository.save(profile);
        return toResponse(updatedProfile);
    }

    private void updateProfileFields(
            CandidateProfile profile,
            ProfileRequest request) {

        profile.setPhone(request.getPhone());
        profile.setLocation(request.getLocation());
        profile.setHeadline(request.getHeadline());
        profile.setYearsOfExperience(
                request.getYearsOfExperience()
        );
        profile.setCurrentCompany(
                request.getCurrentCompany()
        );
        profile.setCurrentRole(
                request.getCurrentRole()
        );
        profile.setLinkedinUrl(
                request.getLinkedinUrl()
        );
        profile.setGithubUrl(
                request.getGithubUrl()
        );
        profile.setPortfolioUrl(
                request.getPortfolioUrl()
        );
    }

    private ProfileResponse toResponse(
            CandidateProfile profile) {

        User user = profile.getUser();

        return new ProfileResponse(
                profile.getId(),
                user.getName(),
                user.getEmail(),
                profile.getPhone(),
                profile.getLocation(),
                profile.getHeadline(),
                profile.getYearsOfExperience(),
                profile.getCurrentCompany(),
                profile.getCurrentRole(),
                profile.getLinkedinUrl(),
                profile.getGithubUrl(),
                profile.getPortfolioUrl()
        );
    }
}