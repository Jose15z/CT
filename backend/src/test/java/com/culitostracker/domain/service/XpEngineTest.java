package com.culitostracker.domain.service;

import com.culitostracker.domain.service.XpEngine.EncounterFact;
import com.culitostracker.domain.service.XpEngine.XpTotals;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class XpEngineTest {

    private final XpEngine engine = new XpEngine();
    private final UUID ana = UUID.randomUUID();
    private final UUID bea = UUID.randomUUID();
    private final LocalDate day = LocalDate.of(2026, 9, 1);

    private EncounterFact plain(UUID partnerId) {
        return new EncounterFact(partnerId, day, null, null, false);
    }

    @Test
    void baseEncounterIsWorthTen() {
        XpTotals totals = engine.compute(List.of(plain(ana)));
        assertThat(totals.totalXp()).isEqualTo(10);
        assertThat(totals.level()).isEqualTo(1);
        assertThat(totals.loyaltyStreak()).isEqualTo(1);
    }

    @Test
    void olderPartnerEarnsMoreUpToTheCap() {
        XpTotals age30 = engine.compute(List.of(new EncounterFact(ana, day, 30, null, false)));
        assertThat(age30.totalXp()).isEqualTo(10 + 12);

        // 70 years old: the bonus caps at +40 instead of +52.
        XpTotals age70 = engine.compute(List.of(new EncounterFact(ana, day, 70, null, false)));
        assertThat(age70.totalXp()).isEqualTo(10 + 40);
    }

    @Test
    void heavierPartnerEarnsMoreUpToTheCap() {
        XpTotals kg80 = engine.compute(List.of(new EncounterFact(ana, day, null, BigDecimal.valueOf(80), false)));
        assertThat(kg80.totalXp()).isEqualTo(10 + 15);

        XpTotals kg200 = engine.compute(List.of(new EncounterFact(ana, day, null, BigDecimal.valueOf(200), false)));
        assertThat(kg200.totalXp()).isEqualTo(10 + 25);

        // Below the threshold there is no bonus and never a penalty.
        XpTotals kg45 = engine.compute(List.of(new EncounterFact(ana, day, null, BigDecimal.valueOf(45), false)));
        assertThat(kg45.totalXp()).isEqualTo(10);
    }

    @Test
    void exclusiveRelationshipMultipliesByOnePointFive() {
        XpTotals totals = engine.compute(List.of(new EncounterFact(ana, day, null, null, true)));
        assertThat(totals.totalXp()).isEqualTo(15);
    }

    @Test
    void loyaltyStreakGrowsAndResetsWhenSwitchingPartners() {
        // Three in a row with Ana: 10, then x1.05, then x1.10.
        XpTotals loyal = engine.compute(List.of(plain(ana), plain(ana), plain(ana)));
        assertThat(loyal.totalXp()).isEqualTo(10 + 11 + 11);
        assertThat(loyal.loyaltyStreak()).isEqualTo(3);
        assertThat(loyal.loyaltyPartnerId()).isEqualTo(ana);

        // Switching to Bea resets the streak: her first is back to base.
        XpTotals switched = engine.compute(List.of(plain(ana), plain(ana), plain(bea)));
        assertThat(switched.totalXp()).isEqualTo(10 + 11 + 10);
        assertThat(switched.loyaltyStreak()).isEqualTo(1);
        assertThat(switched.loyaltyPartnerId()).isEqualTo(bea);
    }

    @Test
    void loyaltyCapsAtFiftyPercent() {
        List<EncounterFact> twelve = java.util.Collections.nCopies(12, plain(ana));
        XpTotals totals = engine.compute(twelve);
        // Streak 11 and 12 both pay 10 x 1.5 = 15: the multiplier stops growing.
        assertThat(engine.xpFor(plain(ana), 11)).isEqualTo(15);
        assertThat(engine.xpFor(plain(ana), 12)).isEqualTo(15);
        assertThat(totals.loyaltyStreak()).isEqualTo(12);
    }

    @Test
    void loyalExclusiveCoupleGetsTheCompoundAdvantage() {
        // Maxed attributes + exclusive + maxed loyalty: 75 x 1.5 x 1.5 = 169.
        EncounterFact best = new EncounterFact(ana, day, 70, BigDecimal.valueOf(200), true);
        assertThat(engine.xpFor(best, 11)).isEqualTo(169);
    }

    @Test
    void aggregatesPerPartner() {
        XpTotals totals = engine.compute(List.of(plain(ana), plain(bea), plain(bea)));
        assertThat(totals.byPartner().get(ana).encounters()).isEqualTo(1);
        assertThat(totals.byPartner().get(bea).encounters()).isEqualTo(2);
        assertThat(totals.byPartner().get(ana).xp()).isEqualTo(10);
        assertThat(totals.byPartner().get(bea).xp()).isEqualTo(10 + 11);
    }

    @Test
    void levelThresholdsAreQuadratic() {
        assertThat(XpEngine.levelFor(0)).isEqualTo(1);
        assertThat(XpEngine.levelFor(99)).isEqualTo(1);
        assertThat(XpEngine.levelFor(100)).isEqualTo(2);
        assertThat(XpEngine.levelFor(299)).isEqualTo(2);
        assertThat(XpEngine.levelFor(300)).isEqualTo(3);

        XpTotals totals = engine.compute(List.of(plain(ana)));
        assertThat(totals.xpIntoLevel()).isEqualTo(10);
        assertThat(totals.xpForNextLevel()).isEqualTo(100);
    }
}
