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

    public void start(String cmd) throws Exception {

    	// String cmd = "python /media/sf_LINUX/microrts/echo.py";
    	
	    try {	
	        System.out.println(cmd);
	        System.out.println(System.getProperty("user.dir"));
	        Process p = Runtime.getRuntime().exec(cmd);
	    }catch (Exception err) {
	        err.printStackTrace();
	    }

        server = new ServerSocket(8080);
        System.out.println("wait for connection on port 8080");
 
		Socket client = server.accept();
        System.out.println("got connection on port 8080");
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));

        out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
        // out = new PrintWriter(client.getOutputStream(),true);

    }
}
