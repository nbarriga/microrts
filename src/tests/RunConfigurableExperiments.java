package tests;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import ai.RandomAI;
import ai.RandomBiasedAI;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.FloodFillPathFinding;
import ai.abstraction.pathfinding.GreedyPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.ahtn.AHTNAI;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ContinuingAI;
import ai.core.InterruptibleAIWithComputationBudget;
import ai.core.PseudoContinuingAI;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleEvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction2;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.mcts.uct.DownsamplingUCT;
import ai.mcts.uct.UCT;
import ai.mcts.uct.UCTUnitActions;
import ai.minimax.ABCD.IDABCD;
import ai.minimax.RTMiniMax.IDRTMinimax;
import ai.minimax.RTMiniMax.IDRTMinimaxRandomized;
import ai.montecarlo.MonteCarlo;
import ai.portfolio.PortfolioAI;
import ai.puppet.BasicConfigurableScript;
import ai.puppet.PuppetNoPlan;
import ai.puppet.PuppetSearchAB;
import ai.puppet.PuppetSearchMCTS;
import ai.puppet.SingleChoiceConfigurableScript;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class RunConfigurableExperiments {
	private static boolean CONTINUING = true;
	private static int TIME = 100;
	private static int MAX_ACTIONS = 100;
	private static int MAX_PLAYOUTS = -1;
	private static int PLAYOUT_TIME = 100;
	private static int MAX_DEPTH = 10;
	private static int RANDOMIZED_AB_REPEATS = 10;
	private static int MAX_FRAMES = 3000;
	
	private static List<AI> bots1 = new LinkedList<AI>();
	private static List<AI> bots2 = new LinkedList<AI>();
	private static List<PhysicalGameState> maps = new LinkedList<PhysicalGameState>();
	static UnitTypeTable utt= new UnitTypeTable(
			UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
			UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
	
	public static PathFinding getPathFinding(){
//		return new BFSPathFinding();
//		return new AStarPathFinding();
		return new FloodFillPathFinding();
	}
	public static EvaluationFunction getEvaluationFunction(){
		return new SimpleEvaluationFunction();
	}
	
	public static void loadMaps(String mapFileName) throws IOException  {
		try (Stream<String> lines = Files.lines(Paths.get(mapFileName), Charset.defaultCharset())) {
			lines.forEachOrdered(line -> {
				try {
					if(!line.startsWith("#")&&!line.isEmpty())maps.add(PhysicalGameState.load(line,utt));
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			});
		}
	}

	public static AI getBot(String botName){
		switch(botName){
		case "RandomAI":
			return new RandomAI();
		case "RandomBiasedAI":
			return new RandomBiasedAI();
		case "LightRush":
			return new LightRush(utt, getPathFinding());
		case "RangedRush":
			return new RangedRush(utt, getPathFinding());
		case "HeavyRush":
			return new HeavyRush(utt, getPathFinding());
		case "WorkerRush":
			return new WorkerRush(utt, getPathFinding());	
		case "BasicConfigurableScript":
			return new BasicConfigurableScript(utt, getPathFinding());
		case "SingleChoiceConfigurableScript":
			return new SingleChoiceConfigurableScript( getPathFinding(),
					new AI[]{new WorkerRush(utt, getPathFinding()),
		                    new LightRush(utt, getPathFinding()),
		                    new RangedRush(utt, getPathFinding()),
		                    new HeavyRush(utt, getPathFinding())});
		case "PortfolioAI":
			return new PortfolioAI(new AI[]{new WorkerRush(utt, getPathFinding()),
                    new LightRush(utt, getPathFinding()),
                    new RangedRush(utt, getPathFinding()),
                    new RandomBiasedAI()}, 
           new boolean[]{true,true,true,false}, 
           TIME, MAX_PLAYOUTS, PLAYOUT_TIME*4, getEvaluationFunction());
		case "IDRTMinimax":
			return new IDRTMinimax(TIME, getEvaluationFunction());
		case "IDRTMinimaxRandomized":
			return new IDRTMinimaxRandomized(TIME, RANDOMIZED_AB_REPEATS, 
					getEvaluationFunction());
		case "IDABCD":
			return new IDABCD(TIME, MAX_PLAYOUTS, new WorkerRush(utt, getPathFinding()), 
					PLAYOUT_TIME, getEvaluationFunction(), false);
		case "MonteCarlo1":
			return new MonteCarlo(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, new RandomBiasedAI(), 
					getEvaluationFunction());
		case "MonteCarlo2":
			return new MonteCarlo(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_ACTIONS, new RandomBiasedAI(), 
					getEvaluationFunction());
			// by setting "MAX_DEPTH = 1" in the next two bots, this effectively makes them Monte Carlo search, instead of Monte Carlo Tree Search
		case "NaiveMCTS1"://MonteCarlo
			return new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, 1, 0.33f, 0.0f, 0.75f, 
					new RandomBiasedAI(), getEvaluationFunction());
		case "NaiveMCTS2"://epsilon-greedy MonteCarlo
			return new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, 1, 1.00f, 0.0f, 0.25f, 
					new RandomBiasedAI(), getEvaluationFunction());
		case "UCT":
			return new UCT(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH, new RandomBiasedAI(), 
					getEvaluationFunction());
		case "DownsamplingUCT":
			return new DownsamplingUCT(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_ACTIONS, MAX_DEPTH, 
					new RandomBiasedAI(), getEvaluationFunction());
		case "UCTUnitActions":
			return new UCTUnitActions(TIME, PLAYOUT_TIME, MAX_DEPTH*10, new RandomBiasedAI(), 
					getEvaluationFunction());
		case "NaiveMCTS3"://NaiveMCTS
			return new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH, 0.33f, 0.0f, 0.75f, 
					new RandomBiasedAI(), getEvaluationFunction());
		case "NaiveMCTS4"://epsilon-greedy MCTS
			return new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH, 1.00f, 0.0f, 0.25f, 
					new RandomBiasedAI(), getEvaluationFunction());
		case "AHTN-LL":
			try {
				return new AHTNAI("ahtn/microrts-ahtn-definition-lowest-level.lisp",TIME, 
						MAX_PLAYOUTS, PLAYOUT_TIME, getEvaluationFunction(),
						new RandomBiasedAI());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		case "AHTN-LLPF":
			try {
				return new AHTNAI("ahtn/microrts-ahtn-definition-low-level.lisp",TIME, 
						MAX_PLAYOUTS, PLAYOUT_TIME, getEvaluationFunction(),
						new RandomBiasedAI());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		case "AHTN-P":
			try {
				return new AHTNAI("ahtn/microrts-ahtn-definition-portfolio.lisp",TIME, 
						MAX_PLAYOUTS, PLAYOUT_TIME, getEvaluationFunction(),
						new RandomBiasedAI());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		case "AHTN-F":
			try {
				return new AHTNAI("ahtn/microrts-ahtn-definition-flexible-portfolio.lisp",TIME, 
						MAX_PLAYOUTS, PLAYOUT_TIME, getEvaluationFunction(),
						new RandomBiasedAI());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		case "AHTN-FST":
			try {
				return new AHTNAI("ahtn/microrts-ahtn-definition-flexible-single-target-portfolio.lisp",TIME, 
						MAX_PLAYOUTS, PLAYOUT_TIME, getEvaluationFunction(),
						new RandomBiasedAI());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		case "PuppetABCDSingle":
			return //new PuppetNoPlan(
					new PuppetSearchAB(
					100, -1,
					5000, -1,
					PLAYOUT_TIME,
					new SingleChoiceConfigurableScript(getPathFinding(),
							new AI[]{
									new WorkerRush(utt, getPathFinding()),
				                    new LightRush(utt, getPathFinding()),
				                    new RangedRush(utt, getPathFinding()),
				                    new HeavyRush(utt, getPathFinding()),
				                    }),
					getEvaluationFunction())
					//)
					;
		case "PuppetABCDSingleNoPlan":
			return new PuppetNoPlan(
					new PuppetSearchAB(
					100, -1,
					-1, -1,
					PLAYOUT_TIME,
					new SingleChoiceConfigurableScript(getPathFinding(),
							new AI[]{
									new WorkerRush(utt, getPathFinding()),
				                    new LightRush(utt, getPathFinding()),
				                    new RangedRush(utt, getPathFinding()),
				                    new HeavyRush(utt, getPathFinding()),
				                    }),
					getEvaluationFunction())
					)
					;
//		case "PuppetABCDBasic":
//			return new PuppetSearchAB(TIME, PLAYOUT_TIME, 256,
//					new BasicConfigurableScript(utt, getPathFinding()), 
//					getEvaluationFunction());
		case "PuppetMCTSSingle":
			return //new PuppetNoPlan(
					new PuppetSearchMCTS(
					100, -1,
					5000, -1,
					PLAYOUT_TIME, PLAYOUT_TIME,
					//new RandomBiasedAI(),
					new WorkerRush(utt, getPathFinding()),
					new SingleChoiceConfigurableScript(getPathFinding(),
							new AI[]{new WorkerRush(utt, getPathFinding()),
				                    new LightRush(utt, getPathFinding()),
				                    new RangedRush(utt, getPathFinding()),
				                    new HeavyRush(utt, getPathFinding())}),
					getEvaluationFunction())
//					)
					;
		case "PuppetMCTSSingleNoPlan":
			return new PuppetNoPlan(new PuppetSearchMCTS(
					100, -1,
					-1, -1,
					PLAYOUT_TIME, PLAYOUT_TIME,
					//new RandomBiasedAI(),
					new WorkerRush(utt, getPathFinding()),
					new SingleChoiceConfigurableScript(getPathFinding(),
							new AI[]{new WorkerRush(utt, getPathFinding()),
				                    new LightRush(utt, getPathFinding()),
				                    new RangedRush(utt, getPathFinding()),
				                    new HeavyRush(utt, getPathFinding())}),
					getEvaluationFunction())
					);
//		case "PuppetMCTSBasic":
//			return new PuppetSearchMCTS(TIME, PLAYOUT_TIME, PLAYOUT_TIME, 256,
//					//new RandomBiasedAI(),
//					new WorkerRush(utt, getPathFinding()),
//					new BasicConfigurableScript(utt, getPathFinding()), 
//					getEvaluationFunction());
		default:
			throw new RuntimeException("AI not found");
		}

	}

	public static void loadBots1(String botFileName) throws IOException{
		try (Stream<String> lines = Files.lines(Paths.get(botFileName), Charset.defaultCharset())) {
			lines.forEachOrdered(line -> {
				try {
					if(!line.startsWith("#")&&!line.isEmpty())bots1.add(getBot(line));
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			});
		}
	}
	public static void loadBots2(String botFileName) throws IOException{
		try (Stream<String> lines = Files.lines(Paths.get(botFileName), Charset.defaultCharset())) {
			lines.forEachOrdered(line -> {
				try {
					if(!line.startsWith("#")&&!line.isEmpty())bots2.add(getBot(line));
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			});
		}
	}
	public static void processBots(List<AI> bots){
		if (CONTINUING) {
			// Find out which of the bots can be used in "continuing" mode:
			List<AI> botstemp = new LinkedList<>();
			for(AI bot:bots) {
				if (bot instanceof AIWithComputationBudget) {
					if (bot instanceof InterruptibleAIWithComputationBudget) {
						botstemp.add(new ContinuingAI((InterruptibleAIWithComputationBudget)bot));
					} else {
						botstemp.add(new PseudoContinuingAI((AIWithComputationBudget)bot));        				
					}
				} else {
					botstemp.add(bot);
				}
			}
			bots.clear();
			bots.addAll(botstemp);
		}
	}
	
    public static void main(String args[]) throws Exception 
    { 
    	boolean asymetric=!args[1].equals("-");
        loadBots1(args[0]);
        if(asymetric)loadBots2(args[1]);
        processBots(bots1);
        if(asymetric)processBots(bots2);
        loadMaps(args[2]);
        PrintStream out = new PrintStream(new File(args[3]));
        int iterations = Integer.parseInt(args[4]);
        
        if(true){
        	if(asymetric){
        		ExperimenterAsymmetric.runExperiments(bots1,bots2,
        				maps, utt, iterations, MAX_FRAMES, 300, true, out);
        	}else{
        		Experimenter.runExperiments(bots1, 
        				maps, utt, iterations, MAX_FRAMES, 300, false, out);
        	}
        }else{// Separate the matches by map:
        	for(PhysicalGameState map:maps){
        		if(asymetric){
        			ExperimenterAsymmetric.runExperiments(bots1,bots2,
        					Collections.singletonList(map), utt, iterations, MAX_FRAMES, 300, false, out);
        		}else{
        			Experimenter.runExperiments(bots1, 
        					Collections.singletonList(map), utt, iterations, MAX_FRAMES, 300, false, out);
        		}
        	}
        }
    }

}
