package lan.groland.eve.market;

public class OrderStats {
	private int nbTraders;
	private float bid;
	public int getNbAsks() {
		return nbAsks;
	}

	public float getAsk() {
		return ask;
	}

	private int nbAsks;
	private float ask;
	
	public OrderStats(int nbTraders, float f, int nbAsks, float a) {
		this.nbTraders = nbTraders;
		this.bid = f;
		this.nbAsks = nbAsks;
		this.ask = a;
	}

	public float getBid() {
		return bid;
	}

	public int nbSellOrders() {
		return nbTraders;
	}

}
