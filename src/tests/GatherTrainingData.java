package tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import javax.swing.JFrame;

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
import ai.puppet.PuppetSearchAB;
import ai.puppet.SingleChoiceConfigurableScript;
import gui.PhysicalGameStatePanel;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class GatherTrainingData {
	private static boolean CONTINUING = true;
	private static int TIME = 100;
	private static int MAX_ACTIONS = 100;
	private static int MAX_PLAYOUTS = -1;
	private static int PLAYOUT_TIME = 200;
	private static int MAX_DEPTH = 10;
	private static int RANDOMIZED_AB_REPEATS = 10;
	private static int MAX_FRAMES = 3000;
	
	private static List<AI> bots1 = new LinkedList<AI>();
	private static List<AI> bots2 = new LinkedList<AI>();
	private static List<PhysicalGameState> maps = new LinkedList<PhysicalGameState>();
	static UnitTypeTable utt= new UnitTypeTable(
			UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
			UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_ALTERNATING);
	
	public static PathFinding getPathFinding(){
//		return new BFSPathFinding();
		return new AStarPathFinding();
//		return new FloodFillPathFinding();
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
//		case "PuppetSingle":
//			return new PuppetSearchAB(TIME, PLAYOUT_TIME,
//					new SingleChoiceConfigurableScript(getPathFinding(),
//							new AI[]{new WorkerRush(utt, getPathFinding()),
//				                    new LightRush(utt, getPathFinding()),
//				                    new RangedRush(utt, getPathFinding()),
//				                    new HeavyRush(utt, getPathFinding())}),
//					getEvaluationFunction());
//		case "PuppetBasic":
//			return new PuppetSearchAB(TIME, PLAYOUT_TIME,
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
        loadBots1(args[0]);
        loadBots2(args[1]);
        processBots(bots1);
        processBots(bots2);
        loadMaps(args[2]);
        PrintStream out = new PrintStream(new File(args[3]));
        int iterations = Integer.parseInt(args[5]);
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(args[4]));
        
        runExperiments(bots1,bots2,
        				maps, utt, iterations, MAX_FRAMES, 300, false, out, oos);
        
       
	      oos.close();

    }
    public static int play(AI ai1, AI ai2, GameState gs, int max_frames, int max_inactive_cycles, int lastTimeActionIssued , JFrame w) throws Exception{
    	System.gc();
    	boolean gameover = false;
    	do {
    		PlayerAction pa1 = ai1.getAction(0, gs);
    		PlayerAction pa2 = ai2.getAction(1, gs);
    		if (gs.issueSafe(pa1)) lastTimeActionIssued = gs.getTime();
    		if (gs.issueSafe(pa2)) lastTimeActionIssued = gs.getTime();
    		gameover = gs.cycle();
    		if (w!=null){ 
    			w.repaint();
    			try {
    				Thread.sleep(1);    // give time to the window to repaint
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}
    	} while (!gameover && 
    			(gs.getTime() < max_frames) && 
    			(gs.getTime() - lastTimeActionIssued < max_inactive_cycles));
    	return lastTimeActionIssued;
    }
    public static void runExperiments(
    		List<AI> initBots, 
    		List<AI> finalBots,
    		List<PhysicalGameState> maps, 
    		UnitTypeTable utt, 
    		int iterations, 
    		int max_cycles, 
    		int max_inactive_cycles, 
    		boolean visualize,
    		PrintStream out,
    		ObjectOutputStream oos) throws Exception {
        Random rnd= new Random();
        

        System.out.println(new Date(System.currentTimeMillis()));
        for(PhysicalGameState pgs:maps) 
        {
        	for (int i = 0; i < iterations; i++) 
        	{
                int cutFrame=rnd.nextInt(2000);
                int numInitCycles=rnd.nextInt(initBots.size())+1;
        		int lastTimeActionIssued = 0;
    			GameState gs = new GameState(pgs.clone(),utt);
    			JFrame w = null;
    			if (visualize) w = PhysicalGameStatePanel.newVisualizer(gs, 600, 600);

    			for(int cycle=0;cycle<numInitCycles;cycle++)
    			{

    				AI ai1=initBots.get(rnd.nextInt(initBots.size())).clone();
    				AI ai2=initBots.get(rnd.nextInt(initBots.size())).clone();
    				ai1.reset();
    				ai2.reset();

    				lastTimeActionIssued=play(ai1,ai2,gs,cutFrame/numInitCycles,max_inactive_cycles,lastTimeActionIssued,w);

    			}

    			//write start state
    			out.println("Starting at frame "+gs.getTime()+" from "+numInitCycles+" init scripts");

    			if(gs.gameover()){
    				out.println("Game over at init phase! Skipping.");
    				continue;
    			}
    			GameState storeGs = gs.clone();
    			
    			int firstPlayerWins=0;
    			int firstPlayerLoses=0;
    			for (int ai1_idx = 0; ai1_idx < finalBots.size(); ai1_idx++) 
    			{
//    				for (int ai2_idx = 0; ai2_idx < finalBots.size(); ai2_idx++) 
//    				{

    					AI ai1 = finalBots.get(ai1_idx).clone();
    					AI ai2 = finalBots.get(ai1_idx).clone();

    					ai1.reset();
    					ai2.reset();

    					GameState newGs = gs.clone();
    					lastTimeActionIssued=play(ai1,ai2,newGs,max_cycles,max_inactive_cycles,lastTimeActionIssued,w);



    					int winner = newGs.winner();
    					
    					if(winner==0){
    						firstPlayerWins++;
    					}else if(winner==1){
    						firstPlayerLoses++;
    					}
//    					out.println("Winner: " + winner + "  in " + newGs.getTime() + " cycles");
//    					out.println(ai1 + " : " + ai1.statisticsString());
//    					out.println(ai2 + " : " + ai2.statisticsString());
//    					out.flush();

    				}                    
//    			}
    			
    			if(firstPlayerWins+firstPlayerLoses>0){
    				out.println("First player win ratio: "+(firstPlayerWins)/((float)firstPlayerWins+firstPlayerLoses));
    				oos.writeObject(storeGs);
    				oos.writeInt(firstPlayerWins*100/(firstPlayerWins+firstPlayerLoses));
    			}else{
    				out.println("All games were ties! Skipping.");
    			}
    			
    			if (w!=null) w.dispose();
        	}
        }
        System.out.println(new Date(System.currentTimeMillis()));

    }

}
