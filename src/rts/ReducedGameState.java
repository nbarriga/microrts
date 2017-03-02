/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rts;

import java.util.LinkedList;
import java.util.List;
import rts.units.Unit;

/**
 *
 * @author santi
 */
public class ReducedGameState extends GameState {
    // creates a reduced game state, 
    public ReducedGameState(GameState gs) {
        super(gs.getPhysicalGameState().cloneKeepingUnits(), gs.getUnitTypeTable());
        unitCancelationCounter = gs.unitCancelationCounter;
        time = gs.time;

        unitActions.putAll(gs.unitActions);
        
        List<Unit> toDelete = new LinkedList<Unit>();
        for(Unit u:pgs.getUnits()) {
                if (!observable(u.getX(),u.getY(), 1-u.getPlayer())) {
                    toDelete.add(u);
                }
        }
        for(Unit u:toDelete){
        	removeUnit(u);
        	unitActions.remove(u);
        }
    }

    public boolean observable(int x, int y, int player) {
        for(Unit u:pgs.getUnits()) {
            if (u.getPlayer() == player) {
                double d = Math.sqrt((u.getX()-x)*(u.getX()-x) + (u.getY()-y)*(u.getY()-y));
                if (d<=u.getType().sightRadius+2) return true;
            }
        }
        
        return false;
    }
}
