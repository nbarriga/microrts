package ai.puppet;

import java.util.Arrays;
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
	public PuppetCNN(int mt, int mi, SingleChoiceConfigurableScript scripts) {
		super(mt, mi);
		this.scripts=scripts;
		reset();
	}

	@Override
	public void reset() {
		net=new CaffeInterface();
		try {
			net.start(8080);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}

	}

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		CNNGameState cnngs=new CNNGameState(gs);
		net.send(cnngs.getHeaderExtra(1, player)+cnngs.getPlanesCompressed()+cnngs.getExtraPlanesCompressed(1,player));   
		scripts.setDefaultChoices();
		scripts.setChoices(Collections.singletonList(new Pair<Integer,Integer>(0,net.getMaxIndex())));
		return scripts.getAction(player, gs); 
	}

	@Override
	public AI clone() {
		// TODO Auto-generated method stub
		return (AI)new PuppetCNN(TIME_BUDGET, ITERATIONS_BUDGET, scripts);
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

}
