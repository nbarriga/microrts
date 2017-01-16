package tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Arrays;


import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import ai.evaluation.EvaluationFunction;
import ai.evaluation.LanchesterEvaluationFunction;
import ai.evaluation.NetEvaluationFunction;
// import ai.evaluation.ScriptEvaluation;
import ai.evaluation.SimpleEvaluationFunction;
import ai.evaluation.SimpleOptEvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.GameState;
import rts.Trace;
import rts.units.UnitTypeTable;

public class EvalSpeed {

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
	public static void main(String[] args) {

		int size=8;
		EvaluationFunction[] ef = {
				new SimpleEvaluationFunction(),
				new SimpleOptEvaluationFunction(),
				new LanchesterEvaluationFunction(),
				new SimpleSqrtEvaluationFunction3(),
				NetEvaluationFunction.getInstance(size)
				};


		List<File> files = new ArrayList<File>();
		listf("../cnn-data/"+size+"x"+size+"replaysTest", files);


		float[][] accurate = new float[ef.length][21];
		int[] counts = new int[21];
		int count=0;
		try {
		
			long start = System.currentTimeMillis();
			List<Sample> allSamples = new ArrayList<Sample>();
			for(File f : files){

				System.out.println("sampling file: "+f.toString());
				Sample samples = getSamples(f, 50);
				allSamples.add(samples);
			}
			long end = System.currentTimeMillis();
			System.out.println("Time: "+(end-start)*1000.0/files.size()+" [us/file]");
				//System.out.println("got samples");

			for(int eval=0;eval<ef.length;eval++){
				System.out.println("testing eval "+eval);
				start = System.currentTimeMillis();
				int i=0;
				for(Sample samples:allSamples){

				//System.out.println("testing game "+(i++));
					for (GameState gs : samples.states) {
						int plotIndex = (gs.getTime()*20/samples.length); //every 5%! 

						//System.out.println("eval: "+ef[eval].toString());
						//System.out.println("eval");
						float evaluation = evaluation = ef[eval].evaluate(0, 1, gs);
						//if(eval==4)evaluation=-evaluation;
						//System.out.println("eval done");
						if(Math.abs(evaluation)<0.00000001f)
							 accurate[eval][plotIndex]+=0.5;
						else if((samples.winner==0 && evaluation>0)||
								(samples.winner==1 && evaluation<0)){
							accurate[eval][plotIndex]++;
						}
						if(eval==0){
							counts[plotIndex]++;
							count++;
						}
					}
				}
				end = System.currentTimeMillis();
				System.out.println("Time: "+(end-start)/(float)count*1000.0+" [us/eval]");
			}

		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	

		for(int e=0;e<ef.length;e++){
			//System.out.print(ef[e].toString()+": ");
			System.out.print("[");
			for(int i=0;i<21;i++){
				System.out.print(accurate[e][i]+", ");
			}
			System.out.println("]");
		}
			System.out.print("[");
			for(int i=0;i<21;i++){
				System.out.print(counts[i]+", ");
			}
			System.out.println("]");
	}
}
