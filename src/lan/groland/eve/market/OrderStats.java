package lan.groland.eve.market;

public class OrderStats {
	private int nbTraders;
	private float bid;
	
	
	/**
	 * Number of <b>active</b> traders (buy orders).
	 * @return
	 */
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

	/**
	 * Number of <b>active</b> traders (sell orders).
	 * @return
	 */
	public int nbSellOrders() {
		return nbTraders;
	}

}
