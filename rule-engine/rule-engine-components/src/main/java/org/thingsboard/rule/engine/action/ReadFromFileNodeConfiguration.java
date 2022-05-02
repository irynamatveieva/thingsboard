package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class ReadFromFileNodeConfiguration implements NodeConfiguration {
    private String path;

    @Override
    public NodeConfiguration defaultConfiguration() {
        ReadFromFileNodeConfiguration configuration = new ReadFromFileNodeConfiguration();
        configuration.setPath("");
        return configuration;
    }
}
