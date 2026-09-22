package com.culitostracker.application;

import com.culitostracker.api.dto.CycleDtos;
import com.culitostracker.api.dto.CycleDtos.CreatePeriodRequest;
import com.culitostracker.api.dto.CycleDtos.CycleProfileResponse;
import com.culitostracker.api.dto.CycleDtos.DayDto;
import com.culitostracker.api.dto.CycleDtos.PeriodRecordResponse;
import com.culitostracker.api.dto.CycleDtos.PredictionsResponse;
import com.culitostracker.api.dto.CycleDtos.UpdateCycleProfileRequest;
import com.culitostracker.api.dto.CycleDtos.UpdatePeriodRequest;
import com.culitostracker.domain.model.AccessScope;
import com.culitostracker.domain.model.CycleProfile;
import com.culitostracker.domain.model.PeriodRecord;
import com.culitostracker.domain.service.CyclePrediction;
import com.culitostracker.domain.service.CyclePredictionService;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.repository.CycleProfileRepository;
import com.culitostracker.repository.PeriodRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CycleService {

    private final CycleProfileRepository cycleProfileRepository;
    private final PeriodRecordRepository periodRecordRepository;
    private final PartnerAccessService partnerAccessService;
    private final CyclePredictionService predictionService;

    public CycleService(CycleProfileRepository cycleProfileRepository,
                        PeriodRecordRepository periodRecordRepository,
                        PartnerAccessService partnerAccessService,
                        CyclePredictionService predictionService) {
        this.cycleProfileRepository = cycleProfileRepository;
        this.periodRecordRepository = periodRecordRepository;
        this.partnerAccessService = partnerAccessService;
        this.predictionService = predictionService;
    }

    @Transactional
    public CycleProfileResponse getProfile(UUID userId, UUID partnerId) {
        partnerAccessService.requireViewable(partnerId, userId, AccessScope.CYCLE);
        return CycleProfileResponse.from(getOrCreateProfile(partnerId));
    }

    @Transactional
    public CycleProfileResponse updateProfile(UUID userId, UUID partnerId,
                                              UpdateCycleProfileRequest request) {
        partnerAccessService.requireOwned(partnerId, userId);
        CycleProfile profile = getOrCreateProfile(partnerId);
        if (request.averageCycleLength() != null) {
            profile.setAverageCycleLength(request.averageCycleLength());
        }
        if (request.averagePeriodLength() != null) {
            profile.setAveragePeriodLength(request.averagePeriodLength());
        }
        if (request.lastPeriodStartDate() != null) {
            profile.setLastPeriodStartDate(request.lastPeriodStartDate());
        }
        if (request.trackingEnabled() != null) {
            profile.setTrackingEnabled(request.trackingEnabled());
        }
        return CycleProfileResponse.from(cycleProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<PeriodRecordResponse> listPeriods(UUID userId, UUID partnerId) {
        partnerAccessService.requireViewable(partnerId, userId, AccessScope.CYCLE);
        return periodsOf(partnerId).stream().map(PeriodRecordResponse::from).toList();
    }

    /** "Her period started (today | on this date)". Recalculation is implicit: predictions always read the records. */
    @Transactional
    public PeriodRecordResponse recordPeriod(UUID userId, UUID partnerId, CreatePeriodRequest request) {
        partnerAccessService.requireOwned(partnerId, userId);
        CycleProfile profile = getOrCreateProfile(partnerId);
        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now();
        if (startDate.isAfter(LocalDate.now())) {
            throw new DomainRuleException("period.futureStart", "A period cannot start in the future");
        }
        if (request.endDate() != null && request.endDate().isBefore(startDate)) {
            throw new DomainRuleException("period.endBeforeStart", "End date is before start date");
        }

        // Same start date twice = update, not duplicate.
        PeriodRecord record = periodsOf(partnerId).stream()
                .filter(r -> r.getStartDate().equals(startDate))
                .findFirst()
                .orElseGet(() -> {
                    PeriodRecord r = new PeriodRecord();
                    r.setCycleProfileId(profile.getId());
                    r.setStartDate(startDate);
                    return r;
                });
        if (request.endDate() != null) {
            record.setEndDate(request.endDate());
        }
        if (request.notes() != null) {
            record.setNotes(request.notes());
        }
        record = periodRecordRepository.save(record);

        if (profile.getLastPeriodStartDate() == null || startDate.isAfter(profile.getLastPeriodStartDate())) {
            profile.setLastPeriodStartDate(startDate);
            cycleProfileRepository.save(profile);
        }
        return PeriodRecordResponse.from(record);
    }

    @Transactional
    public PeriodRecordResponse updatePeriod(UUID userId, UUID periodId, UpdatePeriodRequest request) {
        PeriodRecord record = periodRecordRepository.findByIdAndOwner(periodId, userId)
                .orElseThrow(() -> new NotFoundException("Period record not found"));
        if (request.startDate() != null) {
            record.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            record.setEndDate(request.endDate());
        }
        if (request.notes() != null) {
            record.setNotes(request.notes().isBlank() ? null : request.notes());
        }
        if (record.getEndDate() != null && record.getEndDate().isBefore(record.getStartDate())) {
            throw new DomainRuleException("period.endBeforeStart", "End date is before start date");
        }
        record = periodRecordRepository.save(record);
        syncLastPeriodStart(record.getCycleProfileId());
        return PeriodRecordResponse.from(record);
    }

    @Transactional
    public void deletePeriod(UUID userId, UUID periodId) {
        PeriodRecord record = periodRecordRepository.findByIdAndOwner(periodId, userId)
                .orElseThrow(() -> new NotFoundException("Period record not found"));
        UUID profileId = record.getCycleProfileId();
        periodRecordRepository.delete(record);
        syncLastPeriodStart(profileId);
    }

    @Transactional(readOnly = true)
    public PredictionsResponse predictions(UUID userId, UUID partnerId, LocalDate from, LocalDate to) {
        partnerAccessService.requireViewable(partnerId, userId, AccessScope.CYCLE);
        CycleProfile profile = cycleProfileRepository.findByPartnerId(partnerId).orElse(null);
        LocalDate today = LocalDate.now();
        LocalDate rangeFrom = from != null ? from : today.withDayOfMonth(1);
        LocalDate rangeTo = to != null ? to : rangeFrom.plusMonths(1).minusDays(1);
        if (rangeTo.isBefore(rangeFrom) || rangeFrom.plusYears(2).isBefore(rangeTo)) {
            throw new DomainRuleException("cycle.invalidRange", "Invalid prediction range");
        }

        if (profile == null || !profile.isTrackingEnabled()) {
            return PredictionsResponse.insufficient(profile != null && profile.isTrackingEnabled(), List.of());
        }
        List<PeriodRecord> records = periodRecordRepository
                .findByCycleProfileIdOrderByStartDateDesc(profile.getId());

        Optional<CyclePrediction> predictionOpt = predictionService.predict(profile, records, today);
        List<DayDto> days = predictionService.classifyRange(profile, records, rangeFrom, rangeTo).stream()
                .map(DayDto::from)
                .toList();

        if (predictionOpt.isEmpty()) {
            return PredictionsResponse.insufficient(profile.isTrackingEnabled(), days);
        }
        CyclePrediction p = predictionOpt.get();
        return new PredictionsResponse(false, true, p.cycleLength(), p.periodLength(),
                p.lastPeriodStart(), p.nextPeriodStart(), p.ovulationDate(),
                p.fertileWindowStart(), p.fertileWindowEnd(), p.currentCycleDay(),
                p.currentPhase(), days, CycleDtos.DISCLAIMER_KEYS);
    }

    CycleProfile getOrCreateProfile(UUID partnerId) {
        return cycleProfileRepository.findByPartnerId(partnerId).orElseGet(() -> {
            CycleProfile profile = new CycleProfile();
            profile.setPartnerId(partnerId);
            return cycleProfileRepository.save(profile);
        });
    }

    private List<PeriodRecord> periodsOf(UUID partnerId) {
        return cycleProfileRepository.findByPartnerId(partnerId)
                .map(p -> periodRecordRepository.findByCycleProfileIdOrderByStartDateDesc(p.getId()))
                .orElse(List.of());
    }

    private void syncLastPeriodStart(UUID profileId) {
        cycleProfileRepository.findById(profileId).ifPresent(profile -> {
            LocalDate latest = periodRecordRepository
                    .findByCycleProfileIdOrderByStartDateDesc(profileId).stream()
                    .map(PeriodRecord::getStartDate)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            profile.setLastPeriodStartDate(latest);
            cycleProfileRepository.save(profile);
        });
    }
}
