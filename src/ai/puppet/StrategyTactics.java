package ai.puppet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ai.RandomBiasedAI;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.mcts.naivemcts.NaiveMCTS;
import rts.CNNGameState;
import rts.GameState;
import rts.PlayerAction;
import rts.ReducedGameState;
import rts.UnitAction;
import rts.units.Unit;
import tests.CaffeInterface;
import util.Pair;

public class StrategyTactics extends AIWithComputationBudget {

	AIWithComputationBudget strategyAI;
	AIWithComputationBudget tacticsAI;
	int weightStrategy,weightTactics;
	int origTimeBudget,origItBudget;
	public StrategyTactics(int mt, int mi, int weightStrategy, int weightTactics, AIWithComputationBudget strategyAI, AIWithComputationBudget tacticsAI) {
		super(mt, mi);
		origTimeBudget=mt;
		origItBudget=mi;
		this.strategyAI=strategyAI;
		this.tacticsAI=tacticsAI;
		this.weightStrategy=weightStrategy;
		this.weightTactics=weightTactics;
	}


	@Override
	public void reset() {
		strategyAI.reset();
		tacticsAI.reset();
	}

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		

		ReducedGameState rgs=new ReducedGameState(gs);
		//System.out.println("Frame: "+gs.getTime()+" original size: "+gs.getUnits().size()+", reduced size: "+rgs.getUnits().size());
		boolean p0=false,p1=false;
		for(Unit u:rgs.getUnits()){
			if(u.getPlayer()==0)p0=true;
			if(u.getPlayer()==1)p1=true;
			if(p0&&p1)break;
		}
		if(!(p0&&p1)){
			strategyAI.setTimeBudget(TIME_BUDGET);
			PlayerAction paStrategy=strategyAI.getAction(player, gs);
			return paStrategy;
		}else{
			strategyAI.setTimeBudget(TIME_BUDGET*weightStrategy/(weightStrategy+weightTactics));
			PlayerAction paStrategy=strategyAI.getAction(player, gs);
			tacticsAI.setTimeBudget(TIME_BUDGET*weightTactics/(weightStrategy+weightTactics));
			PlayerAction paTactics=tacticsAI.getAction(player, rgs);
			//				System.out.println("Extra search with "+rgs.getUnits().size()+" units");
			//				System.out.println("actions: "+paExtra.getActions().size());
			//remove non attacking units
			List<Pair<Unit,UnitAction>> toRemove=new ArrayList<Pair<Unit,UnitAction>>();
			for(Pair<Unit,UnitAction> ua:paTactics.getActions()) {
				if(!ua.m_a.getType().canAttack){
					toRemove.add(ua);
					//						System.out.println("removed");
				}
			}
			for(Pair<Unit,UnitAction>ua:toRemove){
				rgs.removeUnit(ua.m_a);
				paTactics.getActions().remove(ua);
			}


			PlayerAction paFull = new PlayerAction();
			//add extra actions
			List<Unit> skip=new ArrayList<Unit>();
			for(Pair<Unit,UnitAction> ua:paTactics.getActions()) {
				if(ua.m_b.resourceUsage(ua.m_a, gs.getPhysicalGameState()).consistentWith(paStrategy.getResourceUsage(), gs)){
					paFull.addUnitAction(ua.m_a, ua.m_b);
					paFull.getResourceUsage().merge(ua.m_b.resourceUsage(ua.m_a, gs.getPhysicalGameState()));
					//						System.out.println("Frame: "+gs.getTime()+", extra action: "+ua);
					skip.add(ua.m_a);
				}
				//					else{
				//						System.out.println("inconsistent");
				//					}
			}

			//add script actions
			for(Pair<Unit,UnitAction> ua:paStrategy.getActions()) {
				boolean found=false;
				for(Unit u:skip){
					if(u.getID()==ua.m_a.getID()){
						found=true;
						break;
					}
				}
				if(found){//skip units that were assigned by the extra AI
					//System.out.println("skipping");
					continue;
				}
				paFull.addUnitAction(ua.m_a, ua.m_b);
				paFull.getResourceUsage().merge(ua.m_b.resourceUsage(ua.m_a, gs.getPhysicalGameState()));
			}
			return paFull; 
		}
	}

	@Override
	public AI clone() {
		return (AI)new StrategyTactics(
				origTimeBudget, 
				origItBudget, 
				weightStrategy,
				weightTactics, 
				(AIWithComputationBudget)strategyAI.clone(),
				(AIWithComputationBudget)tacticsAI.clone());
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String toString(){
		return  getClass().getSimpleName() + 
				"(" +strategyAI.getClass().getSimpleName()+
				", " +tacticsAI.getClass().getSimpleName() + ")";
	}

}
