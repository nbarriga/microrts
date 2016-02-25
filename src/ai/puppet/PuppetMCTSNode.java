package ai.puppet;

import java.util.ArrayList;
import java.util.List;

import ai.mcts.uct.UCTNode;
import ai.puppet.MoveGenerator;
import rts.GameState;
import rts.PlayerAction;

public class PuppetMCTSNode {

	GameState gs;
	float accum_evaluation=0;
	int visit_count=0;
//	static final int n_thr=1;//expansion threshold
	static final float C=1;//exploration constant
	PuppetMCTSNode parent;
	ConfigurableScript<?> script;
	boolean fullState;//do we have moves for both players and just ran a simulation?
	float evaluation_bound;
	
	List<PuppetMCTSNode> children=new ArrayList<PuppetMCTSNode>();
	MoveGenerator moveGenerator;
	Move move;//move that generated this state
//	List<Move> actions=new ArrayList<Move>();
	
	public PuppetMCTSNode(
			GameState gs, 
			ConfigurableScript<?> script, 
			boolean fullState, 
			Move move, 
			PuppetMCTSNode parent,
			float bound) {
		this.parent=parent;
		this.script=script;
		this.move=move;
		this.fullState=fullState;
		evaluation_bound=bound;
		moveGenerator=new MoveGenerator(script.getChoiceCombinations(1-move.player, gs), 1-move.player);
	}

	PuppetMCTSNode selectLeaf(int STEP_PLAYOUT_TIME) throws Exception{
		// if non visited children, visit:        
		if (moveGenerator.hasNext()) {
			Move m=moveGenerator.next();
			//if first player
			if(fullState)
			{
				PuppetMCTSNode node= new PuppetMCTSNode(gs, script,false, m, this,evaluation_bound);
				children.add(node);
				return node.selectLeaf(STEP_PLAYOUT_TIME);
			}
			else//second player
			{
				GameState gs2=gs.clone();
				ConfigurableScript<?> sc1=script.clone();
				sc1.reset();
				ConfigurableScript<?> sc2=script.clone();
				sc2.reset();

				sc1.setChoices(parent.move.choices);
				sc2.setChoices(m.choices);

				PuppetBase.simulate(gs2,sc1,sc2,parent.move.player,m.player, STEP_PLAYOUT_TIME);
				PuppetMCTSNode node= new PuppetMCTSNode(gs2, script,true, m, this, evaluation_bound);//players alternate in 1-2-2-1
				children.add(node);
				return node;
			}
		}
		else//all children expanded, Bandit policy:
		{
			double best_score = 0;
			PuppetMCTSNode best = null;
			for (PuppetMCTSNode child : children) {
				double tmp = childValue(child);
				if (best==null || tmp>best_score) {
					best = child;
					best_score = tmp;
				}
			} 

			if (best==null) {
				//            System.out.println("No more leafs because this node has no children!");
				//            return null;
				return this;
			}else{
				return best.selectLeaf(STEP_PLAYOUT_TIME);
				//        return best;
			}
		}
	}
	
	double childValue(PuppetMCTSNode child) {
        double exploitation = ((double)child.accum_evaluation) / child.visit_count;
        exploitation = exploitation/evaluation_bound;
//        exploitation = (evaluation_bound + exploitation)/(2*evaluation_bound);
       
        double exploration = Math.sqrt(Math.log((double)visit_count)/child.visit_count);
            
//            System.out.println(exploitation + " + " + exploration);

        double tmp = exploitation + C*exploration;
        return tmp;
    }
}
