package com.jobpilot.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Kafka configuration — only activated when {@code kafka.enabled=true}.
 *
 * When kafka.enabled=false (the default for local dev without Docker):
 *  - No KafkaTemplate bean is created
 *  - Producers and consumer beans are skipped via @ConditionalOnProperty
 *  - The app starts and operates fully without a running Kafka broker
 *
 * To enable: add {@code kafka.enabled=true} to application-local.properties
 * and run {@code docker compose up -d} to start the broker.
 */
@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class KafkaConfig {

    /** Fired after a resume is uploaded — triggers async AI parsing. */
    public static final String TOPIC_RESUME_PARSE = "resume.parse.requested";

    /** Fired when a saved job's application status changes. */
    public static final String TOPIC_APPLICATION_STATUS = "application.status.changed";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic resumeParseTopic() {
        return TopicBuilder.name(TOPIC_RESUME_PARSE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic applicationStatusTopic() {
        return TopicBuilder.name(TOPIC_APPLICATION_STATUS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
