package lan.groland.eve.reaction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lan.groland.eve.market.Item;
import lan.groland.eve.market.MarketHelper;

public class ReactionMaterial extends Item {

	int outputQuantity;
	final private boolean complex;
	
	ArrayList<Strategy.Ingredients> components = new ArrayList<Strategy.Ingredients>();
	
	public ReactionMaterial(int id, String name, double volume, boolean complex) {
		super(id, name, volume);
		this.complex = complex;
	}
	
	public Strategy optimizedStrategy() throws IOException{
		Strategy.Ingredients syntesis = null;
		float possibleGain = 0;
		for (Strategy.Ingredients i : components){
			if (i.item instanceof ReactionMaterial && components.size() < 4){
				ReactionMaterial reactionMaterial = (ReactionMaterial) i.item;
				
				float price = MarketHelper.jitaBuy(i.item)*reactionMaterial.outputQuantity;
				
				float gain = price - reactionMaterial.optimizedStrategy().cost();
				if (gain > possibleGain){
					gain = possibleGain;
					syntesis = i;
				}
			}
		}
		
		Strategy strat = new Strategy();
		strat.usedCpu += (complex?3000:1500) + 500; // reactor + output silo
		for (Strategy.Ingredients i : components){
			if (i != syntesis){
				strat.input.add(i);
				strat.usedCpu += 500; // silo or moon harvesting
			}
		}
		
		strat.output.add(new Strategy.Ingredients(outputQuantity, this));
		
		if (syntesis != null){
			ReactionMaterial mat = (ReactionMaterial) syntesis.item;
			if (components.size() == 4){
				/* ne marche pas, code mort */
				for (Strategy.Ingredients i : mat.components){
					strat.input.add(new Strategy.Ingredients(100, i.item));
				}
			} else {
				for (Strategy.Ingredients i : mat.components){
					strat.input.add(i);
				}
				strat.output.add(new Strategy.Ingredients(mat.outputQuantity - syntesis.qty, syntesis.item));
				strat.usedCpu += 3000; // 3 silo + 1 reacteur simple 
			}
		}
		return strat;
	}
	
	public static List<ReactionMaterial> complexReactionFactory() throws SQLException, IOException{
		List<ReactionMaterial> res = new ArrayList<ReactionMaterial>();

		HashMap<String, ReactionMaterial> simpleMap = new HashMap<String, ReactionMaterial>();
		
		Connection conn = DriverManager.getConnection(
				"jdbc:mysql://192.168.45.112:3306/eve", "alex", "chHfn8Zc");
		String strSelect = "select eve_inv_types.type_id, eve_inv_types.name, volume " +
				"from eve_inv_types " +
				"where eve_inv_types.name = ? ";
		System.out.println(strSelect);

		PreparedStatement stmt = conn.prepareStatement(strSelect);
		

		BufferedReader reader = new BufferedReader(new FileReader("simple"));
		String line;
		while ((line = reader.readLine()) != null){
			String[] elems = line.split(",");
			stmt.setString(1, elems[1].trim());
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()) throw new IOException("Unkown item name : " + elems[1]);
			ReactionMaterial r = new ReactionMaterial(rs.getInt(1), rs.getString(2), rs.getFloat(3), false);
			rs.close();			
			r.outputQuantity = Integer.valueOf(elems[0]);
			
			stmt.setString(1, elems[3].trim());
			rs = stmt.executeQuery();
			if (!rs.next()) throw new IOException("Unkown item name : " + elems[3]);
			Item i1 = new Item(rs.getInt(1), rs.getString(2), rs.getFloat(3));
			rs.close();			
			r.components.add(new Strategy.Ingredients(Integer.valueOf(elems[2].trim()), i1));
			
			stmt.setString(1, elems[5].trim());
			rs = stmt.executeQuery();
			if (!rs.next()) throw new IOException("Unkown item name : " + elems[5]);
			Item i2 = new Item(rs.getInt(1), rs.getString(2), rs.getFloat(3));
			rs.close();			
			r.components.add(new Strategy.Ingredients(Integer.valueOf(elems[4].trim()), i2));
			
			simpleMap.put(elems[1], r);
			//res.add(r);
		}
		reader.close();
		
		reader = new BufferedReader(new FileReader("complex"));
		while ((line = reader.readLine()) != null){
			String[] elems = line.split(",");
			stmt.setString(1, elems[1].trim());
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()) throw new IOException("Unkown item name : " + elems[1]);
			ReactionMaterial r = new ReactionMaterial(rs.getInt(1), rs.getString(2), rs.getFloat(3), true);
			rs.close();			
			r.outputQuantity = Integer.valueOf(elems[0]);
			
			for (int i = 2; i < elems.length; i++){
				Integer qty = Integer.valueOf(elems[i++].trim());
				Item item = simpleMap.get(elems[i].trim());
				if (item == null){
					throw new IOException("Unkown simple reaction : " + elems[i]);
				}		
				
				r.components.add(new Strategy.Ingredients(qty, item));
			}
			res.add(r);
		}
		reader.close();
		
		return res;
		
	}
	
	public static List<ReactionMaterial> simpleReactionFactory() throws SQLException, IOException{
		List<ReactionMaterial> res = new ArrayList<ReactionMaterial>();
		
		Connection conn = DriverManager.getConnection(
				"jdbc:mysql://192.168.45.112:3306/eve", "alex", "chHfn8Zc");
		String strSelect = "select eve_inv_types.type_id, eve_inv_types.name, volume " +
				"from eve_inv_types " +
				"where eve_inv_types.name = ? ";
		System.out.println(strSelect);

		PreparedStatement stmt = conn.prepareStatement(strSelect);
		

		BufferedReader reader = new BufferedReader(new FileReader("simple"));
		String line;
		while ((line = reader.readLine()) != null){
			String[] elems = line.split(",");
			stmt.setString(1, elems[1].trim());
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()) throw new IOException("Unkown item name : " + elems[1]);
			ReactionMaterial r = new ReactionMaterial(rs.getInt(1), rs.getString(2), rs.getFloat(3), false);
			rs.close();			
			r.outputQuantity = Integer.valueOf(elems[0]);
			
			stmt.setString(1, elems[3].trim());
			rs = stmt.executeQuery();
			if (!rs.next()) throw new IOException("Unkown item name : " + elems[3]);
			Item i1 = new Item(rs.getInt(1), rs.getString(2), rs.getFloat(3));
			rs.close();			
			r.components.add(new Strategy.Ingredients(Integer.valueOf(elems[2].trim()), i1));
			
			stmt.setString(1, elems[5].trim());
			rs = stmt.executeQuery();
			if (!rs.next()) throw new IOException("Unkown item name : " + elems[5]);
			Item i2 = new Item(rs.getInt(1), rs.getString(2), rs.getFloat(3));
			rs.close();			
			r.components.add(new Strategy.Ingredients(Integer.valueOf(elems[4].trim()), i2));

			res.add(r);
		}
		reader.close();
		
		return res;
		
	}
}
