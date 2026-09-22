package com.culitostracker.application;

import com.culitostracker.api.dto.ObservationDtos.CreateObservationRequest;
import com.culitostracker.api.dto.ObservationDtos.ObservationResponse;
import com.culitostracker.domain.model.PartnerObservation;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.repository.PartnerObservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Observations are the observer's private perceptions. They are never shared
 * with anyone — including the person observed — and the UI always phrases them
 * as "you noted that...".
 */
@Service
public class ObservationService {

    private final PartnerObservationRepository observationRepository;
    private final PartnerAccessService partnerAccessService;

    public ObservationService(PartnerObservationRepository observationRepository,
                              PartnerAccessService partnerAccessService) {
        this.observationRepository = observationRepository;
        this.partnerAccessService = partnerAccessService;
    }

    @Transactional
    public ObservationResponse create(UUID userId, UUID partnerId, CreateObservationRequest request) {
        partnerAccessService.requireOwned(partnerId, userId);
        LocalDate date = request.date() != null ? request.date() : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new DomainRuleException("observation.futureDate", "Cannot observe a future date");
        }
        PartnerObservation observation = new PartnerObservation();
        observation.setObserverUserId(userId);
        observation.setPartnerId(partnerId);
        observation.setObservationType(request.observationType());
        observation.setNote(request.note());
        observation.setObservationDate(date);
        return ObservationResponse.from(observationRepository.save(observation));
    }

    @Transactional(readOnly = true)
    public List<ObservationResponse> list(UUID userId, UUID partnerId) {
        partnerAccessService.requireOwned(partnerId, userId);
        return observationRepository
                .findTop30ByPartnerIdAndObserverUserIdOrderByCreatedAtDesc(partnerId, userId).stream()
                .map(ObservationResponse::from)
                .toList();
    }
}
