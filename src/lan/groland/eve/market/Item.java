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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Item other = (Item) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
}
