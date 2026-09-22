package com.culitostracker.application;

import com.culitostracker.api.dto.MilestoneDtos.CreateMilestoneRequest;
import com.culitostracker.api.dto.MilestoneDtos.MilestoneResponse;
import com.culitostracker.api.dto.MilestoneDtos.UpdateMilestoneRequest;
import com.culitostracker.domain.model.RelationshipMilestone;
import com.culitostracker.repository.RelationshipMilestoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MilestoneService {

    private final RelationshipMilestoneRepository milestoneRepository;
    private final PartnerAccessService partnerAccessService;

    public MilestoneService(RelationshipMilestoneRepository milestoneRepository,
                            PartnerAccessService partnerAccessService) {
        this.milestoneRepository = milestoneRepository;
        this.partnerAccessService = partnerAccessService;
    }

    @Transactional(readOnly = true)
    public List<MilestoneResponse> list(UUID userId, UUID partnerId) {
        partnerAccessService.requireOwned(partnerId, userId);
        return milestoneRepository.findByPartnerIdOrderByDateDesc(partnerId).stream()
                .map(MilestoneResponse::from)
                .toList();
    }

    @Transactional
    public MilestoneResponse create(UUID userId, UUID partnerId, CreateMilestoneRequest request) {
        partnerAccessService.requireOwned(partnerId, userId);
        RelationshipMilestone milestone = new RelationshipMilestone();
        milestone.setPartnerId(partnerId);
        milestone.setType(request.type());
        milestone.setTitle(request.title().trim());
        milestone.setDescription(request.description());
        milestone.setDate(request.date());
        return MilestoneResponse.from(milestoneRepository.save(milestone));
    }

    @Transactional
    public MilestoneResponse update(UUID userId, UUID milestoneId, UpdateMilestoneRequest request) {
        RelationshipMilestone milestone = milestoneRepository.findByIdAndOwner(milestoneId, userId)
                .orElseThrow(() -> new NotFoundException("Milestone not found"));
        if (request.type() != null) {
            milestone.setType(request.type());
        }
        if (request.title() != null) {
            milestone.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            milestone.setDescription(request.description().isBlank() ? null : request.description());
        }
        if (request.date() != null) {
            milestone.setDate(request.date());
        }
        return MilestoneResponse.from(milestoneRepository.save(milestone));
    }

    @Transactional
    public void delete(UUID userId, UUID milestoneId) {
        RelationshipMilestone milestone = milestoneRepository.findByIdAndOwner(milestoneId, userId)
                .orElseThrow(() -> new NotFoundException("Milestone not found"));
        milestoneRepository.delete(milestone);
    }
}
