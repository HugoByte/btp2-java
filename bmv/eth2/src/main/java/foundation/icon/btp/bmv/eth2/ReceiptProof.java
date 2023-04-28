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

import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;

import scorex.util.ArrayList;
import java.util.List;

public class ReceiptProof {
    private byte[] key;
    private byte[] proof;

    public ReceiptProof(byte[] key, byte[] proof) {
        this.key = key;
        this.proof = proof;
    }

    byte[] getKey() {
        return key;
    }

    public byte[][] getProofs() {
        ObjectReader r = Context.newByteArrayObjectReader("RLPn", proof);
        r.beginList();
        byte[][] proofs;
        List<byte[]> encodedProof = new ArrayList<>();
        while(r.hasNext()) {
            List<byte[]> proofList = new ArrayList<>();
            r.beginList();
            while(r.hasNext()) {
                proofList.add(r.readByteArray());
            }
            r.end();
            ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
            w.beginList(proofList.size());
            for (byte[] bytes : proofList)
                w.write(bytes);
            w.end();
            encodedProof.add(w.toByteArray());
        }
        r.end();
        var encodedProofLength = encodedProof.size();
        proofs = new byte[encodedProofLength][];
        for (int i = 0; i < encodedProofLength; i++)
            proofs[i] = encodedProof.get(i);
        return proofs;
    }

    public static ReceiptProof readObject(ObjectReader r) {
        r.beginList();
        var key = r.readByteArray();
        var proof = r.readByteArray();
        r.end();
        return new ReceiptProof(key, proof);
    }

    @Override
    public String toString() {
        return "ReceiptProof{" +
                "key=" + StringUtil.toString(key) +
                ", proof=" + StringUtil.toString(proof) +
                '}';
    }
}
