/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.evaluation;

import rts.GameState;
import rts.PhysicalGameState;
import rts.UnitAction;
import rts.UnitActionAssignment;
import rts.units.*;

/**
 *
 * @author santi
 */
public class LanchesterEvaluationFunction extends EvaluationFunction {    
    // public static float W_BASE 		= 0.13633782f;
    // public static float W_RAX 		= 0.2158416f;
    // public static float W_WORKER 	= 0.30882949f;
    // public static float W_LIGHT 	= 1.17891871f;
    // public static float W_RANGE 	= 1.04003041f;
    // public static float W_HEAVY 	= 1.64735694f;
    // public static float W_MINERALS_CARRIED 	= 0.23123993f;
    // public static float W_MINERALS_MINED 	= -0.02240797f;

    public static float W_BASE      = 0.12900641042498262f;
    public static float W_RAX       = 0.23108197488337265f;
    public static float W_WORKER    = 0.18122298329807154f;
    public static float W_LIGHT     = 1.7496678034331925f;
    public static float W_RANGE     = 1.6793840344563218f;
    public static float W_HEAVY     = 3.9012441116439427f;
    public static float W_MINERALS_CARRIED  = 0.3566229669443759f;
    public static float W_MINERALS_MINED    = 0.30141654836442761f;
    
    // [('Base', 0.12900641042498262), ('H', 3.9012441116439427), ('L', 1.7496678034331925), ('R', 1.6793840344563218), 
    //('Rax', 0.23108197488337265), ('Rc', 0.3566229669443759), ('Rm', 0.30141654836442761), ('W', 0.18122298329807154)]
    public static float order = 1.7f;
    
    public static float sigmoid(float x) {
        return (float) (1.0f/( 1.0f + Math.pow(Math.E,(0.0f - x))));
      }
    
    public float evaluate(int maxplayer, int minplayer, GameState gs) {
        //System.out.println("SimpleEvaluationFunction: " + base_score(maxplayer,gs) + " - " + base_score(minplayer,gs));
//    	if(gs.timeUp())return 0;//tie
    	return 2.0f*sigmoid(base_score(maxplayer,gs) - base_score(minplayer,gs))-1.0f;
    }
    
    public float base_score(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        float score = 0.0f;
        float score_buildings = 0.0f;
        float nr_units = 0.0f;
        float res_carried = 0.0f;
        
        UnitTypeTable utt = gs.getUnitTypeTable();
        	
        for(Unit u:pgs.getUnits()) {
            if (u.getPlayer()==player) {
            	
            	res_carried += u.getResources();
           		//UNITS
        		if(u.getType() == utt.getUnitType("Base"))
        		{
        			score_buildings += W_BASE*u.getHitPoints();
        		}
        		else if(u.getType() == utt.getUnitType("Barracks"))
            	{
        			score_buildings += W_RAX*u.getHitPoints();

            	}
            	else if(u.getType() == utt.getUnitType("Worker"))
            	{
            		nr_units += 1;
            		score += W_WORKER*u.getHitPoints();
            	}
            	else if(u.getType() == utt.getUnitType("Light"))
            	{
            		nr_units += 1;
            		score += W_LIGHT*u.getHitPoints()/(float)u.getMaxHitPoints();
            	}
            	else if(u.getType() == utt.getUnitType("Ranged"))
            	{
            		nr_units += 1;
            		score += W_RANGE*u.getHitPoints();
            	}
            	else if(u.getType() == utt.getUnitType("Heavy"))
            	{
            		nr_units += 1;
            		score += W_HEAVY*u.getHitPoints()/(float)u.getMaxHitPoints();
            	}
            }
            
        }
        
        score = (float) (score * Math.pow(nr_units, order-1));
        
        score += score_buildings + res_carried * W_MINERALS_CARRIED + 
        		gs.getPlayer(player).getResources() * W_MINERALS_MINED;
        
        return score;
    }    
    
    public float upperBound(GameState gs) {
        return 2.0f;
    }
}
