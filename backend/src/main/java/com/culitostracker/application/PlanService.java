package com.culitostracker.application;

import com.culitostracker.domain.model.Plan;
import com.culitostracker.domain.model.User;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.infrastructure.billing.BillingProperties;
import com.culitostracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The one place that answers "what can this user do?". Feature code asks
 * here and never looks at Stripe. Without billing configured every account
 * is PRO, so a fresh deployment has no paywall until the operator wants one.
 */
@Service
public class PlanService {

    private final UserRepository userRepository;
    private final BillingProperties billing;

    public PlanService(UserRepository userRepository, BillingProperties billing) {
        this.userRepository = userRepository;
        this.billing = billing;
    }

    public boolean billingEnabled() {
        return billing.enabled();
    }

    @Transactional(readOnly = true)
    public Plan planOf(UUID userId) {
        if (!billing.enabled()) {
            return Plan.PRO;
        }
        return userRepository.findById(userId).map(User::getPlan).orElse(Plan.FREE);
    }

    public boolean isPro(UUID userId) {
        return planOf(userId) == Plan.PRO;
    }

    /** Throws the code the frontend turns into an upgrade prompt. */
    public void requirePro(UUID userId) {
        if (!isPro(userId)) {
            throw new DomainRuleException("plan.proRequired", "This feature needs the Pro plan");
        }
    }

    /** Trends span for this user's plan. */
    public int maxTrendWeeks(UUID userId) {
        return isPro(userId) ? Integer.MAX_VALUE : Plan.FREE_TRENDS_WEEKS;
    }

    /** Open wishlist items allowed; PRO is unlimited. */
    public int maxWishlistItems(UUID userId) {
        return isPro(userId) ? Integer.MAX_VALUE : Plan.FREE_WISHLIST_ITEMS;
    }

    @Transactional
    public void setPlan(UUID userId, Plan plan) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setPlan(plan);
            userRepository.save(user);
        });
    }
}
