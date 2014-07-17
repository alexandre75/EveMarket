package lan.groland.eve.market;

public class OrderStats {
	private int nbTraders;
	private float bid;
	
	public OrderStats(int nbTraders, float f) {
		this.nbTraders = nbTraders;
		this.bid = f;
	}

	public float getBid() {
		return bid;
	}

	public int nbSellOrders() {
		return nbTraders;
	}

}
