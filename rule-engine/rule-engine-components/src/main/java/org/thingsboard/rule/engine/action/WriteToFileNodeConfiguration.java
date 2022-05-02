package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;



@Data
public class WriteToFileNodeConfiguration implements NodeConfiguration {
    private String path;

    @Override
    public NodeConfiguration defaultConfiguration() {
        WriteToFileNodeConfiguration configuration = new WriteToFileNodeConfiguration();
        configuration.setPath("");
        return configuration;
    }
}
