package tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.zip.ZipInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.FileSystems;

import javax.swing.JFrame;

import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import ai.evaluation.EvaluationFunction;
import ai.evaluation.LanchesterEvaluationFunction;
// import ai.evaluation.ScriptEvaluation;
import ai.evaluation.SimpleEvaluationFunction;
import gui.TraceVisualizer;
import rts.CNNGameState;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Trace;
import rts.units.UnitTypeTable;
import tests.CompareEvaluations.Sample;
import rts.TraceEntry;
import util.Pair;

public class TraceExtracter {

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
	public static Sample getAllSamples(File trace) throws JDOMException, IOException{
		List<GameState> states = new ArrayList<GameState>();

		ZipInputStream zip=new ZipInputStream(new FileInputStream(trace.getAbsolutePath()));
		zip.getNextEntry();
		Trace t = new Trace(new SAXBuilder().build(zip).getRootElement(), utt);

		int winner = t.getGameStateAtCycle(t.getLength()).winner();

		if(winner == -1) {
			return new Sample(states,t.getLength(),-1);
		}

		int step = 10 + generator.nextInt(10);

		int i=0;
	       while(i<t.getLength())
		{
			states.add(t.getGameStateAtCycle(i));
			i=(i+step<t.getLength())?i+step:t.getLength();
		}
		return new Sample(states,t.getLength(),winner);
	}
	public static void main(String[] args) {

		int size = 64;
		String inDir="../cnn-data/"+size+"x"+size+"replays";
		String outDir="../cnn-data/"+size+"x"+size+"extracted";
		List<File> files = new ArrayList<File>();
		listf(inDir, files);
		
		int count=0;
		int testCount=0;
		int sampleTestCount=0;
		for(File f : files){
			try
			{
				

				if(generator.nextInt(10)<1){//test
					Files.copy(f.toPath(),FileSystems.getDefault().getPath(inDir+"Test","game"+testCount+".zip"));
					testCount++;
					Sample samples = getSamples(f, 12);
					for (GameState gs : samples.states) {

						CNNGameState cnngs=new CNNGameState(gs);
						cnngs.writePlanes(outDir+"Test/game"+sampleTestCount);
						cnngs.writeLabel(outDir+"Test/game"+sampleTestCount, samples.winner);
						sampleTestCount++;

					}
					continue;
				}
				//train
				Sample samples = getSamples(f, 12);
				//Sample samples = getAllSamples(f);
				for (GameState gs : samples.states) {

					CNNGameState cnngs=new CNNGameState(gs);
					cnngs.writePlanes(outDir+"/game"+count);
					cnngs.writeLabel(outDir+"/game"+count, samples.winner);
					count++;

				}

			}
			catch (JDOMException e)
			{
				e.printStackTrace();	
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
		}

	}
	

}
