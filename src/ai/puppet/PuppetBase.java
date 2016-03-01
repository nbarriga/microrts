package ai.puppet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import rts.GameState;
import util.Pair;
class MoveGenerator{
	ArrayList<ArrayList<Pair<Integer,Integer>>> choices;
	int current=0;
	int player;
	MoveGenerator(ArrayList<ArrayList<Pair<Integer,Integer>>> choices, int player){
		this.choices=choices;
		this.player=player;
	}
	boolean hasNext(){
		return current<choices.size();
	}

	Move next(){
		return new Move(choices.get(current++),player);
	}
	Move last(){
		return new Move(choices.get(current-1),player);
	}
	void ABcut(){
		current=choices.size();
	}
}
class Move{
	Collection<Pair<Integer,Integer>> choices;
	int player;
	public Move(Collection<Pair<Integer,Integer>> choices, int player){
		this.choices=choices;
		this.player=player;
	}
		public String toString(ConfigurableScript<?> script){
			return "choices: "+choices.stream().map(
					(Pair<Integer,Integer>  p)-> 
					new Pair<String,Integer>(script.choicePointValues[p.m_a].name(),p.m_b))
			.collect(Collectors.toList())+", player: "+player;
		}
}


public abstract class PuppetBase extends AI {


    int MAX_TIME = 100;//ms
	EvaluationFunction eval;
	ConfigurableScript<?> script;
	int lastSearchFrame;
	long lastSearchTime;
	
	PuppetBase(int mt, ConfigurableScript<?> script, EvaluationFunction evaluation) {
		super();
		MAX_TIME=mt;
		this.script=script;
		eval=evaluation;
		lastSearchFrame=-1;
		lastSearchTime=-1;
	}

	@Override
	public void reset() {
		lastSearchFrame=-1;
		lastSearchTime=-1;
		script.reset();
	}


	static void simulate(GameState gs, AI ai1, AI ai2, int player1, int player2, int time)
			throws Exception {
				assert(player1!=player2);
				int timeOut = gs.getTime() + time;
				boolean gameover = false;
				while(!gameover && gs.getTime()<timeOut) {
					if (gs.isComplete()) {
						gameover = gs.cycle();
					} else {
						gs.issue(ai1.getAction(player1, gs));
						gs.issue(ai2.getAction(player2, gs));
					}
				}    
			}

}