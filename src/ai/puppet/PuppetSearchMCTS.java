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
	int EVAL_PLAYOUT_TIME;

	AI policy1, policy2;
	int nPlayouts = 0, totalPlayouts = 0;
	long cummSearchTime;
	
	PuppetMCTSNode root;
	Plan currentPlan;

	public PuppetSearchMCTS(int max_time_per_frame, int max_playouts_per_frame, 
			int max_plan_time, int max_plan_playouts,
			int step_playout_time, int eval_playout_time, 
			AI policy, ConfigurableScript<?> script, EvaluationFunction evaluation) {
		super(max_time_per_frame,max_playouts_per_frame,
				max_plan_time, max_plan_playouts,step_playout_time,
				script,evaluation);
		

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
	}
	
	//todo:this clone method is broken
	@Override
	public AI clone() {
		PuppetSearchMCTS clone = new PuppetSearchMCTS(MAX_TIME,MAX_ITERATIONS,
				PLAN_TIME, PLAN_PLAYOUTS, STEP_PLAYOUT_TIME, EVAL_PLAYOUT_TIME,
				policy1.clone(),script.clone(), eval);
		clone.currentPlan = currentPlan;
		clone.lastSearchFrame = lastSearchFrame;
		clone.lastSearchTime = lastSearchTime;
		return clone;
	}

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		assert(PLAN):"This method can only be called when using a standing plan";
		//Reinitialize the tree
		if(lastSearchFrame==-1||(gs.getTime()-lastSearchFrame)>PLAN_VALIDITY){
			if(DEBUG>=1){
				System.out.println("Restarting after "+(gs.getTime()-lastSearchFrame)+" frames, "
						+(System.currentTimeMillis()-lastSearchTime)+" ms ("+cummSearchTime+" ms)");
			}
			restartSearch(gs, player);
			
		}
        if (DEBUG>=3) System.out.println("Starting MCTS at frame "+gs.getTime()+", player " + player + " with " + MAX_TIME +" ms");
		
        //Expand the tree
        if(root!=null){
        	computeDuringOneGameFrame();
		}
		
		//execute current plan
        if (gs.canExecuteAnyAction(player) && gs.winner()==-1) {
        	currentPlan.update(gs,player);
        	if (DEBUG>=2) System.out.println("Issuing move using choices: " + currentPlan.getChoices());
        	script.setDefaultChoices();
        	script.setChoices(currentPlan.getChoices());
            PlayerAction pa = script.getAction(player, gs); 
            return pa;
        } else {
            return new PlayerAction();
        }
	}
	@Override
	void restartSearch(GameState gs, int player){
		lastSearchFrame=gs.getTime();
		lastSearchTime=System.currentTimeMillis();
		root=new PuppetMCTSNode(gs.clone(),script,player,eval.upperBound(gs));
		totalPlayouts = 0;
		cummSearchTime=0;
	}
	@Override
	PlayerAction getBestActionSoFar() throws Exception{
		assert(!PLAN):"This method can only be called when not using s standing plan";
		System.out.println(root);
		script.setDefaultChoices();
    	script.setChoices(root.actions[root.bestChild().index].choices);
        return script.getAction(root.nextPlayerInSimultaneousNode, root.gs); 
		
	}
	@Override
	void computeDuringOneGameFrame() throws Exception{
		long start = System.currentTimeMillis();
		long end;
		nPlayouts=0;
        if (DEBUG>=2) System.out.println("Search...");
        

        while(true) {
            monteCarloRun();
            nPlayouts++;
            end = System.currentTimeMillis();
            if (MAX_TIME>=0 && (end - start)>=MAX_TIME) break; 
            if (MAX_ITERATIONS>=0 && nPlayouts>=MAX_ITERATIONS) break;    
        }

        cummSearchTime+=(System.currentTimeMillis()-start);
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
		return PLAN 
				&& ((PLAN_PLAYOUTS>=0 && totalPlayouts>=PLAN_PLAYOUTS) 
						|| (PLAN_TIME>=0 && cummSearchTime>PLAN_TIME));
	}
	
	public String toString(){
		return "PuppetSearchMCTS("+script.toString()+")";
	}
}
