package foundation.icon.btp.xcall.sample;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class MessageData {
    private final BigInteger id;
    private final String message;
    private BigInteger ssn;
    private final String sender;
    private final String recipient;
    private int offset;
    private int length;

    public MessageData(BigInteger id,  String sender, String recipient, String message,int offset, int length) {
        this.id = id;
        this.message = message;
        this.sender = sender;
        this.recipient = recipient;
        this.offset = offset;
        this.length = length;
    }

    public BigInteger getId() {
        return id;
    }
    public int getLength() {
        return length;
    }
    public int getOffset() {
        return offset;
    }

    public String getMessage() {
        return message;
    }

    public BigInteger getSvcSn() {
        return ssn;
    }

    public void setSvcSn(BigInteger ssn) {
        this.ssn = ssn;
    }

    public static void writeObject(ObjectWriter w, MessageData data) {
        w.beginList(3);
        w.write(data.id);
        w.write(data.message);
        w.writeNullable(data.ssn);
        w.end();
    }

    public static MessageData readObject(ObjectReader r) {
        r.beginList();
        MessageData rbData = new MessageData(
                r.readBigInteger(),
                r.readString(),
                r.readString(),
                r.readString(),
                r.readInt(),
                r.readInt()
        );
        rbData.setSvcSn(r.readNullable(BigInteger.class));
        r.end();
        return rbData;
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writeObject(writer, this);
        return writer.toByteArray();
    }

    public static MessageData fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return readObject(reader);
    }

    // @Override
    // public boolean equals(Object obj) {
    //     if (this == obj) {
    //         return true;
    //     } else if (!(obj instanceof MessageData)) {
    //         return false;
    //     } else {
    //         MessageData other = (MessageData) obj;
    //         if (this.message == null || other.message == null) {
    //             return false;
    //         }
    //         if (this.message.length != other.message.length) {
    //             return false;
    //         }
    //         for (int i = 0; i < this.message.length; i++) {
    //             if (this.message[i] != other.message[i]) {
    //                 return false;
    //             }
    //         }
    //         return true;
    //     }
    // }
}
