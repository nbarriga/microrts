package ai.puppet;

import ai.core.AI;
import ai.core.InterruptibleAIWithComputationBudget;
import ai.core.ParameterSpecification;

import java.util.Collection;
import java.util.List;
import rts.GameState;
import rts.PlayerAction;
import util.Pair;

public class PuppetNoPlan extends InterruptibleAIWithComputationBudget {

	PuppetBase puppet;
	public PuppetNoPlan(PuppetBase puppet) {
		super(puppet.MAX_TIME, puppet.MAX_ITERATIONS);
		this.puppet=puppet;
	}

	@Override
	public void startNewComputation(int player, GameState gs) throws Exception {
		puppet.restartSearch(gs,player);
	}

	@Override
	public void computeDuringOneGameFrame() throws Exception {
		puppet.computeDuringOneGameFrame();

	}

	@Override
	public PlayerAction getBestActionSoFar() throws Exception {
		return puppet.getBestActionSoFar();
	}

	@Override
	public void reset() {
		puppet.reset();
	}

	@Override
	public AI clone() {
		PuppetNoPlan clone=new PuppetNoPlan(puppet);
		return clone;
	}

	public String toString(){
		return getClass().getSimpleName() + "("+puppet.toString()+")";
	}
	
	@Override
	public String statisticsString() {
		return puppet.statisticsString();
	}   
        
        
    @Override
    public List<ParameterSpecification> getParameters() {
        return puppet.getParameters();
    }    
    
    public Collection<Pair<Integer, Integer>> getBestChoicesSoFar() throws Exception {
		return puppet.getBestChoicesSoFar();
	}
}
