package com.culitostracker.domain.service;

import com.culitostracker.domain.model.AdviceCategory;
import com.culitostracker.domain.model.AdviceSource;
import com.culitostracker.domain.model.CyclePhase;
import com.culitostracker.domain.model.RelationshipStage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.culitostracker.domain.model.CyclePhase.LUTEAL;
import static com.culitostracker.domain.model.CyclePhase.MENSTRUATION;
import static com.culitostracker.domain.model.CyclePhase.OVULATION;
import static com.culitostracker.domain.model.RelationshipStage.DEVELOPING;
import static com.culitostracker.domain.model.RelationshipStage.ESTABLISHED;
import static com.culitostracker.domain.model.RelationshipStage.LONG_TERM;
import static com.culitostracker.domain.model.RelationshipStage.NEW;
import static com.culitostracker.domain.model.RelationshipStage.VERY_LONG_TERM;
import static com.culitostracker.domain.service.TipCatalog.Topic.APPRECIATION;
import static com.culitostracker.domain.service.TipCatalog.Topic.COMMUNICATION;
import static com.culitostracker.domain.service.TipCatalog.Topic.CONFLICT;
import static com.culitostracker.domain.service.TipCatalog.Topic.CURIOSITY;
import static com.culitostracker.domain.service.TipCatalog.Topic.INTIMACY;
import static com.culitostracker.domain.service.TipCatalog.Topic.PLANS;
import static com.culitostracker.domain.service.TipCatalog.Topic.SPACE;
import static com.culitostracker.domain.service.TipCatalog.Topic.SUPPORT;

/**
 * The generic-advice bank. Instead of storing thousands of sentences, a tip
 * is composed from three independent, grammatically complete fragments:
 *
 *   lead (context sentence) + action (one concrete suggestion) + optional closer
 *
 * Every fragment is an i18n key resolved by the frontend, so the bank costs a
 * few hundred strings per language while yielding over 10,000 distinct tips
 * (see {@link #distinctCombinations()}). Leads are tagged with the topics they
 * pair with and, optionally, the relationship stages or cycle phases they are
 * written for; actions belong to one topic. Composition is driven by the
 * caller's seeded Random, so the tip of the day is stable within a day.
 */
public final class TipCatalog {

    private TipCatalog() {
    }

    public static final String COMPOSED_KEY = "tips.composed";

    public enum Topic { COMMUNICATION, APPRECIATION, PLANS, INTIMACY, SPACE, CONFLICT, CURIOSITY, SUPPORT }

    public record Lead(String key, Set<Topic> topics, Set<RelationshipStage> stages, Set<CyclePhase> phases) {
        boolean appliesTo(RelationshipStage stage, CyclePhase phase) {
            boolean stageOk = stages.isEmpty() || (stage != null && stages.contains(stage));
            boolean phaseOk = phases.isEmpty() || (phase != null && phases.contains(phase));
            return stageOk && phaseOk;
        }
    }

    public record Action(String key, Topic topic, AdviceCategory category) {
    }

    private static final Set<Topic> ALL = EnumSet.allOf(Topic.class);
    private static final Set<RelationshipStage> EARLY = EnumSet.of(NEW, DEVELOPING);
    private static final Set<RelationshipStage> SETTLED = EnumSet.of(ESTABLISHED, LONG_TERM, VERY_LONG_TERM);
    private static final Set<RelationshipStage> LONG = EnumSet.of(LONG_TERM, VERY_LONG_TERM);

    private static Lead lead(int n, Set<Topic> topics) {
        return new Lead("tips.lead." + n, topics, Set.of(), Set.of());
    }

    private static Lead lead(int n, Set<Topic> topics, Set<RelationshipStage> stages) {
        return new Lead("tips.lead." + n, topics, stages, Set.of());
    }

    private static Lead phaseLead(int n, Set<Topic> topics, CyclePhase... phases) {
        return new Lead("tips.lead." + n, topics, Set.of(), EnumSet.of(phases[0], phases));
    }

    private static Action action(int n, Topic topic, AdviceCategory category) {
        return new Action("tips.action." + n, topic, category);
    }

    static final List<Lead> LEADS = List.of(
            // General leads: usable with several topics, any stage or phase.
            lead(1, ALL),
            lead(2, ALL),
            lead(3, ALL),
            lead(4, EnumSet.of(COMMUNICATION, CONFLICT, INTIMACY)),
            lead(5, ALL),
            lead(6, EnumSet.of(COMMUNICATION, SUPPORT, INTIMACY, SPACE)),
            lead(7, EnumSet.of(SUPPORT, SPACE, PLANS)),
            lead(8, EnumSet.of(PLANS, CURIOSITY, APPRECIATION)),
            lead(9, EnumSet.of(SPACE, SUPPORT)),
            lead(10, EnumSet.of(COMMUNICATION, CONFLICT, CURIOSITY)),
            lead(11, EnumSet.of(APPRECIATION, CURIOSITY)),
            lead(12, EnumSet.of(APPRECIATION, PLANS)),
            lead(13, EnumSet.of(SUPPORT, SPACE, CONFLICT)),
            lead(14, EnumSet.of(CURIOSITY, COMMUNICATION, PLANS)),
            lead(15, EnumSet.of(COMMUNICATION, SUPPORT, APPRECIATION)),
            lead(16, EnumSet.of(CONFLICT, COMMUNICATION, CURIOSITY)),
            // Communication
            lead(17, EnumSet.of(COMMUNICATION), EARLY),
            lead(18, EnumSet.of(COMMUNICATION), SETTLED),
            lead(19, EnumSet.of(COMMUNICATION)),
            lead(20, EnumSet.of(COMMUNICATION)),
            lead(21, EnumSet.of(COMMUNICATION)),
            // Appreciation
            lead(22, EnumSet.of(APPRECIATION), SETTLED),
            lead(23, EnumSet.of(APPRECIATION)),
            lead(24, EnumSet.of(APPRECIATION)),
            lead(25, EnumSet.of(APPRECIATION), EnumSet.of(DEVELOPING, ESTABLISHED)),
            lead(26, EnumSet.of(APPRECIATION)),
            // Plans
            lead(27, EnumSet.of(PLANS)),
            lead(28, EnumSet.of(PLANS), SETTLED),
            lead(29, EnumSet.of(PLANS), EARLY),
            lead(30, EnumSet.of(PLANS), LONG),
            lead(31, EnumSet.of(PLANS)),
            // Intimacy
            lead(32, EnumSet.of(INTIMACY)),
            lead(33, EnumSet.of(INTIMACY)),
            lead(34, EnumSet.of(INTIMACY), SETTLED),
            lead(35, EnumSet.of(INTIMACY), EARLY),
            lead(36, EnumSet.of(INTIMACY)),
            // Space
            lead(37, EnumSet.of(SPACE)),
            lead(38, EnumSet.of(SPACE)),
            phaseLead(39, EnumSet.of(SPACE), MENSTRUATION, LUTEAL),
            lead(40, EnumSet.of(SPACE), SETTLED),
            lead(41, EnumSet.of(SPACE)),
            // Conflict
            lead(42, EnumSet.of(CONFLICT)),
            lead(43, EnumSet.of(CONFLICT), EARLY),
            lead(44, EnumSet.of(CONFLICT), LONG),
            lead(45, EnumSet.of(CONFLICT)),
            lead(46, EnumSet.of(CONFLICT)),
            // Curiosity
            lead(47, EnumSet.of(CURIOSITY), EARLY),
            lead(48, EnumSet.of(CURIOSITY), SETTLED),
            lead(49, EnumSet.of(CURIOSITY)),
            lead(50, EnumSet.of(CURIOSITY), LONG),
            lead(51, EnumSet.of(CURIOSITY)),
            // Support (cycle-aware ones are phrased as estimates)
            phaseLead(52, EnumSet.of(SUPPORT), MENSTRUATION),
            lead(53, EnumSet.of(SUPPORT)),
            lead(54, EnumSet.of(SUPPORT)),
            lead(55, EnumSet.of(SUPPORT)),
            phaseLead(56, EnumSet.of(INTIMACY), OVULATION));

    static final List<Action> ACTIONS = buildActions();

    private static List<Action> buildActions() {
        List<Action> actions = new ArrayList<>();
        Topic[] order = {COMMUNICATION, APPRECIATION, PLANS, INTIMACY, SPACE, CONFLICT, CURIOSITY, SUPPORT};
        AdviceCategory[] categories = {AdviceCategory.COMMUNICATION, AdviceCategory.AFFECTION,
                AdviceCategory.DATE_IDEA, AdviceCategory.INTIMACY, AdviceCategory.SPACE,
                AdviceCategory.CONFLICT, AdviceCategory.GENERAL, AdviceCategory.SUPPORT};
        int n = 1;
        for (int t = 0; t < order.length; t++) {
            for (int i = 0; i < ACTIONS_PER_TOPIC; i++) {
                AdviceCategory category = categories[t];
                // Half of the "space" actions are about the user's own rest.
                if (order[t] == SPACE && i % 2 == 0) {
                    category = AdviceCategory.SELF_CARE;
                }
                actions.add(action(n++, order[t], category));
            }
        }
        return List.copyOf(actions);
    }

    static final int ACTIONS_PER_TOPIC = 10;
    static final int CLOSERS = 9;

    /** Weighted topic choice: the context nudges what kind of tip fits today. */
    static Map<Topic, Integer> topicWeights(RelationshipStage stage, CyclePhase phase) {
        Map<Topic, Integer> weights = new EnumMap<>(Topic.class);
        for (Topic topic : Topic.values()) {
            weights.put(topic, 2);
        }
        if (phase == MENSTRUATION) {
            weights.merge(SUPPORT, 4, Integer::sum);
            weights.merge(SPACE, 3, Integer::sum);
            weights.put(PLANS, 1);
        } else if (phase == LUTEAL) {
            weights.merge(SUPPORT, 2, Integer::sum);
            weights.merge(SPACE, 1, Integer::sum);
        } else if (phase == OVULATION || phase == CyclePhase.FOLLICULAR) {
            weights.merge(PLANS, 2, Integer::sum);
            weights.merge(INTIMACY, 1, Integer::sum);
        }
        if (stage == NEW) {
            weights.merge(CURIOSITY, 4, Integer::sum);
            weights.merge(COMMUNICATION, 2, Integer::sum);
            weights.merge(INTIMACY, 1, Integer::sum);
        } else if (stage == DEVELOPING) {
            weights.merge(COMMUNICATION, 2, Integer::sum);
            weights.merge(PLANS, 2, Integer::sum);
            weights.merge(CONFLICT, 1, Integer::sum);
        } else if (stage == ESTABLISHED) {
            weights.merge(APPRECIATION, 3, Integer::sum);
            weights.merge(PLANS, 2, Integer::sum);
        } else if (stage == LONG_TERM) {
            weights.merge(CURIOSITY, 2, Integer::sum);
            weights.merge(APPRECIATION, 2, Integer::sum);
            weights.merge(INTIMACY, 1, Integer::sum);
            weights.merge(SPACE, 1, Integer::sum);
        } else if (stage == VERY_LONG_TERM) {
            weights.merge(CURIOSITY, 3, Integer::sum);
            weights.merge(APPRECIATION, 2, Integer::sum);
            weights.merge(INTIMACY, 1, Integer::sum);
        }
        return weights;
    }

    /**
     * Composes one tip for the context. The candidate's params carry the
     * fragment keys ({@code lead}, {@code action}, optional {@code closer})
     * plus {@code name}, so the frontend resolves each piece and joins them.
     */
    public static AdviceCandidate compose(RelationshipStage stage, CyclePhase phase,
                                          String partnerName, int priority, Random seeded) {
        Topic topic = pickWeighted(topicWeights(stage, phase), seeded);

        List<Lead> leads = LEADS.stream()
                .filter(l -> l.topics().contains(topic) && l.appliesTo(stage, phase))
                .toList();
        Lead lead = leads.get(seeded.nextInt(leads.size()));

        List<Action> actions = ACTIONS.stream().filter(a -> a.topic() == topic).toList();
        Action action = actions.get(seeded.nextInt(actions.size()));

        int closer = seeded.nextInt(CLOSERS + 1); // 0 = no closer

        Map<String, Object> params = new HashMap<>();
        params.put("name", partnerName);
        params.put("lead", lead.key());
        params.put("action", action.key());
        if (closer > 0) {
            params.put("closer", "tips.closer." + closer);
        }
        AdviceSource source = lead.phases().isEmpty() ? AdviceSource.RELATIONSHIP : AdviceSource.CYCLE;
        return new AdviceCandidate(action.category(), COMPOSED_KEY, params, priority, source);
    }

    private static Topic pickWeighted(Map<Topic, Integer> weights, Random seeded) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        int roll = seeded.nextInt(total);
        for (Map.Entry<Topic, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        return Topic.COMMUNICATION;
    }

    /** How many distinct tips the bank can produce (any context). */
    public static long distinctCombinations() {
        long total = 0;
        for (Topic topic : Topic.values()) {
            long leads = LEADS.stream().filter(l -> l.topics().contains(topic)).count();
            long actions = ACTIONS.stream().filter(a -> a.topic() == topic).count();
            total += leads * actions * (CLOSERS + 1);
        }
        return total;
    }
}
