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

public class PuppetSearchMCTS extends PuppetBase {
	class Plan{
		Plan(){
		}
		Plan(PuppetMCTSNode root){
			
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
	
	int DEBUG=2;
	int EVAL_PLAYOUT_TIME=100;
	int STEP_PLAYOUT_TIME=100;
	int PLAN_VALIDITY=400;
	AI policy1, policy2;
	int nPlayouts = 0, totalPlayouts = 0;
	int nNodes = 0, totalNodes = 0;
	
	PuppetMCTSNode root;
	Plan currentPlan;
	
	public PuppetSearchMCTS(int max_time, int step_playout_time, int eval_playout_time, 
			AI policy, ConfigurableScript<?> script, EvaluationFunction evaluation) {
		super(max_time,script,evaluation);
		
		STEP_PLAYOUT_TIME=step_playout_time;
		EVAL_PLAYOUT_TIME=eval_playout_time;

		this.policy1=policy.clone();
		this.policy2=policy.clone();
		currentPlan=new Plan();
		root=null;
	}

	@Override
	public void reset() {
		super.reset();
		policy1.reset();
		policy2.reset();
		currentPlan=new Plan();
		root=null;
		nPlayouts = 0; totalPlayouts = 0;
	    nNodes = 0; totalNodes = 0;
	}
	
	//todo:this clone method is broken
	@Override
	public AI clone() {
		PuppetSearchMCTS clone = new PuppetSearchMCTS(MAX_TIME, STEP_PLAYOUT_TIME, EVAL_PLAYOUT_TIME,
				policy1.clone(),script.clone(), eval);
		clone.currentPlan = currentPlan;
		clone.lastSearchFrame = lastSearchFrame;
		clone.lastSearchTime = lastSearchTime;
		return clone;
	}

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		//Reinitialize the tree
		if(lastSearchFrame==-1
				|| searchDone()
				||(gs.getTime()-lastSearchFrame)>PLAN_VALIDITY
				){
			if(DEBUG>=1){
				System.out.println("Restarting after "+(gs.getTime()-lastSearchFrame)+" frames, "
						+(System.currentTimeMillis()-lastSearchTime)+" ms");
			}
			restartSearch(gs, player);
			
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
            return pa;
        } else {
            return new PlayerAction();
        }
	}
	void restartSearch(GameState gs, int player){
		lastSearchFrame=gs.getTime();
		lastSearchTime=System.currentTimeMillis();
		root=new PuppetMCTSNode(gs,script,true,new Move(null,player),null,eval.upperBound(gs));
		totalNodes = 0;
		totalPlayouts = 0;
	}
	
	void MCTS() throws Exception{
        if (DEBUG>=2) System.out.println("Search...");
        long start = System.currentTimeMillis();

        while(System.currentTimeMillis()< (start + MAX_TIME)) {
            monteCarloRun();
        }
        
        if(searchDone()){
        	currentPlan=new Plan(root);
        }
	}
	void monteCarloRun() throws Exception{
		PuppetMCTSNode leaf = root.selectLeaf(STEP_PLAYOUT_TIME);
		policy1.reset();
		policy2.reset();
		GameState gs2=leaf.gs.clone();
		simulate(gs2,policy1, policy2,leaf.parent.move.player,leaf.move.player,EVAL_PLAYOUT_TIME);
		float e=eval.evaluate(leaf.parent.move.player,leaf.move.player, gs2);
		while(leaf!=null) {
            leaf.accum_evaluation += e;
            leaf.visit_count++;
            leaf = leaf.parent;
        }
		totalPlayouts++;
	}
	boolean searchDone(){
		return false;
	}
}
