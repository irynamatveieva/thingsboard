package org.thingsboard.rule.engine.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.credentials.CredentialsType;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.plugin.ComponentClusteringMode;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "azure functions",
        configClazz = TbAzureFunctionsNodeConfiguration.class,
        clusteringMode = ComponentClusteringMode.SINGLETON,
        nodeDescription = "Pushes message data to the Azure Functions",
        nodeDetails = "Will push message data to the Azure Functions</b>.",
        uiResources = {""},
        configDirective = "tbExternalNodeAzureIotHubConfig"//todo: change configDirective
)
public class TbAzureFunctionsNode extends TbAbstractExternalNode {

    private TbHttpClient httpClient;
    private TbAzureFunctionsNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        config = TbNodeUtils.convert(configuration, TbAzureFunctionsNodeConfiguration.class);
        if (CredentialsType.ACCESS_KEY == config.getCredentials().getType()
                && !config.getRestEndpointUrlPattern().contains("?code=")) {
            config.setRestEndpointUrlPattern(config.getRestEndpointUrlPattern() + "?code=" + ((AzureFunctionsCredentials) config.getCredentials()).getAccessKey());
        }
        httpClient = new TbHttpClient(config, ctx.getSharedEventLoop());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        var tbMsg = ackIfNeeded(ctx, msg);
        tbMsg = TbMsg.transformMsgData(tbMsg, getRequestBody(tbMsg));
        httpClient.processMessage(ctx, tbMsg,
                m -> tellSuccess(ctx, m),
                (m, t) -> tellFailure(ctx, m, t));
    }

    @Override
    public void destroy() {
        if (httpClient != null) {
            httpClient.destroy();
        }
    }

    private String getRequestBody(TbMsg msg) {
        ObjectNode requestBodyJson = JacksonUtil.newObjectNode();
        Map<String, String> mappings = processInputKeys(msg);
        mappings.forEach(requestBodyJson::put);
        return JacksonUtil.toString(requestBodyJson);
    }

    private Map<String, String> processInputKeys(TbMsg msg) {
        var mappings = new HashMap<String, String>();
        this.config.getInputKeys().forEach((funcKey, funcValue) -> {
            String patternProcessedFuncKey = TbNodeUtils.processPattern(funcKey, msg);
            String patternProcessedMsgKey = TbNodeUtils.processPattern(funcValue, msg);
            mappings.put(patternProcessedFuncKey, patternProcessedMsgKey);
        });
        return mappings;
    }
}
