package com.culitostracker.application;

import com.culitostracker.domain.model.Plan;
import com.culitostracker.domain.model.User;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.infrastructure.billing.BillingProperties;
import com.culitostracker.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final UUID userId = UUID.randomUUID();

    private PlanService service(boolean billingOn, Plan stored) {
        User user = new User();
        user.setPlan(stored);
        when(users.findById(any())).thenReturn(Optional.of(user));
        BillingProperties props = billingOn
                ? new BillingProperties("sk_test_x", "whsec_x", "price_x")
                : new BillingProperties("", "", "");
        return new PlanService(users, props);
    }

    @Test
    void withoutBillingEveryoneIsPro() {
        PlanService service = service(false, Plan.FREE);
        assertThat(service.planOf(userId)).isEqualTo(Plan.PRO);
        assertThat(service.maxTrendWeeks(userId)).isEqualTo(Integer.MAX_VALUE);
        service.requirePro(userId); // no throw
    }

    @Test
    void withBillingTheStoredPlanRules() {
        PlanService free = service(true, Plan.FREE);
        assertThat(free.planOf(userId)).isEqualTo(Plan.FREE);
        assertThat(free.maxTrendWeeks(userId)).isEqualTo(Plan.FREE_TRENDS_WEEKS);
        assertThat(free.maxWishlistItems(userId)).isEqualTo(Plan.FREE_WISHLIST_ITEMS);
        assertThatThrownBy(() -> free.requirePro(userId))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("Pro");

        PlanService pro = service(true, Plan.PRO);
        assertThat(pro.isPro(userId)).isTrue();
        assertThat(pro.maxWishlistItems(userId)).isEqualTo(Integer.MAX_VALUE);
    }
}
