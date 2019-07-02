package lan.groland.eve.bootstrap;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import lan.groland.eve.domain.market.Trade;

public class TradeFormat extends Format {

  private static final TradeFormat INSTANCE = new TradeFormat();
  private static final long serialVersionUID = 6153665476511594948L;

  @Override
  public StringBuffer format(Object arg0, StringBuffer sb, FieldPosition arg2) {
    Trade trade = (Trade) arg0;
    return sb.append(trade.item().getName())
             .append(" x").append(trade.quantiteAAcheter());
  }

  @Override
  public Object parseObject(String arg0, ParsePosition arg1) {
    throw new UnsupportedOperationException("parseObject");
  }
  
  public static TradeFormat getInstance() {
    return INSTANCE;
  }
}
