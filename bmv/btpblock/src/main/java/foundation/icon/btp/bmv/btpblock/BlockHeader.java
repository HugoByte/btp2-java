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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

public class BlockHeader {
    private static final int HASH_LEN = 32;
    private BigInteger mainHeight;
    private BigInteger round;
    private byte[] nextProofContextHash;
    private NetworkSectionToRoot[] networkSectionToRoot;
    private BigInteger nid;
    private BigInteger updateNumber;
    private byte[] prev;
    private BigInteger messageCount;
    private byte[] messageRoot;
    private byte[] nextProofContext;

    public BigInteger getMainHeight() {
        return mainHeight;
    }

    public BigInteger getRound() {
        return round;
    }

    public byte[] getNextProofContextHash() {
        return nextProofContextHash;
    }

    public NetworkSectionToRoot[] getNetworkSectionToRoot() {
        return networkSectionToRoot;
    }

    public BigInteger getNid() {
        return nid;
    }

    public BigInteger getUpdateNumber() {
        return updateNumber;
    }

    public BigInteger getFirstMessageSn() {
        return updateNumber.shiftRight(1);
    }

    public byte[] getPrev() {
        return prev;
    }

    public BigInteger getMessageCount() {
        return messageCount;
    }

    public byte[] getMessageRoot() {
        return messageRoot;
    }

    public byte[] getNextProofContext() {
        return nextProofContext;
    }

    public BlockHeader(
            BigInteger mainHeight,
            BigInteger round,
            byte[] nextProofContextHash,
            NetworkSectionToRoot[] networkSectionToRoot,
            BigInteger nid,
            BigInteger updateNumber,
            byte[] prev,
            BigInteger messageCount,
            byte[] messageRoot,
            byte[] nextProofContext
    ) {
        this.mainHeight = mainHeight;
        this.round = round;
        this.nextProofContextHash = nextProofContextHash;
        this.networkSectionToRoot = networkSectionToRoot;
        this.nid = nid;
        this.updateNumber = updateNumber;
        this.prev = prev;
        this.messageCount = messageCount;
        this.messageRoot = messageRoot;
        this.nextProofContext = nextProofContext;
    }

    public static BlockHeader readObject(ObjectReader r) {
        r.beginList();
        var mainHeight = r.readNullable(BigInteger.class);
        var round = r.readNullable(BigInteger.class);
        var nextProofContextHash = r.readNullable(byte[].class);
        r.beginList();
        NetworkSectionToRoot[] networkSectionToRoot;
        List<NetworkSectionToRoot> nstoRootList = new ArrayList<>();
        while(r.hasNext()) {
            nstoRootList.add(r.read(NetworkSectionToRoot.class));
        }
        networkSectionToRoot = new NetworkSectionToRoot[nstoRootList.size()];
        for(int i = 0; i < nstoRootList.size(); i++) {
            networkSectionToRoot[i] = nstoRootList.get(i);
        }
        r.end();
        var nid = r.readBigInteger();
        var updateNumber = r.readBigInteger();
        var prev = r.readNullable(byte[].class);
        var messageCount = r.readBigInteger();
        var messageRoot = r.readNullable(byte[].class);
        var nextProofContext = r.readNullable(byte[].class);
        r.end();
        return new BlockHeader(
                mainHeight,
                round,
                nextProofContextHash,
                networkSectionToRoot,
                nid,
                updateNumber,
                prev,
                messageCount,
                messageRoot,
                nextProofContext
        );
    }

    public static BlockHeader fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BlockHeader.readObject(reader);
    }

    private static byte[] concatAndHash(byte[] b1, byte[] b2) {
        byte[] data = new byte[b1.length + b2.length];
        System.arraycopy(b1, 0, data, 0, b1.length);
        if (b2.length == 0){
            return b1;
        }
        System.arraycopy(b2, 0, data, b1.length, b2.length);
        return BTPMessageVerifier.hash(data);
    }

    public byte[] getNetworkSectionsRoot(byte[] leaf) {
        byte[] h = leaf;
        for (NetworkSectionToRoot nsRoot : networkSectionToRoot) {
            if (nsRoot.dir == NetworkSectionToRoot.LEFT) {
                h = concatAndHash(nsRoot.value, h);
            } else if (nsRoot.dir == NetworkSectionToRoot.RIGHT) {
                h = concatAndHash(h, nsRoot.value);
            }
        }
        return h;
    }
}
