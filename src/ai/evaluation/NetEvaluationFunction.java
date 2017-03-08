package ai.evaluation;

import rts.CNNGameState;
import rts.GameState;
import tests.CaffeInterface;


public class NetEvaluationFunction extends EvaluationFunction {  
	
	public int isDNN() {return 1;}

	protected NetEvaluationFunction() {
		// Exists only to defeat instantiation.
	}
	private static NetEvaluationFunction instance = null;
	private CaffeInterface caffe;
	private static int port = 8080;
	private static String path = "data/caffe/";

	private void init(String cmd)
	{
		caffe =  new CaffeInterface();
		try
		{
			caffe.start( port);
			caffe.send(cmd+"\n");
        }
		catch (Exception e) {e.printStackTrace();}
	}
	
	private static int mapSize = -1;
	public static NetEvaluationFunction getInstance(int mapSize) {
		if(instance == null) {
			instance = new NetEvaluationFunction();
			NetEvaluationFunction.mapSize=mapSize;
			instance.init(
					path+mapSize+"x"+mapSize+".prototxt "
					+path+mapSize+"x"+mapSize+".caffemodel");
		}else{
			assert(NetEvaluationFunction.mapSize==mapSize);
		}
		return instance;
	}
	
    public float evaluate(int maxplayer, int minplayer, GameState gs) {
    	assert(gs.getPhysicalGameState().getHeight()==mapSize);
    	//if(GameState.TIME_LIMIT!=-1&&gs.getTime()>=GameState.TIME_LIMIT)return 0.0f;//tie
    	// System.out.println("eval GETTING CALLED");
    	float p0WIN = 0.0f;
    	
		try
		{ 
			CNNGameState cnnGs=new CNNGameState(gs);
			caffe.send(cnnGs.getHeader()+cnnGs.getPlanesCompressed());   
	    	// System.out.println("sent");	
			p0WIN = caffe.readFloat(1);
			//System.out.println("eval: "+p0WIN);
			if(maxplayer == 1) return (p0WIN-0.5f)*2.0f;
			return (0.5f - p0WIN)*2.0f;
		} catch (Exception e) {e.printStackTrace();}
		return p0WIN;
    }
    
    public float upperBound(GameState gs) {
    	return 1.0f;
    }
}
