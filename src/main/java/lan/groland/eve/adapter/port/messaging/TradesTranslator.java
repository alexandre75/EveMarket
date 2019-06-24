package lan.groland.eve.adapter.port.messaging;

import java.io.ByteArrayOutputStream;
import java.util.Collection;

import lan.groland.eve.domain.market.Trade;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

public class TradesTranslator {
  private Collection<Trade> trades;

  public TradesTranslator(Collection<Trade> optimizeCargo) {
    trades = optimizeCargo;
  }

  public byte[] toBytes() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    JsonGenerator gen = Json.createGenerator(out);
    gen.writeStartObject();
    gen.writeStartArray("trades");
    gen.writeStartArray();
    for (Trade trade : trades){
      gen.writeStartObject();
      gen.write("typeid", trade.item().getItemId().typeId());
      gen.write("name", trade.item().getName());
      gen.write("quantity", trade.quantiteAAcheter());
      gen.writeEnd();
    }
    gen.writeEnd();
    gen.writeEnd();
    return out.toByteArray();
  }

}
