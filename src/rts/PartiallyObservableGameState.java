/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rts;

import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import rts.units.Unit;
import rts.units.UnitTypeTable;

/**
 *
 * @author santi
 */
public class PartiallyObservableGameState extends GameState {
    int player;   // the observer player
    
    // creates a partially observable game state, from the point of view of 'player':
    public PartiallyObservableGameState(GameState gs, int a_player) {
        super(gs.getPhysicalGameState().cloneKeepingUnits(), gs.getUnitTypeTable());
        unitCancelationCounter = gs.unitCancelationCounter;
        time = gs.time;

        player = a_player;

        unitActions.putAll(gs.unitActions);
        
        List<Unit> toDelete = new LinkedList<Unit>();
        for(Unit u:pgs.getUnits()) {
            if (u.getPlayer() != player) {
                if (!observable(u.getX(),u.getY())) {
                    toDelete.add(u);
                }
            }
        }
        for(Unit u:toDelete) removeUnit(u);
        for(int y = 0;y<pgs.getHeight();y++) {
            for(int x = 0;x<pgs.getWidth();x++) {
                if (!observable(x, y)) pgs.setTerrain(x, y, PhysicalGameState.TERRAIN_NONE);
            }
        }
    }

    public boolean observable(int x, int y) {
        for(Unit u:pgs.getUnits()) {
            if (u.getPlayer() == player) {
                double d = Math.sqrt((u.getX()-x)*(u.getX()-x) + (u.getY()-y)*(u.getY()-y));
                if (d<=u.getType().sightRadius) return true;
            }
        }
        
        return false;
    }
}
