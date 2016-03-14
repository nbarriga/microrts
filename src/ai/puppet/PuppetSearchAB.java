/**
 * 
 */
package ai.puppet;

import java.util.Collection;
import java.util.Collections;
import java.util.Stack;

import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import rts.GameState;
import rts.PlayerAction;
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
			return m.toString(script)+", score: "+score;
		}
	}

	class ABCDNode {
		PuppetGameState gs;
		Move prevMove; 
		float alpha;
		float beta;
		int depth;
		int nextPlayerInSimultaneousNode;
		MoveGenerator nextMoves;
		Result best;
		ABCDNode following;

		public ABCDNode( 
				PuppetGameState gs, 
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
			nextMoves=new MoveGenerator(script.getChoiceCombinations(toMove(), gs.gs),toMove());
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
			return " time:"+gs.gs.getTime()+" "+/*prevMove+" best="+*/best+"\n"+(following!=null?following.toString():"");
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
					((gs.getTime()-node.gs.gs.getTime())>STEP_PLAYOUT_TIME ||!node.isMaxPlayer())){
				node=node.following;
			}
			//			if(node!=null)
			//				System.out.println("current plan: "+node);
			//			else
			//				System.out.println("no current plan");
		}
		Collection<Pair<Integer, Integer>> getChoices(){
			if(valid()){
				return node.best.m.choices;
			}else{
				return Collections.emptyList();
			}
		}
		boolean valid(){
			return node!=null&&node.best!=null;
		}
		public String toString(){
			return node!=null?node.toString():"";
		}
	}

	protected int DEBUG=1;
	protected int DEPTH;
	protected int MAXPLAYER=-1;
	int nLeaves = 0, totalLeaves = 0;
	long cummSearchTime;

	Stack<ABCDNode> stack=new Stack<ABCDNode>();
	ABCDNode head;
	ABCDNode lastFinishedHead;
	Plan currentPlan;
	TranspositionTable TT=new TranspositionTable(100000);
	CacheTable CT=new CacheTable(100000);

	/**
	 * @param mt
	 * @param mi
	 */
	public PuppetSearchAB(
			int max_time_per_frame, int max_playouts_per_frame, 
			int max_plan_time, int max_plan_playouts, 
			int playout_time,
			ConfigurableScript<?> script, EvaluationFunction evaluation) {
		super(max_time_per_frame,max_playouts_per_frame,
				max_plan_time, max_plan_playouts,playout_time,
				script,evaluation);

		currentPlan=new Plan();
	}

	@Override
	public void reset() {
		super.reset();
		currentPlan=new Plan();
		stack.clear();
		head=null;
		DEPTH=0;
		nLeaves = 0; totalLeaves = 0;
	}
	//todo:this clone method is broken
	@Override
	public AI clone() {
		PuppetSearchAB ps = new PuppetSearchAB(MAX_TIME, MAX_ITERATIONS,PLAN_TIME,PLAN_PLAYOUTS,STEP_PLAYOUT_TIME, script.clone(), eval);
		ps.currentPlan = currentPlan;
		ps.lastSearchFrame = lastSearchFrame;
		ps.lastSearchTime = lastSearchTime;
		return ps;
	}
	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		assert(PLAN):"This method can only be called when using a standing plan";
		if(lastSearchFrame==-1||(gs.getTime()-lastSearchFrame)>PLAN_VALIDITY){
			if(DEBUG>=1){
				System.out.println("Restarting after "+(gs.getTime()-lastSearchFrame)+" frames, "
						+(System.currentTimeMillis()-lastSearchTime)+" ms");
			}
			restartSearch(gs, player);

		}
		if (DEBUG>=2) System.out.println("Starting ABCD at frame "+gs.getTime()+", player " + player + " with " + MAX_TIME +" ms");
		if(!stack.empty()){
			computeDuringOneGameFrame();
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
	@Override
	void restartSearch(GameState gs, int player){
		MAXPLAYER=player;
		lastSearchFrame=gs.getTime();
		lastSearchTime=System.currentTimeMillis();
		stack.clear();
		stack.push(new ABCDNode(
				new PuppetGameState(gs.clone()), 
				null, 
				-EvaluationFunction.VICTORY, 
				EvaluationFunction.VICTORY, 
				0, 
				MAXPLAYER, 
				null));
		head=stack.peek();
		totalLeaves = 0;
		cummSearchTime=0;
		DEPTH=0;
	}
	@Override
	PlayerAction getBestActionSoFar() throws Exception {
		assert(!PLAN):"This method can only be called when not using a standing plan";
		if (DEBUG>=1) System.out.println("ABCD:\n" + currentPlan + " in " 
				+ (System.currentTimeMillis()-lastSearchTime)+" ms, leaves: "+totalLeaves);
		script.setDefaultChoices();
		script.setChoices(currentPlan.getChoices());
		return script.getAction(MAXPLAYER, head.gs.gs); 
	}
	@Override
	void computeDuringOneGameFrame() throws Exception{
		long start = System.currentTimeMillis();
		long prev=start;;
		nLeaves = 0;
		//	        Result bestMove = ABCD(gs, null, alpha, beta, depth, MAXPLAYER,"");
		do{
			if(DEPTH==0){//just started
				DEPTH+=2;
			}else if(stack.empty()){//just finished a depth
				lastFinishedHead=head;
				if (DEBUG>=2) System.out.println("ABCD:\n" + lastFinishedHead + " in " 
						+ (System.currentTimeMillis()-lastSearchTime)+" ms, leaves: "+totalLeaves+
						", depth: "+DEPTH);
				DEPTH+=2;
				stack.push(new ABCDNode(
						new PuppetGameState(head.gs), 
						null, 
						-EvaluationFunction.VICTORY, 
						EvaluationFunction.VICTORY, 
						0, 
						MAXPLAYER, 
						null));
				head=stack.peek();

			}else{//continuing from last frame

			}
//			System.out.println("Depth:" +DEPTH);
			iterativeABCD(DEPTH);
			if(stack.empty()){
				lastFinishedHead=head;
			}
			long next=System.currentTimeMillis();
			cummSearchTime+=next-prev;
			prev=next;
		}while((prev-start)<MAX_TIME && !searchDone());
		//		cummSearchTime+=(System.currentTimeMillis()-start);

		if(!PLAN){
			currentPlan=new Plan(lastFinishedHead);
		}
		if(searchDone()){
			System.out.println(ttHits+"/"+ttQueries+" TT, "+ctHits+"/"+ctQueries+" CT");
			stack.clear();
			currentPlan=new Plan(lastFinishedHead);
			if (DEBUG>=1) System.out.println("ABCD:\n" + currentPlan + " in " 
					+ cummSearchTime
					+" ms, wall time: "+(System.currentTimeMillis()-lastSearchTime)
					+" ms, leaves: "+totalLeaves);
		}
	}
	boolean searchDone(){
//				return stack.empty()&&DEPTH==8;
		return PLAN 
				&& ((PLAN_PLAYOUTS>=0 && totalLeaves>=PLAN_PLAYOUTS) 
						|| (PLAN_TIME>=0 && cummSearchTime>PLAN_TIME));
	}
	int ttHits=0;
	int ttQueries=0;
	int ctHits=0;
	int ctQueries=0;
	boolean tt=true,ct=true;
	protected void iterativeABCD(int maxDepth) throws Exception {
		long start = System.currentTimeMillis();
		assert(maxDepth%2==0);


		if(DEBUG>=2)System.out.println("ABCD at " + head.gs.gs.getTime());

		while(!stack.isEmpty()&&(System.currentTimeMillis()-start)<MAX_TIME&&!searchDone()) {
			if(DEBUG>=2)System.out.println(stack);
			ABCDNode current = stack.peek();

			if(current.prevMove==null){//first side to choose move
				if(current.depth==maxDepth|| current.gs.gs.gameover()){//evaluate
					if(DEBUG>=2)System.out.println("eval");
					nLeaves++;
					totalLeaves++;
					stack.pop();
					ABCDNode parent= stack.peek();
					Result result = new Result(parent.nextMoves.last(),eval.evaluate(MAXPLAYER, 1-MAXPLAYER, current.gs.gs));
					parent.setResult(result, current);
					continue;
				}
				if(current.nextMoves.hasNext()){//check children
					if(tt&&current.nextMoves.current==0){//if first child, check TT first
						Entry ttEntry=TT.lookup(current.gs);
						ttQueries++;
						if(ttEntry!=null){
							current.nextMoves.swapFront(ttEntry._bestMove);
							ttHits++;
//							System.out.println("first");
						}
					}
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
				}else{//all children checked, return up
					stack.pop();
					if(!stack.empty()){
						ABCDNode parent= stack.peek();
						parent.setResult(new Result(parent.nextMoves.last(),current.best.score),current);
//						TT.store(parent.gs, parent.depth, parent.prevMove, parent.best.m, parent.best.score, parent.alpha, parent.beta, maxDepth-parent.depth);
					}
					if(tt)TT.store(current.gs, current.best.m, current.best.score, current.alpha, current.beta, maxDepth-current.depth);
					
					continue;
				}
			}else{//second side to choose move
				if(current.nextMoves.hasNext()){//check children
					if(tt&&current.nextMoves.current==0){//if first child, check TT first
						Entry ttEntry=TT.lookup(current.gs, current.depth, current.prevMove);
						ttQueries++;
						if(ttEntry!=null){
							current.nextMoves.swapFront(ttEntry._bestMove);
							ttHits++;
//							System.out.println("second");
						}
					}
					Move next=current.nextMoves.next();
					PuppetGameState gs2=null;
					CacheEntry ctEntry;

					if(ct){
						ctEntry=CT.lookup(current.gs, current.depth, current.prevMove, next);
						ctQueries++;
						if(ctEntry!=null){
							gs2=ctEntry._state;
							ctHits++;
						}
					}
					if(gs2==null){
						GameState gsTemp = current.gs.gs.clone();

						ConfigurableScript<?> sc1=script.clone();
						sc1.reset();
						ConfigurableScript<?> sc2=script.clone();
						sc2.reset();

						sc1.setChoices(current.prevMove.choices);
						sc2.setChoices(next.choices);

						simulate(gsTemp,sc1,sc2,current.prevMove.player,next.player, STEP_PLAYOUT_TIME);

						gs2=new PuppetGameState(current.gs,gsTemp,current.depth,current.prevMove, next);
						if(ct)CT.store(current.gs, gs2);
					}
					stack.push(new ABCDNode(
							gs2, 
							null, 
							current.alpha, 
							current.beta, 
							current.depth+1, 
							current.nextPlayerInSimultaneousNode, 
							null));
					continue;
				}else{//all children checked, return up
					stack.pop();
					ABCDNode parent= stack.peek();
					parent.setResult(new Result(parent.nextMoves.last(),current.best.score),current);
//					TT.store(parent.gs, parent.best.m, parent.best.score, parent.alpha, parent.beta, maxDepth-parent.depth);
					if(tt)TT.store(current.gs, current.depth, current.prevMove, current.best.m, current.best.score, current.alpha, current.beta, maxDepth-current.depth);
					continue;
				}
			}	
		}
	}

	//	public Result ABCD(GameState gs, Move move1, float alpha, float beta, int depthLeft, int nextPlayerInSimultaneousNode, String indent) throws Exception {
	//		if(DEBUG>=2)System.out.println(indent+"ABCD(" + alpha + "," + beta + ") at " + gs.getTime());
	//
	//		if (depthLeft<=0 || gs.winner()!=-1) {
	//			nLeaves++;
	//
	//
	//			//				System.out.println(gs);
	//			if(DEBUG>=2)System.out.println(indent+"Eval (at " + gs.getTime() + "): "
	//					+ eval.evaluate(MAXPLAYER, 1-MAXPLAYER, gs));
	//			return new Result(null,eval.evaluate(MAXPLAYER, 1-MAXPLAYER, gs));
	//		}
	//
	//	
	//		int toMove=-1;
	//		if(move1==null){
	//			toMove = nextPlayerInSimultaneousNode;
	//			nextPlayerInSimultaneousNode = 1 - nextPlayerInSimultaneousNode;
	//		}else{
	//			toMove=1-move1.player;
	//		}
	//		assert(toMove==0 || toMove==1);
	//		Result best=null;
	//		for(ArrayList<Pair<Integer,Integer>> choices: script.getChoiceCombinations(toMove, gs)){
	//
	//			if(DEBUG>=2)System.out.println(indent+"Node: "+choices);
	//			Result tmp=null;
	//			if(move1==null){
	//				tmp=ABCD(gs,new Move(choices,toMove),alpha,beta,depthLeft-1,nextPlayerInSimultaneousNode,indent+"  ");
	//			}else{
	//				GameState gs2 = gs.clone();
	//				ConfigurableScript<?> sc1=script.clone();
	//				sc1.setChoices(move1.choices);
	//				ConfigurableScript<?> sc2=script.clone();
	//				sc2.setChoices(choices);
	//				simulate(gs2,sc1,sc2,move1.player,toMove, STEP_PLAYOUT_TIME);
	//				tmp=ABCD(gs2,null,alpha,beta,depthLeft-1,nextPlayerInSimultaneousNode,indent+"  ");
	//			}	
	//			if(DEBUG>=2)System.out.println(indent+"result: "+tmp);
	//			if(DEBUG>=2)System.out.println(indent+"ToMove: "+toMove+" Score: "+tmp.score);
	//			if (toMove == MAXPLAYER){
	//				alpha = Math.max(alpha,tmp.score);
	//				if (best==null || tmp.score>best.score) {
	//					best=new Result(new Move(choices,toMove), tmp.score);
	//				}
	//			}
	//			if (toMove == (1-MAXPLAYER)){
	//				beta = Math.min(beta,tmp.score);
	//				if (best==null || tmp.score<best.score) {
	//					best=new Result(new Move(choices,toMove), tmp.score);
	//				}
	//			}
	//			if(alpha>=beta){
	//				if(DEBUG>=2)System.out.println(indent+"Beta cut: "+best.score);
	//				assert(best!=null);
	////				System.out.println(indent+"Value: "+best.score);
	//				return best;
	//			}
	//		}
	//		if(DEBUG>=2)System.out.println(indent+"Final return: "+best.score);
	//		assert(best!=null);
	////		System.out.println(indent+"Value: "+best.score);
	//		return best;
	//		
	//		
	//	}

	public String toString(){
		return "PuppetSearchAB("+script.toString()+")";
	}





}