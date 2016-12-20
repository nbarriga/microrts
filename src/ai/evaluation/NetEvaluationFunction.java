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
	
	private void init(String cmd)
	{
		caffe =  new CaffeInterface();
		try
		{
			caffe.start(cmd);
        }
		catch (Exception e) {e.printStackTrace();}
	}
	
	public static NetEvaluationFunction getInstance() {
		if(instance == null) {
			instance = new NetEvaluationFunction();
			// instance.init("python echo.py");
			instance.init("python src/py/interface.py 8 25");
		}
		return instance;
	}
	
    public float evaluate(int maxplayer, int minplayer, GameState gs) {

    	//if(GameState.TIME_LIMIT!=-1&&gs.getTime()>=GameState.TIME_LIMIT)return 0.0f;//tie
    	// System.out.println("eval GETTING CALLED");
    	float p0WIN = 0.0f;
    	
		try
		{ 
			CNNGameState cnnGs=new CNNGameState(gs);
			caffe.send(cnnGs.writePlanesCompressed());   
	    	// System.out.println("sent");	
			p0WIN = caffe.read();
			if(maxplayer == 0) return (p0WIN-0.5f)*2.0f;
			return (0.5f - p0WIN)*2.0f;
		} catch (Exception e) {e.printStackTrace();}
		return p0WIN;
    }
    
    public float upperBound(GameState gs) {
    	return 1.0f;
    }
}
