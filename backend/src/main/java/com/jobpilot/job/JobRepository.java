package com.jobpilot.job;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobpilot.user.User;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    // Retrieve all jobs belonging to a specific user to enforce security / resource isolation
    List<Job> findByUserOrderByCreatedAtDesc(User user);

    // Retrieve a specific job by id and user to prevent cross-user data tampering
    Optional<Job> findByIdAndUser(Long id, User user);
}
