package lan.groland.eve.reaction;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import lan.groland.eve.market.Item;
import lan.groland.eve.reaction.Strategy.SaleStrategy;

public class BestReaction {
	
	public final static Item GASES = new Item(16634, "Atmospheric Gases", 0.2);
	public final static Item SILICATES = new Item(16636, "Silicates",0.2);
	
	public static void print2xSimpleStrat(Item freeGoo) throws SQLException, IOException{
		List<ReactionMaterial> reactions = ReactionMaterial.simpleReactionFactory();
		
		Iterator<ReactionMaterial> iterSimple = reactions.iterator();
		ReactionMaterial reaction = iterSimple.next();
		Strategy bestSimple = reaction.optimizedStrategy();
		bestSimple.input.add(new Strategy.Ingredients(20, new Item(4051, "Nitrogen Fuel Block", 5)));
		bestSimple.setMoonHarvested(freeGoo);
		double bestSimpleGain = bestSimple.netReturn(4);
		//System.out.println(reaction.getName() + " : " + bestSimpleGain);
		while(iterSimple.hasNext()) {
			reaction = iterSimple.next();
			Strategy tmp = reaction.optimizedStrategy();
			tmp.input.add(new Strategy.Ingredients(20, new Item(4051, "Nitrogen Fuel Block", 5)));
			double gain = tmp.netReturn(4);
			//System.out.println(reaction.getName() + " : " + gain);
			if (gain > bestSimpleGain){
				bestSimpleGain = gain;
				bestSimple = tmp;
			}
		}
		bestSimple.merge(bestSimple); //x2
		System.out.println(bestSimple);
		System.out.println();
		
	}

	public static void main(String[] args) throws SQLException, IOException {
		Item freeGoo = SILICATES;
		
		Strategy.setTrade(SaleStrategy.market);
		
		print2xSimpleStrat(freeGoo);
		
		List<ReactionMaterial> reactions = ReactionMaterial.simpleReactionFactory();
		
		Iterator<ReactionMaterial> iterSimple = reactions.iterator();
		ReactionMaterial reaction = iterSimple.next();
		Strategy bestSimple = reaction.optimizedStrategy();
		bestSimple.setMoonHarvested(freeGoo);
		double bestSimpleGain = bestSimple.netReturn(4);
		//System.out.println(reaction.getName() + " : " + bestSimpleGain);
		while(iterSimple.hasNext()) {
			reaction = iterSimple.next();
			Strategy tmp = reaction.optimizedStrategy();
			double gain = tmp.netReturn(4);
			//System.out.println(reaction.getName() + " : " + gain);
			if (gain > bestSimpleGain){
				bestSimpleGain = gain;
				bestSimple = tmp;
			}
			//System.out.println(bestSimpleGain);
		}
		
		
		Strategy bestComplex = null;
		double bestComplexGain = -1;
		reactions = ReactionMaterial.complexReactionFactory();
		for (ReactionMaterial r : reactions){
			Strategy strategy = r.optimizedStrategy();
			strategy.setMoonHarvested(freeGoo);
			
			if (SaleStrategy.market.sellPrice(freeGoo) * 100*24 > bestSimple.netResult(1)){
				strategy.addMoonHarvesterIfPossible();
			}
			
			if (strategy.freeCpu() >= 3000 && bestSimpleGain > 0){
				strategy.merge(bestSimple);
			}
			strategy.addMoonHarvesterIfPossible();
			strategy.input.add(new Strategy.Ingredients(40, new Item(4051, "Nitrogen Fuel Block", 5)));
			double gain = strategy.netReturn(4);
			if (gain > bestComplexGain){
				bestComplexGain = gain;
				bestComplex = strategy;
			}
		}		
		System.out.println(bestComplex);
	}

}
