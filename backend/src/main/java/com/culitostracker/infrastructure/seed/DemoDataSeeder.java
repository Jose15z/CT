package com.culitostracker.infrastructure.seed;

import com.culitostracker.domain.model.AccessScope;
import com.culitostracker.domain.model.AccessStatus;
import com.culitostracker.domain.model.CycleProfile;
import com.culitostracker.domain.model.LeaderboardProfile;
import com.culitostracker.domain.model.MilestoneType;
import com.culitostracker.domain.model.Mood;
import com.culitostracker.domain.model.ObservationType;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.domain.model.PartnerAccess;
import com.culitostracker.domain.model.PartnerObservation;
import com.culitostracker.domain.model.PeriodRecord;
import com.culitostracker.domain.model.Relationship;
import com.culitostracker.domain.model.RelationshipCheckIn;
import com.culitostracker.domain.model.RelationshipMilestone;
import com.culitostracker.domain.model.RelationshipSituation;
import com.culitostracker.domain.model.RelationshipStatus;
import com.culitostracker.domain.model.RelationshipType;
import com.culitostracker.domain.model.User;
import com.culitostracker.repository.CycleProfileRepository;
import com.culitostracker.repository.LeaderboardProfileRepository;
import com.culitostracker.repository.PartnerAccessRepository;
import com.culitostracker.repository.PartnerObservationRepository;
import com.culitostracker.repository.PartnerRepository;
import com.culitostracker.repository.PeriodRecordRepository;
import com.culitostracker.repository.RelationshipCheckInRepository;
import com.culitostracker.repository.RelationshipMilestoneRepository;
import com.culitostracker.repository.RelationshipRepository;
import com.culitostracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Development-only seed. Runs with the "demo" Spring profile and never in
 * production. Login: demo@culitostracker.local / demo1234 (also
 * laura@culitostracker.local / demo1234 to try the linked-account flow).
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_EMAIL = "demo@culitostracker.local";
    private static final String DEMO_PASSWORD = "demo1234";

    private final UserRepository users;
    private final PartnerRepository partners;
    private final RelationshipRepository relationships;
    private final RelationshipMilestoneRepository milestones;
    private final CycleProfileRepository cycleProfiles;
    private final PeriodRecordRepository periods;
    private final RelationshipCheckInRepository checkIns;
    private final PartnerObservationRepository observations;
    private final PartnerAccessRepository accesses;
    private final LeaderboardProfileRepository leaderboards;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository users, PartnerRepository partners,
                          RelationshipRepository relationships,
                          RelationshipMilestoneRepository milestones,
                          CycleProfileRepository cycleProfiles, PeriodRecordRepository periods,
                          RelationshipCheckInRepository checkIns,
                          PartnerObservationRepository observations,
                          PartnerAccessRepository accesses,
                          LeaderboardProfileRepository leaderboards,
                          PasswordEncoder passwordEncoder) {
        this.users = users;
        this.partners = partners;
        this.relationships = relationships;
        this.milestones = milestones;
        this.cycleProfiles = cycleProfiles;
        this.periods = periods;
        this.checkIns = checkIns;
        this.observations = observations;
        this.accesses = accesses;
        this.leaderboards = leaderboards;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.findByEmail(DEMO_EMAIL).isPresent()) {
            log.info("Demo data already present, skipping seed");
            return;
        }
        log.info("Seeding demo data (profile 'demo')");
        LocalDate today = LocalDate.now();

        User demo = user("demo", DEMO_EMAIL, "Jose", "🍑", RelationshipSituation.IN_RELATIONSHIP, "es");
        User lauraUser = user("laura", "laura@culitostracker.local", "Laura Martínez", "🌸",
                RelationshipSituation.IN_RELATIONSHIP, "es");

        // --- Laura: serious relationship, linked account, full history ---
        Partner laura = partner(demo, "Laura Martínez", "Laura", "🌸", lauraUser.getId());
        Relationship lauraRel = relationship(laura, RelationshipType.SERIOUS_RELATIONSHIP,
                LocalDate.of(2024, 1, 20), LocalDate.of(2024, 2, 14), null, null);

        milestone(laura, MilestoneType.FIRST_DATE, "Primera cita", "Café y una caminata larga",
                LocalDate.of(2024, 1, 20));
        milestone(laura, MilestoneType.STARTED_DATING, "Empezamos a salir", null,
                LocalDate.of(2024, 2, 14));
        milestone(laura, MilestoneType.TRIP, "Viaje a Cartagena", "Primer viaje juntos",
                LocalDate.of(2024, 7, 10));
        milestone(laura, MilestoneType.MOVED_IN_TOGETHER, "Nos mudamos juntos", null,
                LocalDate.of(2025, 3, 1));

        CycleProfile lauraCycle = cycleProfile(laura, 29, 5);
        // Six period records with slightly irregular cycles, most recent ~12 days ago.
        int[] gaps = {29, 28, 30, 27, 29};
        LocalDate start = today.minusDays(12);
        period(lauraCycle, start, start.plusDays(4));
        for (int gap : gaps) {
            start = start.minusDays(gap);
            period(lauraCycle, start, start.plusDays(4));
        }
        lauraCycle.setLastPeriodStartDate(today.minusDays(12));
        cycleProfiles.save(lauraCycle);

        // Check-ins from both sides + mutual consent grants.
        checkIn(lauraRel, demo, Mood.TIRED, 2, 3, 4, 4, null, today);
        checkIn(lauraRel, lauraUser, Mood.HAPPY, 4, 2, 4, 5, null, today);
        checkIn(lauraRel, demo, Mood.HAPPY, 4, 2, 4, 4, null, today.minusDays(1));
        checkIn(lauraRel, lauraUser, Mood.STRESSED, 3, 4, 3, 4, null, today.minusDays(1));
        grant(laura, lauraUser, demo, AccessScope.CHECK_INS);
        grant(laura, demo, lauraUser, AccessScope.CHECK_INS);
        grant(laura, demo, lauraUser, AccessScope.CYCLE);

        observation(demo, laura, ObservationType.STRESSED, "Semana pesada en el trabajo", today);

        // --- Daniela: casual, manual record ---
        Partner daniela = partner(demo, "Daniela Gómez", null, "☀️", null);
        relationship(daniela, RelationshipType.CASUAL, LocalDate.of(2025, 6, 15), null, null, null);
        CycleProfile danielaCycle = cycleProfile(daniela, 28, 4);
        period(danielaCycle, today.minusDays(20), today.minusDays(17));
        period(danielaCycle, today.minusDays(48), today.minusDays(45));
        danielaCycle.setLastPeriodStartDate(today.minusDays(20));
        cycleProfiles.save(danielaCycle);

        // --- Leaderboard: demo user + filler singles ---
        leaderboard(demo, "Jose", true);
        seedFillerUser("donjuan93", "DonJuan93", 14);
        seedFillerUser("elpatron", "ElPatron", 11);
        seedFillerUser("anon7", "Anonymous", 7);

        log.info("Demo data ready: {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
    }

    private User user(String username, String email, String displayName, String avatar,
                      RelationshipSituation situation, String lang) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        u.setDisplayName(displayName);
        u.setAvatarEmoji(avatar);
        u.setRelationshipSituation(situation);
        u.setPreferredLanguage(lang);
        return users.save(u);
    }

    private Partner partner(User owner, String name, String nickname, String avatar, UUID linkedUserId) {
        Partner p = new Partner();
        p.setOwnerUserId(owner.getId());
        p.rename(name);
        p.setNickname(nickname);
        p.setAvatarEmoji(avatar);
        p.setLinkedUserId(linkedUserId);
        return partners.save(p);
    }

    private Relationship relationship(Partner partner, RelationshipType type, LocalDate dating,
                                      LocalDate startDate, LocalDate engagement, LocalDate marriage) {
        Relationship r = new Relationship();
        r.setPartnerId(partner.getId());
        r.setType(type);
        r.setStatus(RelationshipStatus.ACTIVE);
        r.setDatingStartDate(dating);
        r.setRelationshipStartDate(startDate);
        r.setEngagementDate(engagement);
        r.setMarriageDate(marriage);
        return relationships.save(r);
    }

    private void milestone(Partner partner, MilestoneType type, String title, String description,
                           LocalDate date) {
        RelationshipMilestone m = new RelationshipMilestone();
        m.setPartnerId(partner.getId());
        m.setType(type);
        m.setTitle(title);
        m.setDescription(description);
        m.setDate(date);
        milestones.save(m);
    }

    private CycleProfile cycleProfile(Partner partner, int cycleLength, int periodLength) {
        CycleProfile c = new CycleProfile();
        c.setPartnerId(partner.getId());
        c.setAverageCycleLength(cycleLength);
        c.setAveragePeriodLength(periodLength);
        return cycleProfiles.save(c);
    }

    private void period(CycleProfile profile, LocalDate startDate, LocalDate endDate) {
        PeriodRecord p = new PeriodRecord();
        p.setCycleProfileId(profile.getId());
        p.setStartDate(startDate);
        p.setEndDate(endDate);
        periods.save(p);
    }

    private void checkIn(Relationship relationship, User author, Mood mood, int energy, int stress,
                         Integer affection, Integer satisfaction, String note, LocalDate date) {
        RelationshipCheckIn c = new RelationshipCheckIn();
        c.setRelationshipId(relationship.getId());
        c.setAuthorUserId(author.getId());
        c.setMood(mood);
        c.setEnergyLevel(energy);
        c.setStressLevel(stress);
        c.setAffectionLevel(affection);
        c.setRelationshipSatisfaction(satisfaction);
        c.setNote(note);
        c.setCheckInDate(date);
        checkIns.save(c);
    }

    private void grant(Partner partner, User by, User to, AccessScope scope) {
        PartnerAccess a = new PartnerAccess();
        a.setPartnerId(partner.getId());
        a.setGrantedByUserId(by.getId());
        a.setGrantedToUserId(to.getId());
        a.setScope(scope);
        a.setStatus(AccessStatus.ACTIVE);
        accesses.save(a);
    }

    private void observation(User observer, Partner partner, ObservationType type, String note,
                             LocalDate date) {
        PartnerObservation o = new PartnerObservation();
        o.setObserverUserId(observer.getId());
        o.setPartnerId(partner.getId());
        o.setObservationType(type);
        o.setNote(note);
        o.setObservationDate(date);
        observations.save(o);
    }

    private void leaderboard(User user, String alias, boolean showAvatar) {
        LeaderboardProfile p = new LeaderboardProfile();
        p.setUserId(user.getId());
        p.setEnabled(true);
        p.setPublicAlias(alias);
        p.setShowAvatar(showAvatar);
        leaderboards.save(p);
    }

    /** A single user with N casual partner records, only to populate the ranking. */
    private void seedFillerUser(String username, String alias, int partnerCount) {
        User filler = user(username, username + "@culitostracker.local", alias, null,
                RelationshipSituation.SINGLE, "es");
        List<String> names = List.of("Alex", "Sam", "Andrea", "Vale", "Cami", "Dani", "Luci",
                "Mari", "Pau", "Sara", "Tati", "Vero", "Xime", "Yola");
        for (int i = 0; i < partnerCount && i < names.size(); i++) {
            Partner p = partner(filler, names.get(i) + " " + (i + 1), null, null, null);
            relationship(p, RelationshipType.CASUAL, null, null, null, null);
        }
        leaderboard(filler, alias, false);
    }
}
