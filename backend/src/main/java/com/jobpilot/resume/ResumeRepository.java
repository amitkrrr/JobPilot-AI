package com.jobpilot.resume;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByCandidateProfileIdOrderByCreatedAtDesc(Long candidateProfileId);

    Optional<Resume> findByIdAndCandidateProfileId(Long id, Long candidateProfileId);
}
