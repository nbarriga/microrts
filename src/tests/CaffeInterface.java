package tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class CaffeInterface {

    String fromClient;
    String toClient;
    ServerSocket server;
    BufferedReader in;
    BufferedWriter out;

    public void send(String s) throws Exception {
    	// System.out.println("sending: " + s);
    	out.write(s);
    	out.newLine(); //HERE!!!!!!
        out.flush();
    }

    public double readDouble(int i) throws Exception {
    	return readDoubles()[i];
    }
    public float readFloat(int i) throws Exception {
    	return (float)readDoubles()[i];
    }
    public double[] readDoubles() throws Exception {
	    fromClient = in.readLine();
	    return Arrays.asList(fromClient.split(" ")).stream().mapToDouble(s -> Float.parseFloat(s)).toArray();
    }
    public int readInt(int i) throws Exception {
    	return readInts()[i];
    }
    public int[] readInts() throws Exception {
	    fromClient = in.readLine();
	    return Arrays.asList(fromClient.split(" ")).stream().mapToInt(s -> Integer.parseInt(s)).toArray();
    }
    public int getMaxIndex() throws Exception{
    	double[] values=readDoubles();
    	int maxIndex=0;
    	for(int i=1;i<values.length;i++){
    		if(values[i]>values[maxIndex]){
    			maxIndex=i;
    		}
    	}
    	return maxIndex;
    }
    public void start(int port) throws Exception {

    	// String cmd = "python /media/sf_LINUX/microrts/echo.py";
    	
//	    try {	
//	        System.out.println(cmd);
//	        System.out.println(System.getProperty("user.dir"));
//		//Process p = Runtime.getRuntime().exec(cmd);
//	    }catch (Exception err) {
//	        err.printStackTrace();
//	    }

        //server = new ServerSocket(port);
        System.out.println("Connecting to port "+port);
 
		//Socket client = server.accept();
	Socket client=new Socket("skat2",port);  
        System.out.println("Connected to port "+port);
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));

        out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
        // out = new PrintWriter(client.getOutputStream(),true);

    }
}
