package tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

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

    public float read() throws Exception {
	    fromClient = in.readLine();
	    // System.out.println("received: " + Integer.parseInt(fromClient));
	    return Float.parseFloat(fromClient);
    }
    public int readInt() throws Exception {
	    fromClient = in.readLine();
	    // System.out.println("received: " + Integer.parseInt(fromClient));
	    return Integer.parseInt(fromClient);
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
        System.out.println("wait for connection on port "+port);
 
		//Socket client = server.accept();
		Socket client=new Socket("skat2",port);  
        System.out.println("got connection on port "+port);
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));

        out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
        // out = new PrintWriter(client.getOutputStream(),true);

    }
}
