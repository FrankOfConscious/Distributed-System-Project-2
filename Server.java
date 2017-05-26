package EZShare;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Server {
		
	
	// Declare the port number
	static int port;
	static int sport;
	//static String hostname;
	// Identifies the user number connected
	static String advertisedHostName;
	private static int connectionIntervalLimit;
	private static int exchangeInterval;
	private static int counter = 0;
	static boolean debug = false;
	private static final Logger log = Logger.getLogger(Logger.class);
	public static  ArrayList< KeyTuple> resourceList=new ArrayList<KeyTuple>();
	static String secret = null;
	static ArrayList<String> serverRecords=new ArrayList<String>();
	static ArrayList<String> secureServerRecords=new ArrayList<String>();

		
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws ParseException, org.apache.commons.cli.ParseException {
			// Parse CMD options
			//
		Options options = new Options();
		AddOptions(options);
		// accept args from CMD
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;

		try{
			cmd = parser.parse(options, args);
		}catch(Exception e){
			System.out.println("Command is invalid or not found. \nPlease check your command and try again.");
			System.exit(0);
		}
			
		try {
			if (cmd.hasOption("sport")) {
				if (Math.isPort(cmd.getOptionValue("sport"))) {
					sport = Integer.parseInt(cmd.getOptionValue("sport"));
				} else {
					System.out.println("Please provide valid port");
					System.exit(0);
				}
			} else {
				sport = 3781;
			}
		} catch (Exception e) {
			sport = 3781;
		}
			
		try{
			if(Math.isPort(cmd.getOptionValue("port")))
				port = Integer.parseInt(cmd.getOptionValue("port"));
			else{
				System.out.println("Please provide valid port");
				System.exit(0);
			}
		}catch(Exception e){
			port = 3000;
		}
			
		if(cmd.hasOption("connectionintervallimit")){
			try{
				Server.connectionIntervalLimit=Integer.parseInt(cmd.getOptionValue("connectionintervallimit"));
				if(Integer.parseInt(cmd.getOptionValue("connectionintervallimit"))<0){
					System.out.println("Please provide valid connection interval limit( positive integer) arg.");
					System.exit(0);
				}
			}catch(Exception e){
				System.out.println("Please provide valid connection interval limit( positive integer) arg.");
				System.exit(0);
			}
		}else Server.connectionIntervalLimit=1;
		
		if(cmd.hasOption("exchangeinterval")){
			try{
				Server.exchangeInterval=Integer.parseInt(cmd.getOptionValue("exchangeinterval"));
				if(Integer.parseInt(cmd.getOptionValue("exchangeinterval"))<0){
					System.out.println("Please provide valid exchange interval( positive integer) arg.");
					System.exit(0);
				}
			}catch(Exception e){
				System.out.println("Please provide valid exchange interval( postive integer) arg.");
				System.exit(0);
			}
		}else Server.exchangeInterval=600;
		
		if(cmd.hasOption("secret")){
			try{
				Server.secret=cmd.getOptionValue("secret");
			}catch(Exception e){
				System.out.println("Please provide valid secret(String).");
				System.exit(0);
			}
		}else {
			Random rand = new Random();
			Server.secret=getRandomString(rand.nextInt(10)+20);
		}
		if(cmd.hasOption("advertisedhostname")){
			try{
				Server.advertisedHostName=cmd.getOptionValue("advertisedhostname");
			}catch(Exception e){
				System.out.println("Please provide valid advertised hostname(String).");
				System.exit(0);
			}
		}else {
			InetAddress gethost;
			try {
				gethost = InetAddress.getLocalHost();
				Server.advertisedHostName=gethost.getHostName();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				System.out.println("Fail to get hostname of OS.\nTry to provide an advertised hostname manually.");
			} 
		}
		
		if(cmd.hasOption("debug")) {
			Server.debug=true;
		}	
		else debug=false;

		
////////////////////////start to work with initial settings/////////////////////////////
////////////////////////from here we create 2 threads//////////
		
		ScheduledExecutorService twoThread = Executors.newScheduledThreadPool(2);
		
		twoThread.execute(() -> secureThreadGo());
		//twoThread.execute(() -> insecureThreadGo());

		twoThread.schedule(() -> insecureThreadGo(), 500, TimeUnit.MILLISECONDS);
		
	}
	
	
	private static void secureThreadGo() {
		System.setProperty("javax.net.ssl.trustStore", "sks/server.jks");
	    
		System.setProperty("javax.net.ssl.keyStore","sks/server.jks");
		//Password to access the private key from the keystore file
	    System.setProperty("javax.net.ssl.keyStorePassword","aalt_s");
		
	    try{
	    	SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			SSLServerSocket sslserversocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(sport);
			sslserversocket.setNeedClientAuth(true);
			
			log.info("Starting the EZshare Server");
			log.info("using secret: "+Server.secret);
			log.info("using advertiesd hostname: "+advertisedHostName);
			log.info("bound to port "+sport);
			// debugging
			log.info("****this is secure server");
			log.info("started");
			
			// This is the thread for exchanging server records between servers
			//***********************
			ScheduledExecutorService secureExecutor = Executors.newScheduledThreadPool(2);
			
			secureExecutor.scheduleAtFixedRate(() -> secureExe(), 5, exchangeInterval, TimeUnit.SECONDS);
			//**********************
			// Wait for connections.
			boolean connected = false;
			long timeLimit = System.currentTimeMillis() + connectionIntervalLimit*1000;
			
			while(true){
				
				//accept connection from client and creat a sslsocket
				SSLSocket sslclient = (SSLSocket) sslserversocket.accept();
				
				if (System.currentTimeMillis() < timeLimit && connected) {
					continue;
				}
				
				// debugging
				counter++;
				System.out.println("Secure Server: Client "+counter+": Applying for connection!");
				
				// Start a new thread for a connection
				secureExecutor.execute(() ->serveSSLClient(sslclient));
				
				connected = true;
				timeLimit = System.currentTimeMillis() + connectionIntervalLimit*1000;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void insecureThreadGo() {
		ServerSocketFactory factory = ServerSocketFactory.getDefault();
		try(ServerSocket server = factory.createServerSocket(port)){
			log.info("Starting the EZshare Server");
			log.info("using secret: "+Server.secret);
			log.info("using advertiesd hostname: "+advertisedHostName);
			log.info("bound to port "+port);
			// debugging
			log.info("****this is insecure server");
			log.info("started");
			
			// This is the thread for exchanging server records between servers
			//***********************
			
			ScheduledExecutorService secureExecutor = Executors.newScheduledThreadPool(2);
			
			secureExecutor.scheduleAtFixedRate(() -> insecureExe(), 5, exchangeInterval, TimeUnit.SECONDS);
			
			//**********************
			// Wait for connections.
			boolean connected = false;
			long timeLimit = System.currentTimeMillis() + connectionIntervalLimit*1000;
			
			while(true){
				Socket client = server.accept();
				
				if (System.currentTimeMillis() < timeLimit && connected) {
					continue;
				}
				
				// debugging
				counter++;
				System.out.println("Insecure Sever : Client "+counter+": Applying for connection!");
				// Start a new thread for a connection
				
				secureExecutor.execute(() ->serveClient(client));

				connected = true;
				timeLimit = System.currentTimeMillis() + connectionIntervalLimit*1000;
			}				
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static void serveSSLClient(SSLSocket sslclient) {
		try(SSLSocket clientSocket = sslclient){
			
			// The JSON Parser
			JSONParser parser = new JSONParser();
			// Input stream
			DataInputStream input = new DataInputStream(clientSocket.
					getInputStream());
			// Output Stream
		    DataOutputStream output = new DataOutputStream(clientSocket.
		    		getOutputStream());
//			    System.out.println("CLIENT: "+input.readUTF());
//			    output.writeUTF("Server: Hi Client "+counter+" !!!");
		    
		    // debugging
		    String serverName;
		    serverName = "secure Server **** ";
			
		    while(true){
		    	String message;
		    	if((message=input.readUTF())!= null){
		    		// Attempt to convert read data to JSON
		    		JSONObject command = (JSONObject) parser.parse(message);
		    		if(debug){
		    			log.debug(serverName + "RECIEVED: "+command.toJSONString());
		    		}
		    		
		    		// start to check which thread it belongs to
		    		JSONArray result;
		    		result = SSLMath.parseCommand(command, output);
		    		
		    		
		    		for(int i=0;i<result.size();i++){
			    		
			    		output.writeUTF(((JSONObject)result.get(i)).toJSONString());
			    		output.flush();	
			    		if(debug){
			    			//debugging
			    			log.debug(serverName + "SENT: "+((JSONObject) result.get(i)).toJSONString());
			    		}
		    		}
		    		break;
		    	}
		    }
		    
		    output.close();
    		input.close();
		    
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Wrong I/O, maybe your are trying to connect secure server via insecure socket");
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
	}
	
	private static void serveClient(Socket client) {
		try(Socket clientSocket = client){
			
			// The JSON Parser
			JSONParser parser = new JSONParser();
			// Input stream
			DataInputStream input = new DataInputStream(clientSocket.
					getInputStream());
			// Output Stream
		    DataOutputStream output = new DataOutputStream(clientSocket.
		    		getOutputStream());
//			    System.out.println("CLIENT: "+input.readUTF());
//			    output.writeUTF("Server: Hi Client "+counter+" !!!");
		    
		    // debugging
		    String serverName;
			serverName = "Insecure Server **** ";
			
		    while(true){
		    	if(input.available() > 0){
		    		// Attempt to convert read data to JSON
		    		JSONObject command = (JSONObject) parser.parse(input.readUTF());
		    		if(debug){
		    			log.debug(serverName + "RECIEVED: "+command.toJSONString());
		    		}
		    		
		    		
		    		// start to check which thread it belongs to
		    		JSONArray result;
		    		
			    	result = Math.parseCommand(command, output);
		    		
		    		
		    		for(int i=0;i<result.size();i++){
			    		
			    		output.writeUTF(((JSONObject)result.get(i)).toJSONString());
			    		output.flush();	
			    		if(debug){
			    			//debugging
			    			log.debug(serverName + "SENT: "+((JSONObject) result.get(i)).toJSONString());
			    		}
		    		}
		    		break;
		    	}
		    }
		    
		    output.close();
    		input.close();
		    
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	
	
	public static void AddOptions(Options options) {
		options.addOption("debug", false, "Print debut information");
		options.addOption("secret", true, "Server secret");
		options.addOption("port", true, "server port, an integer");
		options.addOption("sport", true, "secure server port, an integeer");
		options.addOption("exchangeinterval", true, "exchange interval in seconds");
		options.addOption("connectionintervallimit", true, "connection interval limit in seconds");
		options.addOption("advertisedhostname", true, "advertised hostname");
		

	}

	private static String getRandomString(int length){
	     String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	     Random random=new Random();
	     StringBuffer sb=new StringBuffer();
	     for(int i=0;i<length;i++){
	       int number=random.nextInt(62);
	       sb.append(str.charAt(number));
	     }
	     return sb.toString();
	 }
	
	
	private static void secureExe() {
		// debugging
		String serverName = "Secure Server";
		int serverListSize = secureServerRecords.size();
		System.out.println("1: " + serverName + " has server records of size: " + serverListSize);
		
		String secureList = "";
		for (int k = 0; k < serverListSize; k++) {
			secureList += secureServerRecords.get(k) + ", ";
		}
		
		System.out.println("2: " + serverName + " has serverList:" + secureList);
		
		if (secureServerRecords.size() == 0) {
			// debugging
			System.out.println("3: " + serverName + " : No servers to excahnge");
		
		} else {
			
			int selectedIndex = (new Random()).nextInt(secureServerRecords.size());
			
			String host_ip = secureServerRecords.get(selectedIndex);
			// debugging
			System.out.println("4: " + serverName + " selected server: " + host_ip);
			
			String[] host_ip_arr = host_ip.split(":");
			String host_name = host_ip_arr[0];
			int ip_add = Integer.parseInt(host_ip_arr[1]);
			
			JSONObject exchangeCommand = new JSONObject();
			String records = "";
			for (int i = 0; i<secureServerRecords.size(); i++) {
				records += secureServerRecords.get(i) + ",";
			}
			try {
				// add local address
				records += InetAddress.getLocalHost().getHostAddress() + ":" + sport;
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			exchangeCommand.put("command", "EXCHANGE");
			exchangeCommand.put("serverList", records);
			
			try(Socket randomServer = new Socket(host_name, ip_add)){
				DataInputStream input = new DataInputStream(randomServer.getInputStream());
				DataOutputStream output = new DataOutputStream(randomServer.getOutputStream());
			
				output.writeUTF(exchangeCommand.toJSONString());
				output.flush();
				
				//System.out.println("Command sent");
				
				// Time limit for execution
				long start = System.currentTimeMillis();
				long end = start + 5 * 1000;
				boolean isReachable = false;
				while(System.currentTimeMillis() < end) {
					if (input.available() > 0) {
						isReachable = true;
						String result = input.readUTF();
						// debugging
						System.out.println("5:" + serverName + " : Received from other server: " + result);
					}
				}
				if (!isReachable) {
					secureServerRecords.remove(selectedIndex);
					// debugging
					System.out.println("6: " + serverName + ": Removed unreachable server: " + secureServerRecords.get(selectedIndex));
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				
				secureServerRecords.remove(selectedIndex);
				// debugging
				System.out.println("7: " + serverName + ": Removed unreachable server: " + secureServerRecords.get(selectedIndex));
			}
		}
	}
	
	
	private static void insecureExe() {
		
		String serverName = "Insecure Server";
		// debugging
		int serverListSize = serverRecords.size();
		System.out.println("1: " + serverName + " has server records of size: " + serverListSize);
		
		String secureList = "";
		for (int k = 0; k < serverListSize; k++) {
			secureList += serverRecords.get(k);
		}
		System.out.println("2: " + serverName + " has serverList:" + secureList);
		
		if (serverRecords.size() == 0) {
			// debugging
			System.out.println("3: " + serverName + " : No servers to excahnge");
		} else {
			
			int selectedIndex = (new Random()).nextInt(serverRecords.size());
		
			String host_ip = serverRecords.get(selectedIndex);
			// debugging
			System.out.println("4: " + serverName + "selected server: " + host_ip);
			String[] host_ip_arr = host_ip.split(":");
			String host_name = host_ip_arr[0];
			int ip_add = Integer.parseInt(host_ip_arr[1]);
			JSONObject exchangeCommand = new JSONObject();
			String records = "";
			for (int i = 0; i<serverRecords.size(); i++) {
				records += serverRecords.get(i) + ",";
			}
			try {
				records += InetAddress.getLocalHost().getHostAddress() + ":" + port;
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			exchangeCommand.put("command", "EXCHANGE");
			exchangeCommand.put("serverList", records);
			
			try(Socket randomServer = new Socket(host_name, ip_add)){
				DataInputStream input = new DataInputStream(randomServer.getInputStream());
				DataOutputStream output = new DataOutputStream(randomServer.getOutputStream());
			
				output.writeUTF(exchangeCommand.toJSONString());
				output.flush();
				
				//System.out.println("Command sent");
				
				// Time limit for execution
				long start = System.currentTimeMillis();
				long end = start + 5 * 1000;
				boolean isReachable = false;
				while(System.currentTimeMillis() < end) {
					if (input.available() > 0) {
						isReachable = true;
						String result = input.readUTF();
						System.out.println(serverName + " : Received from other server: " + result);
					}
				}
				if (!isReachable) {
					serverRecords.remove(selectedIndex);
					// debugging
					System.out.println("6: " + serverName + ": Removed unreachable server: " + serverRecords.get(selectedIndex));
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				serverRecords.remove(selectedIndex);
				// debugging
				System.out.println("7: " + serverName + " removed unreachable server: " + serverRecords.get(selectedIndex));
			}
		}
	}
}
