package com.jobpilot.chat;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobpilot.dto.chat.ChatMessageResponse;
import com.jobpilot.dto.chat.ChatRequest;
import com.jobpilot.dto.chat.ChatResponse;
import com.jobpilot.dto.job.JobResponse;
import com.jobpilot.dto.profile.ProfileResponse;
import com.jobpilot.search.ChatSearchResult;
import com.jobpilot.search.JobSearchService;
import com.jobpilot.search.DeepLinkService;
import com.jobpilot.search.JobSearchRequest;
import com.jobpilot.search.NormalizedJob;
import com.jobpilot.user.CandidateProfileService;
import com.jobpilot.user.User;
import com.jobpilot.user.UserRepository;

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

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final AIChatClient aiChatClient;
    private final CandidateProfileService profileService;
    private final JobSearchService jobSearchService;
    private final DeepLinkService deepLinkService;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatController(
            AIChatClient aiChatClient,
            CandidateProfileService profileService,
            JobSearchService jobSearchService,
            DeepLinkService deepLinkService,
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository) {
        this.aiChatClient = aiChatClient;
        this.profileService = profileService;
        this.jobSearchService = jobSearchService;
        this.deepLinkService = deepLinkService;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/history")
    public List<ChatMessageResponse> getChatHistory(Authentication authentication) {
        if (authentication == null) {
            return Collections.emptyList();
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<ChatMessage> messages = chatMessageRepository.findByUserOrderByCreatedAtDesc(
                user, PageRequest.of(0, 50));
        Collections.reverse(messages);
        return messages.stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ChatResponse processChatMessage(
            Authentication authentication,
            @RequestBody ChatRequest request) {

        User user = null;
        if (authentication != null) {
            user = userRepository.findByEmail(authentication.getName()).orElse(null);
        }

        // 1. Load profile context for personalized intent extraction
        ProfileResponse profile = null;
        try {
            if (authentication != null) {
                profile = profileService.getProfile(authentication.getName());
            }
        } catch (Exception e) {
            log.debug("No profile context for: {}", authentication != null ? authentication.getName() : "unknown");
        }

        // 2. Ask Python/Gemini to classify intent — returns structured data, never jobs
        AIChatClient.SearchIntent intent = aiChatClient.classifyIntent(request.getMessage(), profile, request.getLastIntent());

        ChatResponse response;

        // 3a. AI service offline — deterministic keyword fallback
        if (intent == null) {
            response = new ChatResponse(
                "I'm having trouble reaching my AI brain right now. Please try again in a moment.",
                java.util.Collections.emptyList()
            );
        } else if (!intent.isJobSearch()) {
            // 3b. General conversational question — return Gemini's reply
            String reply = intent.getReply() != null && !intent.getReply().isBlank()
                    ? intent.getReply()
                    : "Hello! I'm JobPilot. Ask me to find jobs — e.g. 'Find me Java Spring Boot jobs in Bangalore!'";
            response = new ChatResponse(reply, java.util.Collections.emptyList());
        } else {
            // 3c. Job search — Spring Boot orchestrates connectors
            log.info("Job search intent: role={} skills={} location={}",
                    intent.getRole(), intent.getSkills(), intent.getLocation());

            JobSearchRequest searchRequest = intent.toSearchRequest();

            // F#4: Profile-aware search defaults
            if (profile != null) {
                if ((searchRequest.getRole() == null || searchRequest.getRole().isBlank())
                        && profile.getCurrentRole() != null && !profile.getCurrentRole().isBlank()) {
                    searchRequest.setRole(profile.getCurrentRole().trim());
                }
                if ((searchRequest.getSkills() == null || searchRequest.getSkills().isEmpty())
                        && profile.getSkills() != null && !profile.getSkills().isBlank()) {
                    List<String> profileSkills = java.util.Arrays.stream(profile.getSkills().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                    if (!profileSkills.isEmpty()) {
                        searchRequest.setSkills(profileSkills);
                    }
                }
                if ((searchRequest.getLocation() == null || searchRequest.getLocation().isBlank())
                        && profile.getLocation() != null && !profile.getLocation().isBlank()) {
                    searchRequest.setLocation(profile.getLocation().trim());
                }
            }

            searchRequest.setPage(request.getPage());
            ChatSearchResult result = jobSearchService.search(searchRequest);

            // Convert NormalizedJob → lightweight chat job cards
            List<JobResponse> jobCards = result.getJobs().stream()
                    .map(this::toJobCard)
                    .collect(Collectors.toList());

            response = new ChatResponse(result.getSummary(), jobCards);
            response.setHasMore(jobCards.size() == 20);
            response.setLastIntent(intent);
            response.setSearchLinks(deepLinkService.buildLinks(searchRequest));
        }

        // Save chat history
        if (user != null) {
            try {
                chatMessageRepository.save(new ChatMessage(user, "USER", request.getMessage()));
                chatMessageRepository.save(new ChatMessage(user, "AI", response.getResponse()));
            } catch (Exception e) {
                log.warn("Failed to persist chat message: {}", e.getMessage());
            }
        }

        return response;
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
        card.setWorkStyle(inferWorkStyle(job));
        return card;
    }

    /**
     * Infers REMOTE / HYBRID / ONSITE from the location string.
     * Adzuna and Jooble both surface hybrid listings explicitly.
     */
    private String inferWorkStyle(NormalizedJob job) {
        String loc = job.getLocation() != null ? job.getLocation().toLowerCase() : "";
        String desc = job.getDescription() != null ? job.getDescription().toLowerCase() : "";
        if (loc.contains("remote") || desc.contains("fully remote") || desc.contains("work from home")) {
            return "REMOTE";
        }
        if (loc.contains("hybrid") || desc.contains("hybrid")) {
            return "HYBRID";
        }
        return "ONSITE";
    }
}
