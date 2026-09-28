package com.culitostracker.application;

import com.culitostracker.domain.model.PushSubscription;
import com.culitostracker.infrastructure.push.PushProperties;
import com.culitostracker.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Web Push delivery. Subscriptions are per browser; dead ones (404/410 from
 * the push service) are pruned on the spot. Payloads are small JSON objects
 * the service worker turns into notifications.
 */
@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final PushSubscriptionRepository subscriptionRepository;
    private final PushProperties properties;
    private final ObjectMapper objectMapper;
    /** Built on first send: BouncyCastle + the HTTP client cost real memory in a 256 MB box. */
    private volatile PushService client;

    public WebPushService(PushSubscriptionRepository subscriptionRepository,
                          PushProperties properties,
                          ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        if (!properties.enabled()) {
            log.info("Web Push disabled: VAPID keys not configured");
        }
    }

    public boolean enabled() {
        return properties.enabled();
    }

    private PushService client() throws Exception {
        PushService current = client;
        if (current == null) {
            synchronized (this) {
                if (client == null) {
                    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                        Security.addProvider(new BouncyCastleProvider());
                    }
                    client = new PushService(properties.publicKey(), properties.privateKey(),
                            properties.subject() == null ? "mailto:admin@culitostracker.local" : properties.subject());
                }
                current = client;
            }
        }
        return current;
    }

    public String publicKey() {
        return properties.publicKey();
    }

    @Transactional
    public void subscribe(UUID userId, String endpoint, String p256dh, String auth) {
        PushSubscription subscription = subscriptionRepository.findByEndpoint(endpoint)
                .orElseGet(PushSubscription::new);
        subscription.setUserId(userId); // a re-used browser endpoint follows its current user
        subscription.setEndpoint(endpoint);
        subscription.setP256dh(p256dh);
        subscription.setAuth(auth);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(UUID userId, String endpoint) {
        subscriptionRepository.findByEndpoint(endpoint)
                .filter(s -> s.getUserId().equals(userId))
                .ifPresent(subscriptionRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean hasSubscriptions(UUID userId) {
        return subscriptionRepository.countByUserId(userId) > 0;
    }

    /** Sends to every device of the user; returns how many deliveries were accepted. */
    @Transactional
    public int send(UUID userId, String title, String body, String url) {
        if (!enabled()) {
            return 0;
        }
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserId(userId);
        int delivered = 0;
        for (PushSubscription subscription : subscriptions) {
            try {
                byte[] payload = objectMapper.writeValueAsBytes(Map.of("title", title, "body", body, "url", url));
                HttpResponse response = client().send(new Notification(
                        subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(), payload));
                int status = response.getStatusLine().getStatusCode();
                if (status == 404 || status == 410) {
                    subscriptionRepository.delete(subscription); // browser unsubscribed
                } else if (status >= 200 && status < 300) {
                    delivered++;
                } else {
                    log.warn("Push to user {} returned {}", userId, status);
                }
            } catch (Exception e) {
                log.warn("Push to user {} failed: {}", userId, e.getMessage());
            }
        }
        return delivered;
    }
}
