package com.jobpilot.job;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRequirementRepository extends JpaRepository<JobRequirement, Long> {

    Optional<JobRequirement> findByJob(Job job);
}
