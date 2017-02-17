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

import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import ai.evaluation.EvaluationFunction;
import ai.evaluation.LanchesterEvaluationFunction;
// import ai.evaluation.ScriptEvaluation;
import ai.evaluation.SimpleEvaluationFunction;
import ai.evaluation.NetEvaluationFunction;
import ai.core.AI;
import ai.abstraction.pathfinding.FloodFillPathFinding;
import ai.abstraction.WorkerRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.HeavyRush;
import rts.CNNGameState;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Trace;
import rts.units.UnitTypeTable;

public class PuppetDQN {

	static UnitTypeTable utt = new UnitTypeTable(
			UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
			UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
	
	static Random generator = new Random(66);
	
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
				gs.issue(ai1.getAction(player1, gs));
				gs.issue(ai2.getAction(player2, gs));
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
	public static void main(String[] args) throws Exception{
		//Algorithm 1: deep Q-learning with experience replay.
		int M=100;
		float epsilon_start = 1;
		float epsilon_end = 0.1f;
		float epsilon_anneal = 100;
		int numActions = 4;
		int action_time = 50;
		AI[] ais = {
				new WorkerRush(utt,new FloodFillPathFinding()),
				new LightRush(utt,new FloodFillPathFinding()),
				new RangedRush(utt,new FloodFillPathFinding()),
				new HeavyRush(utt,new FloodFillPathFinding()),
		};
		CaffeInterface net=new CaffeInterface();
		net.start(8080);
		for(int ep=0;ep<M;ep++){
			float epsilon = ep* (epsilon_end-epsilon_start)/epsilon_anneal+epsilon_start;
			System.out.println("eps: "+epsilon);

			GameState gs=new GameState(PhysicalGameState.load("maps/basesWorkers8x8.xml", utt),utt);
			CNNGameState cnngs=new CNNGameState(gs);
			while(!gs.gameover()){
				String current=cnngs.writeHeader()+cnngs.writePlanesCompressed();
				int action;
				if(generator.nextDouble()<epsilon){
					action=generator.nextInt(numActions);
				}
				else{
					net.send("eval\n"+current);
					action=net.readInt();
					System.out.println(""+action);

				}
				int opponent=0;//use an older network for self-play
				simulate(gs, ais[action], ais[opponent], 0, 1, action_time);
				int winner =gs.winner();
				float reward=gs.gameover()||winner<0?0:1-2*winner;
				net.send("store\n"+current);
				net.send(Integer.toString(action));
				net.send(Float.toString(reward));
				String next=cnngs.writeHeader()+cnngs.writePlanesCompressed();
				assert current != next;
				net.send(next);
				net.send(Integer.toString(gs.gameover()?1:0));
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
