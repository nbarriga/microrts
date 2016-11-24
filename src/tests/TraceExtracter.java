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
			        
	    List<TraceEntry> entries = t.getEntries();
	    int nrEntries = entries.size();

	    int winner = new GameState(entries.get(nrEntries-1).getPhysicalGameState(), utt).winner();
	    if(winner == -1) {
	    	return new Sample(states,nrEntries,-1);
	    }
	    

	    for(int i=0; i<n; i++)
	    {

	    	int frameNR = generator.nextInt(nrEntries);

	    	GameState gs = new GameState(entries.get(frameNR).getPhysicalGameState(), utt);
		    states.add(gs);
	    }
		return new Sample(states,nrEntries,winner);
	}
	public static void main(String[] args) {

		// Random generator = new Random(23);
		Random generator = new Random(66);
		int invalid = 0;
		
		
		
		// EvaluationFunction ef = new SimpleEvaluationFunction();
//		EvaluationFunction ef = new ScriptEvaluation();
		// EvaluationFunction ef = new LanchesterEvaluationFunction();
//		EvaluationFunction ef = NetEvaluationFunction.getInstance();

//		int w =0, l= 0, d= 0;
//		float ingameAccuracy = 0;
//		float netAccuracy = 0;
//		int label = 0;
//		int count = 0;
		
		List<File> files = new ArrayList<File>();
		listf("8x8rep", files);
		
//		float[] evalAcc = new float[21];
//		int[] netAcc = new int[21];
//		int[] counts = new int[21];
//
//		BufferedWriter outputWriter = null;
//		try
//		{
//			outputWriter = new BufferedWriter(new FileWriter("temp_acc.txt"));
//		}catch (Exception e) {
//			e.printStackTrace();
//		}

		// TalkPython tp = new TalkPython();
		// tp.init();
		
		// TalkCPP test =  new TalkCPP();
		// try
		// {
  //      	test.start();
  //      }catch (Exception e) {
		// 	e.printStackTrace();
		// }

//		long timeEVAL = 0;
//		long timeNET = 0;
//		long start;

		int count=0;
		for(File f : files){
			try
			{
				
				Sample samples = getSamples(f, 3);
			
//				if(samples.winner == -1) {d += 1; continue;}
//				else if(samples.winner == 1) l += 1;
//				else if(samples.winner == 0) w += 1;
//			

				for (GameState gs : samples.states) {

					CNNGameState cnngs=new CNNGameState(gs);
					cnngs.writePlanes("test/game"+count);
					cnngs.writeLabel("test/game"+count, samples.winner);
					count++;
//			    	int plotIndex = (gs.getTime()*20/samples.length); //every 5%! 
			    	
				    
			    	// String s = gs.writePlanes2();
//			    	gs.writeUnitDataset("dataset_units.txt");
//			    	gs.writeLabel("dataset_units_labels.txt", winner);
			        	
//			    	PrintWriter out = new PrintWriter("testgame.txt");
//			    	out.print(s);
//			    	out.close();
			    	// System.in.read();
			    	// tp.send(s);
			    	// test.run();
			    	// float evaluation = 0;
			    	// float r_net = ef.evaluate(0,1,gs);
			    
			    	// Thread.sleep(100*1000);
//			    	start = System.nanoTime();
//			    	
		    		// test.send(s);
		    		// r_net = test.read();
		    		    		
//			    	for(int tt=0;tt<10000;tt++)
//			    	try{
			    	// evaluation = ef.evaluate(0, 1, gs);	
			    	// outputWriter.write(Float.toString(evaluation)+" ");
//			    	}catch (RuntimeException e)
//			    	{s
//			    		System.out.println("SKIPPING!");
//			    		continue;
//			    	}
			    	
//			    	counts[plotIndex] += 1;
			    	
//			    	if(plotIndex == 0)
//			    	{
//			    		System.out.format("prediction %.4f winner %d %n",evaluation,winner);
//			    		if(evaluation == 0)
//						{ 
//							System.out.println("ZERO");
//						}
//			    		
//			    	}
//			    	timeEVAL += (System.nanoTime() - start);
//	
//			    	float  r_net;
//			    	start = System.nanoTime();
//			    	for(int tt=0;tt<1000;tt++)
//			    	{
//			    		// test.send(s);
//			    		// r_net = test.read();
//			    		r_net = ef.evaluate(0,1,gs);
//			    	}			    		
//			    	timeNET += (System.nanoTime() - start);

//			    	System.out.format("%time (ns) %d%n",System.nanoTime() - start);
//			    	System.out.print("results: " + r_net);
			    	
//			    	System.in.read();
					// if(winner == 0 && Math.abs(evaluation) < 0.00001)
					// 	ingameAccuracy +=1;
					// if(Math.abs(r_net) < 0.00000001f)
					// { 
					// 	netAccuracy +=0.5f;
					// 	netAcc[plotIndex] +=0.5f;				
					// }
					// else if((winner == 1 && r_net < 0) || (winner ==0 && r_net > 0))
					// {
					// 	netAccuracy +=1;
					// 	netAcc[plotIndex] +=1;
					// }
					
					//transform labels!
//					if (samples.winner == 1) label = 0;
//					else if (samples.winner == 0) label = 1;

//					if(label == r_net)
//					{
//						netAccuracy +=1;
//						netAcc[plotIndex] +=1;
//
//					}
			
					// gs.writePlanes("8x8_S3/game"+count);
					 
					
					// System.out.format("time (ns): eval-%,8d net-%,8d",timeEVAL,timeNET);
					// System.in.read();

//					count++;
					// if(count % 10 == 0) 
//					System.out.print(" "+count);

//					if(count % 10 == 0) 
//					{
//						int nrSim = count*1000*SAMPLES;
//						System.out.format("%ntime (ns): eval-%d net-%d%n",timeEVAL/nrSim,timeNET/nrSim);
//						
//						System.out.print(" "+count + " [invalid: "+ invalid + "]");
//						
//
//						// System.out.format("  accuracy ingame: %.4f",ingameAccuracy/(double)(count));
//						// System.out.println("wins/draws/losses: " + w*100.0*SAMPLES/count + "/" + d*100.0*SAMPLES/count + "/"+ l*100.0*SAMPLES/count);
//						// System.out.format("  accuracy net: %.4f%n", netAccuracy/(double)(count));
//
////						System.in.read();
//					}
			    }

			    // System.in.read();
			}
			catch (JDOMException e)
			{
				e.printStackTrace();	
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
		}

//		try
//		{
//			outputWriter.write(Arrays.toString(evalAcc));
//			outputWriter.write("\n");
//		    outputWriter.write(Arrays.toString(netAcc));
//		    outputWriter.write("\n");
//		    outputWriter.write(Arrays.toString(counts));
//		    outputWriter.flush();
//		    outputWriter.close();
//	    } catch (Exception e) {
//				e.printStackTrace();
//		}
	}
	

}
