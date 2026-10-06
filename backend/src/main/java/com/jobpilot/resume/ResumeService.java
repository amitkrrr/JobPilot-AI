package com.jobpilot.resume;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobpilot.dto.resume.ResumeResponse;
import com.jobpilot.storage.StorageService;
import com.jobpilot.user.CandidateProfile;
import com.jobpilot.user.CandidateProfileRepository;
import com.jobpilot.user.User;
import com.jobpilot.user.UserRepository;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final CandidateProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final AIParsingClient aiParsingClient;

    public ResumeService(
            ResumeRepository resumeRepository,
            CandidateProfileRepository profileRepository,
            UserRepository userRepository,
            StorageService storageService,
            AIParsingClient aiParsingClient) {

        this.resumeRepository = resumeRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.aiParsingClient = aiParsingClient;
    }

    public ResumeResponse uploadResume(String email, String fileName, String contentType, byte[] bytes) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CandidateProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found. Please create your profile first."));

        // Generate clean unique S3 key: resumes/{profileId}/{UUID}-{filename}
        String cleanFileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
        String s3Key = "resumes/" + profile.getId() + "/" + UUID.randomUUID() + "-" + cleanFileName;

        // Upload to S3/MinIO
        storageService.uploadFile(s3Key, bytes, contentType);

        // Save metadata in PostgreSQL
        Resume resume = new Resume(profile, fileName, contentType, s3Key);
        Resume savedResume = resumeRepository.save(resume);

        return toResponse(savedResume);
    }

    public List<ResumeResponse> getResumes(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CandidateProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found."));

        return resumeRepository.findByCandidateProfileIdOrderByCreatedAtDesc(profile.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public byte[] downloadResume(String email, Long resumeId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CandidateProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found."));

        // Retrieve and enforce profile-level authorization: ensure this resume belongs to this candidate!
        Resume resume = resumeRepository.findByIdAndCandidateProfileId(resumeId, profile.getId())
                .orElseThrow(() -> new IllegalArgumentException("Resume not found or access denied."));

        return storageService.downloadFile(resume.getS3Key());
    }

    public Resume getResumeEntity(String email, Long resumeId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CandidateProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found."));

        return resumeRepository.findByIdAndCandidateProfileId(resumeId, profile.getId())
                .orElseThrow(() -> new IllegalArgumentException("Resume not found or access denied."));
    }

    public com.jobpilot.dto.profile.ProfileResponse parseAndPopulateProfile(String email, Long resumeId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CandidateProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found."));

        Resume resume = resumeRepository.findByIdAndCandidateProfileId(resumeId, profile.getId())
                .orElseThrow(() -> new IllegalArgumentException("Resume not found or access denied."));

        // Trigger Python AI Service
        com.jobpilot.dto.resume.ResumeParsedResponse parsed = aiParsingClient.parseResume(resume.getS3Key());

        // Auto-populate Candidate Profile with AI-extracted results!
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

        CandidateProfile savedProfile = profileRepository.save(profile);

        // Return updated profile details
        return new com.jobpilot.dto.profile.ProfileResponse(
                savedProfile.getId(),
                user.getName(),
                user.getEmail(),
                savedProfile.getPhone(),
                savedProfile.getLocation(),
                savedProfile.getHeadline(),
                savedProfile.getYearsOfExperience(),
                savedProfile.getCurrentCompany(),
                savedProfile.getCurrentRole(),
                savedProfile.getLinkedinUrl(),
                savedProfile.getGithubUrl(),
                savedProfile.getPortfolioUrl(),
                savedProfile.getSkills()
        );
    }

    @Transactional
    public void deleteResume(String email, Long resumeId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CandidateProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found."));

        Resume resume = resumeRepository.findByIdAndCandidateProfileId(resumeId, profile.getId())
                .orElseThrow(() -> new IllegalArgumentException("Resume not found or access denied."));

        // Delete from MinIO first, then remove DB record
        storageService.deleteFile(resume.getS3Key());
        resumeRepository.delete(resume);
    }

    private ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getFileName(),
                resume.getContentType(),
                resume.getCreatedAt()
        );
    }
}
