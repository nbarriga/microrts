package rts;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import rts.units.Unit;
import rts.units.UnitType;

/**
 * This class extends a GameState to be able to write the planes needed for neural networks.
 * @author nbarriga
 *
 */
public class CNNGameState extends GameState {

	private static final long serialVersionUID = 6307744147199270926L;
	private HashMap<Integer,Integer> type2plane;
	private static int nrPlanes = 25;
	private boolean planes[][][];
	public CNNGameState(GameState gs) {
		super(gs.getPhysicalGameState(), gs.utt);
		unitActions=gs.unitActions;
		unitCancelationCounter=gs.unitCancelationCounter;
		initPlaneData();
	}


	private void initPlaneData()
	{
		type2plane = new HashMap<Integer,Integer>();
		planes = new boolean[nrPlanes][pgs.getWidth()][pgs.getHeight()];

		UnitType baseType = utt.getUnitType("Base");
		UnitType barracksType = utt.getUnitType("Barracks");
		UnitType workerType = utt.getUnitType("Worker");
		UnitType lightType = utt.getUnitType("Light");
		UnitType rangedType = utt.getUnitType("Ranged");
		UnitType heavyType = utt.getUnitType("Heavy");

		type2plane.put(baseType.ID,0);
		type2plane.put(barracksType.ID,1);
		type2plane.put(workerType.ID,2);
		type2plane.put(lightType.ID,3);
		type2plane.put(rangedType.ID,4);
		type2plane.put(heavyType.ID,5);
	}
	public String writePlanesCompressed() 
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		PrintStream out = new PrintStream(baos);

		fillPlanes();
		int p = planes.length;
		int w = pgs.getWidth();
		int h = pgs.getHeight();

		out.println(w + " " + h + " " + p);

		for(int i = 0; i<p; i++)
		{
			for(int j=0; j<w; j++)
			{
				for(int k=0; k<h; k++)
				{
					if(planes[i][j][k])
						out.print((i*w*h+k*w+j)+" ");
				}
			}
		}       

		return baos.toString();
	}
	
	public String writePlanes()  
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		PrintStream out = new PrintStream(baos);

		fillPlanes();
		int p = planes.length;
		int w = pgs.getWidth();
		int h = pgs.getHeight();

		out.println(w + " " + h + " " + p);

		for(int i = 0; i<p; i++)
		{
			for(int k=0; k<h; k++)
			{
				for(int j=0; j<w; j++)
				{
					out.print((planes[i][j][k]?1:0)+" ");
				}
				out.println();
			}
		}
		return baos.toString();
	}

	public void writePlanes(String filename) throws FileNotFoundException 
	{
		writePlanes(filename, false);
	}
	public void writePlanes(String filename, boolean compressed) throws FileNotFoundException 
	{
		PrintStream out = new PrintStream(new File(filename));
		out.print(compressed?writePlanesCompressed():writePlanes());
	}

	public void writeLabel(String filename, int label) throws FileNotFoundException
	{
		PrintStream out = new PrintStream(new FileOutputStream(filename, true));
		out.println(label);
		out.close();
	}

	public void fillPlanes()
	{
		for (boolean[][] plane: planes){
			for (boolean[] row: plane){
				Arrays.fill(row, false);
			}
		}
		int k = 0;
		int remainingT =0;

		List<Unit> units = new ArrayList<Unit>(pgs.units);
		HashMap<Unit,Integer> remainingTimes = new HashMap<Unit,Integer>();


		for(UnitActionAssignment uaa:unitActions.values()) 
		{
			if (uaa.action.type==UnitAction.TYPE_PRODUCE) 
			{
				Unit newUnit  = new Unit(uaa.unit.getPlayer(),uaa.action.getUnitType(),
						uaa.unit.getX()+ UnitAction.DIRECTION_OFFSET_X[uaa.action.parameter],
						uaa.unit.getY()+ UnitAction.DIRECTION_OFFSET_Y[uaa.action.parameter]
								,0);

				units.add(newUnit);
				remainingT = uaa.action.ETA(uaa.unit) + uaa.time - getTime();       
				remainingTimes.put(newUnit, remainingT);    
			}
		}       

		for(Unit u:units) {
			k=0;
			//1 plane for each unit type (6 total)
			if(type2plane.containsKey(u.getType().ID))
				planes[k+type2plane.get(u.getType().ID)][u.getX()][u.getY()] = true;


			k = k+6;
			//my units & his units planes (1+1)
			if(u.getPlayer() >= 0)
				planes[k+u.getPlayer()][u.getX()][u.getY()] = true;

			k=k+2;
			//HP planes - 1,2,3,4,5+
			if(u.getHitPoints() > 4){
				planes[k+4][u.getX()][u.getY()] = true;
			}else{
				planes[k+u.getHitPoints()-1][u.getX()][u.getY()] = true;
			}
			k=k+5;
			//frames to completion - 0-25,25-50,50-80,80-120,120+
			if(remainingTimes.containsKey(u)) //not completed
			{
				remainingT = remainingTimes.get(u);

				if(remainingT < 25) planes[k][u.getX()][u.getY()] = true;
				else if (remainingT < 50) planes[k+1][u.getX()][u.getY()] = true;
				else if (remainingT < 80) planes[k+2][u.getX()][u.getY()] = true;
				else if (remainingT < 120) planes[k+3][u.getX()][u.getY()] = true;
				else planes[k+4][u.getX()][u.getY()] = true;
			}

			k=k+5;
			//RESOURCES?    
			//1,2,3,4,5,6-10,10+

			if(u.getType().isResource && u.getResources() > 0)
			{
				//	              if(u.getResources() == 0) planes[k+5][u.getX()][u.getY()] = true;
				if(u.getResources() >= 10) planes[k+6][u.getX()][u.getY()] = true;
				else if(u.getResources() > 5) planes[k+5][u.getX()][u.getY()] = true;
				else planes[k+u.getResources()-1][u.getX()][u.getY()] = true;
			}

			if(u.getType().isStockpile && getPlayer(u.getPlayer()).getResources() > 0)
			{
				int res = getPlayer(u.getPlayer()).getResources();

				//	                if(res== 0) planes[k+5][u.getX()][u.getY()] = true;
				if(res >= 10) planes[k+6][u.getX()][u.getY()] = true;
				else if(res > 5) planes[k+5][u.getX()][u.getY()] = true;
				else planes[k+res-1][u.getX()][u.getY()] = true;
			}

		}
	}
}
