package com.mennangok1.reserved.security;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

// In-memory cache only (Spring Boot's default ConcurrentMapCacheManager) — no Redis in MVP, per CLAUDE.md decision #2.
@Configuration
@EnableCaching
public class CacheConfig {
}
