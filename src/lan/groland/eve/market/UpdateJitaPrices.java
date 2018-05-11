package lan.groland.eve.market;

import io.swagger.client.ApiException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;

public class UpdateJitaPrices {

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws ApiException 
	 */
	public static void main(String[] args) throws SQLException, IOException, InterruptedException, ApiException {
		try (Connection conn = DriverManager.getConnection(
				"jdbc:mysql://192.168.45.112:3306/eve", "alex", "chHfn8Zc")){
			//  is returned in a "ResultSet" object.
			String strSelect = "select name, type_id "+
					"from eve_inv_types where name <> 'Unknown Type';";
			//		+" having count(items_history.quantity) / 27.0 > 0.6; ";
			System.out.println(strSelect);

			Statement stmt = conn.createStatement();
			ResultSet rset = stmt.executeQuery(strSelect);

			System.out.println("Preselect done");
			
			OrdersStatsBuilder stats = OrdersStatsBuilder.newInstance(new long[]{60003760}, 10000002, true);
			PreparedStatement insert = conn.prepareStatement("update eve_inv_types set jita_price_sell = ? where type_id = ?");
			while(rset.next()) {   // Move the cursor to the next row
				int id = rset.getInt("type_id");
				String name = rset.getString("name");
				OrderStats order = stats.get(id);
				if (order == null){
				System.out.println("No price for:" + name);
				} else {
					insert.setInt(2, id);
					insert.setDouble(1, order.getBid());
					insert.execute();
					System.out.println("("+id+") -> " + order.getBid()); 
				}
			}
		}

	}

}

class Inserter implements Callable<Inserter> {

	private int id;
	private double price;
	public Inserter(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public double getPrice() {
		return price;
	}

	@Override
	public Inserter call() throws IOException {
		OrderStats stats = OrdersStatsBuilder.build(id, new long[]{60003760}, 10000002, true);
		price = stats.getBid();
		return this;
	}

}