package com.jobpilot.chat;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobpilot.dto.chat.ChatRequest;
import com.jobpilot.dto.chat.ChatResponse;
import com.jobpilot.dto.job.JobResponse;
import com.jobpilot.dto.profile.ProfileResponse;
import com.jobpilot.search.ChatSearchResult;
import com.jobpilot.search.JobSearchService;
import com.jobpilot.search.NormalizedJob;
import com.jobpilot.user.CandidateProfileService;

/**
 * Chat Orchestrator — Spring Boot is the decision-maker here.
 *
 * Flow:
 *   1. Call Python /api/ai/intent → get structured SearchIntent
 *   2. If JOB_SEARCH → hand off to JobSearchService (connectors + dedup + rank)
 *   3. If GENERAL_CHAT → return Gemini's conversational reply directly
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AIChatClient aiChatClient;
    private final CandidateProfileService profileService;
    private final JobSearchService jobSearchService;

    public ChatController(
            AIChatClient aiChatClient,
            CandidateProfileService profileService,
            JobSearchService jobSearchService) {
        this.aiChatClient = aiChatClient;
        this.profileService = profileService;
        this.jobSearchService = jobSearchService;
    }

    @PostMapping
    public ChatResponse processChatMessage(
            Authentication authentication,
            @RequestBody ChatRequest request) {

        // 1. Load profile context for personalized intent extraction
        ProfileResponse profile = null;
        try {
            profile = profileService.getProfile(authentication.getName());
        } catch (Exception e) {
            System.out.println("No profile context for: " + authentication.getName());
        }

        // 2. Ask Python/Gemini to classify intent — returns structured data, never jobs
        AIChatClient.SearchIntent intent = aiChatClient.classifyIntent(request.getMessage(), profile);

        // 3a. AI service offline — deterministic keyword fallback
        if (intent == null) {
            return new ChatResponse(
                "I'm having trouble reaching my AI brain right now. Please try again in a moment.",
                java.util.Collections.emptyList()
            );
        }

        // 3b. General conversational question — return Gemini's reply
        if (!intent.isJobSearch()) {
            String reply = intent.getReply() != null && !intent.getReply().isBlank()
                    ? intent.getReply()
                    : "Hello! I'm JobPilot. Ask me to find jobs — e.g. 'Find me Java Spring Boot jobs in Bangalore!'";
            return new ChatResponse(reply, java.util.Collections.emptyList());
        }

        // 3c. Job search — Spring Boot orchestrates connectors
        System.out.println("Job search intent: role=" + intent.getRole()
                + " skills=" + intent.getSkills()
                + " location=" + intent.getLocation());

        ChatSearchResult result = jobSearchService.search(intent.toSearchRequest());

        // Convert NormalizedJob → lightweight chat job cards
        List<JobResponse> jobCards = result.getJobs().stream()
                .map(this::toJobCard)
                .collect(Collectors.toList());

        return new ChatResponse(result.getSummary(), jobCards);
    }

    /** Maps a NormalizedJob to a JobResponse usable by the frontend chat card. */
    private JobResponse toJobCard(NormalizedJob job) {
        JobResponse card = new JobResponse();
        card.setTitle(job.getTitle());
        card.setCompany(job.getCompany());
        card.setLocation(job.getLocation());
        card.setDescription(job.getDescription() != null
                ? job.getDescription().length() > 500
                    ? job.getDescription().substring(0, 500) + "..."
                    : job.getDescription()
                : "");
        card.setSourceUrl(job.getJobUrl());
        card.setSalaryInfo(job.getSalaryInfo());
        card.setWorkStyle(job.isRemoteByLocation() ? "REMOTE" : "ONSITE");
        return card;
    }
}
