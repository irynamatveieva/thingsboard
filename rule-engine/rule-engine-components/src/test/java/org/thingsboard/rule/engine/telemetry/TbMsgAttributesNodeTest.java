/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.DataConstants.NOTIFY_DEVICE_METADATA_KEY;

@Slf4j
class TbMsgAttributesNodeTest extends AbstractRuleNodeUpgradeTest {

    private TenantId tenantId;
    private DeviceId deviceId;
    private TbMsgAttributesNode node;

    @BeforeEach
    void setUp() {
        tenantId = new TenantId(UUID.fromString("6c18691e-4470-4766-9739-aface71d761f"));
        deviceId = new DeviceId(UUID.fromString("b66159d7-c77e-45e8-bb41-a8f557f434c1"));
        node = spy(TbMsgAttributesNode.class);
    }

    @Test
    void testFilterChangedAttr_whenCurrentAttributesEmpty_thenReturnNewAttributes() {
        List<AttributeKvEntry> newAttributes = new ArrayList<>();

        List<AttributeKvEntry> filtered = node.filterChangedAttr(Collections.emptyList(), newAttributes);
        assertThat(filtered).isSameAs(newAttributes);
    }

    @Test
    void testFilterChangedAttr_whenCurrentAttributesContainsInAnyOrderNewAttributes_thenReturnEmptyList() {
        List<AttributeKvEntry> currentAttributes = List.of(
                new BaseAttributeKvEntry(1694000000L, new StringDataEntry("address", "Peremohy ave 1")),
                new BaseAttributeKvEntry(1694000000L, new BooleanDataEntry("valid", true)),
                new BaseAttributeKvEntry(1694000000L, new LongDataEntry("counter", 100L)),
                new BaseAttributeKvEntry(1694000000L, new DoubleDataEntry("temp", -18.35)),
                new BaseAttributeKvEntry(1694000000L, new JsonDataEntry("json", "{\"warning\":\"out of paper\"}"))
        );
        List<AttributeKvEntry> newAttributes = new ArrayList<>(currentAttributes);
        newAttributes.add(newAttributes.get(0));
        newAttributes.remove(0);
        assertThat(newAttributes).hasSize(currentAttributes.size());
        assertThat(currentAttributes).isNotEmpty();
        assertThat(newAttributes).containsExactlyInAnyOrderElementsOf(currentAttributes);

        List<AttributeKvEntry> filtered = node.filterChangedAttr(currentAttributes, newAttributes);
        assertThat(filtered).isEmpty(); //no changes
    }

    @Test
    void testFilterChangedAttr_whenCurrentAttributesContainsInAnyOrderNewAttributes_thenReturnExpectedList() {
        List<AttributeKvEntry> currentAttributes = List.of(
                new BaseAttributeKvEntry(1694000000L, new StringDataEntry("address", "Peremohy ave 1")),
                new BaseAttributeKvEntry(1694000000L, new BooleanDataEntry("valid", true)),
                new BaseAttributeKvEntry(1694000000L, new LongDataEntry("counter", 100L)),
                new BaseAttributeKvEntry(1694000000L, new DoubleDataEntry("temp", -18.35)),
                new BaseAttributeKvEntry(1694000000L, new JsonDataEntry("json", "{\"warning\":\"out of paper\"}"))
        );
        List<AttributeKvEntry> newAttributes = List.of(
                new BaseAttributeKvEntry(1694000999L, new JsonDataEntry("json", "{\"status\":\"OK\"}")), // value changed, reordered
                new BaseAttributeKvEntry(1694000999L, new StringDataEntry("valid", "true")), //type changed
                new BaseAttributeKvEntry(1694000999L, new LongDataEntry("counter", 101L)), //value changed
                new BaseAttributeKvEntry(1694000999L, new DoubleDataEntry("temp", -18.35)),
                new BaseAttributeKvEntry(1694000999L, new StringDataEntry("address", "Peremohy ave 1")) // reordered
        );
        List<AttributeKvEntry> expected = List.of(
                new BaseAttributeKvEntry(1694000999L, new StringDataEntry("valid", "true")),
                new BaseAttributeKvEntry(1694000999L, new LongDataEntry("counter", 101L)),
                new BaseAttributeKvEntry(1694000999L, new JsonDataEntry("json", "{\"status\":\"OK\"}"))
        );

        List<AttributeKvEntry> filtered = node.filterChangedAttr(currentAttributes, newAttributes);
        assertThat(filtered).containsExactlyInAnyOrderElementsOf(expected);
    }

    // Notify device backward-compatibility test arguments
    private static Stream<Arguments> givenNotifyDeviceMdValue_whenSaveAndNotify_thenVerifyExpectedArgumentForNotifyDeviceInSaveAndNotifyMethod() {
        return Stream.of(
                Arguments.of(null, true),
                Arguments.of("null", false),
                Arguments.of("true", true),
                Arguments.of("false", false)
        );
    }

    // Notify device backward-compatibility test
    @ParameterizedTest
    @MethodSource
    void givenNotifyDeviceMdValue_whenSaveAndNotify_thenVerifyExpectedArgumentForNotifyDeviceInSaveAndNotifyMethod(String mdValue, boolean expectedArgumentValue) throws TbNodeException {
        var ctxMock = mock(TbContext.class);
        var telemetryServiceMock = mock(RuleEngineTelemetryService.class);
        ObjectNode defaultConfig = (ObjectNode) JacksonUtil.valueToTree(new TbMsgAttributesNodeConfiguration().defaultConfiguration());
        defaultConfig.put("notifyDevice", false);
        var tbNodeConfiguration = new TbNodeConfiguration(defaultConfig);

        assertThat(defaultConfig.has("notifyDevice")).as("pre condition has notifyDevice").isTrue();

        when(ctxMock.getTenantId()).thenReturn(tenantId);
        when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);
        willCallRealMethod().given(node).init(any(TbContext.class), any(TbNodeConfiguration.class));
        willCallRealMethod().given(node).saveAttr(any(), eq(ctxMock), any(TbMsg.class), any(AttributeScope.class), anyBoolean());

        node.init(ctxMock, tbNodeConfiguration);

        TbMsgMetaData md = new TbMsgMetaData();
        if (mdValue != null) {
            md.putValue(NOTIFY_DEVICE_METADATA_KEY, mdValue);
        }
        // dummy list with one ts kv to pass the empty list check.
        var testTbMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, md, TbMsg.EMPTY_STRING);
        List<AttributeKvEntry> testAttrList = List.of(new BaseAttributeKvEntry(0L, new StringDataEntry("testKey", "testValue")));

        node.saveAttr(testAttrList, ctxMock, testTbMsg, AttributeScope.SHARED_SCOPE, false);

        ArgumentCaptor<Boolean> notifyDeviceCaptor = ArgumentCaptor.forClass(Boolean.class);

        verify(telemetryServiceMock, times(1)).saveAndNotify(
                eq(tenantId), eq(deviceId), eq(AttributeScope.SHARED_SCOPE),
                eq(testAttrList), notifyDeviceCaptor.capture(), any()
        );
        boolean notifyDevice = notifyDeviceCaptor.getValue();
        assertThat(notifyDevice).isEqualTo(expectedArgumentValue);
    }

    @Test
    void givenUseTemplateTrueAndNoMetadataKeyInMsg_whenOnMsg_thenSaveAttributes() throws Exception {
        var ctx = mock(TbContext.class);
        var config = new TbMsgAttributesNodeConfiguration().defaultConfiguration();
        config.setUseAttributesScopeTemplate(true);
        config.setScope("${scopeInMetadata}");
        config.setUpdateAttributesOnlyOnValueChange(false);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);
        var telemetryService = mockTelemetryServiceSaveAndNotify(ctx);

        final Map<String, String> mdMap = Map.of("scope", "SERVER_SCOPE");
        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\"}";
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, new TbMsgMetaData(mdMap), data);

        node.onMsg(ctx, msg);

        verify(telemetryService).saveAndNotify(eq(ctx.getTenantId()), eq(msg.getOriginator()), eq(AttributeScope.SERVER_SCOPE),
                anyList(), eq(true), eq(new TelemetryNodeCallback(ctx, msg)));
        verify(ctx).tellSuccess(eq(msg));
    }

    @Test
    void givenUseTemplateTrueAndSpongeCaseDataKey_whenOnMsg_thenSaveAttributes() throws Exception {
        var ctx = mock(TbContext.class);
        var config = new TbMsgAttributesNodeConfiguration().defaultConfiguration();
        config.setUseAttributesScopeTemplate(true);
        config.setScope("$[scopeInData]");
        config.setUpdateAttributesOnlyOnValueChange(false);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);
        var telemetryService = mockTelemetryServiceSaveAndNotify(ctx);

        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\", \"scopeInData\": \" SeRvEr_ScOpE \"}";
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, TbMsgMetaData.EMPTY, data);

        node.onMsg(ctx, msg);

        verify(telemetryService).saveAndNotify(eq(ctx.getTenantId()), eq(msg.getOriginator()), eq(AttributeScope.SERVER_SCOPE),
                anyList(), eq(true), eq(new TelemetryNodeCallback(ctx, msg)));
        verify(ctx).tellSuccess(eq(msg));
    }

    @Test
    void givenUseTemplateTrueAndInvalidMetadataKey_whenOnMsg_thenThrowException() throws TbNodeException {
        var ctx = mock(TbContext.class);
        var config = new TbMsgAttributesNodeConfiguration().defaultConfiguration();
        config.setUseAttributesScopeTemplate(true);
        config.setScope("${scopeInMetadata}");
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\"}";
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, TbMsgMetaData.EMPTY, data);

        Assertions.assertThatThrownBy(() -> node.onMsg(ctx, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to parse scope! No enum constant for name: " + config.getScope());
    }

    @Test
    void givenUseTemplateTrueAndMetadataKeyNonExistingScope_whenOnMsg_thenThrowException() throws TbNodeException {
        var ctx = mock(TbContext.class);
        var config = new TbMsgAttributesNodeConfiguration().defaultConfiguration();
        config.setUseAttributesScopeTemplate(true);
        config.setScope("${scopeInMetadata}");
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        final Map<String, String> mdMap = Map.of("scopeInMetadata", "ANOTHER_SCOPE");
        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\"}";
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, new TbMsgMetaData(mdMap), data);

        assertThatThrownBy(() -> node.onMsg(ctx, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to parse scope! No enum constant for name: " + msg.getMetaData().getValue("scopeInMetadata"));

    }

    @Test
    void givenScopeIsNull_whenOnMsg_thenThrowException() throws TbNodeException {
        var ctx = mock(TbContext.class);
        var config = new TbMsgAttributesNodeConfiguration().defaultConfiguration();
        config.setScope(null);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        final Map<String, String> mdMap = Map.of("scopeInMetadata", "ANOTHER_SCOPE");
        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\"}";
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, new TbMsgMetaData(mdMap), data);

        assertThatThrownBy(() -> node.onMsg(ctx, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to parse scope! Provided value is null!");
    }

    @Test
    void givenDefaultConfig_whenInit_thenOk() {
        var ctx = mock(TbContext.class);
        var config = new TbMsgAttributesNodeConfiguration().defaultConfiguration();
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        assertThatCode(() -> node.init(ctx, nodeConfiguration))
                .doesNotThrowAnyException();
    }

    private RuleEngineTelemetryService mockTelemetryServiceSaveAndNotify(TbContext ctx) {
        var telemetryService = mock(RuleEngineTelemetryService.class);

        willReturn(telemetryService).given(ctx).getTelemetryService();
        willAnswer(invocation -> {
            TelemetryNodeCallback callBack = invocation.getArgument(5);
            callBack.onSuccess(null);
            return null;
        }).given(telemetryService).saveAndNotify(
                any(), any(), any(AttributeScope.class), anyList(), anyBoolean(), any());
        return telemetryService;
    }

    // Rule nodes upgrade
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":\"false\",\"sendAttributesUpdatedNotification\":\"false\"}",
                        true,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":false,\"useAttributesScopeTemplate\":false}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}",
                        true,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true,\"useAttributesScopeTemplate\":false}"),
                // all flags are booleans
                Arguments.of(1,
                        "{\"scope\":\"SHARED_SCOPE\",\"notifyDevice\":true,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}",
                        true,
                        "{\"scope\":\"SHARED_SCOPE\",\"notifyDevice\":true,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true,\"useAttributesScopeTemplate\":false}"),
                // no boolean flags set
                Arguments.of(1,
                        "{\"scope\":\"CLIENT_SCOPE\"}",
                        true,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":true,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true,\"useAttributesScopeTemplate\":false}"),
                // all flags are boolean strings
                Arguments.of(1,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":\"false\",\"sendAttributesUpdatedNotification\":\"false\",\"updateAttributesOnlyOnValueChange\":\"true\"}",
                        true,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true,\"useAttributesScopeTemplate\":false}"),
                // at least one flag is boolean string
                Arguments.of(1,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":\"false\",\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}",
                        true,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true,\"useAttributesScopeTemplate\":false}"),
                // notify device flag is null
                Arguments.of(1,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":\"null\",\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}",
                        true,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":true,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true,\"useAttributesScopeTemplate\":false}"),
                // config for version 2
                Arguments.of(2,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true}",
                        true,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false, \"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true,\"useAttributesScopeTemplate\":false}"),
                // config for version 3 with upgrade from version 2
                Arguments.of(2,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false,\"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true,\"useAttributesScopeTemplate\":false}",
                        false,
                        "{\"scope\":\"CLIENT_SCOPE\",\"notifyDevice\":false, \"sendAttributesUpdatedNotification\":false,\"updateAttributesOnlyOnValueChange\":true,\"useAttributesScopeTemplate\":false}")
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

}
