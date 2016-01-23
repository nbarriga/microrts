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
public class PuppetSearchAB extends AI {
	class Move{
		Collection<Pair<Integer,Integer>> choices;
		int player;
		public Move(Collection<Pair<Integer,Integer>> choices, int player){
			this.choices=choices;
			this.player=player;
		}
		@Override
		public String toString(){
			return "choices: "+choices.stream().map(
					(Pair<Integer,Integer>  p)-> 
					new Pair<String,Integer>(script.choicePointValues[p.m_a].name(),p.m_b))
			.collect(Collectors.toList())+", player: "+player;
		}
	}
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
	class MoveGenerator{
		ArrayList<ArrayList<Pair<Integer,Integer>>> choices;
		int current=0;
		int player;
		MoveGenerator(ArrayList<ArrayList<Pair<Integer,Integer>>> choices, int player){
			this.choices=choices;
			this.player=player;
		}
		boolean hasNext(){
			return current<choices.size();
		}
		
		Move next(){
			return new Move(choices.get(current++),player);
		}
		Move last(){
			return new Move(choices.get(current-1),player);
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
	    }
	    int toMove(){
	    	if(prevMove==null)return nextPlayerInSimultaneousNode;
	    	else return (1-prevMove.player);
	    }
	    boolean isMaxPlayer(){
	    	return toMove()==MAXPLAYER;
	    }
	    void setResult(Result result){
			if(best==null){
				best=result;
			}else if((isMaxPlayer()&&result.score>best.score)
					||(!isMaxPlayer()&&result.score<best.score)){
				best=result;
			}
	    }
	    void setScore(Result result){
			if((isMaxPlayer()&&result.score>best.score)
					||(!isMaxPlayer()&&result.score<best.score)){
				best.score=result.score;
			}
	    }
	    boolean alphaBetaCut(){
	    	return false;
	    }
	    public String toString(){
	    	return "NODEDEPTH:"+depth+" "+prevMove+" best="+best;
	    }
	}

    protected int MAX_TIME = 100;//ms
	protected int DEBUG=1;
	protected int MAXDEPTH=2;
	protected EvaluationFunction eval;
	protected int stepPlayoutTime=400;
	protected ConfigurableScript<?> script;
	protected int MAXPLAYER=-1;
	protected Collection<Pair<Integer,Integer>> lastChoices;
	protected int lastSearchFrame;
	protected long lastSearchTime;
    int nLeaves = 0;
    int nNodes = 0;
    
    Stack<ABCDNode> stack=new Stack<ABCDNode>();
    ABCDNode head;
	/**
	 * @param mt
	 * @param mi
	 */
	public PuppetSearchAB(int mt, ConfigurableScript<?> script, EvaluationFunction evaluation) {
		
		MAX_TIME=mt;
		eval=evaluation;
		this.script=script;
		lastChoices= Collections.emptyList();
		lastSearchFrame=-1;
	}

	@Override
	public void reset() {
		lastChoices= Collections.emptyList();
		lastSearchFrame=-1;
		stack.clear();
		script.reset();
	}


	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		MAXPLAYER=player;
		if(lastSearchFrame==-1
				//||(gs.getTime()-lastSearchTime)>=(stepPlayoutTime*MAXDEPTH/2)
				||stack.empty()){
			if(DEBUG>=1){
				System.out.println("Restarting after "+(gs.getTime()-lastSearchFrame)+" frames");
				System.out.println("Restarting after "+(System.currentTimeMillis()-lastSearchTime)+" ms");
			}
			restartSearch(gs);
			
		}
		ABCD(player,gs,MAXDEPTH);
        if (gs.canExecuteAnyAction(player) && gs.winner()==-1) {
        	if (DEBUG>=2) System.out.println("Issuing move using choices: " + lastChoices);
        	script.setDefaultChoices();
        	script.setChoices(lastChoices);
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
		if(stack.empty()){
			stack.push(new ABCDNode(
					gs, 
					null, 
					-EvaluationFunction.VICTORY, 
					EvaluationFunction.VICTORY, 
					0, 
					MAXPLAYER, 
					null));
		}
		head=stack.peek();
	}
	protected void ABCD(int player, GameState gs, int depth) throws Exception{
		 long start = System.currentTimeMillis();
	        nLeaves = 0;
	        nNodes = 0;
	        float alpha = -EvaluationFunction.VICTORY;
	        float beta = EvaluationFunction.VICTORY;
	        if (DEBUG>=2) System.out.println("Starting ABCD at frame "+gs.getTime()+", player " + player + " with " + MAX_TIME +" ms");
//	        Result bestMove = ABCD(gs, null, alpha, beta, depth, MAXPLAYER,"");
	        iterativeABCD(MAXDEPTH);
	        
	        if (DEBUG>=1) System.out.println("ABCD: " + head.best + " in " 
	        + (System.currentTimeMillis()-start)+" ms, Nodes: "+nNodes+", leaves: "+nLeaves);
	        
	        if(stack.empty()){
	        	lastChoices= head.best.m.choices;
	        	if (DEBUG>=1) System.out.println("ABCD: " + head.best + " in " 
	        	        + (System.currentTimeMillis()-lastSearchTime)+" ms");
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
				if(current.depth==maxDepth|| gs.winner()!=-1){//evaluate
					if(DEBUG>=2)System.out.println("eval");
					nLeaves++;
					stack.pop();
					ABCDNode parent= stack.peek();
					Result result = new Result(parent.nextMoves.last(),eval.evaluate(MAXPLAYER, 1-MAXPLAYER, current.gs));
					parent.setResult(result);
					continue;
				}
				if(current.nextMoves.hasNext()){
					if(DEBUG>=2)System.out.println("current.nextMoves.hasNext()");
					stack.push(new ABCDNode(
							gs, 
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
						parent.setResult(new Result(parent.nextMoves.last(),current.best.score));
					}
					continue;
				}
			}else{
//				if(DEBUG>=2)System.out.println("player2");
				if(current.nextMoves.hasNext()){
					GameState gs2 = gs.clone();

					ConfigurableScript<?> sc1=script.clone();
					ConfigurableScript<?> sc2=script.clone();

					sc1.setChoices(current.prevMove.choices);

					Move next=current.nextMoves.next();
					sc2.setChoices(next.choices);

					simulate(gs2,sc1,sc2,current.prevMove.player,next.player, stepPlayoutTime);
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
					parent.setResult(new Result(parent.nextMoves.last(),current.best.score));
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
				simulate(gs2,sc1,sc2,move1.player,toMove, stepPlayoutTime);
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
	protected void simulate(GameState gs, AI ai1, AI ai2, int player1, int player2, int time) throws Exception{
		assert(player1!=player2);
		int timeOut = gs.getTime() + time;
		boolean gameover = false;
		while(!gameover && gs.getTime()<timeOut) {
			if (gs.isComplete()) {
				gameover = gs.cycle();
			} else {
				gs.issue(ai1.getAction(player1, gs));
				gs.issue(ai2.getAction(player2, gs));
			}
		}    
	}

	@Override
	public AI clone() {
		PuppetSearchAB ps = new PuppetSearchAB(MAX_TIME, script.clone(), eval);
		ps.lastChoices = lastChoices;
		ps.lastSearchFrame = lastSearchFrame;
		return ps;
	}


}
