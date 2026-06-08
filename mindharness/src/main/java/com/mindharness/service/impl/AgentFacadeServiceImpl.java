package com.mindharness.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mindharness.converter.AgentConverter;
import com.mindharness.exception.BizException;
import com.mindharness.mapper.AgentMapper;
import com.mindharness.model.dto.AgentDTO;
import com.mindharness.model.entity.Agent;
import com.mindharness.model.request.CreateAgentRequest;
import com.mindharness.model.request.UpdateAgentRequest;
import com.mindharness.model.response.CreateAgentResponse;
import com.mindharness.model.response.GetAgentsResponse;
import com.mindharness.model.vo.AgentVO;
import com.mindharness.service.AgentFacadeService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@AllArgsConstructor
@Slf4j
public class AgentFacadeServiceImpl implements AgentFacadeService {

    private final AgentMapper agentMapper;
    private final AgentConverter agentConverter;

    /** 头像回收站目录 */
    private static final Path AVATAR_DIR = Path.of("uploads/avatars");
    private static final Path RECYCLE_DIR = AVATAR_DIR.resolve(".recycle");

    @Override
    public GetAgentsResponse getAgents() {
        List<Agent> agents = agentMapper.selectAll();
        List<AgentVO> result = new ArrayList<>();
        for (Agent agent : agents) {
            try {
                AgentVO vo = agentConverter.toVO(agent);
                result.add(vo);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return GetAgentsResponse.builder()
                .agents(result.toArray(new AgentVO[0]))
                .build();
    }

    @Override
    public CreateAgentResponse createAgent(CreateAgentRequest request) {
        try {
            // 将 CreateAgentRequest 转换为 AgentDTO
            AgentDTO agentDTO = agentConverter.toDTO(request);
            
            // 将 AgentDTO 转换为 Agent 实体
            Agent agent = agentConverter.toEntity(agentDTO);
            
            // 设置创建时间和更新时间
            OffsetDateTime now = OffsetDateTime.now();
            agent.setCreatedAt(now);
            agent.setUpdatedAt(now);
            
            // 插入数据库，ID 由数据库自动生成
            int result = agentMapper.insert(agent);
            if (result <= 0) {
                throw new BizException("创建 agent 失败");
            }
            
            // 返回生成的 agentId
            return CreateAgentResponse.builder()
                    .agentId(agent.getId())
                    .build();
        } catch (JsonProcessingException e) {
            throw new BizException("创建 agent 时发生序列化错误: " + e.getMessage());
        }
    }

    @Override
    public void deleteAgent(String agentId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException("Agent 不存在: " + agentId);
        }

        // 将关联的头像文件移至回收站
        moveAvatarToRecycle(agent.getAvatar());

        int result = agentMapper.deleteById(agentId);
        if (result <= 0) {
            throw new BizException("删除 agent 失败");
        }
    }

    /**
     * 将头像文件从 uploads/avatars/ 移至 uploads/avatars/.recycle/
     */
    private void moveAvatarToRecycle(String avatarFileName) {
        if (avatarFileName == null || avatarFileName.isBlank()) {
            return;
        }
        try {
            Path avatarPath = AVATAR_DIR.resolve(avatarFileName);
            if (Files.exists(avatarPath)) {
                Files.createDirectories(RECYCLE_DIR);
                Files.move(avatarPath, RECYCLE_DIR.resolve(avatarFileName),
                        StandardCopyOption.REPLACE_EXISTING);
                log.info("头像已移至回收站: {}", RECYCLE_DIR.resolve(avatarFileName));
            }
        } catch (IOException e) {
            log.warn("移动头像到回收站失败 ({}): {}", avatarFileName, e.getMessage());
            // 不阻止删除操作，头像丢失总比删不掉好
        }
    }

    @Override
    public void updateAgent(String agentId, UpdateAgentRequest request) {
        try {
            // 查询现有的 agent
            Agent existingAgent = agentMapper.selectById(agentId);
            if (existingAgent == null) {
                throw new BizException("Agent 不存在: " + agentId);
            }
            
            // 将现有 Agent 转换为 AgentDTO
            AgentDTO agentDTO = agentConverter.toDTO(existingAgent);
            
            // 使用 UpdateAgentRequest 更新 AgentDTO
            agentConverter.updateDTOFromRequest(agentDTO, request);
            
            // 将更新后的 AgentDTO 转换回 Agent 实体
            Agent updatedAgent = agentConverter.toEntity(agentDTO);
            
            // 保留原有的 ID 和创建时间
            updatedAgent.setId(existingAgent.getId());
            updatedAgent.setCreatedAt(existingAgent.getCreatedAt());
            updatedAgent.setUpdatedAt(OffsetDateTime.now());
            
            // 更新数据库
            int result = agentMapper.updateById(updatedAgent);
            if (result <= 0) {
                throw new BizException("更新 agent 失败");
            }
        } catch (JsonProcessingException e) {
            throw new BizException("更新 agent 时发生序列化错误: " + e.getMessage());
        }
    }
}
