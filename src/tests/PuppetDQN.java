package tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.zip.ZipInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import ai.evaluation.EvaluationFunction;
import ai.evaluation.LanchesterEvaluationFunction;
// import ai.evaluation.ScriptEvaluation;
import ai.evaluation.SimpleEvaluationFunction;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.evaluation.NetEvaluationFunction;
import ai.core.AI;
import ai.core.ContinuingAI;
import ai.abstraction.pathfinding.FloodFillPathFinding;
import ai.abstraction.WorkerRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.RandomBiasedAI;
import ai.abstraction.HeavyRush;
import rts.CNNGameState;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Trace;
import rts.units.UnitTypeTable;
import ai.puppet.BasicConfigurableScript;
import ai.puppet.PuppetNoPlan;
import ai.puppet.PuppetSearchAB;
import ai.puppet.PuppetSearchMCTS;
import ai.puppet.SingleChoiceConfigurableScript;
import ai.puppet.PuppetCNN;
import ai.abstraction.pathfinding.PathFinding;

public class PuppetDQN {

	static UnitTypeTable utt = new UnitTypeTable(
			UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
			UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
	
	static Random generator = new Random();
	
	public static void listf(String directoryName, List<File> files) {
	    File directory = new File(directoryName);

	    // get all the files from a directory
	    File[] fList = directory.listFiles();

	    Arrays.sort(fList);
	    
	    for (File file : fList) {
	        if (file.isFile()) {
	            files.add(file);
	        } else if (file.isDirectory()) {
	            listf(file.getAbsolutePath(), files);
	        }
	    }
	}
	static class Sample{
		
		public Sample(List<GameState> states, int length, int winner) {
			this.states = states;
			this.length = length;
			this.winner = winner;
		}
		public List<GameState> states;
		public int length;
		public int winner;
			
	}
	public static Sample getSamples(File trace, int n) throws JDOMException, IOException{
		List<GameState> states = new ArrayList<GameState>();

		ZipInputStream zip=new ZipInputStream(new FileInputStream(trace.getAbsolutePath()));
		zip.getNextEntry();
		Trace t = new Trace(new SAXBuilder().build(zip).getRootElement(), utt);
		zip.close();

		int winner = t.getGameStateAtCycle(t.getLength()).winner();

		if(winner == -1) {
			return new Sample(states,t.getLength(),-1);
		}


		for(int i=0; i<n; i++)
		{
			states.add(t.getGameStateAtCycle(generator.nextInt(t.getLength())));
		}
		return new Sample(states,t.getLength(),winner);
	}
	static void simulate(GameState gs, AI ai1, AI ai2, int player1, int player2, int time)
		throws Exception {
		assert(player1!=player2);
		int timeOut = gs.getTime() + time;
		boolean gameover = gs.gameover();
		while(!gameover && gs.getTime()<timeOut) {
			if (gs.isComplete()) {
				gameover = gs.cycle();
			} else {
				gs.issueSafe(ai1.getAction(player1, gs));
				gs.issueSafe(ai2.getAction(player2, gs));
			}
		}
	}
	//simulate until next action for player
	static int simulateOneMove(GameState gs, PuppetCNN ai1, PuppetCNN ai2, int player1, int player2, int player)
		throws Exception {
		assert(player1!=player2);
		boolean gameover = gs.gameover();
		if(!gs.canExecuteAnyAction(player)) throw new Exception();
		do{
			gs.issueSafe(ai1.getAction(player1, gs));
			gs.issueSafe(ai2.getAction(player2, gs));
			gameover = gs.cycle();
		}while(!gameover && !gs.canExecuteAnyAction(player));
		return player==player1?ai1.getLastAction():ai2.getLastAction();
	}
	private static void createDir(String dir){
		File d = new File(dir);
		d.mkdirs();
	}

	class Transition{
		String current;
		int action;
		float reward;
		String next;
	}
	public static boolean gameover(GameState gs){
		return gs.gameover()||gs.getTime()>12000;
	}
	static int numActions = 4;
	static int action_time = 100;
	//static String map="maps/basesWorkers8x8.xml";
	//static String map="maps/BroodWar/(4)BloodBath.scmA.xml";
	//
	static PathFinding getPathFinding(){return new FloodFillPathFinding();}
	static EvaluationFunction getEvaluationFunction(){return NetEvaluationFunction.getInstance(128);}
	
	static ArrayList<PhysicalGameState> maps=new ArrayList<PhysicalGameState>();
	static AI[] ais = {
			new WorkerRush(utt,getPathFinding()),
			new LightRush(utt,getPathFinding()),
			new RangedRush(utt,getPathFinding()),
			new HeavyRush(utt,getPathFinding()),
			//new ContinuingAI(new NaiveMCTS(100, -1, 100, 10, 0.33f, 0.0f, 0.75f, 
			//		new RandomBiasedAI(), new SimpleEvaluationFunction()))
	/*	                        new PuppetNoPlan(
                                        new PuppetSearchAB(
                                                        100, -1,
                                                        -1, -1,
                                                        100, -1,
                                                        new SingleChoiceConfigurableScript(getPathFinding(),
                                                                        new AI[]{
                                                                                        new WorkerRush(utt, getPathFinding()),
                                                                                        new LightRush(utt, getPathFinding()),
                                                                                        new RangedRush(utt, getPathFinding()),
                                                                                        new HeavyRush(utt, getPathFinding()),
                                                        }),
                                                        getEvaluationFunction())),
					new PuppetNoPlan(
                                        new PuppetSearchAB(
                                                        100, -1,
                                                        -1, -1,
                                                        100, -1,
                                                        new BasicConfigurableScript(utt, getPathFinding()),
                                                        getEvaluationFunction())),
	*/	new PuppetCNN(
			new SingleChoiceConfigurableScript(getPathFinding(),
				new AI[]{new WorkerRush(utt, getPathFinding()),
					new LightRush(utt, getPathFinding()),
					new RangedRush(utt, getPathFinding()),
					new HeavyRush(utt, getPathFinding())}
				)
			)
	};
	static ArrayList<PuppetCNN> puppetCNNPool = new ArrayList<PuppetCNN>(Arrays.asList(
		new PuppetCNN(
			new SingleChoiceConfigurableScript(getPathFinding(),
				new AI[]{new WorkerRush(utt, getPathFinding()),
					new LightRush(utt, getPathFinding()),
					new RangedRush(utt, getPathFinding()),
					new HeavyRush(utt, getPathFinding())}
				), "../cnn-rts/DQNdeploy.prototxt","../cnn-rts/snapshots/DQN-iter-0.caffemodel", true
			)));
	static PuppetCNN puppetCNNcurrent =new PuppetCNN(
			new SingleChoiceConfigurableScript(getPathFinding(),
				new AI[]{new WorkerRush(utt, getPathFinding()),
					new LightRush(utt, getPathFinding()),
					new RangedRush(utt, getPathFinding()),
					new HeavyRush(utt, getPathFinding())}
				), "../cnn-rts/DQNdeploy.prototxt","../cnn-rts/snapshots/DQN-current.caffemodel", true
			);

	public static void test(CaffeInterface net) throws Exception{
		System.out.println("Testing");
		puppetCNNcurrent.sample(false);
	 	ExperimenterAsymmetric.runExperiments(Arrays.asList(puppetCNNcurrent),Arrays.asList(ais),maps, utt, 1, 12000, 300, false, new PrintStream(System.out), false, false, null);
		puppetCNNcurrent.sample(true);
	}
	public static void main(String[] args) throws Exception{
		try (Stream<String> lines = Files.lines(Paths.get("maps.txt"), Charset.defaultCharset())) {
			lines.forEachOrdered(line -> {
				try {
					if(!line.startsWith("#")&&!line.isEmpty())maps.add(PhysicalGameState.load(line,utt));
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			});
		}
		//Algorithm 1: deep Q-learning with experience replay.
		int M=10000;
		int C=1000;
		float epsilon_start = 1.0f;
		float epsilon_end = 0.1f;
		float epsilon_anneal = M;

		CaffeInterface net=new CaffeInterface();
		net.start(8081);

		int it=0;
		for(int ep=0;ep<M;ep++){
			GameState gs=new GameState(maps.get(generator.nextInt(maps.size())).clone(),utt);
			CNNGameState cnngs=new CNNGameState(gs);
			
			PuppetCNN opponent=(PuppetCNN)puppetCNNPool.get(generator.nextInt(puppetCNNPool.size())).clone();//use an older network for self-play

			opponent.reset();
			puppetCNNcurrent.reset();
			int player = generator.nextInt(2);
			while(!gameover(gs)){
				//float epsilon = java.lang.Math.max(epsilon_end,epsilon_start - it*(epsilon_start-epsilon_end)/epsilon_anneal);
				String current = cnngs.getHeaderExtraCompressed(1, player)+cnngs.getPlanesCompressed();
				
				int action = simulateOneMove(gs, puppetCNNcurrent, opponent, player, 1-player, player);

				int winner = gs.winner();
				float reward=((!gameover(gs)) || winner<0)?0:(winner==player?1:-1);
				net.send(current);//current state
				net.send(Integer.toString(action));//action
				net.send(Float.toString(reward));//reward
				String next = cnngs.getHeaderExtraCompressed(1, player)+cnngs.getPlanesCompressed();
				assert current != next;
				net.send(next);//next state
				net.send(Integer.toString(gameover(gs)?1:0));//is it terminal
				it++;
			}
			if(it%C==0){
				System.out.println("Adding new opponent to pool");
				puppetCNNPool.add(new PuppetCNN(
							new SingleChoiceConfigurableScript(getPathFinding(),
								new AI[]{new WorkerRush(utt, getPathFinding()),
									new LightRush(utt, getPathFinding()),
									new RangedRush(utt, getPathFinding()),
									new HeavyRush(utt, getPathFinding())}
								), "../cnn-rts/DQNdeploy.prototxt","../cnn-rts/snapshots/DQN-iter-"+it+".caffemodel", true)
						);
			}
		}
		

		

	}
	

}
