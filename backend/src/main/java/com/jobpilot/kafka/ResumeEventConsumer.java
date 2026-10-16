package com.jobpilot.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.jobpilot.config.KafkaConfig;
import com.jobpilot.resume.AIParsingClient;
import com.jobpilot.resume.Resume;
import com.jobpilot.resume.ResumeRepository;
import com.jobpilot.user.CandidateProfile;
import com.jobpilot.user.CandidateProfileRepository;

/**
 * Consumes {@code resume.parse.requested} events and asynchronously calls the
 * Python AI service to parse the resume and populate the candidate profile.
 *
 * Message payload format: {@code "<resumeId>:<s3Key>"}
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class ResumeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ResumeEventConsumer.class);

    private final ResumeRepository resumeRepository;
    private final CandidateProfileRepository profileRepository;
    private final AIParsingClient aiParsingClient;

    public ResumeEventConsumer(
            ResumeRepository resumeRepository,
            CandidateProfileRepository profileRepository,
            AIParsingClient aiParsingClient) {
        this.resumeRepository = resumeRepository;
        this.profileRepository = profileRepository;
        this.aiParsingClient = aiParsingClient;
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_RESUME_PARSE,
            groupId = "${spring.kafka.consumer.group-id:jobpilot-resume-group}")
    public void onResumeParseRequested(String payload) {
        log.info("Received resume parse event: {}", payload);
        try {
            int sep = payload.indexOf(':');
            if (sep < 0) {
                log.warn("Malformed resume parse payload (missing ':'): {}", payload);
                return;
            }
            long resumeId = Long.parseLong(payload.substring(0, sep));
            String s3Key = payload.substring(sep + 1);

            Resume resume = resumeRepository.findById(resumeId).orElse(null);
            if (resume == null) {
                log.warn("Resume not found for id={}, skipping parse", resumeId);
                return;
            }

            com.jobpilot.dto.resume.ResumeParsedResponse parsed = aiParsingClient.parseResume(s3Key);

            CandidateProfile profile = resume.getCandidateProfile();
            if (profile == null) {
                log.warn("No candidate profile linked to resumeId={}", resumeId);
                return;
            }

            // Populate profile fields only if not already set
            if (parsed.getPhone() != null && profile.getPhone() == null)           profile.setPhone(parsed.getPhone());
            if (parsed.getLocation() != null && profile.getLocation() == null)     profile.setLocation(parsed.getLocation());
            if (parsed.getHeadline() != null && profile.getHeadline() == null)     profile.setHeadline(parsed.getHeadline());
            if (parsed.getYearsOfExperience() != null && profile.getYearsOfExperience() == null)
                profile.setYearsOfExperience(parsed.getYearsOfExperience());
            if (parsed.getCurrentCompany() != null && profile.getCurrentCompany() == null)
                profile.setCurrentCompany(parsed.getCurrentCompany());
            if (parsed.getCurrentRole() != null && profile.getCurrentRole() == null)
                profile.setCurrentRole(parsed.getCurrentRole());
            if (parsed.getLinkedinUrl() != null && profile.getLinkedinUrl() == null)
                profile.setLinkedinUrl(parsed.getLinkedinUrl());
            if (parsed.getGithubUrl() != null && profile.getGithubUrl() == null)
                profile.setGithubUrl(parsed.getGithubUrl());
            if (parsed.getPortfolioUrl() != null && profile.getPortfolioUrl() == null)
                profile.setPortfolioUrl(parsed.getPortfolioUrl());
            if (parsed.getSkills() != null && !parsed.getSkills().isEmpty() && (profile.getSkills() == null || profile.getSkills().isBlank()))
                profile.setSkills(String.join(", ", parsed.getSkills()));

            profileRepository.save(profile);
            log.info("Async resume parse complete for resumeId={}", resumeId);
        } catch (Exception e) {
            log.error("Error processing resume parse event [payload={}]: {}", payload, e.getMessage(), e);
        }
    }
}
