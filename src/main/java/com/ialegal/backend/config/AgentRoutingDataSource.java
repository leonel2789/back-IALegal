package com.ialegal.backend.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class AgentRoutingDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> currentAgentType = new ThreadLocal<>();

    public static void setCurrentAgentType(String agentType) {
        currentAgentType.set(agentType);
    }

    public static void clearCurrentAgentType() {
        currentAgentType.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return currentAgentType.get();
    }
}