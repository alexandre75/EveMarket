package lan.groland.eve.adapter.port.messaging;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import lan.groland.eve.domain.market.Trade;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

public class TradesTranslator {
  private JsonGenerator output;

  public TradesTranslator(OutputStream out) {
    output = Json.createGenerator(out);
  }

  public void write(Collection<Trade> trades) {
    output.writeStartObject();
    output.writeStartArray("trades");
    for (Trade trade : trades){
      output.writeStartObject();
      output.write("typeid", trade.item().getItemId().typeId());
      output.write("name", trade.item().getName());
      output.write("quantity", trade.quantiteAAcheter());
      output.writeEnd();
    }
    output.writeEnd();
    output.writeEnd();
    output.flush();
  }
}
