package com.aicyber.backend.reward.repository;

import com.aicyber.backend.reward.dto.RewardEntryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class RewardQueryRepositoryIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RewardQueryRepository repository;

    @Test
    void returnsOnlyRewardEntriesOwnedByTheRequestedUser() {
        UUID firstUserId = createUser("First Customer");
        UUID secondUserId = createUser("Second Customer");
        UUID programId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO reward_programs (id, code, name) VALUES (?, ?, 'Privacy Test Program')",
                programId,
                "PRIVACY-" + programId
        );

        String firstOrderReference = "SO-FIRST-" + shortId();
        String secondOrderReference = "SO-SECOND-" + shortId();
        UUID firstOrderId = createOrder(firstUserId, firstOrderReference);
        UUID secondOrderId = createOrder(secondUserId, secondOrderReference);
        createQueueEntry(programId, firstOrderId, 1);
        createQueueEntry(programId, secondOrderId, 2);

        List<RewardEntryResponse> firstUserEntries = repository.findEntries(programId, firstUserId);
        List<RewardEntryResponse> secondUserEntries = repository.findEntries(programId, secondUserId);

        assertEquals(1, firstUserEntries.size());
        assertEquals(firstOrderReference, firstUserEntries.get(0).orderReference());
        assertEquals(1, secondUserEntries.size());
        assertEquals(secondOrderReference, secondUserEntries.get(0).orderReference());
    }

    private UUID createUser(String displayName) {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, 'test-password-hash', ?)",
                userId,
                userId + "@example.com",
                displayName
        );
        return userId;
    }

    private UUID createOrder(UUID userId, String reference) {
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO sales_orders " +
                        "(id, user_id, order_reference, amount_cents, status, paid_at, reward_eligible_at) " +
                        "VALUES (?, ?, ?, 200000, 'REWARD_ELIGIBLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                orderId,
                userId,
                reference
        );
        return orderId;
    }

    private void createQueueEntry(UUID programId, UUID orderId, long sequence) {
        jdbcTemplate.update(
                "INSERT INTO reward_queue_entries " +
                        "(id, program_id, order_id, queue_sequence, target_amount_cents) VALUES (?, ?, ?, ?, 200000)",
                UUID.randomUUID(),
                programId,
                orderId,
                sequence
        );
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
