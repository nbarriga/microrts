package ai.puppet;

import java.util.Collection;

import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.pathfinding.PathFinding;
import rts.GameState;
import util.Pair;

public abstract class ConfigurableScript extends AbstractionLayerAI {
	class Options{
		int id;
		int options[];
		public Options(int id,int[] options){
			this.id=id;
			this.options=options;
		}
		public int numOptions(){
			return options.length;
		}
		public int getOption(int o){
			return options[0];
		}
	}
	
	public ConfigurableScript(PathFinding a_pf) {
		super(a_pf);
	}
	
	 public void reset(){
		initializeChoices();
		setDefaultChoices();
	 }
	public abstract Collection<Options> getAllChoicePoints();
	public abstract Collection<Options> getApplicableChoicePoints(int player, GameState gs);
	public abstract void setChoices(Collection<Pair<Integer,Integer>> choices);
	public abstract void setDefaultChoices();
	public abstract void initializeChoices();
}
