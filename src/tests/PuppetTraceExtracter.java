package tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Arrays;
import java.util.Collection;
import java.nio.file.Files;
import java.nio.file.FileSystems;

import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import ai.evaluation.EvaluationFunction;
import ai.evaluation.LanchesterEvaluationFunction;
// import ai.evaluation.ScriptEvaluation;
import ai.evaluation.SimpleEvaluationFunction;
import ai.puppet.PuppetNoPlan;
import ai.puppet.PuppetSearchAB;
import ai.puppet.SingleChoiceConfigurableScript;
import ai.evaluation.NetEvaluationFunction;
import ai.core.AI;
import ai.abstraction.pathfinding.FloodFillPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.abstraction.WorkerRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.HeavyRush;
import rts.CNNGameState;
import rts.GameState;
import rts.Trace;
import rts.units.UnitTypeTable;
import util.Pair;

public class PuppetTraceExtracter {

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
				gs.issue(ai1.getAction(player1, gs));
				gs.issue(ai2.getAction(player2, gs));
			}
		}
	}
	private static void createDir(String dir){
		File d = new File(dir);
		d.mkdirs();
	}
	static PathFinding getPathFinding(){return new FloodFillPathFinding();}


	public static void main(String[] args) throws Exception{

		//int size = 64;
		//String inDir="../cnn-data/"+size+"x"+size+"replays";
		//String outDir="../cnn-data/"+size+"x"+size+"extracted";
		if(args.length!=4)
			 System.out.println("Usage: prog size inDirectory outDirectory samplesPerGame searchTime");
		int size = Integer.parseInt(args[0]);
		String inDir=args[1];
		inDir = inDir.replaceAll("/$", "");
		String outDir=args[2];
		outDir = outDir.replaceAll("/$", "");
		createDir(outDir);
		createDir(outDir+"Test");
		createDir(outDir+"ReplaysTest");
		int samplesPerGame = Integer.parseInt(args[3]);
		List<File> files = new ArrayList<File>();
		listf(inDir, files);
		int searchTime = Integer.parseInt(args[4]);
		
//		AI[] ais = {
//				new WorkerRush(utt,getPathFinding()),
//				new LightRush(utt,getPathFinding()),
//				new RangedRush(utt,getPathFinding()),
//				new HeavyRush(utt,getPathFinding()),
//			};
		PuppetNoPlan puppetOrig=new PuppetNoPlan(
				new PuppetSearchAB(
						searchTime, -1,
						-1, -1,
						100,
						new SingleChoiceConfigurableScript(getPathFinding(),
								new AI[]{
										new WorkerRush(utt, getPathFinding()),
										new LightRush(utt, getPathFinding()),
										new RangedRush(utt, getPathFinding()),
										new HeavyRush(utt, getPathFinding()),
						}),
						//new SimpleEvaluationFunction()
						NetEvaluationFunction.getInstance(size)
						)
                );
		//EvaluationFunction ef =   NetEvaluationFunction.getInstance(size);
		int count=0;
		int testCount=0;
		int sampleTestCount=0;
		int fn=0;
		for(File f : files){
			System.out.println(fn+"/"+files.size()+" files processed");
			boolean test=false;
			if(generator.nextInt(10)<1){//test
				test=true;
				Files.copy(f.toPath(),FileSystems.getDefault().getPath(outDir+"ReplaysTest","game"+testCount+".zip"));
				testCount++;
			}
			Sample samples = getSamples(f, samplesPerGame);
			//Sample samples = getAllSamples(f);
			for (GameState gs : samples.states) {
				for(int p=0;p<2;p++){
					PuppetNoPlan puppet=(PuppetNoPlan)puppetOrig.clone();
					puppet.reset();
					System.gc();
					puppet.startNewComputation(p, gs);
					puppet.computeDuringOneGameFrame();
					Collection<Pair<Integer, Integer>> choices = puppet.getBestChoicesSoFar();
					assert choices.size()==1;
					for(Pair<Integer, Integer> choice:choices){
						System.out.println(choice.m_a+": "+choice.m_b);
						CNNGameState cnngs=new CNNGameState(gs);
						if(test){
							cnngs.writePlanesExtra(outDir+"Test/game"+sampleTestCount,1,p);
							cnngs.writeLabel(outDir+"Test/game"+sampleTestCount, choice.m_b);
							sampleTestCount++;
						}else{
							cnngs.writePlanesExtra(outDir+"/game"+count,1,p);
							cnngs.writeLabel(outDir+"/game"+count, choice.m_b);
							count++;
						}
					}
					puppet.startNewComputation(p, gs);//just for statistics
					System.out.println(puppet.statisticsString());
				}
			}
			fn++;
		}

	}
	

}
