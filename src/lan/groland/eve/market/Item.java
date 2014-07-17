package lan.groland.eve.market;

public class Item {
	private int id;
	private String name;
	private double volume;
	
	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public double getVolume() {
		return volume;
	}

	public Item(int id, String name, double volume){
		this.id = id;
		this.name = name;
		this.volume = volume;
	}
}
