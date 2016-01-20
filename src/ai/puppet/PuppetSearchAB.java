/**
 * 
 */
package ai.puppet;

import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.evaluation.EvaluationFunction;
import ai.minimax.MiniMaxResult;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;

/**
 * @author nbarriga
 *
 */
public class PuppetSearchAB extends AIWithComputationBudget {

	class Move{
		AI script;
		int player;
		public Move(AI script, int player){
			this.script=script;
			this.player=player;
		}
		@Override
		public String toString(){
			return script+", player: "+player;
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
	protected int evalPlayoutTime=0;
	protected int stepPlayoutTime=1200;
	protected AI playoutAI;
	protected AI scripts[];
	protected int MAXPLAYER;
	protected AI lastScript;
	protected int lastSearchTime;
    int nLeaves = 0;
    int nNodes = 0;
	/**
	 * @param mt
	 * @param mi
	 */
	public PuppetSearchAB(int mt, int mi, AI scripts[], EvaluationFunction evaluation) {
		super(mt, mi);
		eval=evaluation;
		this.scripts=scripts;
		playoutAI=scripts[0];
		lastScript=null;
		lastSearchTime=-1;
	}

	/* (non-Javadoc)
	 * @see ai.core.AI#reset()
	 */
	@Override
	public void reset() {
		// TODO Auto-generated method stub
		lastScript=null;
		lastSearchTime=-1;
	}

	/* (non-Javadoc)
	 * @see ai.core.AI#getAction(int, rts.GameState)
	 */
	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		MAXPLAYER=player;
		if(lastSearchTime==-1
				||(gs.getTime()-lastSearchTime)>(stepPlayoutTime*MAXDEPTH/2)){
			lastSearchTime=gs.getTime();
			lastScript=ABCD(player, gs, MAXDEPTH);
		}
        if (gs.canExecuteAnyAction(player) && gs.winner()==-1) {
        	if (DEBUG>=3) System.out.println("Issuing move using script: " + lastScript);
            PlayerAction pa = lastScript.getAction(player, gs); 
            //pa.fillWithNones(gs, player, defaultNONEduration);
            return pa;
        } else {
            return new PlayerAction();
        }
	}
	protected AI ABCD(int player, GameState gs, int depth) throws Exception{
		 long start = System.currentTimeMillis();
	        nLeaves = 0;
	        nNodes = 0;
	        float alpha = -EvaluationFunction.VICTORY;
	        float beta = EvaluationFunction.VICTORY;
	        if (DEBUG>=1) System.out.println("Starting ABCD... " + player + " with " + MAX_TIME +" ms");
	        Result bestMove = ABCD(gs, null, alpha, beta, depth, MAXPLAYER,"");
	        if (DEBUG>=1) System.out.println("ABCD: " + bestMove.m.script + " in " 
	        + (System.currentTimeMillis()-start)+" ms, Nodes: "+nNodes+", leaves: "+nLeaves);
	        
	        return bestMove.m.script;
		
	}
	public Result ABCD(GameState gs, Move move1, float alpha, float beta, int depthLeft, int nextPlayerInSimultaneousNode, String indent) throws Exception {
		if(DEBUG>=2)System.out.println(indent+"ABCD(" + alpha + "," + beta + ") at " + gs.getTime());
		nNodes++;

		if (depthLeft<=0 || gs.winner()!=-1) {
			nLeaves++;

			// Run the play out:
			if(evalPlayoutTime>0){
				GameState gs2 = gs.clone();
				simulate(gs2,playoutAI.clone(),playoutAI.clone(),0,1,evalPlayoutTime);  
				if(DEBUG>=2)System.out.println(indent+"Eval (at " + gs2.getTime() + "): "
						+ eval.evaluate(MAXPLAYER, 1-MAXPLAYER, gs2));
				//          System.out.println(gs);
				return new Result(null,eval.evaluate(MAXPLAYER, 1-MAXPLAYER, gs2));
			}else{
//				System.out.println(gs);
				if(DEBUG>=2)System.out.println(indent+"Eval (at " + gs.getTime() + "): "
						+ eval.evaluate(MAXPLAYER, 1-MAXPLAYER, gs));
				return new Result(null,eval.evaluate(MAXPLAYER, 1-MAXPLAYER, gs));
			}
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
		for(AI script:scripts){

			if(DEBUG>=2)System.out.println(indent+"Node: "+script);
			Result tmp=null;
			if(move1==null){
				tmp=ABCD(gs,new Move(script.clone(),toMove),alpha,beta,depthLeft-1,nextPlayerInSimultaneousNode,indent+"  ");
			}else{
				GameState gs2 = gs.clone();
				simulate(gs2,move1.script.clone(),script.clone(),move1.player,toMove, stepPlayoutTime);
				tmp=ABCD(gs2,null,alpha,beta,depthLeft-1,nextPlayerInSimultaneousNode,indent+"  ");
			}	
			if(DEBUG>=2)System.out.println(indent+"result: "+tmp);
			if(DEBUG>=2)System.out.println(indent+"ToMove: "+toMove+" Score: "+tmp.score);
			if (toMove == MAXPLAYER){
				alpha = Math.max(alpha,tmp.score);
				if (best==null || tmp.score>best.score) {
					best=new Result(new Move(script.clone(),toMove), tmp.score);
				}
			}
			if (toMove == (1-MAXPLAYER)){
				beta = Math.min(beta,tmp.score);
				if (best==null || tmp.score<best.score) {
					best=new Result(new Move(script.clone(),toMove), tmp.score);
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
		return new PuppetSearchAB(MAX_TIME, MAX_ITERATIONS, scripts, eval);
	}

}
