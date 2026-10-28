package com.aicyber.backend.reward.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class RewardQueryRepositoryIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired RewardQueryRepository repository;

    @Test
    void returnsOnlyCommitmentsOwnedByTheRequestedUser() {
        UUID programId = createProgram();
        UUID firstUser = createUser();
        UUID secondUser = createUser();
        createDiscountedOrder(programId, firstUser, 1);
        createDiscountedOrder(programId, secondUser, 2);

        assertEquals(1, repository.findEntries(programId, firstUser).size());
        assertEquals(1, repository.findEntries(programId, secondUser).size());
        assertEquals(1, repository.findEntries(programId, firstUser).get(0).founderNumber());
        assertEquals(2, repository.findEntries(programId, secondUser).get(0).founderNumber());
    }

    private UUID createProgram() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO reward_programs (id, code, name) VALUES (?, ?, 'Privacy Test')", id, "PRIVACY-" + id);
        for (Object[] tier : new Object[][] {{"L", "Launch", 1, 10, 1500, 50000}, {"E", "Early", 11, 25, 1200, 50000}, {"F", "Founder", 26, 50, 1000, 50000}}) {
            jdbcTemplate.update("INSERT INTO reward_founder_tiers (id, program_id, tier_code, display_name, position_start, position_end, rate_basis_points, cap_cents) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), id, tier[0], tier[1], tier[2], tier[3], tier[4], tier[5]);
        }
        return id;
    }

    private UUID createUser() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, 'hash', 'Customer')", id, id + "@example.com");
        return id;
    }

    private void createDiscountedOrder(UUID programId, UUID userId, int position) {
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO sales_orders (id, user_id, order_reference, amount_cents, product_subtotal_cents, discount_cents, status, paid_at) VALUES (?, ?, ?, 190000, 200000, 10000, 'PAID', CURRENT_TIMESTAMP)", orderId, userId, "SO-" + orderId);
        UUID tierId = jdbcTemplate.queryForObject(
                "SELECT id FROM reward_founder_tiers WHERE program_id = ? AND ? BETWEEN position_start AND position_end",
                UUID.class, programId, position);
        jdbcTemplate.update("""
                INSERT INTO reward_commitments
                    (id, program_id, tier_id, order_id, user_id, founder_sequence, reward_month,
                     qualifying_at, purchase_amount_cents, eligible_spend_cents, rate_basis_points,
                     cap_cents, cashback_amount_cents, status, paid_at)
                VALUES (?, ?, ?, ?, ?, ?, date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE 'Australia/Melbourne')::date,
                        CURRENT_TIMESTAMP, 200000, 200000, 500, 999999999999, 10000, 'PAID', CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), programId, tierId, orderId, userId, position);
    }
}
