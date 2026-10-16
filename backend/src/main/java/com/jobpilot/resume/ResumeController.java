package com.jobpilot.resume;

import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jobpilot.dto.resume.ResumeOptimizeResponse;
import com.jobpilot.dto.resume.ResumeResponse;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping
    public ResumeResponse uploadResume(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        return resumeService.uploadResume(
                authentication.getName(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes()
        );
    }

    @GetMapping
    public List<ResumeResponse> getResumes(Authentication authentication) {
        return resumeService.getResumes(authentication.getName());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadResume(
            Authentication authentication,
            @PathVariable Long id) {

        byte[] data = resumeService.downloadResume(authentication.getName(), id);
        Resume resume = resumeService.getResumeEntity(authentication.getName(), id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resume.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resume.getFileName() + "\"")
                .body(data);
    }

    @PostMapping("/{id}/parse")
    public com.jobpilot.dto.profile.ProfileResponse parseResume(
            Authentication authentication,
            @PathVariable Long id) {
        return resumeService.parseAndPopulateProfile(authentication.getName(), id);
    }

    @PostMapping("/{id}/optimize")
    public ResumeOptimizeResponse optimizeResume(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(required = false) String targetRole,
            @RequestParam(required = false) Long targetJobId) {
        return resumeService.optimizeResume(authentication.getName(), id, targetRole, targetJobId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(
            Authentication authentication,
            @PathVariable Long id) {
        resumeService.deleteResume(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
