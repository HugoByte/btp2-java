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

package foundation.icon.btp.bmv.btpblock;

import foundation.icon.btp.lib.BMV;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.score.util.Logger;
import foundation.icon.score.util.StringUtil;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;
import score.annotation.Optional;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class BTPMessageVerifier implements BMV {
    private static final Logger logger = Logger.getLogger(BTPMessageVerifier.class);
    private static String HASH = "keccak-256";
    private static String SIGNATURE_ALG = "ecdsa-secp256k1";
    private final VarDB<BMVProperties> propertiesDB = Context.newVarDB("properties", BMVProperties.class);

    public BTPMessageVerifier(
            @Optional String srcNetworkID,
            @Optional int networkTypeID,
            @Optional Address bmc,
            @Optional byte[] blockHeader,
            @Optional BigInteger seqOffset
    ) {
        BMVProperties bmvProperties = getProperties();
        if (srcNetworkID == null && networkTypeID == 0 && bmc == null && blockHeader == null && seqOffset.signum() == 0) return;
        if (srcNetworkID != null) bmvProperties.setSrcNetworkID(srcNetworkID.getBytes());
        bmvProperties.setNetworkTypeID(networkTypeID);
        if (bmc != null) bmvProperties.setBmc(bmc);
        bmvProperties.setSequenceOffset(seqOffset);
        if (blockHeader != null) handleFirstBlockHeader(BlockHeader.fromBytes(blockHeader), bmvProperties);
    }

    public BMVProperties getProperties() {
        return propertiesDB.getOrDefault(BMVProperties.DEFAULT);
    }

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, byte[] _msg) {
        BTPAddress curAddr = BTPAddress.valueOf(_bmc);
        BTPAddress prevAddr = BTPAddress.valueOf(_prev);
        checkAccessible(curAddr, prevAddr);
        var bmvProperties = getProperties();
        var lastSeq = bmvProperties.getLastSequence();
        var seq = bmvProperties.getSequenceOffset().add(lastSeq);
        if (seq.compareTo(_seq) != 0) throw BMVException.unknown("invalid sequence");
        RelayMessage relayMessages = RelayMessage.fromBytes(_msg);
        RelayMessage.TypePrefixedMessage[] typePrefixedMessages = relayMessages.getMessages();
        BlockUpdate blockUpdate = null;
        List<byte[]> msgList = new ArrayList<>();
        for (RelayMessage.TypePrefixedMessage message : typePrefixedMessages) {
            Object msg = message.getMessage();
            if (msg instanceof BlockUpdate) {
                blockUpdate = (BlockUpdate) msg;
                handleBlockUpdateMessage(blockUpdate);
            } else if (msg instanceof MessageProof) {
                var msgs = handleMessageProof((MessageProof) msg, blockUpdate);
                for(byte[] m : msgs) {
                    msgList.add(m);
                }
            }
        }
        var retSize = msgList.size();
        var ret = new byte[retSize][];
        if (retSize > 0) {
            for (int i = 0; i < retSize; i ++) {
                ret[i] = msgList.get(i);
            }
        }
        return ret;
    }

    @External(readonly = true)
    public BMVStatus getStatus() {
        var properties = getProperties();
        BMVStatus s = new BMVStatus();
        s.setHeight(properties.getHeight().longValue());
        s.setExtra(new BMVStatusExtra(
                properties.getSequenceOffset(),
                properties.getLastFirstMessageSN(),
                properties.getLastMessageCount()).toBytes());
        return s;
    }

    private void handleFirstBlockHeader(BlockHeader blockHeader, BMVProperties bmvProperties) {
        var prev = blockHeader.getPrev();
        if (prev != null) throw BMVException.unknown("not first blockUpdate");
        var updateNumber = blockHeader.getUpdateNumber();
        var blockUpdateNid = blockHeader.getNid();
        var msgCnt = blockHeader.getMessageCount();
        var msgRoot = blockHeader.getMessageRoot();
        NetworkSection ns = new NetworkSection(
                blockUpdateNid,
                updateNumber,
                null,
                msgCnt,
                msgRoot
        );
        var nsHash = ns.hash();
        var nextProofContextHash = blockHeader.getNextProofContextHash();
        var nextProofContext = blockHeader.getNextProofContext();
        if (!Arrays.equals(hash(nextProofContext), nextProofContextHash))
            throw BMVException.unknown("mismatch Hash of proofContext");
        bmvProperties.setNetworkID(blockUpdateNid);
        bmvProperties.setProofContextHash(nextProofContextHash);
        bmvProperties.setProofContext(nextProofContext);
        bmvProperties.setLastNetworkSectionHash(nsHash);
        bmvProperties.setLastSequence(BigInteger.ZERO);
        bmvProperties.setLastMessagesRoot(msgRoot);
        bmvProperties.setLastMessageCount(msgCnt);
        bmvProperties.setLastFirstMessageSN(blockHeader.getFirstMessageSn());
        bmvProperties.setHeight(blockHeader.getMainHeight());
        propertiesDB.set(bmvProperties);
    }

    private void handleBlockUpdateMessage(BlockUpdate blockUpdate) {
        var bmvProperties = getProperties();
        var networkID = bmvProperties.getNetworkID();
        var blockHeader = blockUpdate.getBlockHeader();
        var updateNumber = blockHeader.getUpdateNumber();
        var blockUpdateNid = blockHeader.getNid();
        var prev = blockHeader.getPrev();
        var firstMessageSn = bmvProperties.getLastFirstMessageSN();
        var messageCount = bmvProperties.getLastMessageCount();
        var seqOffset = bmvProperties.getSequenceOffset();
        var messageSn = firstMessageSn.add(messageCount).subtract(seqOffset);
        if (messageSn.compareTo(blockHeader.getFirstMessageSn()) > 0) {
            throw BMVException.alreadyVerified("already verified");
        } else if (messageSn.compareTo(blockHeader.getFirstMessageSn()) < 0) {
            throw BMVException.notVerifiable("not verifiable blockUpdate");
        }
        if (bmvProperties.getRemainMessageCount().compareTo(BigInteger.ZERO) != 0) throw BMVException.unknown("remain must be zero");
        if (networkID.compareTo(blockUpdateNid) != 0) throw BMVException.unknown("invalid network id");
        if (!Arrays.equals(bmvProperties.getLastNetworkSectionHash(), prev)) throw BMVException.unknown("mismatch networkSectionHash");
        NetworkSection ns = new NetworkSection(
                blockUpdateNid,
                updateNumber,
                prev,
                blockHeader.getMessageCount(),
                blockHeader.getMessageRoot()
        );
        var nsHash = ns.hash();
        var nsRoot = blockHeader.getNetworkSectionsRoot(nsHash);
        var nextProofContextHash = blockHeader.getNextProofContextHash();
        NetworkTypeSection nts = new NetworkTypeSection(nextProofContextHash, nsRoot);
        var srcNetworkID = bmvProperties.getSrcNetworkID();
        var networkTypeID = bmvProperties.getNetworkTypeID();
        var height = blockHeader.getMainHeight();
        var round = blockHeader.getRound();
        var ntsHash = nts.hash();
        NetworkTypeSectionDecision decision = new NetworkTypeSectionDecision(
                srcNetworkID, networkTypeID, height.longValue(), round.intValue(), ntsHash);
        Proofs proofs = Proofs.fromBytes(blockUpdate.getBlockProof());
        var isUpdate = updateNumber.and(BigInteger.ONE).compareTo(BigInteger.ONE) == 0;
        verifyProof(decision, proofs);
        if (isUpdate) {
            var nextProofContext = blockHeader.getNextProofContext();
            verifyProofContextData(nextProofContextHash, nextProofContext, bmvProperties.getProofContextHash());
            bmvProperties.setProofContextHash(nextProofContextHash);
            bmvProperties.setProofContext(nextProofContext);
        }
        bmvProperties.setLastMessagesRoot(blockHeader.getMessageRoot());
        bmvProperties.setLastMessageCount(blockHeader.getMessageCount());
        bmvProperties.setLastFirstMessageSN(blockHeader.getFirstMessageSn());
        bmvProperties.setLastNetworkSectionHash(nsHash);
        bmvProperties.setHeight(blockHeader.getMainHeight());
        propertiesDB.set(bmvProperties);
    }

    private void verifyProofContextData(byte[] proofContextHash, byte[] proofContext, byte[] currentProofContextHash) {
        if (Arrays.equals(currentProofContextHash, proofContextHash)) throw BMVException.unknown("mismatch UpdateFlag");
        if (!Arrays.equals(hash(proofContext), proofContextHash)) throw BMVException.unknown("mismatch Hash of NextProofContext");
    }

    private void verifyProof(NetworkTypeSectionDecision decision, Proofs proofs) {
        byte[] decisionHash = decision.hash();
        byte[][] sigs = proofs.getProofs();
        List<EthAddress> verifiedValidator = new ArrayList<>();
        var bmvProperties = getProperties();
        var proofContextBytes = bmvProperties.getProofContext();
        var proofContext = ProofContext.fromBytes(proofContextBytes);
        for (byte[] sig : sigs) {
            if (sig == null){
                continue;
            }
            EthAddress address = recoverAddress(decisionHash, sig);
            if (!proofContext.isValidator(address)) throw BMVException.unknown("invalid validator : " + address);
            if (verifiedValidator.contains(address)) throw BMVException.unknown("duplicated validator : " + address);
            verifiedValidator.add(address);
        }
        var verified = verifiedValidator.size();
        var validatorsCnt = proofContext.getValidators().length;
        //quorum = validator * 2/3
        if (verified * 3 <= validatorsCnt * 2)
            throw BMVException.unknown("not enough proof parts num of validator : " + validatorsCnt + ", num of proof parts : " + verified);
    }

    private byte[][] handleMessageProof(MessageProof messageProof, BlockUpdate blockUpdate) {
        byte[] expectedMessageRoot;
        BigInteger expectedMessageCnt;
        var bmvProperties = getProperties();
        if (bmvProperties.getRemainMessageCount().compareTo(BigInteger.ZERO) <= 0)
            throw BMVException.unknown("remaining message count must greater than zero");
        MessageProof.ProveResult result = messageProof.proveMessage();
        if (bmvProperties.getProcessedMessageCount().compareTo(BigInteger.valueOf(result.offset)) != 0)
            throw BMVException.unknown("invalid ProofInLeft.NumberOfLeaf");
        if (blockUpdate != null ) {
            var blockHeader = blockUpdate.getBlockHeader();
            expectedMessageRoot = blockHeader.getMessageRoot();
            expectedMessageCnt = blockHeader.getMessageCount();
            if (0 < result.offset) {
                throw BMVException.unknown("ProofInLeft should be empty");
            }
        } else {
            expectedMessageRoot = bmvProperties.getLastMessagesRoot();
            expectedMessageCnt = bmvProperties.getLastMessageCount();
            if (bmvProperties.getLastSequence().subtract(bmvProperties.getLastFirstMessageSN()).intValue() != result.offset) {
                throw BMVException.unknown("mismatch ProofInLeft");
            }
        }
        if (expectedMessageCnt.intValue() != result.total) {
            var rightProofNodes = messageProof.getRightProofNodes();
            for (int i = 0; i < rightProofNodes.length; i++) {
                logger.println("ProofInRight["+i+"] : " + "NumOfLeaf:"+rightProofNodes[i].getNumOfLeaf()
                        + "value:" + StringUtil.bytesToHex(rightProofNodes[i].getValue()));
            }
            throw BMVException.unknown(
                    "mismatch MessageCount offset:" + result.offset +
                            ", expected:" + expectedMessageCnt +
                            ", count :" + result.total);
        }
        if (!Arrays.equals(result.hash, expectedMessageRoot)) throw BMVException.unknown("mismatch MessagesRoot");
        var msgCnt = messageProof.getMessages().length;
        var remainCnt = result.total - result.offset - msgCnt;
        if (remainCnt == 0) {
            bmvProperties.setLastMessagesRoot(null);
        }
        bmvProperties.setLastSequence(bmvProperties.getLastSequence().add(BigInteger.valueOf(msgCnt)));
        propertiesDB.set(bmvProperties);
        return messageProof.getMessages();
    }

    static byte[] hash(byte[] msg) {
        return Context.hash(HASH, msg);
    }

    static EthAddress recoverAddress(byte[] msg, byte[] sig) {
        byte[] publicKey = Context.recoverKey(SIGNATURE_ALG, msg, sig, false);
        int sliceLen = publicKey.length - 1;
        byte[] sliced = new byte[sliceLen];
        System.arraycopy(publicKey, 1, sliced, 0, sliceLen);
        byte[] hashed = hash(sliced);
        byte[] addr = new byte[20];
        System.arraycopy(hashed, 12, addr, 0, 20);
        return new EthAddress(addr);
    }

    private void checkAccessible(BTPAddress curAddr, BTPAddress fromAddress) {
        BMVProperties properties = getProperties();
        if (!properties.getNetwork().equals(fromAddress.net())) {
            throw BMVException.unknown("invalid prev bmc");
        } else if (!Context.getCaller().equals(properties.getBmc())) {
            throw BMVException.unknown("invalid caller bmc");
        } else if (!Address.fromString(curAddr.account()).equals(properties.getBmc())) {
            throw BMVException.unknown("invalid current bmc");
        }
    }
}
