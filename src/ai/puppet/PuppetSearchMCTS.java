package ai.puppet;

import java.util.Collection;
import java.util.Collections;

import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import ai.puppet.PuppetSearchAB.ABCDNode;
import ai.puppet.PuppetSearchAB.Plan;
import rts.GameState;
import rts.PlayerAction;
import util.Pair;

public class PuppetSearchMCTS extends AI {
	class Plan{
		Plan(){
		}
		void update(GameState gs){
		}
		Collection<Pair<Integer, Integer>> getChoices(){
			return null;
		}
		boolean valid(){
			return false;
		}
	}
	
	int MAX_TIME = 100;//ms
	int DEBUG=1;
	EvaluationFunction eval;
	int EVAL_PLAYOUT_TIME=100;
	int STEP_PLAYOUT_TIME=100;
	ConfigurableScript<?> script;
	AI policy;
	int lastSearchFrame;
	long lastSearchTime;
	int nPlayouts = 0, totalPlayouts = 0;
	int nNodes = 0, totalNodes = 0;
	
	PuppetMCTSNode root;
	Plan currentPlan;
	
	public PuppetSearchMCTS(int max_time, int step_playout_time, int eval_playout_time, 
			AI policy, ConfigurableScript<?> script, EvaluationFunction evaluation) {
		MAX_TIME=max_time;
		EVAL_PLAYOUT_TIME=eval_playout_time;
		STEP_PLAYOUT_TIME=step_playout_time;
		eval=evaluation;
		this.script=script;
		this.policy=policy;
		lastSearchFrame=-1;
		currentPlan=new Plan();
		root=null;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		MAXPLAYER=player;
		
		//Reinitialize the tree
		if(lastSearchFrame==-1
//				||(gs.getTime()-lastSearchFrame)>=(stepPlayoutTime)
				||(stack.empty()&&(gs.getTime()-lastSearchFrame)>PLAYOUT_TIME*MAXDEPTH/4)
//				||stack.empty()
				){
			if(DEBUG>=1){
				System.out.println("Restarting after "+(gs.getTime()-lastSearchFrame)+" frames, "
						+(System.currentTimeMillis()-lastSearchTime)+" ms");
			}
			restartSearch(gs);
			
		}
        if (DEBUG>=2) System.out.println("Starting ABCD at frame "+gs.getTime()+", player " + player + " with " + MAX_TIME +" ms");
		
        //Expand the tree
        if(root!=null){
			MCTS();
		}
		
		//execute current plan
        if (gs.canExecuteAnyAction(player) && gs.winner()==-1) {
        	if (DEBUG>=2) System.out.println("Issuing move using choices: " + currentPlan.getChoices());
        	currentPlan.update(gs);
        	script.setDefaultChoices();
        	script.setChoices(currentPlan.getChoices());
            PlayerAction pa = script.getAction(player, gs); 
            //pa.fillWithNones(gs, player, defaultNONEduration);
            return pa;
        } else {
            return new PlayerAction();
        }
	}

	void MCTS(){
        if (DEBUG>=2) System.out.println("Search...");
        long start = System.currentTimeMillis();
        long cutOffTime = start + MAX_TIME;
        if (MAX_TIME<=0) cutOffTime = 0;

//        System.out.println(start + " + " + available_time + " = " + cutOffTime);

        while(System.currentTimeMillis()< (start + MAX_TIME)) {
            monteCarloRun();
        }
        
        if(searchDone()){
        	currentPlan=new Plan(root);
        }
	}
	
	@Override
	public AI clone() {
		// TODO Auto-generated method stub
		return null;
	}

}
