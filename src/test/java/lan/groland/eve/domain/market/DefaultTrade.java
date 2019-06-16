package lan.groland.eve.domain.market;

import java.util.Comparator;
import java.util.Optional;

public class DefaultTrade implements Trade {
  private static final Comparator<Trade> TRADE_COMPARATOR = Comparator.comparing(Trade::dailyBenefit);

  @Override
  public int compareTo(Trade arg0) {
    return TRADE_COMPARATOR.compare(this, arg0);
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
    return Optional.of(this);
  }

  @Override
  public int quantiteAAcheter() {
    return 0;
  }

  @Override
  public Item item() {
    return null;
  }
}
