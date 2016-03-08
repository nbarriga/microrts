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
		PuppetMCTSNode node;
		Plan(){
			node=null;
		}
		Plan(PuppetMCTSNode root){
			node=root;
		}
		void update(GameState gs, int player){
			while(valid()&&
					((gs.getTime()-node.gs.getTime())>STEP_PLAYOUT_TIME ||node.bestChild().player()!=player)){
				node=node.bestChild();
			}
		}
		Collection<Pair<Integer, Integer>> getChoices(){
			if(valid()){
				return node.actions[node.bestChild().index].choices;
			}else{
				return Collections.emptyList();
			}
		}
		boolean valid(){
			return node!=null&&node.bestChild()!=null;
		}
		
		public String toString(){
//			String str="";
//			PuppetMCTSNode n=node;
//			while(n!=null&&n.bestChild()!=null){
//				str+="Player: "+n.toMove+", choices: "+n.bestChild().prevMove.choices+"Node: "+n+"\n";
//				n=n.bestChild();
//			}
//			return str;
			return node.toString();
		}
	}
	
	int DEBUG=1;
	int EVAL_PLAYOUT_TIME=100;
	int STEP_PLAYOUT_TIME=100;
	int MAX_PLAYOUTS=512;
	int PLAN_VALIDITY=400;
	AI policy1, policy2;
	int nPlayouts = 0, totalPlayouts = 0;
	int nNodes = 0, totalNodes = 0;
	
	PuppetMCTSNode root;
	Plan currentPlan;
	long cummSearchTime;
	public PuppetSearchMCTS(int max_time, int step_playout_time, int eval_playout_time, 
			int max_playouts,
			AI policy, ConfigurableScript<?> script, EvaluationFunction evaluation) {
		super(max_time,script,evaluation);
		
		STEP_PLAYOUT_TIME=step_playout_time;
		EVAL_PLAYOUT_TIME=eval_playout_time;
		MAX_PLAYOUTS=max_playouts;
		PLAN_VALIDITY=(int) (STEP_PLAYOUT_TIME*1.5);
		
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
				MAX_PLAYOUTS,
				policy1.clone(),script.clone(), eval);
		clone.currentPlan = currentPlan;
		clone.lastSearchFrame = lastSearchFrame;
		clone.lastSearchTime = lastSearchTime;
		return clone;
	}

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		long start = System.currentTimeMillis();
		//Reinitialize the tree
		if(lastSearchFrame==-1
//				|| searchDone()//always replan
				||(gs.getTime()-lastSearchFrame)>PLAN_VALIDITY
//				||!currentPlan.valid()
				){
			if(DEBUG>=1){
				System.out.println("Restarting after "+(gs.getTime()-lastSearchFrame)+" frames, "
						+(System.currentTimeMillis()-lastSearchTime)+" ms ("+cummSearchTime+" ms)");
			}
			restartSearch(gs, player);
			
		}
        if (DEBUG>=3) System.out.println("Starting MCTS at frame "+gs.getTime()+", player " + player + " with " + MAX_TIME +" ms");
		
        //Expand the tree
        if(root!=null){
			MCTS(start + MAX_TIME);
		}
		
		//execute current plan
        if (gs.canExecuteAnyAction(player) && gs.winner()==-1) {
        	currentPlan.update(gs,player);
        	if (DEBUG>=2) System.out.println("Issuing move using choices: " + currentPlan.getChoices());
        	script.setDefaultChoices();
        	script.setChoices(currentPlan.getChoices());
            PlayerAction pa = script.getAction(player, gs); 
            cummSearchTime+=(System.currentTimeMillis()-start);
            return pa;
        } else {
        	cummSearchTime+=(System.currentTimeMillis()-start);
            return new PlayerAction();
        }
	}
	void restartSearch(GameState gs, int player){
		lastSearchFrame=gs.getTime();
		lastSearchTime=System.currentTimeMillis();
		root=new PuppetMCTSNode(gs.clone(),script,player,eval.upperBound(gs));
		nPlayouts = 0;
		cummSearchTime=0;
	}
	
	void MCTS(long cutoffTime) throws Exception{
        if (DEBUG>=2) System.out.println("Search...");
        

        while(/*(root.gs.getTime()==0 &&!searchDone()) ||*/ System.currentTimeMillis()< cutoffTime) {
            monteCarloRun();
        }
        
        if(searchDone()){
        	currentPlan=new Plan(root);
        	root=null;
        	if (DEBUG>=1) System.out.println("Done. Updating Plan:\n"+currentPlan);
        	
        }
        
	}
	void monteCarloRun() throws Exception{
		PuppetMCTSNode leaf = root.selectLeaf(STEP_PLAYOUT_TIME);
		policy1.reset();
		policy2.reset();
		GameState gs2=leaf.gs.clone();
		simulate(gs2,policy1, policy2,leaf.parent.player(),leaf.player(),EVAL_PLAYOUT_TIME);
		float e=eval.evaluate(leaf.player(),1-leaf.player(), gs2);
		leaf.update(e, leaf.player());
		totalPlayouts++;
		nPlayouts++;
	}
	boolean searchDone(){
		return nPlayouts>=MAX_PLAYOUTS;
	}
	
	public String toString(){
		return "PuppetSearchMCTS("+script.toString()+")";
	}
}
