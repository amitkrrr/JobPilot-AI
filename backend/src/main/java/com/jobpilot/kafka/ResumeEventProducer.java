package com.jobpilot.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.jobpilot.config.KafkaConfig;

/**
 * Publishes resume-related events to the {@code resume.parse.requested} Kafka topic.
 *
 * Message format (plain string): the S3 key of the uploaded resume PDF.
 * The consumer reads this key and calls the Python AI parsing service.
 *
 * Only activated when {@code kafka.enabled=true} (i.e. a Kafka broker is reachable).
 * When disabled the bean is not created and no events are fired — the synchronous
 * /parse endpoint remains fully functional.
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class ResumeEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ResumeEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ResumeEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish an async parse request for the given resume.
     *
     * @param resumeId  database ID of the Resume entity
     * @param s3Key     MinIO / S3 object key so the consumer can fetch the file
     */
    public void publishParseRequest(Long resumeId, String s3Key) {
        String payload = resumeId + ":" + s3Key;
        kafkaTemplate.send(KafkaConfig.TOPIC_RESUME_PARSE, String.valueOf(resumeId), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish resume parse event for resumeId={}: {}", resumeId, ex.getMessage());
                    } else {
                        log.info("Resume parse event published: topic={} resumeId={} offset={}",
                                KafkaConfig.TOPIC_RESUME_PARSE,
                                resumeId,
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
