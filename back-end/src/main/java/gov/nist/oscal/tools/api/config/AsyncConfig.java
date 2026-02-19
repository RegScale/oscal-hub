package gov.nist.oscal.tools.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for handling long-running operations.
 *
 * This configuration provides thread pools for:
 * - OSCAL processing (validation, conversion, profile resolution)
 * - Export operations (CSV, JSON exports)
 * - Batch operations
 *
 * Using async processing prevents blocking the main request thread
 * and improves overall application responsiveness.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool for OSCAL processing operations.
     * These are CPU-intensive operations like validation and conversion.
     */
    @Bean(name = "oscalTaskExecutor")
    public Executor oscalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("oscal-async-");
        executor.setRejectedExecutionHandler((r, exec) -> {
            throw new RuntimeException("OSCAL processing queue is full. Please try again later.");
        });
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool for export operations.
     * These are I/O-bound operations that can take time for large datasets.
     */
    @Bean(name = "exportTaskExecutor")
    public Executor exportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("export-async-");
        executor.setRejectedExecutionHandler((r, exec) -> {
            throw new RuntimeException("Export queue is full. Please try again later.");
        });
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool for batch operations.
     * Handles multi-file validation and conversion requests.
     */
    @Bean(name = "batchTaskExecutor")
    public Executor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("batch-async-");
        executor.setRejectedExecutionHandler((r, exec) -> {
            throw new RuntimeException("Batch processing queue is full. Please try again later.");
        });
        executor.initialize();
        return executor;
    }

    /**
     * General purpose async executor.
     * Used for miscellaneous async tasks like notifications.
     */
    @Bean(name = "generalTaskExecutor")
    public Executor generalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("general-async-");
        executor.initialize();
        return executor;
    }
}
