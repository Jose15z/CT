package com.culitostracker.repository;

import com.culitostracker.domain.model.LeaderboardProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LeaderboardProfileRepository extends JpaRepository<LeaderboardProfile, UUID> {

    /**
     * Ranking rows. Only opted-in profiles with a public alias appear, and the
     * only figures exposed are the alias, optionally the avatar, and the score:
     * distinct normalized partner names, soft-deleted included, so deleting and
     * re-creating the same partner never inflates the count.
     *
     * The window filter uses each name's FIRST registration timestamp.
     */
    @Query(value = """
            SELECT CAST(lp.user_id AS varchar)   AS userId,
                   lp.public_alias               AS alias,
                   lp.show_avatar                AS showAvatar,
                   u.avatar_emoji                AS avatarEmoji,
                   COALESCE(s.score, 0)          AS score
            FROM leaderboard_profiles lp
            JOIN users u ON u.id = lp.user_id
            LEFT JOIN (
                SELECT f.owner_user_id, COUNT(*) AS score
                FROM (
                    SELECT owner_user_id, normalized_name, MIN(created_at) AS first_created
                    FROM partners
                    GROUP BY owner_user_id, normalized_name
                ) f
                WHERE (CAST(:fromTs AS timestamptz) IS NULL OR f.first_created >= :fromTs)
                GROUP BY f.owner_user_id
            ) s ON s.owner_user_id = lp.user_id
            WHERE lp.enabled = TRUE
              AND lp.public_alias IS NOT NULL
              AND btrim(lp.public_alias) <> ''
            ORDER BY score DESC, lp.public_alias ASC
            LIMIT 100
            """, nativeQuery = true)
    List<LeaderboardRow> ranking(@Param("fromTs") Instant fromTs);

    /** The authenticated user's own score for a window, opted in or not. */
    @Query(value = """
            SELECT COUNT(*)
            FROM (
                SELECT owner_user_id, normalized_name, MIN(created_at) AS first_created
                FROM partners
                WHERE owner_user_id = :userId
                GROUP BY owner_user_id, normalized_name
            ) f
            WHERE (CAST(:fromTs AS timestamptz) IS NULL OR f.first_created >= :fromTs)
            """, nativeQuery = true)
    long scoreForUser(@Param("userId") UUID userId, @Param("fromTs") Instant fromTs);

    interface LeaderboardRow {
        String getUserId();
        String getAlias();
        Boolean getShowAvatar();
        String getAvatarEmoji();
        Long getScore();
    }
}
