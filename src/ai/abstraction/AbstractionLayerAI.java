/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.abstraction;

import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import rts.*;
import rts.units.Unit;
import rts.units.UnitType;
import util.Pair;

/**
 *
 * @author santi
 */
public abstract class AbstractionLayerAI extends AI {
    // set this to true, if you believe there is a bug, and want to verify that actions
    // being generated are actually possible before sending to the game.
    public static boolean VERIFY_ACTION_CORRECTNESS = false;
    
    // functionality that this abstraction layer offers:
    // 1) You can forget about issuing actions to all units, just issue the ones you want, the rest are set to NONE automatically
    // 2) High level actions (using A*):
    //      - move(x,y)
    //      - train(type)
    //      - build(type,x,y)
    //      - harvest(target)
    //      - attack(target)
    
    protected HashMap<Unit,AbstractAction> actions = new LinkedHashMap<>();
    protected PathFinding pf = null;
            
    public AbstractionLayerAI(PathFinding a_pf) {
        pf = a_pf;
    }
           
    
    @Override
    public void reset(){
    	actions.clear();
    }
    

    @Override
    public abstract AI clone();
    
    
    public PlayerAction translateActions(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        PlayerAction pa = new PlayerAction();
        List<Pair<Unit,UnitAction>> desires = new ArrayList<>();
        
        // Execute abstract actions:
        List<Unit> toDelete = new ArrayList<>();
         ResourceUsage ru = new ResourceUsage();
        for(AbstractAction aa:actions.values()) {
            if (!pgs.getUnits().contains(aa.unit)) {
                // The unit is dead:
                toDelete.add(aa.unit);
            } else {
                if (aa.completed(gs)) {
                    // the action is complete:
                    toDelete.add(aa.unit);
                } else {
                    if (gs.getActionAssignment(aa.unit)==null) {
                        UnitAction ua = aa.execute(gs,ru);
                        if (ua!=null) {
                            if (VERIFY_ACTION_CORRECTNESS) {
                                // verify that the action is actually feasible:
                                List<UnitAction> ual = aa.unit.getUnitActions(gs);
                                if (ual.contains(ua)) desires.add(new Pair<>(aa.unit,ua));
                            } else { 
                                desires.add(new Pair<>(aa.unit,ua));
                            }
                        	ru.merge(ua.resourceUsage(aa.unit, pgs));
                        }
                        
                    }
                }
            }
        }
        for(Unit u:toDelete) actions.remove(u);
        
        // compose desires:
        ResourceUsage r = gs.getResourceUsage();
        pa.setResourceUsage(r);
        for(Pair<Unit,UnitAction> desire:desires) {
            ResourceUsage r2 = desire.m_b.resourceUsage(desire.m_a, pgs);
            if (pa.consistentWith(r2, gs)) {
                pa.addUnitAction(desire.m_a, desire.m_b);
                pa.getResourceUsage().merge(r2);
            }
        }
        
        pa.fillWithNones(gs,player, 10);
        return pa;        
    }
    
    public AbstractAction getAbstractAction(Unit u) {
        return actions.get(u);
    }
    
    public void move(Unit u, int x,int y) {
        actions.put(u,new Move(u,x,y, pf));
    }

    
    public void train(Unit u, UnitType unit_type) {
        actions.put(u,new Train(u,unit_type));
    }


    public void build(Unit u, UnitType unit_type, int x, int y) {
        actions.put(u,new Build(u,unit_type,x,y, pf));
    }


    public void harvest(Unit u, Unit target, Unit base) {
        actions.put(u,new Harvest(u,target, base, pf));
    }

    
    public void attack(Unit u, Unit target) {
        actions.put(u,new Attack(u,target, pf));
    }

    public void idle(Unit u) {
        actions.put(u,new Idle(u));
    }
    public int findBuildingPosition(List<Integer> reserved, int desiredX, int desiredY, Player p, PhysicalGameState pgs) {

    	boolean[][] free=pgs.getAllFree();
        int x,y;
        for(int l=1;l<Math.max(pgs.getHeight(),pgs.getWidth());l++){
        	for(int side=0;side<4;side++){
        		switch(side){
        		case 0://up
        			y=desiredY-l;
        			if(y<0)continue;
        			for(int dx=-l;dx<=l;dx++){
        				x=desiredX+dx;
        				if(x<0||x>=pgs.getWidth())continue;
        				int pos = x + y * pgs.getWidth();
        				if (!reserved.contains(pos) && free[x][y]) {
        	        		return pos;
        	        	}
        			}
        			break;
        		case 1://right
        			x=desiredX+l;
        			if(x>=pgs.getWidth())continue;
        			for(int dy=-l;dy<=l;dy++){
        				y=desiredY+dy;
        				if(y<0||y>=pgs.getHeight())continue;
        				int pos = x + y * pgs.getWidth();
        				if (!reserved.contains(pos) && free[x][y]) {
        	        		return pos;
        	        	}
        			}
        			break;
        		case 2://down
        			y=desiredY+l;
        			if(y>=pgs.getHeight())continue;
        			for(int dx=-l;dx<=l;dx++){
        				x=desiredX+dx;
        				if(x<0||x>=pgs.getWidth())continue;
        				int pos = x + y * pgs.getWidth();
        				if (!reserved.contains(pos) && free[x][y]) {
        	        		return pos;
        	        	}
        			}
        			break;
        		case 3://left
        			x=desiredX-l;
        			if(x<0)continue;
        			for(int dy=-l;dy<=l;dy++){
        				y=desiredY+dy;
        				if(y<0||y>=pgs.getHeight())continue;
        				int pos = x + y * pgs.getWidth();
        				if (!reserved.contains(pos) && free[x][y]) {
        	        		return pos;
        	        	}
        			}
        			break;
        		}
        	}
        }
        return -1;
    }
    
    
    public boolean buildIfNotAlreadyBuilding(Unit u, UnitType type, int desiredX, int desiredY, List<Integer> reservedPositions, Player p, PhysicalGameState pgs){
    	AbstractAction action=getAbstractAction(u);
    	if(!(action instanceof Build) || ((Build)action).type != type){
    		int pos = findBuildingPosition(reservedPositions, desiredX, desiredY, p, pgs);
    		build(u, type, pos % pgs.getWidth(), pos / pgs.getWidth());
            reservedPositions.add(pos);
    		return true;
    	}else{
    		return false;
    	}
    }
    

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + pf + ")";
    }
    
    
    public PathFinding getPathFinding() {
        return pf;
    }
    
    
    public void setPathFinding(PathFinding a_pf) {
        pf = a_pf;
    }
}   
