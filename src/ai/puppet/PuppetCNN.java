package ai.puppet;

import java.util.Collections;
import java.util.List;

import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import rts.CNNGameState;
import rts.GameState;
import rts.PlayerAction;
import tests.CaffeInterface;
import util.Pair;

public class PuppetCNN extends AIWithComputationBudget {

	CaffeInterface net=new CaffeInterface();
	SingleChoiceConfigurableScript scripts;
	int switchTime;
	public PuppetCNN(int mt, int mi, int switchTime, SingleChoiceConfigurableScript scripts) {
		super(mt, mi);
		this.switchTime=switchTime;
		this.scripts=scripts;
		reset();
	}

	@Override
	public void reset() {
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

	int lastAction=20000;
	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		if(gs.getTime()<lastAction || gs.getTime()>lastAction+switchTime){
		
		CNNGameState cnngs=new CNNGameState(gs);
		net.send(cnngs.getHeaderExtra(1, player)+cnngs.getPlanesCompressed()+cnngs.getExtraPlanesCompressed(1,player));   
		scripts.setDefaultChoices();
		int action = net.getMaxIndex();
		scripts.setChoices(Collections.singletonList(new Pair<Integer,Integer>(0,action)));
			lastAction=gs.getTime();
			//System.out.println("Setting action "+action+" at frame "+gs.getTime());
		}else{
		}
		return scripts.getAction(player, gs); 
	}

	@Override
	public AI clone() {
		// TODO Auto-generated method stub
		return (AI)new PuppetCNN(TIME_BUDGET, ITERATIONS_BUDGET, switchTime, scripts);
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

}
