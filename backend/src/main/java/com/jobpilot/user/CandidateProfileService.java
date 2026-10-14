package com.jobpilot.user;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobpilot.dto.profile.ProfileRequest;
import com.jobpilot.dto.profile.ProfileResponse;
import com.jobpilot.dto.resume.ResumeParsedResponse;
import com.jobpilot.resume.AIParsingClient;
import com.jobpilot.resume.Resume;
import com.jobpilot.resume.ResumeRepository;
import com.jobpilot.storage.StorageService;

@Service
public class CandidateProfileService {

    private static final Logger log = LoggerFactory.getLogger(CandidateProfileService.class);

    private final CandidateProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final AIParsingClient aiParsingClient;
    private final ResumeRepository resumeRepository;

    public CandidateProfileService(
            CandidateProfileRepository profileRepository,
            UserRepository userRepository,
            StorageService storageService,
            AIParsingClient aiParsingClient,
            ResumeRepository resumeRepository) {

        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.aiParsingClient = aiParsingClient;
        this.resumeRepository = resumeRepository;
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

    @Transactional
    public ProfileResponse importProfileFromResume(
            String email,
            String fileName,
            String contentType,
            byte[] bytes) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (profileRepository.existsByUserId(user.getId())) {
            throw new IllegalArgumentException("Profile already exists");
        }

        // 1. Persist a shell CandidateProfile so we have an ID for the S3 key structure
        CandidateProfile profile = new CandidateProfile(user);
        profile = profileRepository.save(profile);

        // 2. Upload file to S3
        String cleanFileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
        String s3Key = "resumes/" + profile.getId() + "/" + UUID.randomUUID() + "-" + cleanFileName;
        storageService.uploadFile(s3Key, bytes, contentType);

        // 3. Save Resume metadata
        Resume resume = new Resume(profile, fileName, contentType, s3Key);
        resumeRepository.save(resume);

        // 4. Trigger AI Resume Parsing
        try {
            ResumeParsedResponse parsed = aiParsingClient.parseResume(s3Key);

            // 5. Populate profile fields
            if (parsed.getPhone() != null) {
                profile.setPhone(parsed.getPhone());
            }
            if (parsed.getLocation() != null) {
                profile.setLocation(parsed.getLocation());
            }
            if (parsed.getHeadline() != null) {
                profile.setHeadline(parsed.getHeadline());
            }
            if (parsed.getYearsOfExperience() != null) {
                profile.setYearsOfExperience(parsed.getYearsOfExperience());
            }
            if (parsed.getCurrentCompany() != null) {
                profile.setCurrentCompany(parsed.getCurrentCompany());
            }
            if (parsed.getCurrentRole() != null) {
                profile.setCurrentRole(parsed.getCurrentRole());
            }
            if (parsed.getLinkedinUrl() != null) {
                profile.setLinkedinUrl(parsed.getLinkedinUrl());
            }
            if (parsed.getGithubUrl() != null) {
                profile.setGithubUrl(parsed.getGithubUrl());
            }
            if (parsed.getPortfolioUrl() != null) {
                profile.setPortfolioUrl(parsed.getPortfolioUrl());
            }
            if (parsed.getSkills() != null && !parsed.getSkills().isEmpty()) {
                profile.setSkills(String.join(", ", parsed.getSkills()));
            }

            // Save updated profile with AI values
            profile = profileRepository.save(profile);
        } catch (Exception e) {
            log.warn("AI parsing failed during profile import, keeping shell profile: {}", e.getMessage());
            profile.setHeadline("Imported Candidate Profile");
            profile = profileRepository.save(profile);
        }

        return toResponse(profile);
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
        profile.setSkills(
                request.getSkills()
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
                profile.getPortfolioUrl(),
                profile.getSkills()
        );
    }
}