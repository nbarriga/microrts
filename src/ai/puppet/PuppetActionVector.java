package ai.puppet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ai.RandomBiasedAI;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.mcts.naivemcts.NaiveMCTS;
import rts.CNNGameState;
import rts.GameState;
import rts.PlayerAction;
import rts.ReducedGameState;
import rts.UnitAction;
import rts.units.Unit;
import tests.CaffeInterface;
import util.Pair;

public class PuppetActionVector extends AI {

	CaffeInterface net=null;
	SingleChoiceConfigurableScript scripts;
	public PuppetActionVector(SingleChoiceConfigurableScript scripts) {
		this.scripts=scripts;
	}

	void establishConnection(int size){
		net=new CaffeInterface();
		try {
			net.start(8081);
			net.send("data/caffe/actionvector"+size+".prototxt data/caffe/actionvector"+size+".caffemodel\n");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void reset() {
		if(net!=null){
			try{
				net.close();
			}catch(Exception e) {
				e.printStackTrace();
			}
			net=null;
		}
	}

	int maxChoices=4;
	int maxDepth=4;
	int action=-1;
	private double search(double[] values, int depth, int index, int player){
		if(depth==maxDepth+1){
			return depth%2==player?-values[index]:values[index];
		}
		double score=-10;
		for(int i=0;i<maxChoices;i++){
			double val = -search(values,depth+1, index+i*(int)(Math.round(Math.pow(maxChoices,maxDepth-depth))),1-player);
			if(val>score){
				score=val;
				if(depth==1){
					action=i;
				}
			}
		}
		return score;
	}
	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		if(net==null)establishConnection(gs.getPhysicalGameState().getWidth());
		if (!gs.canExecuteAnyAction(player)){
			return new PlayerAction(); 
		}
		CNNGameState cnngs=new CNNGameState(gs);
		net.send(cnngs.getHeader()+cnngs.getPlanesCompressed());  
		
		long before=System.currentTimeMillis();
		double[] values=net.readDoubles();
		long after=System.currentTimeMillis();
		//double[] values={-1,0.9,0.5,-0.4};
		assert(values.length==Math.round(Math.pow(maxChoices, maxDepth)));
		action=-1;
		double score=search(values,1,0,player);
		assert(action!=-1);
		System.out.println("action: "+action+", score:"+score+", time: "+(after-before)+"ms ");
		
		scripts.setDefaultChoices();
		scripts.setChoices(Collections.singletonList(new Pair<Integer,Integer>(0,action)));

		return scripts.getAction(player, gs);
	}

	@Override
	public AI clone() {
		// TODO Auto-generated method stub
		return (AI)new PuppetActionVector((SingleChoiceConfigurableScript)scripts.clone());
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String toString(){
		return  getClass().getSimpleName();
	}

}
