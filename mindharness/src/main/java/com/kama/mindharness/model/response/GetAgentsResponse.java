package com.kama.mindharness.model.response;

import com.kama.mindharness.model.vo.AgentVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetAgentsResponse {
    private AgentVO[] agents;
}
