package com.culitostracker.application;

import com.culitostracker.api.dto.BillingDtos.BillingMe;
import com.culitostracker.domain.model.Plan;
import com.culitostracker.domain.model.Subscription;
import com.culitostracker.domain.model.User;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.infrastructure.billing.BillingProperties;
import com.culitostracker.infrastructure.config.AppUrls;
import com.culitostracker.repository.SubscriptionRepository;
import com.culitostracker.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Stripe glue. The app never stores card data: Checkout collects payment,
 * the Customer Portal manages it, and webhooks tell us the subscription
 * state, which is mirrored locally and turned into a plan.
 */
@Service
public class StripeBillingService {

    private static final Logger log = LoggerFactory.getLogger(StripeBillingService.class);

    private final BillingProperties properties;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PlanService planService;
    private final AppUrls appUrls;

    public StripeBillingService(BillingProperties properties,
                                SubscriptionRepository subscriptionRepository,
                                UserRepository userRepository,
                                PlanService planService,
                                AppUrls appUrls) {
        this.properties = properties;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.planService = planService;
        this.appUrls = appUrls;
        if (properties.enabled()) {
            Stripe.apiKey = properties.stripeSecretKey();
        } else {
            log.info("Billing disabled: Stripe keys not configured; every account is PRO");
        }
    }

    @Transactional(readOnly = true)
    public BillingMe me(UUID userId) {
        Optional<Subscription> subscription = subscriptionRepository.findById(userId);
        return new BillingMe(properties.enabled(), planService.planOf(userId),
                subscription.map(Subscription::getStatus).orElse(null),
                subscription.map(Subscription::getCurrentPeriodEnd).orElse(null),
                Plan.FREE_TRENDS_WEEKS, Plan.FREE_WISHLIST_ITEMS);
    }

    /** Stripe Checkout URL for the Pro subscription. */
    @Transactional(readOnly = true)
    public String checkoutUrl(UUID userId) {
        requireEnabled();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Optional<Subscription> existing = subscriptionRepository.findById(userId);
        try {
            SessionCreateParams.Builder params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(appUrls.frontend() + "/settings?billing=success")
                    .setCancelUrl(appUrls.frontend() + "/settings?billing=cancel")
                    .setClientReferenceId(userId.toString())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(properties.stripePriceId())
                            .setQuantity(1L)
                            .build());
            String customer = existing.map(Subscription::getProviderCustomerId).orElse(null);
            if (customer != null) {
                params.setCustomer(customer);
            } else {
                params.setCustomerEmail(user.getEmail());
            }
            return Session.create(params.build()).getUrl();
        } catch (StripeException e) {
            log.error("Stripe checkout failed for user {}", userId, e);
            throw new DomainRuleException("billing.providerError", "Payment provider error");
        }
    }

    /** Customer Portal URL (change card, cancel, invoices). */
    @Transactional(readOnly = true)
    public String portalUrl(UUID userId) {
        requireEnabled();
        Subscription subscription = subscriptionRepository.findById(userId)
                .filter(s -> s.getProviderCustomerId() != null)
                .orElseThrow(() -> new DomainRuleException("billing.noSubscription", "No subscription yet"));
        try {
            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(subscription.getProviderCustomerId())
                            .setReturnUrl(appUrls.frontend() + "/settings")
                            .build();
            return com.stripe.model.billingportal.Session.create(params).getUrl();
        } catch (StripeException e) {
            log.error("Stripe portal failed for user {}", userId, e);
            throw new DomainRuleException("billing.providerError", "Payment provider error");
        }
    }

    /** Verifies the signature and applies the events we care about. */
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        requireEnabled();
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, properties.stripeWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new DomainRuleException("billing.badSignature", "Invalid webhook signature");
        }
        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        switch (event.getType()) {
            case "checkout.session.completed" -> {
                if (object instanceof Session session && session.getClientReferenceId() != null) {
                    UUID userId = UUID.fromString(session.getClientReferenceId());
                    applySubscription(userId, session.getCustomer(), session.getSubscription(), "active", null);
                }
            }
            case "customer.subscription.updated", "customer.subscription.deleted" -> {
                if (object instanceof com.stripe.model.Subscription sub) {
                    Instant periodEnd = periodEndOf(sub);
                    String status = "customer.subscription.deleted".equals(event.getType()) ? "canceled" : sub.getStatus();
                    subscriptionRepository.findByProviderSubscriptionId(sub.getId())
                            .or(() -> subscriptionRepository.findByProviderCustomerId(sub.getCustomer()))
                            .ifPresent(local -> applySubscription(local.getUserId(), sub.getCustomer(),
                                    sub.getId(), status, periodEnd));
                }
            }
            default -> log.debug("Ignoring Stripe event {}", event.getType());
        }
    }

    /** Idempotent: mirrors the provider state and derives the plan from it. */
    @Transactional
    public void applySubscription(UUID userId, String customerId, String subscriptionId,
                                  String status, Instant currentPeriodEnd) {
        Subscription local = subscriptionRepository.findById(userId).orElseGet(() -> {
            Subscription s = new Subscription();
            s.setUserId(userId);
            return s;
        });
        if (customerId != null) {
            local.setProviderCustomerId(customerId);
        }
        if (subscriptionId != null) {
            local.setProviderSubscriptionId(subscriptionId);
        }
        local.setStatus(status);
        if (currentPeriodEnd != null) {
            local.setCurrentPeriodEnd(currentPeriodEnd);
        }
        subscriptionRepository.save(local);
        planService.setPlan(userId, local.grantsAccess() ? Plan.PRO : Plan.FREE);
    }

    private static Instant periodEndOf(com.stripe.model.Subscription sub) {
        try {
            var items = sub.getItems();
            if (items != null && items.getData() != null && !items.getData().isEmpty()) {
                Long end = items.getData().get(0).getCurrentPeriodEnd();
                return end == null ? null : Instant.ofEpochSecond(end);
            }
        } catch (RuntimeException ignored) {
            // API version differences: the period end is informational only.
        }
        return null;
    }

    private void requireEnabled() {
        if (!properties.enabled()) {
            throw new DomainRuleException("billing.disabled", "Billing is not configured");
        }
    }
}
