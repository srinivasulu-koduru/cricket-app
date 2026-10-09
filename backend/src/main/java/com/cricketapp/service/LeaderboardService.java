package com.cricketapp.service;

import com.cricketapp.dto.LeaderboardDto;
import com.cricketapp.dto.LeaderboardDto.PlayerRankingDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class LeaderboardService {

    private final JdbcTemplate jdbcTemplate;

    public LeaderboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public LeaderboardDto getLeaderboard(String category, String timeframe) {
        if (!StringUtils.hasText(category)) {
            category = "runs";
        }
        category = category.toLowerCase();

        List<PlayerRankingDto> rankings = switch (category) {
            case "sixes" -> getMostSixes();
            case "fours" -> getMostFours();
            case "wickets" -> getMostWickets();
            case "economy" -> getBestEconomy();
            case "strike_rate" -> getBestBattingStrikeRate();
            case "runs" -> getMostRuns();
            case "maidens" -> getMostMaidens();
            default -> getMostRuns();
        };

        // Assign ranks (handling ties properly)
        double lastValue = -1;
        int currentRank = 1;
        int trueRank = 1;

        for (PlayerRankingDto r : rankings) {
            if (lastValue == -1) {
                r.setRank(currentRank);
            } else if (r.getValue() == lastValue) {
                r.setRank(currentRank); // tie
            } else {
                currentRank = trueRank;
                r.setRank(currentRank);
            }
            lastValue = r.getValue();
            trueRank++;
        }

        return new LeaderboardDto(category, "All Time", rankings);
    }

    private List<PlayerRankingDto> getMostRuns() {
        String sql = """
            SELECT 
                u.user_id, u.name, p.profile_photo_url,
                SUM(b.runs_scored) as total_runs,
                COUNT(DISTINCT b.match_id) as matches,
                SUM(CASE WHEN b.is_boundary = true AND b.runs_scored = 6 THEN 1 ELSE 0 END) as sixes,
                SUM(CASE WHEN b.is_boundary = true AND b.runs_scored = 4 THEN 1 ELSE 0 END) as fours,
                SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'NO_BALL') THEN 1 ELSE 0 END) as balls_faced,
                (SELECT t.name FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE tm.user_id = u.id AND tm.status = 'ACTIVE' LIMIT 1) as team_name
            FROM ball_events b
            JOIN matches m ON b.match_id = m.id
            JOIN users u ON b.striker_user_id = u.id
            LEFT JOIN player_profiles p ON u.id = p.user_id
            WHERE m.status = 'COMPLETED'
            GROUP BY u.id, u.user_id, u.name, p.profile_photo_url
            ORDER BY total_runs DESC, balls_faced ASC
            LIMIT 50
        """;

        return jdbcTemplate.query(sql, this::mapBattingRow);
    }

    private List<PlayerRankingDto> getMostSixes() {
        String sql = """
            SELECT 
                u.user_id, u.name, p.profile_photo_url,
                SUM(b.runs_scored) as total_runs,
                COUNT(DISTINCT b.match_id) as matches,
                SUM(CASE WHEN b.is_boundary = true AND b.runs_scored = 6 THEN 1 ELSE 0 END) as sixes,
                SUM(CASE WHEN b.is_boundary = true AND b.runs_scored = 4 THEN 1 ELSE 0 END) as fours,
                SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'NO_BALL') THEN 1 ELSE 0 END) as balls_faced,
                (SELECT t.name FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE tm.user_id = u.id AND tm.status = 'ACTIVE' LIMIT 1) as team_name
            FROM ball_events b
            JOIN matches m ON b.match_id = m.id
            JOIN users u ON b.striker_user_id = u.id
            LEFT JOIN player_profiles p ON u.id = p.user_id
            WHERE m.status = 'COMPLETED'
            GROUP BY u.id, u.user_id, u.name, p.profile_photo_url
            HAVING sixes > 0
            ORDER BY sixes DESC, total_runs DESC
            LIMIT 50
        """;

        return jdbcTemplate.query(sql, this::mapBattingRow);
    }

    private List<PlayerRankingDto> getMostFours() {
        String sql = """
            SELECT 
                u.user_id, u.name, p.profile_photo_url,
                SUM(b.runs_scored) as total_runs,
                COUNT(DISTINCT b.match_id) as matches,
                SUM(CASE WHEN b.is_boundary = true AND b.runs_scored = 6 THEN 1 ELSE 0 END) as sixes,
                SUM(CASE WHEN b.is_boundary = true AND b.runs_scored = 4 THEN 1 ELSE 0 END) as fours,
                SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'NO_BALL') THEN 1 ELSE 0 END) as balls_faced,
                (SELECT t.name FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE tm.user_id = u.id AND tm.status = 'ACTIVE' LIMIT 1) as team_name
            FROM ball_events b
            JOIN matches m ON b.match_id = m.id
            JOIN users u ON b.striker_user_id = u.id
            LEFT JOIN player_profiles p ON u.id = p.user_id
            WHERE m.status = 'COMPLETED'
            GROUP BY u.id, u.user_id, u.name, p.profile_photo_url
            HAVING fours > 0
            ORDER BY fours DESC, total_runs DESC
            LIMIT 50
        """;

        return jdbcTemplate.query(sql, this::mapBattingRow);
    }

    private List<PlayerRankingDto> getBestBattingStrikeRate() {
        String sql = """
            SELECT 
                u.user_id, u.name, p.profile_photo_url,
                SUM(b.runs_scored) as total_runs,
                COUNT(DISTINCT b.match_id) as matches,
                SUM(CASE WHEN b.is_boundary = true AND b.runs_scored = 6 THEN 1 ELSE 0 END) as sixes,
                SUM(CASE WHEN b.is_boundary = true AND b.runs_scored = 4 THEN 1 ELSE 0 END) as fours,
                SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'NO_BALL') THEN 1 ELSE 0 END) as balls_faced,
                (SELECT t.name FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE tm.user_id = u.id AND tm.status = 'ACTIVE' LIMIT 1) as team_name
            FROM ball_events b
            JOIN matches m ON b.match_id = m.id
            JOIN users u ON b.striker_user_id = u.id
            LEFT JOIN player_profiles p ON u.id = p.user_id
            WHERE m.status = 'COMPLETED'
            GROUP BY u.id, u.user_id, u.name, p.profile_photo_url
            HAVING balls_faced >= 20
            ORDER BY (SUM(b.runs_scored) * 100.0 / balls_faced) DESC, total_runs DESC
            LIMIT 50
        """;

        return jdbcTemplate.query(sql, this::mapBattingRow);
    }

    private List<PlayerRankingDto> getMostWickets() {
        String sql = """
            SELECT 
                u.user_id, u.name, p.profile_photo_url,
                COUNT(DISTINCT b.match_id) as matches,
                SUM(CASE WHEN b.is_wicket = true AND b.wicket_type IN ('BOWLED', 'CAUGHT', 'LBW', 'STUMPED', 'HIT_WICKET', 'CAUGHT_AND_BOWLED') THEN 1 ELSE 0 END) as wickets,
                SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'BYE', 'LEG_BYE') THEN b.runs_scored ELSE b.runs_scored + b.extra_runs END) as runs_conceded,
                SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'BYE', 'LEG_BYE', 'WICKET') THEN 1 ELSE 0 END) as legal_balls,
                (SELECT t.name FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE tm.user_id = u.id AND tm.status = 'ACTIVE' LIMIT 1) as team_name
            FROM ball_events b
            JOIN matches m ON b.match_id = m.id
            JOIN users u ON b.bowler_user_id = u.id
            LEFT JOIN player_profiles p ON u.id = p.user_id
            WHERE m.status = 'COMPLETED'
            GROUP BY u.id, u.user_id, u.name, p.profile_photo_url
            HAVING wickets > 0
            ORDER BY wickets DESC, (SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'BYE', 'LEG_BYE') THEN b.runs_scored ELSE b.runs_scored + b.extra_runs END) * 6.0 / NULLIF(SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'BYE', 'LEG_BYE', 'WICKET') THEN 1 ELSE 0 END), 0)) ASC
            LIMIT 50
        """;

        return jdbcTemplate.query(sql, this::mapBowlingRow);
    }

    private List<PlayerRankingDto> getBestEconomy() {
        String sql = """
            SELECT 
                u.user_id, u.name, p.profile_photo_url,
                COUNT(DISTINCT b.match_id) as matches,
                SUM(CASE WHEN b.is_wicket = true AND b.wicket_type IN ('BOWLED', 'CAUGHT', 'LBW', 'STUMPED', 'HIT_WICKET', 'CAUGHT_AND_BOWLED') THEN 1 ELSE 0 END) as wickets,
                SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'BYE', 'LEG_BYE') THEN b.runs_scored ELSE b.runs_scored + b.extra_runs END) as runs_conceded,
                SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'BYE', 'LEG_BYE', 'WICKET') THEN 1 ELSE 0 END) as legal_balls,
                (SELECT t.name FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE tm.user_id = u.id AND tm.status = 'ACTIVE' LIMIT 1) as team_name
            FROM ball_events b
            JOIN matches m ON b.match_id = m.id
            JOIN users u ON b.bowler_user_id = u.id
            LEFT JOIN player_profiles p ON u.id = p.user_id
            WHERE m.status = 'COMPLETED'
            GROUP BY u.id, u.user_id, u.name, p.profile_photo_url
            HAVING legal_balls >= 60
            ORDER BY (SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'BYE', 'LEG_BYE') THEN b.runs_scored ELSE b.runs_scored + b.extra_runs END) * 6.0 / NULLIF(SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'BYE', 'LEG_BYE', 'WICKET') THEN 1 ELSE 0 END), 0)) ASC, wickets DESC
            LIMIT 50
        """;

        return jdbcTemplate.query(sql, this::mapBowlingRow);
    }

    private List<PlayerRankingDto> getMostMaidens() {
        String sql = """
            SELECT 
                u.user_id, u.name, p.profile_photo_url,
                COUNT(DISTINCT m.id) as matches,
                SUM(CASE WHEN b.is_wicket = true AND b.wicket_type IN ('BOWLED', 'CAUGHT', 'LBW', 'STUMPED', 'HIT_WICKET', 'CAUGHT_AND_BOWLED') THEN 1 ELSE 0 END) as wickets,
                SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'BYE', 'LEG_BYE', 'WICKET') THEN 1 ELSE 0 END) as legal_balls,
                SUM(CASE WHEN b.extra_type IS NULL OR b.extra_type IN ('NONE', 'BYE', 'LEG_BYE') THEN b.runs_scored ELSE b.runs_scored + b.extra_runs END) as runs_conceded,
                COALESCE(mo.maidens, 0) as maidens,
                (SELECT t.name FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE tm.user_id = u.id AND tm.status = 'ACTIVE' LIMIT 1) as team_name
            FROM ball_events b
            JOIN matches m ON b.match_id = m.id
            JOIN users u ON b.bowler_user_id = u.id
            LEFT JOIN player_profiles p ON u.id = p.user_id
            LEFT JOIN (
                SELECT bowler_user_id, COUNT(*) as maidens FROM (
                    SELECT be.bowler_user_id, be.match_id, be.innings_id, be.over_number,
                        SUM(CASE WHEN be.extra_type IS NULL OR be.extra_type IN ('NONE', 'BYE', 'LEG_BYE') THEN be.runs_scored ELSE be.runs_scored + be.extra_runs END) as over_runs,
                        SUM(CASE WHEN be.extra_type IS NULL OR be.extra_type IN ('NONE', 'BYE', 'LEG_BYE', 'WICKET') THEN 1 ELSE 0 END) as over_legal_balls
                    FROM ball_events be
                    JOIN matches me ON be.match_id = me.id
                    WHERE me.status = 'COMPLETED'
                    GROUP BY be.bowler_user_id, be.match_id, be.innings_id, be.over_number
                ) AS over_stats
                WHERE over_runs = 0 AND over_legal_balls = 6
                GROUP BY bowler_user_id
            ) AS mo ON u.id = mo.bowler_user_id
            WHERE m.status = 'COMPLETED'
            GROUP BY u.id, u.user_id, u.name, p.profile_photo_url, mo.maidens
            HAVING COALESCE(mo.maidens, 0) > 0
            ORDER BY maidens DESC, wickets DESC
            LIMIT 50
        """;

        return jdbcTemplate.query(sql, this::mapBowlingRow);
    }

    private PlayerRankingDto mapBattingRow(ResultSet rs, int rowNum) throws SQLException {
        PlayerRankingDto dto = new PlayerRankingDto();
        dto.setUserId(rs.getString("user_id"));
        dto.setName(rs.getString("name"));
        dto.setProfilePhotoUrl(rs.getString("profile_photo_url"));
        dto.setTeamName(rs.getString("team_name"));
        
        int runs = rs.getInt("total_runs");
        int balls = rs.getInt("balls_faced");
        
        dto.setRuns(runs);
        dto.setMatches(rs.getInt("matches"));
        dto.setSixes(rs.getInt("sixes"));
        dto.setFours(rs.getInt("fours"));
        
        double sr = balls > 0 ? (runs * 100.0) / balls : 0;
        dto.setStrikeRate(Math.round(sr * 100.0) / 100.0);
        
        // Dynamic value for sorting in UI (since we order by this in SQL anyway, we assign the primary metric)
        try {
            double value = rs.getDouble(rs.getMetaData().getColumnName(4)); // 4th column is always the primary metric we sorted by in the specific query
            dto.setValue(value);
        } catch(Exception e) {
            dto.setValue(runs);
        }

        return dto;
    }

    private PlayerRankingDto mapBowlingRow(ResultSet rs, int rowNum) throws SQLException {
        PlayerRankingDto dto = new PlayerRankingDto();
        dto.setUserId(rs.getString("user_id"));
        dto.setName(rs.getString("name"));
        dto.setProfilePhotoUrl(rs.getString("profile_photo_url"));
        dto.setTeamName(rs.getString("team_name"));
        
        int wickets = rs.getInt("wickets");
        int runsConceded = rs.getInt("runs_conceded");
        int legalBalls = rs.getInt("legal_balls");
        
        dto.setWickets(wickets);
        dto.setMatches(rs.getInt("matches"));
        
        double economy = legalBalls > 0 ? (runsConceded * 6.0) / legalBalls : 0;
        dto.setEconomy(Math.round(economy * 100.0) / 100.0);
        
        try {
            if(rs.getMetaData().getColumnName(5).equalsIgnoreCase("wickets")) {
                dto.setValue(wickets);
            } else if(rs.getMetaData().getColumnName(5).equalsIgnoreCase("maidens") || 
                      rs.getMetaData().getColumnName(8).equalsIgnoreCase("maidens")) { // fallback check if maidens is selected
                
                try {
                    int maidens = rs.getInt("maidens");
                    dto.setMaidens(maidens);
                    dto.setValue(maidens);
                } catch (Exception ex) {
                    dto.setValue(wickets);
                }
            } else {
                dto.setValue(dto.getEconomy());
            }
        } catch(Exception e) {
            dto.setValue(wickets);
        }
        
        return dto;
    }
}
