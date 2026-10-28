package com.aicyber.backend.reward.repository;

import com.aicyber.backend.reward.service.RewardQueryService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public class FounderDiscountProgramRepository {
    private final JdbcTemplate jdbc;

    public FounderDiscountProgramRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public UUID ensureProgram() {
        UUID proposedId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO reward_programs (id, code, name, currency, status)
                VALUES (?, ?, 'JON.PC Founder 5% Discount', 'AUD', 'ACTIVE')
                ON CONFLICT (code) DO NOTHING
                """, proposedId, RewardQueryService.PROGRAM_CODE);
        UUID programId = jdbc.queryForObject(
                "SELECT id FROM reward_programs WHERE code = ?", UUID.class, RewardQueryService.PROGRAM_CODE);
        jdbc.update("""
                UPDATE reward_programs
                SET name = 'JON.PC Founder 5% Discount', max_positions = 50,
                    max_liability_cents = 2500000, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, programId);
        insertTier(programId, "LAUNCH", 1, 10);
        insertTier(programId, "EARLY", 11, 25);
        insertTier(programId, "FOUNDER", 26, 50);
        return programId;
    }

    private void insertTier(UUID programId, String code, int start, int end) {
        jdbc.update("""
                INSERT INTO reward_founder_tiers
                    (id, program_id, tier_code, display_name, position_start, position_end,
                     rate_basis_points, cap_cents)
                VALUES (?, ?, ?, 'Founder 5% Discount', ?, ?, 500, 999999999999)
                ON CONFLICT (program_id, tier_code) DO UPDATE SET
                    display_name = EXCLUDED.display_name,
                    position_start = EXCLUDED.position_start,
                    position_end = EXCLUDED.position_end,
                    rate_basis_points = EXCLUDED.rate_basis_points,
                    cap_cents = EXCLUDED.cap_cents
                """, UUID.randomUUID(), programId, code, start, end);
    }
}
