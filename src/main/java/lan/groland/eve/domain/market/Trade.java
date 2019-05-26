package lan.groland.eve.domain.market;

import java.util.Optional;

public interface Trade {

  double getBenefParJour();

  int volume();

  double capital();

  double unitSoldDay();

  double lastMonthMargin();

  double expectedMargin();

  double benefit();

  /**
   * Adjust the trade so that fulfilling it does not requires more than {@code maxCash}
   * @param maxCash investment threshold
   * @return an adjusted trade or empty
   */
  Optional<Trade> ajust(double maxCash);

  String multiBuyString();

}