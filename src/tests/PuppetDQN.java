package tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
		return gs.gameover()||gs.getTime()>5000;
	}
	static int numActions = 4;
	static int action_time = 100;
	//static String map="maps/basesWorkers8x8.xml";
	//static String map="maps/BroodWar/(4)BloodBath.scmA.xml";
	static PathFinding getPathFinding(){return new FloodFillPathFinding();}
	static ArrayList<String> maps=new ArrayList<String>();
	static AI[] ais = {
			new WorkerRush(utt,getPathFinding()),
			new LightRush(utt,getPathFinding()),
			new RangedRush(utt,getPathFinding()),
			new HeavyRush(utt,getPathFinding()),
			//new ContinuingAI(new NaiveMCTS(100, -1, 100, 10, 0.33f, 0.0f, 0.75f, 
			//		new RandomBiasedAI(), new SimpleEvaluationFunction()))
		                        new PuppetNoPlan(
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
                                                        new SimpleEvaluationFunction())
                                        )
	};
	public static void test(CaffeInterface net) throws Exception{
		System.out.println("Testing");
		int M=10;

		//float[][] score=new float[4][4];
		int numScores=ais.length+2;
		float[][] score=new float[numScores][ais.length];
		for(int k=0;k<numScores;k++){
			 System.out.println("Calculating score");
			for(int ep=0;ep<M;ep++){
				//for(int i=0;i<numActions;i++){

				for(int j=0;j<ais.length;j++){


					GameState gs=new GameState(PhysicalGameState.load(maps.get(generator.nextInt(maps.size())), utt),utt);
					CNNGameState cnngs=new CNNGameState(gs);
					for(int i=0;i<ais.length;i++){
						ais[i].reset();
					}
					AI opp=ais[j].clone();
					opp.reset();
					while(!gameover(gs)){
						if(k==numScores-2){
							String current=cnngs.getHeader()+cnngs.getPlanesCompressed();
							net.send("eval\n"+current);
							int action=net.readInt(0);
							assert action<numActions;
							//if(ep+j==0)
							//	System.out.print(action+" ");
							simulate(gs, ais[action].clone(), opp, 0, 1, action_time);
						}else if(k==numScores-1){
							simulate(gs, ais[generator.nextInt(numActions)].clone(), opp, 0, 1, action_time);
						}else{
							simulate(gs, ais[k].clone(), opp, 0, 1, action_time);
						}

						//simulate(gs, ais[i], ais[j], 0, 1, 10000);
					}
					switch(gs.winner()){
						case -1:
							score[k][j]+=0.5;
							//score[i][j]+=0.5;
							//score[j][i]+=0.5;
							break;
						case 0:
							score[k][j]+=1;
							//score[i][j]+=1;
							break;
							//				case 1:
							//					score[j][i]+=1;
							//					break;
					}
					//}
				}
			}
		}
		//for(int i=0;i<numActions;i++){
		System.out.println();
		for(int k=0;k<numScores;k++){
			float acum=0;
			if(k==numScores-2)
				System.out.print("net: ");
			else if(k==numScores-1)
				System.out.print("rnd: ");
			else
				System.out.print(k+": ");
			for(int j=0;j<ais.length;j++){
				System.out.print(score[k][j]*100.0/M+" ");
				acum+=score[k][j];
				//System.out.print(score[i][j]+" ");
			}
			System.out.println(" "+acum*100.0/(M*ais.length));
		}
		//}
	}
	public static void init(CaffeInterface net) throws Exception{
		int it=0;
		while(true){
			if(it>=500)break;
			GameState gs=new GameState(PhysicalGameState.load(maps.get(generator.nextInt(maps.size())), utt),utt);
			CNNGameState cnngs=new CNNGameState(gs);
			
			for(int i=0;i<ais.length;i++){
				ais[i].reset();
			}
	
			AI opponent=ais[generator.nextInt(ais.length)].clone();//use an older network for self-play
			opponent.reset();
			while(!gameover(gs)){
				if((++it)>=500)break;
				String current=cnngs.getHeader()+cnngs.getPlanesCompressed();
				int action=generator.nextInt(numActions);

				
				simulate(gs, ais[action], opponent, 0, 1, action_time);
				int winner =gs.winner();
				float reward=(!gameover(gs))||winner<0?0:1-2*winner;
				net.send("store\n"+current);//current state
				net.send(Integer.toString(action));//action
				net.send(Float.toString(reward));//reward
				String next=cnngs.getHeader()+cnngs.getPlanesCompressed();
				assert current != next;
				net.send(next);//next state
				net.send(Integer.toString(gameover(gs)?1:0));//is it terminal

			}
		}
	}
	public static void main(String[] args) throws Exception{
		try (Stream<String> lines = Files.lines(Paths.get("maps.txt"), Charset.defaultCharset())) {
			lines.forEachOrdered(line -> {
				try {
					if(!line.startsWith("#")&&!line.isEmpty())maps.add(line);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			});
		}
		//Algorithm 1: deep Q-learning with experience replay.
		int M=10000;
		float epsilon_start = 1.0f;
		float epsilon_end = 0.1f;
		float epsilon_anneal = M;

		CaffeInterface net=new CaffeInterface();
		net.start(8080);

					test(net);
		System.out.println("Filling D");
		init(net);
		int it=0;
		for(int ep=0;ep<M;ep++){
			GameState gs=new GameState(PhysicalGameState.load(maps.get(generator.nextInt(maps.size())), utt),utt);
			CNNGameState cnngs=new CNNGameState(gs);
			for(int j=0;j<ais.length;j++){
				ais[j].reset();
			}
			AI opponent=ais[generator.nextInt(ais.length)].clone();//use an older network for self-play
			opponent.reset();
			while(!gameover(gs)){
				float epsilon = java.lang.Math.max(epsilon_end,epsilon_start - it*(epsilon_start-epsilon_end)/epsilon_anneal);
				if(it%1000==0){
					test(net);
					System.out.println("Training, epsilon: "+epsilon+", episode: "+ep+", frame: "+it);
				}
				it++;
				String current=cnngs.getHeader()+cnngs.getPlanesCompressed();
				int action;
				if(generator.nextDouble()<epsilon){
					action=generator.nextInt(numActions);
				}
				else{
					net.send("eval\n"+current);
					action=net.readInt(0);
					assert action<numActions;
					//System.out.println(""+action);

				}
				
				simulate(gs, ais[action], opponent, 0, 1, action_time);
				int winner =gs.winner();
				float reward=(!gameover(gs))||winner<0?0:1-2*winner;
				net.send("store\n"+current);//current state
				net.send(Integer.toString(action));//action
				net.send(Float.toString(reward));//reward
				String next=cnngs.getHeader()+cnngs.getPlanesCompressed();
				assert current != next;
				net.send(next);//next state
				net.send(Integer.toString(gameover(gs)?1:0));//is it terminal
			}
		}
		

		
//		//int size = 64;
//		//String inDir="../cnn-data/"+size+"x"+size+"replays";
//		//String outDir="../cnn-data/"+size+"x"+size+"extracted";
//		if(args.length!=3)
//			 System.out.println("Usage: prog size inDirectory outDirectory samplesPerGame");
//		int size = Integer.parseInt(args[0]);
//		String inDir=args[1];
//		inDir = inDir.replaceAll("/$", "");
//		String outDir=args[2];
//		outDir = outDir.replaceAll("/$", "");
//		createDir(outDir);
//		createDir(outDir+"Test");
//		createDir(inDir+"Test");
//		int samplesPerGame = Integer.parseInt(args[3]);
//		List<File> files = new ArrayList<File>();
//		listf(inDir, files);
//		
//		AI ais[] = new AI[]{
//			new WorkerRush(utt,new FloodFillPathFinding()),
//			new LightRush(utt,new FloodFillPathFinding()),
//			new RangedRush(utt,new FloodFillPathFinding()),
//			new HeavyRush(utt,new FloodFillPathFinding()),
//			};
//		EvaluationFunction ef =   NetEvaluationFunction.getInstance(size);
//		int count=0;
//		int testCount=0;
//		int sampleTestCount=0;
//		for(File f : files){
//			try
//			{
//				boolean test=false;
//				if(generator.nextInt(10)<1){//test
//					test=true;
//					Files.copy(f.toPath(),FileSystems.getDefault().getPath(inDir+"Test","game"+testCount+".zip"));
//					testCount++;
//				}
//				Sample samples = getSamples(f, samplesPerGame);
//				//Sample samples = getAllSamples(f);
//				for (GameState gs : samples.states) {
//
//					for(int i=0;i<ais.length;i++){
//						for(int j=0;j<ais.length;j++){
//							//writePlanes of original GS 
//							//write script planes
//							CNNGameState cnngs=new CNNGameState(gs);
//							if(test){
//								cnngs.writePlanesExtra(outDir+"Test/game"+sampleTestCount, ais.length, i, j);
//							}else{
//								cnngs.writePlanesExtra(outDir+"/game"+count, ais.length, i, j);
//							}
//							//simulate game for each script combination
//							GameState copyGs = gs.clone();
//							simulate(copyGs, ais[i], ais[j], 0, 1, 200);
//							//call network eval
//							float eval = ef.evaluate(0,1,copyGs);
//							//writeLabel: eval
//							if(test){
//								cnngs.writeLabel(outDir+"Test/game"+sampleTestCount, eval);
//								sampleTestCount++;
//							}else{
//								cnngs.writeLabel(outDir+"/game"+count, eval);
//								count++;
//							}
//						}
//					}
//
//				}
//
//			}
//			catch (JDOMException e)
//			{
//				e.printStackTrace();	
//			} catch (IOException e) 
//			{
//				e.printStackTrace();
//			}
//		}

	}
	

}
