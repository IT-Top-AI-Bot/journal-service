package com.aquadev.ittopai.service.journal.token;

import com.aquadev.ittopai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class JournalUserIdResolver {

    private static final Duration CACHE_TTL = Duration.ofDays(7);

    private final UserRepository userRepository;
    private final StringRedisTemplate redis;

    public long resolve(long telegramUserId) {
        String key = redisKey(telegramUserId);
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            return Long.parseLong(cached);
        }

        Long journalUserId = userRepository.findJournalUserIdByTelegramId(telegramUserId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        redis.opsForValue().set(key, String.valueOf(journalUserId), CACHE_TTL);
        return journalUserId;
    }

    public void put(long telegramUserId, long journalUserId) {
        redis.opsForValue().set(redisKey(telegramUserId), String.valueOf(journalUserId), CACHE_TTL);
    }

    public void evict(long telegramUserId) {
        redis.delete(redisKey(telegramUserId));
    }

    private String redisKey(long telegramUserId) {
        return JournalRedisKeys.telegramToJournalMap(telegramUserId);
    }
}
