package ai.puppet;

import java.util.Collection;
import java.util.Collections;

import ai.core.AI;
import ai.evaluation.EvaluationFunction;
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

	
	PuppetMCTSNode root;
	Plan currentPlan;
	float C;//UCT exploration constant
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
		clearStats();
	}
	@Override
	public String statisticsString() {
		return "Average Number of Leaves: "+allLeaves/allSearches+
				", Average Time: "+allTime/allSearches;
	}
	void clearStats(){
		allTime=allLeaves=0;
		allSearches=-1;
	}
	long allLeaves;
	long allTime;
	long allSearches;
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

	private void setC(GameState gs){
		if(gs.getPhysicalGameState().getWidth()<=8){
			C=1.0f;
		}else  if(gs.getPhysicalGameState().getWidth()<=16){
			C=10.0f;
		}else  {
			C=0.1f;
		}
	}
	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		assert(PLAN):"This method can only be called when using a standing plan";
		setC(gs);
		//Reinitialize the tree
		if(lastSearchFrame==-1||root==null//||(gs.getTime()-lastSearchFrame)>PLAN_VALIDITY
				){
			if(DEBUG>=1){
				System.out.println("Restarting after "+(gs.getTime()-lastSearchFrame)+" frames, "
						+(System.currentTimeMillis()-lastSearchTime)+" ms ("+totalTime+" ms)");
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
		setC(gs);
		lastSearchFrame=gs.getTime();
		lastSearchTime=System.currentTimeMillis();
		root=new PuppetMCTSNode(gs.clone(),script,C,player,eval.upperBound(gs));
		allLeaves+=totalLeaves;
		allTime+=totalTime;
		allSearches++;
		totalLeaves = 0;
		totalTime=0;
	}
	@Override
	PlayerAction getBestActionSoFar() throws Exception{
		assert(!PLAN):"This method can only be called when not using s standing plan";
		if (DEBUG>=1) System.out.println("Done. Moves:\n"+root+ " in " 
				+ totalTime
				+" ms, wall time: "+(System.currentTimeMillis()-lastSearchTime)
				+" ms, playouts: "+totalLeaves);
		script.setDefaultChoices();
    	script.setChoices(root.actions[root.bestChild().index].choices);
        return script.getAction(root.nextPlayerInSimultaneousNode, root.gs); 
		
	}
	@Override
	void computeDuringOneGameFrame() throws Exception{
		frameStartTime = System.currentTimeMillis();
		long prev=frameStartTime;
		frameLeaves=0;
        if (DEBUG>=2) System.out.println("Search...");
        

        do{
            monteCarloRun();

            long next=System.currentTimeMillis();
			totalTime+=next-prev;
			prev=next;
			frameTime=next-frameStartTime;
        }while(!frameBudgetExpired() && !searchDone());

        if(searchDone()){
        	currentPlan=new Plan(root);
        	root=null;
        	if (DEBUG>=1) System.out.println("Done. Updating Plan:\n"+currentPlan+ " in " 
					+ totalTime
					+" ms, wall time: "+(System.currentTimeMillis()-lastSearchTime)
					+" ms, playouts: "+totalLeaves);
        }        
	}
	void monteCarloRun() throws Exception{
		PuppetMCTSNode leaf = root.selectLeaf(STEP_PLAYOUT_TIME);
		float e;
		if(!leaf.gs.gameover()){
            frameLeaves++;
            totalLeaves++;
			policy1.reset();
			policy2.reset();
			GameState gs2=leaf.gs.clone();
			simulate(gs2,policy1, policy2,leaf.parent.player(),leaf.player(),EVAL_PLAYOUT_TIME);
			e=eval.evaluate(leaf.player(),1-leaf.player(), gs2);
		}else{
			e=eval.evaluate(leaf.player(),1-leaf.player(), leaf.gs);
		}
		leaf.update(e, leaf.player());
	}
	boolean searchDone(){
		return PLAN && planBudgetExpired();
	}
	
	public String toString(){
		return "PuppetSearchMCTS("+script.toString()+")";
	}
}
