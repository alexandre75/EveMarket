package lan.groland.eve.domain.market;

import java.util.Optional;

public interface Trade extends Comparable<Trade> {

  double dailyBenefit();

  double volume();

  double capital();

  double unitSoldDay();

  double lastMonthMargin();

  double expectedMargin();

  /**
   * Adjust the trade so that fulfilling it does not requires more than {@code maxCash}
   * @param maxCash investment threshold
   * @return an adjusted trade or empty
   */
  Optional<Trade> adjust(double maxCash);
  
  int quantiteAAcheter();

  Item item();
}