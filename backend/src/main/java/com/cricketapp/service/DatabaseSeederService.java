package com.cricketapp.service;

import com.cricketapp.entity.*;
import com.cricketapp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class DatabaseSeederService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeederService.class);

    private final UserRepository userRepository;
    private final PlayerProfileRepository profileRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final MatchRepository matchRepository;
    private final MatchInvitationRepository matchInvitationRepository;
    private final MatchPlayingXiRepository matchPlayingXiRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeederService(
            UserRepository userRepository,
            PlayerProfileRepository profileRepository,
            TeamRepository teamRepository,
            TeamMemberRepository memberRepository,
            TeamInvitationRepository teamInvitationRepository,
            MatchRepository matchRepository,
            MatchInvitationRepository matchInvitationRepository,
            MatchPlayingXiRepository matchPlayingXiRepository,
            OtpVerificationRepository otpVerificationRepository,
            PendingRegistrationRepository pendingRegistrationRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.teamInvitationRepository = teamInvitationRepository;
        this.matchRepository = matchRepository;
        this.matchInvitationRepository = matchInvitationRepository;
        this.matchPlayingXiRepository = matchPlayingXiRepository;
        this.otpVerificationRepository = otpVerificationRepository;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Map<String, Object> resetAndSeedDatabase() {
        logger.info("Wiping existing database records...");

        // 1. Delete in reverse dependency order
        matchPlayingXiRepository.deleteAllInBatch();
        matchInvitationRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        teamInvitationRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
        profileRepository.deleteAllInBatch();
        otpVerificationRepository.deleteAllInBatch();
        pendingRegistrationRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        logger.info("Database wiped cleanly. Seeding 28 player profiles...");

        String defaultPasswordHash = passwordEncoder.encode("Password123!");

        // 2. Create 28 Player Accounts & Profiles
        List<PlayerData> playersData = Arrays.asList(
            new PlayerData("K Srinivasulu", "srinivasulu@cricket.com", "CRK100001", "+91 9876543201", PlayingRole.ALL_ROUNDER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_MEDIUM, "Anantapur, AP", "Passionate all-rounder and captain of Lions XI."),
            new PlayerData("Sunil Mylu", "sunil@cricket.com", "CRK100002", "+91 9876543202", PlayingRole.ALL_ROUNDER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_FAST, "Hyderabad, TS", "Aggressive all-rounder and captain of Kings XI."),
            new PlayerData("Rohit Sharma", "rohit@cricket.com", "CRK100003", "+91 9876543203", PlayingRole.BATTER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_OFF_BREAK, "Mumbai, MH", "Top-order opening batter known for clean hitting."),
            new PlayerData("Virat Kohli", "virat@cricket.com", "CRK100004", "+91 9876543204", PlayingRole.BATTER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_MEDIUM, "Delhi, DL", "Master anchor batter and run-chase specialist."),
            new PlayerData("K L Rahul", "klrahul@cricket.com", "CRK100005", "+91 9876543205", PlayingRole.WICKET_KEEPER, BattingStyle.RIGHT_HANDED, BowlingStyle.NONE, "Bengaluru, KA", "Versatile wicket-keeper batter with solid technique."),
            new PlayerData("Hardik Pandya", "hardik@cricket.com", "CRK100006", "+91 9876543206", PlayingRole.ALL_ROUNDER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_MEDIUM_FAST, "Baroda, GJ", "Explosive finisher and seam-bowling all-rounder."),
            new PlayerData("Jasprit Bumrah", "bumrah@cricket.com", "CRK100007", "+91 9876543207", PlayingRole.BOWLER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_FAST, "Ahmedabad, GJ", "World-class yorker specialist and pace bowler."),
            new PlayerData("Ravindra Jadeja", "jadeja@cricket.com", "CRK100008", "+91 9876543208", PlayingRole.ALL_ROUNDER, BattingStyle.LEFT_HANDED, BowlingStyle.LEFT_ARM_ORTHODOX, "Jamnagar, GJ", "Dynamic gun fielder, left-arm spinner and batter."),
            new PlayerData("Rishabh Pant", "pant@cricket.com", "CRK100009", "+91 9876543209", PlayingRole.WICKET_KEEPER, BattingStyle.LEFT_HANDED, BowlingStyle.NONE, "Roorkee, UK", "Dynamic left-handed wicket-keeper batter."),
            new PlayerData("Shubman Gill", "gill@cricket.com", "CRK100010", "+91 9876543210", PlayingRole.BATTER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_OFF_BREAK, "Fazilka, PB", "Elegant top-order batter with stroke play."),
            new PlayerData("Mohammed Shami", "shami@cricket.com", "CRK100011", "+91 9876543211", PlayingRole.BOWLER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_FAST, "Amroha, UP", "Seam bowler with lethal upright seam presentation."),
            new PlayerData("Kuldeep Yadav", "kuldeep@cricket.com", "CRK100012", "+91 9876543212", PlayingRole.BOWLER, BattingStyle.LEFT_HANDED, BowlingStyle.LEFT_ARM_CHINAMAN, "Kanpur, UP", "Left-arm wrist spinner with deceptive variations."),
            new PlayerData("Yashasvi Jaiswal", "jaiswal@cricket.com", "CRK100013", "+91 9876543213", PlayingRole.BATTER, BattingStyle.LEFT_HANDED, BowlingStyle.RIGHT_ARM_LEG_BREAK, "Bhadohi, UP", "Attacking young left-handed opening batter."),
            new PlayerData("Shreyas Iyer", "shreyas@cricket.com", "CRK100014", "+91 9876543214", PlayingRole.BATTER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_LEG_BREAK, "Mumbai, MH", "Middle-order batter and spin conqueror."),
            new PlayerData("Suryakumar Yadav", "surya@cricket.com", "CRK100015", "+91 9876543215", PlayingRole.BATTER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_MEDIUM, "Mumbai, MH", "360-degree T20 specialist batter."),
            new PlayerData("Sanju Samson", "samson@cricket.com", "CRK100016", "+91 9876543216", PlayingRole.WICKET_KEEPER, BattingStyle.RIGHT_HANDED, BowlingStyle.NONE, "Trivandrum, KL", "Classy wicketkeeper-batter with high strike rate."),
            new PlayerData("Axar Patel", "axar@cricket.com", "CRK100017", "+91 9876543217", PlayingRole.ALL_ROUNDER, BattingStyle.LEFT_HANDED, BowlingStyle.LEFT_ARM_ORTHODOX, "Nadiad, GJ", "Accurate left-arm spinner and handy lower-order batter."),
            new PlayerData("Mohammed Siraj", "siraj@cricket.com", "CRK100018", "+91 9876543218", PlayingRole.BOWLER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_FAST, "Hyderabad, TS", "Aggressive pace bowler with outswing."),
            new PlayerData("Arshdeep Singh", "arshdeep@cricket.com", "CRK100019", "+91 9876543219", PlayingRole.BOWLER, BattingStyle.LEFT_HANDED, BowlingStyle.LEFT_ARM_MEDIUM_FAST, "Kharar, PB", "Left-arm death bowler with precise yorkers."),
            new PlayerData("Yuzvendra Chahal", "chahal@cricket.com", "CRK100020", "+91 9876543220", PlayingRole.BOWLER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_LEG_BREAK, "Jind, HR", "Crafty leg-spinner and wicket-taking bowler."),
            new PlayerData("Rinku Singh", "rinku@cricket.com", "CRK100021", "+91 9876543221", PlayingRole.BATTER, BattingStyle.LEFT_HANDED, BowlingStyle.RIGHT_ARM_OFF_BREAK, "Aligarh, UP", "Clutch middle-order finisher batter."),
            new PlayerData("Shivam Dube", "dube@cricket.com", "CRK100022", "+91 9876543222", PlayingRole.ALL_ROUNDER, BattingStyle.LEFT_HANDED, BowlingStyle.RIGHT_ARM_MEDIUM, "Mumbai, MH", "Big-hitting middle-order batter against spin."),
            new PlayerData("Ishant Sharma", "ishant@cricket.com", "CRK100023", "+91 9876543223", PlayingRole.BOWLER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_MEDIUM_FAST, "Delhi, DL", "Experienced tall pace bowler."),
            new PlayerData("Prithvi Shaw", "shaw@cricket.com", "CRK100024", "+91 9876543224", PlayingRole.BATTER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_OFF_BREAK, "Thane, MH", "Aggressive powerplay opening batter."),
            new PlayerData("Devdutt Padikkal", "padikkal@cricket.com", "CRK100025", "+91 9876543225", PlayingRole.BATTER, BattingStyle.LEFT_HANDED, BowlingStyle.RIGHT_ARM_OFF_BREAK, "Edapal, KL", "Stylish left-handed top-order batter."),
            new PlayerData("Ruturaj Gaikwad", "ruturaj@cricket.com", "CRK100026", "+91 9876543226", PlayingRole.BATTER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_OFF_BREAK, "Pune, MH", "Technically sound top-order batter."),
            new PlayerData("Washington Sundar", "washington@cricket.com", "CRK100027", "+91 9876543227", PlayingRole.ALL_ROUNDER, BattingStyle.LEFT_HANDED, BowlingStyle.RIGHT_ARM_OFF_BREAK, "Chennai, TN", "Off-spin bowling all-rounder with powerplay accuracy."),
            new PlayerData("Umran Malik", "umran@cricket.com", "CRK100028", "+91 9876543228", PlayingRole.BOWLER, BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_FAST, "Jammu, JK", "Raw speed bowler capable of 150+ km/h pace.")
        );

        Map<String, User> createdUsers = new HashMap<>();

        for (PlayerData d : playersData) {
            User user = new User(d.userId, d.name, d.email, defaultPasswordHash, true);
            user = userRepository.save(user);
            createdUsers.put(d.userId, user);

            PlayerProfile profile = new PlayerProfile(user);
            profile.setDisplayName(d.name);
            profile.setPlayingRole(d.playingRole);
            profile.setBattingStyle(d.battingStyle);
            profile.setBowlingStyle(d.bowlingStyle);
            profile.setLocation(d.location);
            profile.setBio(d.bio);
            profileRepository.save(profile);
        }

        // 3. Create Teams
        User creatorA = createdUsers.get("CRK100001"); // K Srinivasulu
        User creatorB = createdUsers.get("CRK100002"); // Sunil Mylu
        User creatorC = createdUsers.get("CRK100026"); // Ruturaj Gaikwad

        Team teamA = new Team("TEAM100001", "Lions XI", "Premier T20 squad led by K Srinivasulu.", creatorA, "JOIN_LIONS_1001");
        teamA = teamRepository.save(teamA);

        Team teamB = new Team("TEAM100002", "Kings XI", "Champion squad led by Sunil Mylu.", creatorB, "JOIN_KINGS_1002");
        teamB = teamRepository.save(teamB);

        Team teamC = new Team("TEAM100003", "Warriors XI", "Challengers squad led by Ruturaj Gaikwad.", creatorC, "JOIN_WARRIORS_1003");
        teamC = teamRepository.save(teamC);

        // 4. Assign Team Members
        // Team A (Lions XI) Members (12 members)
        List<String> teamAUserIds = Arrays.asList(
            "CRK100001", "CRK100003", "CRK100004", "CRK100005", "CRK100006",
            "CRK100007", "CRK100008", "CRK100009", "CRK100010", "CRK100011",
            "CRK100012", "CRK100013"
        );
        for (String uId : teamAUserIds) {
            User u = createdUsers.get(uId);
            TeamRole role = uId.equals("CRK100001") ? TeamRole.OWNER : TeamRole.PLAYER;
            TeamMember tm = new TeamMember(teamA, u, role);
            memberRepository.save(tm);
        }

        // Team B (Kings XI) Members (11 members)
        List<String> teamBUserIds = Arrays.asList(
            "CRK100002", "CRK100014", "CRK100015", "CRK100016", "CRK100017",
            "CRK100018", "CRK100019", "CRK100020", "CRK100021", "CRK100022",
            "CRK100023"
        );
        for (String uId : teamBUserIds) {
            User u = createdUsers.get(uId);
            TeamRole role = uId.equals("CRK100002") ? TeamRole.OWNER : TeamRole.PLAYER;
            TeamMember tm = new TeamMember(teamB, u, role);
            memberRepository.save(tm);
        }

        // Team C (Warriors XI) Members (4 members)
        List<String> teamCUserIds = Arrays.asList("CRK100024", "CRK100025", "CRK100026", "CRK100027");
        for (String uId : teamCUserIds) {
            User u = createdUsers.get(uId);
            TeamRole role = uId.equals("CRK100026") ? TeamRole.OWNER : TeamRole.PLAYER;
            TeamMember tm = new TeamMember(teamC, u, role);
            memberRepository.save(tm);
        }

        // 5. Create Sample Match Fixtures
        // Match 1: Lions vs Kings (SCHEDULED)
        Match match1 = new Match(
            "MATCH914781",
            "Lions vs Kings Premier Clash",
            teamA,
            teamB,
            creatorA,
            LocalDate.now().plusDays(2),
            LocalTime.of(9, 30),
            "SRIT Cricket Stadium, Anantapur",
            MatchFormat.T20,
            20,
            "High-stakes inaugural fixture between Lions XI and Kings XI."
        );
        match1.setStatus(MatchStatus.SCHEDULED);
        match1.setScorer(createdUsers.get("CRK100005")); // KL Rahul as assigned scorer
        match1 = matchRepository.save(match1);

        // Match Invitation for Match 1 (ACCEPTED)
        MatchInvitation inv1 = new MatchInvitation(match1, teamA, teamB, creatorB, creatorA);
        inv1.setStatus(MatchInvitationStatus.ACCEPTED);
        matchInvitationRepository.save(inv1);

        // Seed Initial Playing XI for Lions XI (Team A)
        List<String> xiUserIdsA = Arrays.asList(
            "CRK100001", "CRK100003", "CRK100004", "CRK100005", "CRK100006",
            "CRK100007", "CRK100008", "CRK100009", "CRK100010", "CRK100011", "CRK100012"
        );
        for (String uId : xiUserIdsA) {
            User u = createdUsers.get(uId);
            boolean isCap = uId.equals("CRK100001");
            boolean isWk = uId.equals("CRK100005");
            MatchPlayingXi xi = new MatchPlayingXi(match1, teamA, u, isCap, isWk);
            matchPlayingXiRepository.save(xi);
        }

        // Seed Initial Playing XI for Kings XI (Team B)
        List<String> xiUserIdsB = Arrays.asList(
            "CRK100002", "CRK100014", "CRK100015", "CRK100016", "CRK100017",
            "CRK100018", "CRK100019", "CRK100020", "CRK100021", "CRK100022", "CRK100023"
        );
        for (String uId : xiUserIdsB) {
            User u = createdUsers.get(uId);
            boolean isCap = uId.equals("CRK100002");
            boolean isWk = uId.equals("CRK100016");
            MatchPlayingXi xi = new MatchPlayingXi(match1, teamB, u, isCap, isWk);
            matchPlayingXiRepository.save(xi);
        }

        // Match 2: Lions vs Warriors (PENDING_CONFIRMATION)
        Match match2 = new Match(
            "MATCH914782",
            "Lions vs Warriors Challenge",
            teamA,
            teamC,
            creatorA,
            LocalDate.now().plusDays(5),
            LocalTime.of(14, 0),
            "Chinnaswamy Stadium, Bengaluru",
            MatchFormat.T20,
            20,
            "T20 Challenge Cup Match."
        );
        match2.setStatus(MatchStatus.PENDING_CONFIRMATION);
        match2 = matchRepository.save(match2);

        MatchInvitation inv2 = new MatchInvitation(match2, teamA, teamC, creatorC, creatorA);
        inv2.setStatus(MatchInvitationStatus.PENDING);
        matchInvitationRepository.save(inv2);

        logger.info("Database reset and seeded successfully with 28 players, 3 teams, 2 matches and playing XIs!");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Database wiped and re-seeded with 28 realistic cricket player profiles!");
        response.put("totalPlayers", 28);
        response.put("totalTeams", 3);
        response.put("totalMatches", 2);
        response.put("defaultPassword", "Password123!");

        return response;
    }

    private static class PlayerData {
        String name;
        String email;
        String userId;
        String mobile;
        PlayingRole playingRole;
        BattingStyle battingStyle;
        BowlingStyle bowlingStyle;
        String location;
        String bio;

        PlayerData(String name, String email, String userId, String mobile, PlayingRole playingRole, BattingStyle battingStyle, BowlingStyle bowlingStyle, String location, String bio) {
            this.name = name;
            this.email = email;
            this.userId = userId;
            this.mobile = mobile;
            this.playingRole = playingRole;
            this.battingStyle = battingStyle;
            this.bowlingStyle = bowlingStyle;
            this.location = location;
            this.bio = bio;
        }
    }
}
