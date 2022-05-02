package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "write to file",
        configClazz = WriteToFileNodeConfiguration.class,
        nodeDescription = "Write incoming messages to file",
        nodeDetails = "Write incoming Message to file. ",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "",
        icon = "menu"
)
public class WriteToFileNode implements TbNode {

    private WriteToFileNodeConfiguration config;
    private Path filePath;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, WriteToFileNodeConfiguration.class);
        filePath = Paths.get(config.getPath());
        try {
            Files.createFile(filePath);
        } catch (IOException e) {
            if (e instanceof FileAlreadyExistsException) {
                log.warn("File: {} already exists!", filePath);
            } else {
                throw new RuntimeException("Failed to create file due to: ", e);
            }
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        ArrayNode arrayNode;
        try {
            if (Files.readAllBytes(filePath).length == 0) {
                arrayNode = JacksonUtil.OBJECT_MAPPER.createArrayNode();
            } else {
                arrayNode = (ArrayNode) JacksonUtil.fromBytes(Files.readAllBytes(filePath), JsonNode.class);
            }
            arrayNode.add(JacksonUtil.fromString(msg.getData(), JsonNode.class));
//            log.info("test: {}", arrayNode);
            Files.write(filePath, arrayNode.toPrettyString().getBytes(StandardCharsets.UTF_8));
            ctx.tellSuccess(msg);
        } catch (IOException e) {
            ctx.tellFailure(msg, new RuntimeException("Failed to write to file due to: ", e));
        }
    }

    @Override
    public void destroy() {
    }

}

