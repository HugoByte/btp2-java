/*
 * Copyright 2023 ICON Foundation
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

package foundation.icon.btp.bmv.eth2;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class BMVProperties {
    public static final BMVProperties DEFAULT;

    static {
        DEFAULT = new BMVProperties();
    }

    private byte[] srcNetworkID;
    private byte[] genesisValidatorsHash;
    private byte[] currentSyncCommittee;
    private byte[] nextSyncCommittee;
    private Address bmc;
    private LightClientHeader finalizedHeader;
    private BigInteger lastMsgSeq;
    private BigInteger lastMsgSlot;

    public byte[] getSrcNetworkID() {
        return srcNetworkID;
    }

    public void setSrcNetworkID(byte[] srcNetworkID) {
        this.srcNetworkID = srcNetworkID;
    }

    Address getBmc() {
        return bmc;
    }

    void setBmc(Address bmc) {
        this.bmc = bmc;
    }

    byte[] getCurrentSyncCommittee() {
        return currentSyncCommittee;
    }

    void setCurrentSyncCommittee(byte[] syncCommittee) {
        this.currentSyncCommittee = syncCommittee;
    }

    public byte[] getNextSyncCommittee() {
        return nextSyncCommittee;
    }

    public void setNextSyncCommittee(byte[] nextSyncCommittee) {
        this.nextSyncCommittee = nextSyncCommittee;
    }

    public void setNextSyncCommittee(SyncCommittee nextSyncCommittee) {
        byte[] value = nextSyncCommittee != null ? SyncCommittee.serialize(nextSyncCommittee) : null;
        setNextSyncCommittee(value);
    }

    byte[] getGenesisValidatorsHash() {
        return genesisValidatorsHash;
    }

    void setGenesisValidatorsHash(byte[] genesisValidatorsHash) {
        this.genesisValidatorsHash = genesisValidatorsHash;
    }

    LightClientHeader getFinalizedHeader() {
        return finalizedHeader;
    }

    void setFinalizedHeader(LightClientHeader finalizedHeader) {
        this.finalizedHeader = finalizedHeader;
    }

    public BigInteger getLastMsgSlot() {
        return lastMsgSlot;
    }

    public void setLastMsgSlot(BigInteger lastMsgSlot) {
        this.lastMsgSlot = lastMsgSlot;
    }

    public BigInteger getLastMsgSeq() {
        return lastMsgSeq;
    }

    public void setLastMsgSeq(BigInteger lastMsgSeq) {
        this.lastMsgSeq = lastMsgSeq;
    }

    public String getNetwork() {
        var stringSrc = new String(srcNetworkID);
        var delimIndex = stringSrc.lastIndexOf("/");
        return stringSrc.substring(delimIndex + 1);
    }

    public static BMVProperties readObject(ObjectReader r) {
        r.beginList();
        var object = new BMVProperties();
        object.setSrcNetworkID(r.readByteArray());
        object.setGenesisValidatorsHash(r.readByteArray());
        object.setCurrentSyncCommittee(r.readByteArray());
        object.setNextSyncCommittee(r.readNullable(byte[].class));
        object.setBmc(r.readAddress());
        object.setFinalizedHeader(r.read(LightClientHeader.class));
        object.setLastMsgSlot(r.readBigInteger());
        object.setLastMsgSeq(r.readBigInteger());
        r.end();
        return object;
    }

    public static void writeObject(ObjectWriter w, BMVProperties obj) {
        w.beginList(9);
        w.write(obj.srcNetworkID);
        w.write(obj.genesisValidatorsHash);
        w.write(obj.currentSyncCommittee);
        w.writeNullable(obj.nextSyncCommittee);
        w.write(obj.bmc);
        w.write(obj.finalizedHeader);
        w.write(obj.lastMsgSlot);
        w.write(obj.lastMsgSeq);
        w.end();
    }
}
