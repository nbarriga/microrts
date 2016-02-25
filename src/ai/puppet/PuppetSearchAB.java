/**
 * 
 */
package ai.puppet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Stack;
import java.util.stream.Collectors;

import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.InterruptibleAIWithComputationBudget;
import ai.evaluation.EvaluationFunction;
import ai.minimax.ABCD.ABCDNode;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import util.Pair;



/**
 * @author nbarriga
 *
 */
public class PuppetSearchAB extends PuppetBase {
	
	class Result{
		Move m;
		float score;
		public Result(Move m, float score){
			this.m=m;
			this.score=score;
		}
		@Override
		public String toString(){
			return m+", score: "+score;
		}
	}
	
	class ABCDNode {
		GameState gs;
		Move prevMove; 
		float alpha;
		float beta;
		int depth;
		int nextPlayerInSimultaneousNode;
		MoveGenerator nextMoves;
		Result best;
	    ABCDNode following;
	   
	    public ABCDNode( 
	    		GameState gs, 
	    		Move prevMove, 
	    		float alpha, 
	    		float beta, 
	    		int depth, 
	    		int nextPlayerInSimultaneousNode,
	    		Result best) {
	    	this.gs=gs;
	    	this.prevMove=prevMove;
	    	this.alpha=alpha;
	    	this.beta=beta;
	    	this.depth=depth;
	    	this.nextPlayerInSimultaneousNode=nextPlayerInSimultaneousNode;
	    	this.best=best;
	    	nextMoves=new MoveGenerator(script.getChoiceCombinations(toMove(), gs),toMove());
	    	following=null;
	    }
	    int toMove(){
	    	if(prevMove==null)return nextPlayerInSimultaneousNode;
	    	else return (1-prevMove.player);
	    }
	    boolean isMaxPlayer(){
	    	return toMove()==MAXPLAYER;
	    }
	    void setResult(Result result, ABCDNode node){
	    	if(best==null){
	    		best=result;
	    		following=node;
	    	}else if(isMaxPlayer()){
	    		alpha = Math.max(alpha,best.score);
	    		if(result.score>best.score){
	    			best=result;
	    			following=node;
	    		}
	    	}else if(!isMaxPlayer()){
	    		beta = Math.min(beta,best.score);
	    		if(result.score<best.score){
	    			best=result;
	    			following=node;
	    		}
	    	}
	    	if(alpha>=beta){
				nextMoves.ABcut();
			}
	    }
	    public String toString(){
	    	return " time:"+gs.getTime()+" "+/*prevMove+" best="+*/best+"\n"+(following!=null?following.toString():"");
	    }
	}
	
	class Plan{
		ABCDNode node;
		Plan(ABCDNode node){
			this.node=node;
		}
		Plan(){
			node=null;
		}
		void update(GameState gs){
			while(node!=null&&
					((gs.getTime()-node.gs.getTime())>PLAYOUT_TIME ||!node.isMaxPlayer())){
				node=node.following;
			}
//			if(node!=null)
//				System.out.println("current plan: "+node);
//			else
//				System.out.println("no current plan");
		}
		Collection<Pair<Integer, Integer>> getChoices(){
			if(node!=null&&node.best!=null){
				return node.best.m.choices;
			}else{
				return Collections.emptyList();
			}
		}
		boolean valid(){
			return node!=null&&node.best!=null;
		}
	}

    protected int MAX_TIME = 100;//ms
	protected int DEBUG=1;
	protected int MAXDEPTH=6;
	protected EvaluationFunction eval;
	protected int PLAYOUT_TIME=100;
	protected ConfigurableScript<?> script;
	protected int MAXPLAYER=-1;
	protected int lastSearchFrame;
	protected long lastSearchTime;
    int nLeaves = 0, totalLeaves = 0;
    int nNodes = 0, totalNodes = 0;
    
    Stack<ABCDNode> stack=new Stack<ABCDNode>();
    ABCDNode head;
    Plan currentPlan;
   
    
	/**
	 * @param mt
	 * @param mi
	 */
	public PuppetSearchAB(int mt, int pt, ConfigurableScript<?> script, EvaluationFunction evaluation) {
		super(mt,script,evaluation);
		PLAYOUT_TIME=pt;
		currentPlan=new Plan();
	}

	@Override
	public void reset() {
		super.reset();
		currentPlan=new Plan();
		stack.clear();
		nLeaves = 0; totalLeaves = 0;
	    nNodes = 0; totalNodes = 0;
	}
	//todo:this clone method is broken
	@Override
	public AI clone() {
		PuppetSearchAB ps = new PuppetSearchAB(MAX_TIME, PLAYOUT_TIME, script.clone(), eval);
		ps.currentPlan = currentPlan;
		ps.lastSearchFrame = lastSearchFrame;
		ps.lastSearchTime = lastSearchTime;
		return ps;
	}
	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		MAXPLAYER=player;
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
		if(!stack.empty()){
			ABCD();
		}
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
	protected void restartSearch(GameState gs){
		lastSearchFrame=gs.getTime();
		lastSearchTime=System.currentTimeMillis();
		stack.clear();
		stack.push(new ABCDNode(
				gs.clone(), 
				null, 
				-EvaluationFunction.VICTORY, 
				EvaluationFunction.VICTORY, 
				0, 
				MAXPLAYER, 
				null));
		head=stack.peek();
		totalLeaves = 0;
		totalNodes = 0;
	}
	protected void ABCD() throws Exception{
		 long start = System.currentTimeMillis();
	        nLeaves = 0;
	        nNodes = 0;
//	        Result bestMove = ABCD(gs, null, alpha, beta, depth, MAXPLAYER,"");
	        iterativeABCD(MAXDEPTH);
	        totalLeaves+=nLeaves;
	        totalNodes+=nNodes;
	        if (DEBUG>=2) System.out.println("ABCD: " + head.best + " in " 
	        + (System.currentTimeMillis()-start)+" ms, Nodes: "+nNodes+", leaves: "+nLeaves);
	        
	        if(stack.empty()){
	        	currentPlan=new Plan(head);
	        	if (DEBUG>=1) System.out.println("ABCD:\n" + head + " in " 
	        			+ (System.currentTimeMillis()-lastSearchTime)+" ms, Nodes: "
	        			+totalNodes+", leaves: "+totalLeaves);
	        }
		
	}
	protected void iterativeABCD(int maxDepth) throws Exception {
		 long start = System.currentTimeMillis();
		assert(maxDepth%2==0);
		
		GameState gs=head.gs;
		
		if(DEBUG>=2)System.out.println("ABCD at " + gs.getTime());


		
		while(!stack.isEmpty()&&(System.currentTimeMillis()-start)<MAX_TIME) {
			if(DEBUG>=2)System.out.println(stack);
			ABCDNode current = stack.peek();
			nNodes++;

			if(current.prevMove==null){
//				if(DEBUG>=2)System.out.println("current.prevMove==null");
				if(current.depth==maxDepth|| current.gs.winner()!=-1){//evaluate
					if(DEBUG>=2)System.out.println("eval");
					nLeaves++;
					stack.pop();
					ABCDNode parent= stack.peek();
					Result result = new Result(parent.nextMoves.last(),eval.evaluate(MAXPLAYER, 1-MAXPLAYER, current.gs));
					parent.setResult(result, current);
					continue;
				}
				if(current.nextMoves.hasNext()){
					if(DEBUG>=2)System.out.println("current.nextMoves.hasNext()");
					stack.push(new ABCDNode(
							current.gs, 
							current.nextMoves.next(), 
							current.alpha, 
							current.beta, 
							current.depth+1, 
							1-current.nextPlayerInSimultaneousNode, 
							null));
					continue;
				}else{
					stack.pop();
					if(!stack.empty()){
						ABCDNode parent= stack.peek();
						parent.setResult(new Result(parent.nextMoves.last(),current.best.score),current);
					}
					continue;
				}
			}else{
//				if(DEBUG>=2)System.out.println("player2");
				if(current.nextMoves.hasNext()){
					GameState gs2 = current.gs.clone();

					ConfigurableScript<?> sc1=script.clone();
					sc1.reset();
					ConfigurableScript<?> sc2=script.clone();
					sc2.reset();

					sc1.setChoices(current.prevMove.choices);

					Move next=current.nextMoves.next();
					sc2.setChoices(next.choices);

					simulate(gs2,sc1,sc2,current.prevMove.player,next.player, PLAYOUT_TIME);
					stack.push(new ABCDNode(
							gs2, 
							null, 
							current.alpha, 
							current.beta, 
							current.depth+1, 
							current.nextPlayerInSimultaneousNode, 
							null));
					continue;
				}else{
					stack.pop();
					ABCDNode parent= stack.peek();
					parent.setResult(new Result(parent.nextMoves.last(),current.best.score),current);
					continue;
				}
			}	
		}
	}
	
	public Result ABCD(GameState gs, Move move1, float alpha, float beta, int depthLeft, int nextPlayerInSimultaneousNode, String indent) throws Exception {
		if(DEBUG>=2)System.out.println(indent+"ABCD(" + alpha + "," + beta + ") at " + gs.getTime());
		nNodes++;

		if (depthLeft<=0 || gs.winner()!=-1) {
			nLeaves++;


			//				System.out.println(gs);
			if(DEBUG>=2)System.out.println(indent+"Eval (at " + gs.getTime() + "): "
					+ eval.evaluate(MAXPLAYER, 1-MAXPLAYER, gs));
			return new Result(null,eval.evaluate(MAXPLAYER, 1-MAXPLAYER, gs));
		}

	
		int toMove=-1;
		if(move1==null){
			toMove = nextPlayerInSimultaneousNode;
			nextPlayerInSimultaneousNode = 1 - nextPlayerInSimultaneousNode;
		}else{
			toMove=1-move1.player;
		}
		assert(toMove==0 || toMove==1);
		Result best=null;
		for(ArrayList<Pair<Integer,Integer>> choices: script.getChoiceCombinations(toMove, gs)){

			if(DEBUG>=2)System.out.println(indent+"Node: "+choices);
			Result tmp=null;
			if(move1==null){
				tmp=ABCD(gs,new Move(choices,toMove),alpha,beta,depthLeft-1,nextPlayerInSimultaneousNode,indent+"  ");
			}else{
				GameState gs2 = gs.clone();
				ConfigurableScript<?> sc1=script.clone();
				sc1.setChoices(move1.choices);
				ConfigurableScript<?> sc2=script.clone();
				sc2.setChoices(choices);
				simulate(gs2,sc1,sc2,move1.player,toMove, PLAYOUT_TIME);
				tmp=ABCD(gs2,null,alpha,beta,depthLeft-1,nextPlayerInSimultaneousNode,indent+"  ");
			}	
			if(DEBUG>=2)System.out.println(indent+"result: "+tmp);
			if(DEBUG>=2)System.out.println(indent+"ToMove: "+toMove+" Score: "+tmp.score);
			if (toMove == MAXPLAYER){
				alpha = Math.max(alpha,tmp.score);
				if (best==null || tmp.score>best.score) {
					best=new Result(new Move(choices,toMove), tmp.score);
				}
			}
			if (toMove == (1-MAXPLAYER)){
				beta = Math.min(beta,tmp.score);
				if (best==null || tmp.score<best.score) {
					best=new Result(new Move(choices,toMove), tmp.score);
				}
			}
			if(alpha>=beta){
				if(DEBUG>=2)System.out.println(indent+"Beta cut: "+best.score);
				assert(best!=null);
//				System.out.println(indent+"Value: "+best.score);
				return best;
			}
		}
		if(DEBUG>=2)System.out.println(indent+"Final return: "+best.score);
		assert(best!=null);
//		System.out.println(indent+"Value: "+best.score);
		return best;
		
		
	}




}
