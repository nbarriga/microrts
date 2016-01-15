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

import org.jdom.JDOMException;

import com.sun.javafx.geom.transform.GeneralTransform3D;

import ai.RandomAI;
import ai.RandomBiasedAI;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.GreedyPathFinding;
import ai.ahtn.AHTNAI;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ContinuingAI;
import ai.core.InterruptibleAIWithComputationBudget;
import ai.core.PseudoContinuingAI;
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
import ai.puppet.PuppetSearchAB;
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
	
	private static List<AI> bots1 = new LinkedList<AI>();
	private static List<AI> bots2 = new LinkedList<AI>();
	private static List<PhysicalGameState> maps = new LinkedList<PhysicalGameState>();
	
	public static void loadMaps(String mapFileName) throws IOException  {
		try (Stream<String> lines = Files.lines(Paths.get(mapFileName), Charset.defaultCharset())) {
			lines.forEachOrdered(line -> {
				try {
					if(!line.startsWith("#")&&!line.isEmpty())maps.add(PhysicalGameState.load(line,UnitTypeTable.utt));
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
			return new LightRush(UnitTypeTable.utt, new BFSPathFinding());
		case "RangedRush":
			return new RangedRush(UnitTypeTable.utt, new BFSPathFinding());
		case "HeavyRush":
			return new HeavyRush(UnitTypeTable.utt, new BFSPathFinding());
		case "WorkerRush":
			return new WorkerRush(UnitTypeTable.utt, new BFSPathFinding());		
		case "PortfolioAI":
			return new PortfolioAI(new AI[]{new WorkerRush(UnitTypeTable.utt, new BFSPathFinding()),
                    new LightRush(UnitTypeTable.utt, new BFSPathFinding()),
                    new RangedRush(UnitTypeTable.utt, new BFSPathFinding()),
                    new RandomBiasedAI()}, 
           new boolean[]{true,true,true,false}, 
           TIME, MAX_PLAYOUTS, PLAYOUT_TIME*4, new SimpleSqrtEvaluationFunction3());
		case "IDRTMinimax":
			return new IDRTMinimax(TIME, new SimpleSqrtEvaluationFunction3());
		case "IDRTMinimaxRandomized":
			return new IDRTMinimaxRandomized(TIME, RANDOMIZED_AB_REPEATS, 
					new SimpleSqrtEvaluationFunction3());
		case "IDABCD":
			return new IDABCD(TIME, MAX_PLAYOUTS, new WorkerRush(UnitTypeTable.utt, new GreedyPathFinding()), 
					PLAYOUT_TIME, new SimpleSqrtEvaluationFunction3(), false);
		case "MonteCarlo1":
			return new MonteCarlo(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, new RandomBiasedAI(), 
					new SimpleSqrtEvaluationFunction3());
		case "MonteCarlo2":
			return new MonteCarlo(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_ACTIONS, new RandomBiasedAI(), 
					new SimpleSqrtEvaluationFunction3());
			// by setting "MAX_DEPTH = 1" in the next two bots, this effectively makes them Monte Carlo search, instead of Monte Carlo Tree Search
		case "NaiveMCTS1"://MonteCarlo
			return new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, 1, 0.33f, 0.0f, 0.75f, 
					new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3());
		case "NaiveMCTS2"://epsilon-greedy MonteCarlo
			return new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, 1, 1.00f, 0.0f, 0.25f, 
					new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3());
		case "UCT":
			return new UCT(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH, new RandomBiasedAI(), 
					new SimpleSqrtEvaluationFunction3());
		case "DownsamplingUCT":
			return new DownsamplingUCT(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_ACTIONS, MAX_DEPTH, 
					new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3());
		case "UCTUnitActions":
			return new UCTUnitActions(TIME, PLAYOUT_TIME, MAX_DEPTH*10, new RandomBiasedAI(), 
					new SimpleSqrtEvaluationFunction3());
		case "NaiveMCTS3"://NaiveMCTS
			return new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH, 0.33f, 0.0f, 0.75f, 
					new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3());
		case "NaiveMCTS4"://epsilon-greedy MCTS
			return new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH, 1.00f, 0.0f, 0.25f, 
					new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3());
		case "AHTN-LL":
			try {
				return new AHTNAI("ahtn/microrts-ahtn-definition-lowest-level.lisp",TIME, 
						MAX_PLAYOUTS, PLAYOUT_TIME, new SimpleSqrtEvaluationFunction3(),
						new RandomBiasedAI());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		case "AHTN-LLPF":
			try {
				return new AHTNAI("ahtn/microrts-ahtn-definition-low-level.lisp",TIME, 
						MAX_PLAYOUTS, PLAYOUT_TIME, new SimpleSqrtEvaluationFunction3(),
						new RandomBiasedAI());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		case "AHTN-P":
			try {
				return new AHTNAI("ahtn/microrts-ahtn-definition-portfolio.lisp",TIME, 
						MAX_PLAYOUTS, PLAYOUT_TIME, new SimpleSqrtEvaluationFunction3(),
						new RandomBiasedAI());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		case "AHTN-F":
			try {
				return new AHTNAI("ahtn/microrts-ahtn-definition-flexible-portfolio.lisp",TIME, 
						MAX_PLAYOUTS, PLAYOUT_TIME, new SimpleSqrtEvaluationFunction3(),
						new RandomBiasedAI());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		case "AHTN-FST":
			try {
				return new AHTNAI("ahtn/microrts-ahtn-definition-flexible-single-target-portfolio.lisp",TIME, 
						MAX_PLAYOUTS, PLAYOUT_TIME, new SimpleSqrtEvaluationFunction3(),
						new RandomBiasedAI());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		case "Puppet":
			return new PuppetSearchAB(TIME, MAX_PLAYOUTS, 
					new AI[]{
							new WorkerRush(UnitTypeTable.utt, new BFSPathFinding()),
							new LightRush(UnitTypeTable.utt, new BFSPathFinding()),
							new RangedRush(UnitTypeTable.utt, new BFSPathFinding()),
							new HeavyRush(UnitTypeTable.utt, new BFSPathFinding())
							}, 
					new SimpleSqrtEvaluationFunction3());
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
        loadBots1(args[0]);
        loadBots2(args[1]);
        processBots(bots1);
        processBots(bots2);
        loadMaps(args[2]);
        PrintStream out = new PrintStream(new File(args[3]));
        int iterations = Integer.parseInt(args[4]);
        
  //        Experimenter.runExperimentsPartiallyObservable(bots, maps, 10, 3000, 300, true, out);
        //Experimenter.runExperiments(bots, maps, 10, 3000, 300, false, out);
      
        // Separate the matches by map:
        for(PhysicalGameState map:maps){
        	ExperimenterAsymmetric.runExperiments(bots1,bots2, 
        			Collections.singletonList(map), iterations, 5000, 300, true, out);
        }
//        Experimenter.runGame(getBot("RangedRush"), getBot("RandomBiasedAI"), 
////                 Experimenter.runGame(new ContinuingAI((InterruptibleAIWithComputationBudget)getBot("IDABCD")), 
////                		 getBot("RandomAI"), 
//        		PhysicalGameState.load("maps/BloodBath.xml",UnitTypeTable.utt), 1, 5000, 
//        		300, true,  out, false);
        
//      ExperimenterAsymmetric.runExperiments(
////    	    Collections.singletonList(new ContinuingAI((InterruptibleAIWithComputationBudget)getBot("IDABCD"))),
//    		  Collections.singletonList(getBot("RangedRush")),
//    		  Collections.singletonList(getBot("RandomBiasedAI")), 
//    		  Collections.singletonList(PhysicalGameState.load("maps/BloodBath.xml",UnitTypeTable.utt)), 
//    		  1, 5000, 300, true, out);
    }

}
