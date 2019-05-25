package lan.groland.eve.domain.market;

import java.text.NumberFormat;

public class Trade {
	private static final NumberFormat INTEGER_INSTANCE = NumberFormat.getIntegerInstance();
	{
		INTEGER_INSTANCE.setGroupingUsed(false);
	}
	
	private double euristic;
	private double quantitéJounalière;
	private double sellPrice;
	private double buyPrice;
	private Item item;
	
	public Trade(Item item, double euristic, double quantitéJounalière, double sellPrice, double buyPrice) {
		this.item = item;
		this.euristic = euristic;
		this.quantitéJounalière = quantitéJounalière;
		this.sellPrice = sellPrice;
		this.buyPrice = buyPrice;				
	}

	public String toString(){
		return item.getName() + "(" + item.getId() +") , " + quantitéJounalière + "," +buyPrice + ","+sellPrice + "," + euristic + "," + item.getVolume();
		
	}
	
	public String multiBuyString(){
		return item.getName() + " x"+ INTEGER_INSTANCE.format(quantitéJounalière);
	}
	
	public double getBenefParJour(){
		return euristic;
	}
	
	public double benefice(){
		return (sellPrice - buyPrice) *quantitéJounalière;
	}

	public double volume() {
		return quantitéJounalière * item.getVolume();
	}

	public double capital() {
		return buyPrice * quantitéJounalière;
	}

	public boolean ajust(double capMax) {
		if (capital() < capMax) return true;
		
		double newQty = Math.floor(capMax / buyPrice);
		if (newQty < 1) {
			return false;
		}
		euristic = euristic /quantitéJounalière*newQty;
		quantitéJounalière = newQty;
		return true;
	}
}
