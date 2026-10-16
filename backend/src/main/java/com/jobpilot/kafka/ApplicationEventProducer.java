package com.jobpilot.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.jobpilot.config.KafkaConfig;
import com.jobpilot.job.JobStatus;

/**
 * Publishes application status-change events to the
 * {@code application.status.changed} Kafka topic.
 *
 * Message format (plain string): {@code "<userId>:<jobId>:<newStatus>"}
 *
 * Only activated when {@code kafka.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class ApplicationEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ApplicationEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ApplicationEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * @param userId    the user who owns the application
     * @param jobId     the job whose status changed
     * @param newStatus the new {@link JobStatus}
     */
    public void publishStatusChange(Long userId, Long jobId, JobStatus newStatus) {
        String key = String.valueOf(jobId);
        String payload = userId + ":" + jobId + ":" + newStatus.name();
        kafkaTemplate.send(KafkaConfig.TOPIC_APPLICATION_STATUS, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish application status event jobId={} status={}: {}",
                                jobId, newStatus, ex.getMessage());
                    } else {
                        log.info("Application status event published: jobId={} status={} offset={}",
                                jobId, newStatus, result.getRecordMetadata().offset());
                    }
                });
    }
}
