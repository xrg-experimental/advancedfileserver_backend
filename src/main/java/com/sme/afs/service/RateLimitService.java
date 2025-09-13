package com.sme.afs.service;

import com.sme.afs.config.BlobUrlProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();
    private final BlobUrlProperties blobUrlProperties;

    public boolean isAllowed(String key) {
        var rl = blobUrlProperties.getRateLimit();
        if (rl == null || !rl.isEnabled()) {
            return true;
        }
        Bucket bucket = cache.computeIfAbsent(key, this::createNewBucket);
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}", key);
        }

        return allowed;
    }

    private Bucket createNewBucket(String key) {
        Bandwidth limit;
        var rl = blobUrlProperties.getRateLimit();

        if (key.startsWith("download:ip:")) {
            limit = Bandwidth.classic(
                    rl.getDownloadPerIp().getMaxRequests(),
                    Refill.intervally(
                            rl.getDownloadPerIp().getMaxRequests(),
                            Duration.ofMinutes(rl.getDownloadPerIp().getWindowMinutes())
                    )
            );
        } else if (key.startsWith("download:user:")) {
            limit = Bandwidth.classic(
                    rl.getDownloadPerUser().getMaxRequests(),
                    Refill.intervally(
                            rl.getDownloadPerUser().getMaxRequests(),
                            Duration.ofMinutes(rl.getDownloadPerUser().getWindowMinutes())
                    )
            );
        } else if (key.startsWith("token:validation:")) {
            limit = Bandwidth.classic(
                    rl.getTokenValidation().getMaxRequests(),
                    Refill.intervally(
                            rl.getTokenValidation().getMaxRequests(),
                            Duration.ofSeconds(rl.getTokenValidation().getWindowSeconds())
                    )
            );
        } else {
            // Default fallback: conservative 20 req/min
            limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)));
        }

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public void clearExpiredEntries() {
        // Clean up old entries periodically
        if (cache.size() > 10000) {
            cache.clear();
            log.info("Cleared rate limit cache");
        }
    }
}