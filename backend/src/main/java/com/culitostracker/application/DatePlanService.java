package com.culitostracker.application;

import com.culitostracker.api.dto.DatePlanDtos.CreateDatePlanRequest;
import com.culitostracker.api.dto.DatePlanDtos.DatePlanResponse;
import com.culitostracker.api.dto.DatePlanDtos.UpdateDatePlanRequest;
import com.culitostracker.domain.model.DatePlan;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.repository.DatePlanRepository;
import com.culitostracker.repository.PartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DatePlanService {

    private final DatePlanRepository datePlanRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerAccessService partnerAccessService;

    public DatePlanService(DatePlanRepository datePlanRepository,
                           PartnerRepository partnerRepository,
                           PartnerAccessService partnerAccessService) {
        this.datePlanRepository = datePlanRepository;
        this.partnerRepository = partnerRepository;
        this.partnerAccessService = partnerAccessService;
    }

    @Transactional(readOnly = true)
    public List<DatePlanResponse> list(UUID userId, LocalDate from, LocalDate to) {
        return withNames(userId,
                datePlanRepository.findByOwnerUserIdAndDateBetweenOrderByDateAscStartTimeAsc(userId, from, to));
    }

    @Transactional(readOnly = true)
    public List<DatePlanResponse> upcoming(UUID userId) {
        return withNames(userId,
                datePlanRepository.findFirst5ByOwnerUserIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                        userId, LocalDate.now()));
    }

    @Transactional
    public DatePlanResponse create(UUID userId, CreateDatePlanRequest request) {
        Partner partner = partnerAccessService.requireOwned(request.partnerId(), userId);
        DatePlan plan = new DatePlan();
        plan.setOwnerUserId(userId);
        plan.setPartnerId(partner.getId());
        plan.setTitle(request.title().trim());
        plan.setLocation(blankToNull(request.location()));
        plan.setNotes(blankToNull(request.notes()));
        plan.setDate(request.date());
        plan.setStartTime(request.startTime());
        return DatePlanResponse.from(datePlanRepository.save(plan), partner.getName());
    }

    @Transactional
    public DatePlanResponse update(UUID userId, UUID planId, UpdateDatePlanRequest request) {
        DatePlan plan = datePlanRepository.findByIdAndOwnerUserId(planId, userId)
                .orElseThrow(() -> new NotFoundException("Date plan not found"));
        if (request.title() != null) {
            plan.setTitle(request.title().trim());
        }
        if (request.location() != null) {
            plan.setLocation(blankToNull(request.location()));
        }
        if (request.notes() != null) {
            plan.setNotes(blankToNull(request.notes()));
        }
        if (request.date() != null) {
            plan.setDate(request.date());
        }
        if (request.startTime() != null) {
            plan.setStartTime(request.startTime());
        }
        plan = datePlanRepository.save(plan);
        String name = partnerRepository.findById(plan.getPartnerId())
                .map(Partner::getName).orElse(null);
        return DatePlanResponse.from(plan, name);
    }

    @Transactional
    public void delete(UUID userId, UUID planId) {
        DatePlan plan = datePlanRepository.findByIdAndOwnerUserId(planId, userId)
                .orElseThrow(() -> new NotFoundException("Date plan not found"));
        datePlanRepository.delete(plan);
    }

    private List<DatePlanResponse> withNames(UUID userId, List<DatePlan> plans) {
        Map<UUID, String> names = partnerRepository.findByOwnerUserIdOrderByCreatedAtDesc(userId).stream()
                .collect(Collectors.toMap(Partner::getId, Partner::getName, (a, b) -> a));
        return plans.stream()
                .map(plan -> DatePlanResponse.from(plan, names.get(plan.getPartnerId())))
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
