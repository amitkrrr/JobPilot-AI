package com.jobpilot.resume;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

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

    public ResumeService(
            ResumeRepository resumeRepository,
            CandidateProfileRepository profileRepository,
            UserRepository userRepository,
            StorageService storageService) {

        this.resumeRepository = resumeRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
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

    private ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getFileName(),
                resume.getContentType(),
                resume.getCreatedAt()
        );
    }
}
