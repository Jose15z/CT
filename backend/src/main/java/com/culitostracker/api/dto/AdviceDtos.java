package com.culitostracker.api.dto;

import com.culitostracker.domain.model.AdviceCategory;
import com.culitostracker.domain.model.AdviceSource;
import com.culitostracker.domain.service.AdviceCandidate;

import java.util.List;
import java.util.Map;

public final class AdviceDtos {

    private AdviceDtos() {
    }

    public record AdviceItem(AdviceCategory category,
                             String messageKey,
                             Map<String, Object> params,
                             AdviceSource source) {

        public static AdviceItem from(AdviceCandidate c) {
            return new AdviceItem(c.category(), c.messageKey(), c.params(), c.source());
        }
    }

    public record AdviceResponse(List<AdviceItem> items) {
    }
}
