/**
 * 
 */
package ai.puppet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.evaluation.EvaluationFunction;
import rts.GameState;
import rts.PlayerAction;
import util.Pair;

/**
 * @author nbarriga
 *
 */
public class PuppetSearchAB extends AIWithComputationBudget {

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
	protected int DEBUG=1;
	protected int MAXDEPTH=2;
	protected EvaluationFunction eval;
	protected int stepPlayoutTime=400;
	protected ConfigurableScript<?> script;
	protected int MAXPLAYER;
	protected Collection<Pair<Integer,Integer>> lastChoices;
	protected int lastSearchTime;
    int nLeaves = 0;
    int nNodes = 0;
	/**
	 * @param mt
	 * @param mi
	 */
	public PuppetSearchAB(int mt, int mi, ConfigurableScript<?> script, EvaluationFunction evaluation) {
		super(mt, mi);
		eval=evaluation;
		this.script=script;
		lastChoices=null;
		lastSearchTime=-1;
	}

	@Override
	public void reset() {
		lastChoices=null;
		lastSearchTime=-1;
	}

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		MAXPLAYER=player;
		if(lastSearchTime==-1
				||(gs.getTime()-lastSearchTime)>(stepPlayoutTime*MAXDEPTH/2)){
			lastSearchTime=gs.getTime();
			lastChoices=ABCD(player, gs, MAXDEPTH);
		}
        if (gs.canExecuteAnyAction(player) && gs.winner()==-1) {
        	if (DEBUG>=3) System.out.println("Issuing move using choices: " + lastChoices);
        	script.setDefaultChoices();
        	script.setChoices(lastChoices);
            PlayerAction pa = script.getAction(player, gs); 
            //pa.fillWithNones(gs, player, defaultNONEduration);
            return pa;
        } else {
            return new PlayerAction();
        }
	}
	protected Collection<Pair<Integer,Integer>> ABCD(int player, GameState gs, int depth) throws Exception{
		 long start = System.currentTimeMillis();
	        nLeaves = 0;
	        nNodes = 0;
	        float alpha = -EvaluationFunction.VICTORY;
	        float beta = EvaluationFunction.VICTORY;
	        if (DEBUG>=1) System.out.println("Starting ABCD... " + player + " with " + MAX_TIME +" ms");
	        Result bestMove = ABCD(gs, null, alpha, beta, depth, MAXPLAYER,"");
	        if (DEBUG>=1) System.out.println("ABCD: " + bestMove + " in " 
	        + (System.currentTimeMillis()-start)+" ms, Nodes: "+nNodes+", leaves: "+nLeaves);
	        
	        return bestMove.m.choices;
		
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
	/* (non-Javadoc)
	 * @see ai.core.AI#clone()
	 */
	@Override
	public AI clone() {
		PuppetSearchAB ps = new PuppetSearchAB(MAX_TIME, MAX_ITERATIONS, script, eval);
		ps.lastChoices = lastChoices;
		ps.lastSearchTime = lastSearchTime;
		return ps;
	}

}
