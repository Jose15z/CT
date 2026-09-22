package com.culitostracker.repository;

import com.culitostracker.domain.model.CyclePhase;
import com.culitostracker.domain.model.DailyTip;
import com.culitostracker.domain.model.RelationshipStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DailyTipRepository extends JpaRepository<DailyTip, UUID> {

    List<DailyTip> findByRelationshipStageAndActiveTrue(RelationshipStage stage);

    List<DailyTip> findByCyclePhaseAndActiveTrue(CyclePhase phase);

    List<DailyTip> findByRelationshipStageIsNullAndCyclePhaseIsNullAndActiveTrue();
}
