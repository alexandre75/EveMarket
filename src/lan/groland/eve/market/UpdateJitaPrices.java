package lan.groland.eve.market;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UpdateJitaPrices {

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws SQLException, IOException, InterruptedException {
		try (Connection conn = DriverManager.getConnection(
				"jdbc:mysql://192.168.1.133:3306/eve", "alex", "chHfn8Zc")){
			//  is returned in a "ResultSet" object.
			String strSelect = "select name, type_id "+
					"from eve_inv_types where name <> 'Unknown Type';";
			//		+" having count(items_history.quantity) / 27.0 > 0.6; ";
			System.out.println(strSelect);

			Statement stmt = conn.createStatement();
			ResultSet rset = stmt.executeQuery(strSelect);

			System.out.println("Preselect done");
			ArrayList<Future<Inserter> > futures = new ArrayList<>();
			ThreadPoolExecutor pool = new ThreadPoolExecutor(50, 50, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
			while(rset.next()) {   // Move the cursor to the next row
				int id = rset.getInt("type_id");
				String name = rset.getString("name");
				Inserter inserter = new Inserter(id);
				Future<Inserter> future = pool.submit(inserter);
				futures.add(future);
			}
			pool.shutdown();
			PreparedStatement insert = conn.prepareStatement("update eve_inv_types set jita_price_sell = ? where type_id = ?");
			for (Future<Inserter> fut : futures){
				int tmp = 0;
				try {
					tmp = fut.get().getId();
					insert.setInt(2, fut.get().getId());
					insert.setDouble(1, fut.get().getPrice());
					insert.execute();
					System.out.println("("+fut.get().getId()+") -> " + fut.get().getPrice()); 
				} catch(ExecutionException e){
					e.printStackTrace();
					System.out.println(tmp + " -> " + e.getCause().getMessage()); 
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
		OrderStats stats = OrdersStatsBuilder.build(id, 60003760, 10000002, true);
		price = stats.getBid();
		return this;
	}

}