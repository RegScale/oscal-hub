package gov.nist.oscal.tools.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine for high-performance in-memory caching.
 *
 * This configuration provides multiple cache instances with different TTLs
 * optimized for different types of data:
 *
 * - analytics: For dashboard/analytics data (5 minute TTL)
 * - auditStats: For audit log statistics (2 minute TTL)
 * - library: For library item data (5 minute TTL)
 * - health: For health check data (30 second TTL)
 * - compliance: For static compliance data like SOC 2 controls (1 hour TTL)
 * - tags: For tag lists (10 minute TTL)
 *
 * Cache eviction occurs automatically based on TTL or when max size is reached.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Default cache manager with moderate TTL (5 minutes)
     * Used for general-purpose caching
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()); // Enable statistics for monitoring
        return cacheManager;
    }

    /**
     * Short-lived cache for rapidly changing data (30 seconds)
     * Used for health checks and real-time status
     */
    @Bean
    public CacheManager shortLivedCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "health", "systemStatus", "componentHealth");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .recordStats());
        return cacheManager;
    }

    /**
     * Medium-lived cache for semi-static data (2-5 minutes)
     * Used for analytics, statistics, and aggregations
     */
    @Bean
    public CacheManager analyticsCache() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "analytics", "auditStats", "libraryAnalytics", "artifactAnalytics");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }

    /**
     * Long-lived cache for static/rarely changing data (1 hour)
     * Used for SOC 2 controls, compliance data, templates
     */
    @Bean
    public CacheManager longLivedCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "compliance", "soc2Controls", "templates", "oscalTypes");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats());
        return cacheManager;
    }

    /**
     * Tag cache with moderate TTL (10 minutes)
     * Used for tag lists which change infrequently
     */
    @Bean
    public CacheManager tagCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "tags", "popularTags", "libraryTags", "artifactTags");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(300)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }
}
