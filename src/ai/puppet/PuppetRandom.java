package ai.puppet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import rts.CNNGameState;
import rts.GameState;
import rts.PlayerAction;
import tests.CaffeInterface;
import util.Pair;

public class PuppetRandom extends AI {

	CaffeInterface net=new CaffeInterface();
	SingleChoiceConfigurableScript scripts;
	 static Random generator = new Random();
	 int switchTime;
	public PuppetRandom(int switchTime,SingleChoiceConfigurableScript scripts) {
		this.scripts=scripts;
		this.switchTime=switchTime;
	}

	@Override
	public void reset() {

	}

	int lastAction=20000;
	int action=-1;
	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		if(gs.getTime()<lastAction || gs.getTime()>lastAction+switchTime){
		scripts.setDefaultChoices();
		int action = generator.nextInt(scripts.scripts.length);
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
		return (AI)new PuppetRandom( switchTime,scripts);
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

}
