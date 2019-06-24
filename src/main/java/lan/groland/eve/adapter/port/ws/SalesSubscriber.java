package lan.groland.eve.adapter.port.ws;

import java.io.ByteArrayInputStream;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Subscription;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import lan.groland.eve.domain.market.Sales;

public class SalesSubscriber implements BodySubscriber<Sales> {

  private List<ByteBuffer> buf = new ArrayList<>();
  
  private final CompletableFuture<Sales> result = new CompletableFuture<>();
  
  @Override
  public void onComplete() {
    JsonReader historiesReader = Json.createReader(new ByteArrayInputStream(join(buf)));
    JsonObject historiesObj = historiesReader.readObject();
    Sales sales = new Sales(historiesObj.getJsonNumber("quantity").doubleValue(),
                            historiesObj.getJsonNumber("median").doubleValue());
    result.complete(sales);
  }

  private static byte[] join(List<ByteBuffer> bytes) {
    int size = bytes.stream().mapToInt(ByteBuffer::remaining).sum();
    byte[] res = new byte[size];

    int from = 0;
    for (ByteBuffer buf : bytes) {
        int read = buf.remaining();
        buf.get(res, from, read);
        from += read;
    }
    return res;
  }

  @Override
  public void onError(Throwable arg0) {
    buf.clear();
    result.completeExceptionally(arg0);
  }

  @Override
  public void onNext(List<ByteBuffer> arg0) {
    buf.addAll(arg0);
  }

  @Override
  public void onSubscribe(Subscription arg0) {
    arg0.request(Long.MAX_VALUE);
  }

  @Override
  public CompletionStage<Sales> getBody() {
    return result;
  }

}
