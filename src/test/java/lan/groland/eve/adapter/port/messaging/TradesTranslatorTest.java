package lan.groland.eve.adapter.port.messaging;

import com.google.common.collect.ImmutableList;
import lan.groland.eve.domain.market.Item;
import lan.groland.eve.domain.market.Trade;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.testng.Assert.*;

public class TradesTranslatorTest {

  @Test
  public void testWrite() {
    Trade trade1 = new Trade(){

      @Override
      public int compareTo(Trade trade) {
        return 0;
      }

      @Override
      public double dailyBenefit() {
        return 0;
      }

      @Override
      public double volume() {
        return 0;
      }

      @Override
      public double capital() {
        return 0;
      }

      @Override
      public double unitSoldDay() {
        return 0;
      }

      @Override
      public double lastMonthMargin() {
        return 0;
      }

      @Override
      public double expectedMargin() {
        return 0;
      }

      @Override
      public Optional<Trade> adjust(double maxCash) {
        return Optional.empty();
      }

      @Override
      public int quantiteAAcheter() {
        return 0;
      }

      @Override
      public Item item() {
        return new Item(3, "alex", 6);
      }
    };

    Collection<Trade> trades = ImmutableList.of(trade1, trade1);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    TradesTranslator translator = new TradesTranslator(output);
    translator.write(trades);
    String res = new String(output.toByteArray(), StandardCharsets.UTF_8);

    Assert.assertThat(res, is("{\"trades\":[{\"typeid\":3,\"name\":\"alex\",\"quantity\":0},{\"typeid\":3,\"name\":\"alex\",\"quantity\":0}]}"));
  }
}