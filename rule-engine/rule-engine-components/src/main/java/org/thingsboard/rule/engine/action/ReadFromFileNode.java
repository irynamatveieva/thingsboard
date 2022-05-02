package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.AbstractVerifier;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "read from file",
        configClazz = ReadFromFileNodeConfiguration.class,
        nodeDescription = "Read data from file",
        nodeDetails = "Read data from file.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "",
        icon = "menu"
)
public class ReadFromFileNode implements TbNode {
    private ReadFromFileNodeConfiguration config;
    private Path filePath;


    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, ReadFromFileNodeConfiguration.class);
        filePath = Paths.get(config.getPath());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        ArrayNode arrayNode;
        try {
            if (Files.readAllBytes(filePath).length == 0) {
                arrayNode = JacksonUtil.fromBytes(Files.readAllBytes(filePath), ArrayNode.class);
                for (JsonNode node : arrayNode) {
                    TbMsg tbMsg = ctx.transformMsg(msg, msg.getType(), msg.getOriginator(), msg.getMetaData(), node.toPrettyString());
                    ctx.tellSuccess(tbMsg);
                }
            } else {
                throw new RuntimeException("Failed to read from file");
            }
        } catch (
                IOException e) {
            ctx.tellFailure(msg, new RuntimeException("Failed to read from file due to: ", e));
        }
    }

    @Override
    public void destroy() {

    }
}
