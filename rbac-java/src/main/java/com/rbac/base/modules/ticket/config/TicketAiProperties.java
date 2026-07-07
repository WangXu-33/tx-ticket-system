package com.rbac.base.modules.ticket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 代码生成/修改时间：2026-07-05。
 * 功能说明：工单 AI/Agent 能力配置，预留后续接入 Spring AI、LangChain4j 或企业大模型网关的位置。
 * 入参：application.yml 中 ticket.ai 配置或环境变量。
 * 出参：TicketAiService 读取的 AI 配置对象。
 * 异常场景：未配置 key 时保持本地规则模式，不影响工单主流程。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ticket.ai")
public class TicketAiProperties {
    private boolean enabled = false;
    private String provider = "local-rule";
    private String model = "";
    private String apiKey = "";
    private String baseUrl = "";
}
