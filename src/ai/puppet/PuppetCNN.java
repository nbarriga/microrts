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

public class PuppetCNN extends AIWithComputationBudget {

	CaffeInterface net=new CaffeInterface();
	SingleChoiceConfigurableScript scripts;
	AIWithComputationBudget extraAI;
	int switchTime;
	public PuppetCNN(int mt, int mi, int switchTime, SingleChoiceConfigurableScript scripts, AIWithComputationBudget extraAI) {
		super(mt, mi);
		this.switchTime=switchTime;
		this.scripts=scripts;
		this.extraAI=extraAI;
		reset();
	}

	@Override
	public void reset() {
		if(extraAI!=null)extraAI.reset();
		net=new CaffeInterface();
		try {
			net.start(8080);
			net.send("data/caffe/puppet128.prototxt data/caffe/puppet128.caffemodel");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}

	}

	int lastAction=20000;
	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		if(gs.getTime()<lastAction || gs.getTime()>lastAction+switchTime){

			CNNGameState cnngs=new CNNGameState(gs);
			net.send(cnngs.getHeaderExtra(1, player)+cnngs.getPlanesCompressed()+cnngs.getExtraPlanesCompressed(1,player));   
			scripts.setDefaultChoices();
			int action = net.getMaxIndex();
			scripts.setChoices(Collections.singletonList(new Pair<Integer,Integer>(0,action)));
			lastAction=gs.getTime();
			//System.out.println("Setting action "+action+" at frame "+gs.getTime());
			
		}
		PlayerAction paScript=scripts.getAction(player, gs);

		if(extraAI!=null){
			extraAI.setTimeBudget(TIME_BUDGET);

			ReducedGameState rgs=new ReducedGameState(gs);
			//System.out.println("Frame: "+gs.getTime()+" original size: "+gs.getUnits().size()+", reduced size: "+rgs.getUnits().size());
			boolean p0=false,p1=false;
			for(Unit u:rgs.getUnits()){
				if(u.getPlayer()==0)p0=true;
				if(u.getPlayer()==1)p1=true;
				if(p0&&p1)break;
			}
			if(p0&&p1){
				PlayerAction paExtra=extraAI.getAction(player, rgs);
//				System.out.println("Extra search with "+rgs.getUnits().size()+" units");
//				System.out.println("actions: "+paExtra.getActions().size());
				//remove non attacking units
				List<Pair<Unit,UnitAction>> toRemove=new ArrayList<Pair<Unit,UnitAction>>();
				for(Pair<Unit,UnitAction> ua:paExtra.getActions()) {
					if(!ua.m_a.getType().canAttack){
						toRemove.add(ua);
//						System.out.println("removed");
					}
				}
				for(Pair<Unit,UnitAction>ua:toRemove){
					rgs.removeUnit(ua.m_a);
					paExtra.getActions().remove(ua);
				}
				
				
				PlayerAction paFull = new PlayerAction();
				//add extra actions
				List<Unit> skip=new ArrayList<Unit>();
				for(Pair<Unit,UnitAction> ua:paExtra.getActions()) {
					if(ua.m_b.resourceUsage(ua.m_a, gs.getPhysicalGameState()).consistentWith(paScript.getResourceUsage(), gs)){
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
				for(Pair<Unit,UnitAction> ua:paScript.getActions()) {
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
		return paScript;
	}

	@Override
	public AI clone() {
		// TODO Auto-generated method stub
		return (AI)new PuppetCNN(TIME_BUDGET, ITERATIONS_BUDGET, switchTime, (SingleChoiceConfigurableScript)scripts.clone(),extraAI!=null?(AIWithComputationBudget) extraAI.clone():null);
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

}
