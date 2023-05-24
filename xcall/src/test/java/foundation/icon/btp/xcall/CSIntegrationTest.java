/*
 * Copyright 2022 ICON Foundation
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

package foundation.icon.btp.xcall;

import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMCIntegrationTest;
import foundation.icon.btp.xcall.sample.DAppProxySampleScoreClient;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.util.Map;
import java.util.function.Consumer;

public interface CSIntegrationTest extends BTPIntegrationTest {

    CallServiceScoreClient callSvc = new CallServiceScoreClient(DefaultScoreClient.of(
            System.getProperties(), Map.of(
                    "_bmc", MockBMCIntegrationTest.mockBMC._address())));

    FeeManageScoreClient feeManager = new FeeManageScoreClient(callSvc);

    DAppProxySampleScoreClient sampleClient = new DAppProxySampleScoreClient(
            DefaultScoreClient.of("sample.",
                    System.getProperties(), Map.of(
                            "_callService", callSvc._address())));

    static Consumer<TransactionResult> callMessageEvent(
            Consumer<CallServiceEventScoreClient.CallMessage> consumer) {
        return eventLogChecker(
                CallServiceEventScoreClient.CallMessage::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> callMessageSentEvent(
            Consumer<CallServiceEventScoreClient.CallMessageSent> consumer) {
        return eventLogChecker(
                CallServiceEventScoreClient.CallMessageSent::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> responseMessageEvent(
            Consumer<CallServiceEventScoreClient.ResponseMessage> consumer) {
        return eventLogChecker(
                CallServiceEventScoreClient.ResponseMessage::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> rollbackMessageEvent(
            Consumer<CallServiceEventScoreClient.RollbackMessage> consumer) {
        return eventLogChecker(
                CallServiceEventScoreClient.RollbackMessage::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> callExecutedEvent(
            Consumer<CallServiceEventScoreClient.CallExecuted> consumer) {
        return eventLogChecker(
                CallServiceEventScoreClient.CallExecuted::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> rollbackExecutedEvent(
            Consumer<CallServiceEventScoreClient.RollbackExecuted> consumer) {
        return eventLogChecker(
                CallServiceEventScoreClient.RollbackExecuted::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> responseMessageEventShouldNotExists() {
        return eventLogShouldNotExistsChecker(CallServiceEventScoreClient.ResponseMessage::eventLogs);
    }

    static Consumer<TransactionResult> rollbackMessageEventShouldNotExists() {
        return eventLogShouldNotExistsChecker(CallServiceEventScoreClient.RollbackMessage::eventLogs);
    }

    static <T> Consumer<TransactionResult> eventLogChecker(
            EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                callSvc._address(), supplier, consumer);
    }

    static <T> Consumer<TransactionResult> eventLogShouldNotExistsChecker(
            ScoreIntegrationTest.EventLogsSupplier<T> supplier) {
        return ScoreIntegrationTest.eventLogShouldNotExistsChecker(
                callSvc._address(), supplier);
    }

    static Consumer<TransactionResult> messageReceivedEvent(
            Consumer<DAppProxySampleScoreClient.MessageReceived> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                sampleClient._address(),
                DAppProxySampleScoreClient.MessageReceived::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> rollbackDataReceivedEvent (
            Consumer<DAppProxySampleScoreClient.RollbackDataReceived> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                sampleClient._address(),
                DAppProxySampleScoreClient.RollbackDataReceived::eventLogs,
                consumer);
    }
}
