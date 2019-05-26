package lan.groland.eve.domain.market;

import javax.annotation.concurrent.Immutable;

@Immutable
public class Sales {
  public final double quantity;
  public final double price;
  
  public Sales(double quantity, double price) {
    this.quantity = quantity;
    this.price = price;
  }

  public double getQuantity() {
    return quantity;
  }

  public double getPrice() {
    return price;
  }

  @Override
  public String toString() {
    return "Sales [quantity=" + quantity + ", price=" + price + "]";
  }
}
