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
	String definition=null;
	String model=null;
	boolean sample=false;
	public PuppetCNN(SingleChoiceConfigurableScript scripts) {
		this.scripts=scripts;
	}
	public PuppetCNN(SingleChoiceConfigurableScript scripts, String definition, String model, boolean sample) {
		this(scripts);
		this.definition=definition;
		this.model=model;
		this.sample=sample;
	}

	void establishConnection(int size){
		net=new CaffeInterface();
		try {
			net.start(8080);
			if(model!=null && definition!=null){
				net.send(definition+" "+model+"\n");
			}else{
				net.send("data/caffe/puppet"+size+".prototxt data/caffe/puppet"+size+".caffemodel\n");
			}
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

	int lastAction=-1;

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		if(!gs.canExecuteAnyAction(player))return null;
		if(net==null)establishConnection(gs.getPhysicalGameState().getWidth());

		CNNGameState cnngs=new CNNGameState(gs);
		net.send(cnngs.getHeaderExtraCompressed(1, player)+cnngs.getPlanesCompressed());   
		scripts.setDefaultChoices();
		lastAction = sample?net.sampleIndex():net.getMaxIndex();
		scripts.setChoices(Collections.singletonList(new Pair<Integer,Integer>(0,lastAction)));

		return scripts.getAction(player, gs);
	}

	public int getLastAction(){
		return lastAction;
	}
	public void sample(boolean sample){
		this.sample=sample;
	}
	@Override
	public AI clone() {
		// TODO Auto-generated method stub
		return (AI)new PuppetCNN((SingleChoiceConfigurableScript)scripts.clone(),definition,model,sample);
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
