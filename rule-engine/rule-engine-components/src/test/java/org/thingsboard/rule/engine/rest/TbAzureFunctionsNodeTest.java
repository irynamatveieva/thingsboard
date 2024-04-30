package org.thingsboard.rule.engine.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;

@ExtendWith(MockitoExtension.class)
public class TbAzureFunctionsNodeTest {

    private static final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("5681f8ad-8cb6-4901-af4a-3882a701adf0"));

    private TbAzureFunctionsNode node;
    private TbAzureFunctionsNodeConfiguration config;

    @Mock
    private TbContext ctxMock;

    @BeforeEach
    public void setUp() {
        node = new TbAzureFunctionsNode();
        config = new TbAzureFunctionsNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOk() {
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        assertThatNoException().isThrownBy(() -> node.init(ctxMock, configuration));
    }

    @Test
    public void givenParams_whenOnMsg_thenTellSuccess() throws TbNodeException {
        config.setInputKeys(Map.of("${funcKeyTemplate}", "${msgKeyTemplate}"));
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        String data = """
                {
                "msgKey": 10
                }
                """;
        Map<String, String> metadata = new HashMap<>();
        metadata.put("funcKeyTemplate", "funcKey");
        metadata.put("msgKeyTemplate", "msgKey");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, new TbMsgMetaData(metadata), data);

    }

}