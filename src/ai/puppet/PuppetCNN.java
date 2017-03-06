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

public class PuppetCNN extends AI {

	CaffeInterface net=null;
	SingleChoiceConfigurableScript scripts;
	public PuppetCNN(SingleChoiceConfigurableScript scripts) {
		this.scripts=scripts;
	}

	void establishConnection(){
		net=new CaffeInterface();
		try {
			net.start(8080);
			net.send("data/caffe/puppet128.prototxt data/caffe/puppet128.caffemodel");
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

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		if(net==null)establishConnection();

		CNNGameState cnngs=new CNNGameState(gs);
		net.send(cnngs.getHeaderExtra(1, player)+cnngs.getPlanesCompressed()+cnngs.getExtraPlanesCompressed(1,player));   
		scripts.setDefaultChoices();
		int action = net.getMaxIndex();
		scripts.setChoices(Collections.singletonList(new Pair<Integer,Integer>(0,action)));

		return scripts.getAction(player, gs);
	}

	@Override
	public AI clone() {
		// TODO Auto-generated method stub
		return (AI)new PuppetCNN((SingleChoiceConfigurableScript)scripts.clone());
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
