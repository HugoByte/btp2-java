package foundation.icon.btp.xcall.sample;

import foundation.icon.btp.xcall.CallServiceReceiver;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconStringConverter;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.Wallet;
import java.lang.Object;
import java.lang.String;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;
import score.annotation.Optional;

public final class DAppProxySampleScoreClient extends DefaultScoreClient implements CallServiceReceiver {
  public DAppProxySampleScoreClient(String url, BigInteger nid, Wallet wallet, Address address) {
    super(url, nid, wallet, address);
  }

  public DAppProxySampleScoreClient(String url, BigInteger nid, BigInteger stepLimit, Wallet wallet,
      Address address) {
    super(url, nid, stepLimit, wallet, address);
  }

  public DAppProxySampleScoreClient(DefaultScoreClient client) {
    super(client);
  }

  public DAppProxySampleScoreClient(DefaultScoreClient client, Wallet wallet) {
    super(client, wallet);
  }

  public static DAppProxySampleScoreClient _of(Properties properties) {
    return _of("", properties);
  }

  public static DAppProxySampleScoreClient _of(String prefix, Properties properties) {
    return new DAppProxySampleScoreClient(DefaultScoreClient.of(prefix, properties));
  }

  /**
   * To payable, use sendMessage(BigInteger valueForPayable, ...)
   */
  public void sendMessage(String _to, int _type, String _data, @Optional byte[] _rollback) {
    Map<String,Object> params = new HashMap<>();
    params.put("_to",_to);
    params.put("_type",_type);
    params.put("_data",_data);
    params.put("_rollback",_rollback);
    super._send("sendMessage", params);
  }

  public void sendMessage(Consumer<TransactionResult> consumerFunc, String _to, int _type,
      String _data, @Optional byte[] _rollback) {
    Map<String,Object> params = new HashMap<>();
    params.put("_to",_to);
    params.put("_type",_type);
    params.put("_data",_data);
    params.put("_rollback",_rollback);
    consumerFunc.accept(super._send("sendMessage", params));
  }

  public void sendMessage(BigInteger valueForPayable, String _to, int _type, String _data,
      @Optional byte[] _rollback) {
    Map<String,Object> params = new HashMap<>();
    params.put("_to",_to);
    params.put("_type",_type);
    params.put("_data",_data);
    params.put("_rollback",_rollback);
    super._send(valueForPayable, "sendMessage", params);
  }

  public void sendMessage(Consumer<TransactionResult> consumerFunc, BigInteger valueForPayable,
      String _to, int _type, String _data, @Optional byte[] _rollback) {
    Map<String,Object> params = new HashMap<>();
    params.put("_to",_to);
    params.put("_type",_type);
    params.put("_data",_data);
    params.put("_rollback",_rollback);
    consumerFunc.accept(super._send(valueForPayable, "sendMessage", params));
  }

  public void handleCallMessage(String _from, byte[] _data) {
    Map<String,Object> params = new HashMap<>();
    params.put("_from",_from);
    params.put("_data",_data);
    super._send("handleCallMessage", params);
  }

  public void handleCallMessage(Consumer<TransactionResult> consumerFunc, String _from,
      byte[] _data) {
    Map<String,Object> params = new HashMap<>();
    params.put("_from",_from);
    params.put("_data",_data);
    consumerFunc.accept(super._send("handleCallMessage", params));
  }

  public Consumer<TransactionResult> MessageReceived(Consumer<List<MessageReceived>> consumerFunc,
      Predicate<MessageReceived> filter) {
    return (txr) -> consumerFunc.accept(MessageReceived.eventLogs(txr, this.address, filter));
  }

  public Consumer<TransactionResult> RollbackDataReceived(
      Consumer<List<RollbackDataReceived>> consumerFunc, Predicate<RollbackDataReceived> filter) {
    return (txr) -> consumerFunc.accept(RollbackDataReceived.eventLogs(txr, this.address, filter));
  }

  public static DAppProxySampleScoreClient _deploy(String url, BigInteger nid, Wallet wallet,
      String scoreFilePath, score.Address _callService) {
    Map<String,Object> params = new HashMap<>();
    params.put("_callService",_callService);
    return new DAppProxySampleScoreClient(DefaultScoreClient._deploy(url,nid,wallet,scoreFilePath,params));
  }

  public static DAppProxySampleScoreClient _of(String prefix, Properties properties,
      score.Address _callService) {
    Map<String,Object> params = new HashMap<>();
    params.put("_callService",_callService);
    return new DAppProxySampleScoreClient(DefaultScoreClient.of(prefix, properties, params));
  }

  public static class MessageReceived {
    public static final String SIGNATURE = "MessageReceived(str,bytes)";

    public static final int INDEXED = 0;

    private final String _from;

    private final byte[] _data;

    public MessageReceived(TransactionResult.EventLog el) {
      List<String> indexed = el.getIndexed();
      List<String> data = el.getData();
      this._from=data.get(0);
      this._data=IconStringConverter.toBytes(data.get(1));
    }

    public String get_from() {
      return this._from;
    }

    public byte[] get_data() {
      return this._data;
    }

    public static List<MessageReceived> eventLogs(TransactionResult txr, Address address,
        Predicate<MessageReceived> filter) {
      return DefaultScoreClient.eventLogs(txr, SIGNATURE, address, MessageReceived::new, filter);
    }
  }

  public static class RollbackDataReceived {
    public static final String SIGNATURE = "RollbackDataReceived(str,int,bytes)";

    public static final int INDEXED = 0;

    private final String _from;

    private final BigInteger _ssn;

    private final byte[] _rollback;

    public RollbackDataReceived(TransactionResult.EventLog el) {
      List<String> indexed = el.getIndexed();
      List<String> data = el.getData();
      this._from=data.get(0);
      this._ssn=IconStringConverter.toBigInteger(data.get(1));
      this._rollback=IconStringConverter.toBytes(data.get(2));
    }

    public String get_from() {
      return this._from;
    }

    public BigInteger get_ssn() {
      return this._ssn;
    }

    public byte[] get_rollback() {
      return this._rollback;
    }

    public static List<RollbackDataReceived> eventLogs(TransactionResult txr, Address address,
        Predicate<RollbackDataReceived> filter) {
      return DefaultScoreClient.eventLogs(txr, SIGNATURE, address, RollbackDataReceived::new, filter);
    }
  }
}
