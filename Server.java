package EZShare2;
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
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;

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
	static ArrayList<String> subscribeID=new ArrayList<String>();
	static HashMap subscribedItem=new HashMap();

		
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
		ExecutorService twoThread = Executors.newFixedThreadPool(2);
		
		twoThread.execute(() -> insecureThreadGo());
		
		twoThread.execute(() -> secureThreadGo());
	}
	
	
	private static void secureThreadGo() {
		
		ServerSocketFactory factory = ServerSocketFactory.getDefault();
		try(ServerSocket server = factory.createServerSocket(sport)){
			log.info("Starting the EZshare Server");
			log.info("using secret: "+Server.secret);
			log.info("using advertiesd hostname: "+advertisedHostName);
			log.info("bound to port "+sport);
			// debugging
			log.info("this is secure server");
			log.info("started");
			
			// This is the thread for exchanging server records between servers
			//***********************
			ScheduledExecutorService secureExecutor = Executors.newScheduledThreadPool(2);
			
			secureExecutor.scheduleAtFixedRate(() -> secureExe(), 5, connectionIntervalLimit, TimeUnit.SECONDS);
			//**********************
			// Wait for connections.
			boolean connected = false;
			long timeLimit = System.currentTimeMillis() + connectionIntervalLimit*1000;
			
			while(true){
				Socket client = server.accept();
				
				if (System.currentTimeMillis() < timeLimit && connected) {
					continue;
				}
				
				counter++;
				System.out.println("Client "+counter+": Applying for connection!");
				
				// Start a new thread for a connection
				secureExecutor.execute(() ->serveClient(client, true));
				
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
			log.info("this is insecure server");
			log.info("started");
			
			// This is the thread for exchanging server records between servers
			//***********************
			Thread t2 = new Thread(() -> {
				try {
					exchangeServerRec();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			t2.start();
			
			//**********************
			// Wait for connections.
			boolean connected = false;
			long timeLimit = System.currentTimeMillis() + connectionIntervalLimit*1000;
			
			while(true){
				Socket client = server.accept();
				
				if (System.currentTimeMillis() < timeLimit && connected) {
					continue;
				}
				
				counter++;
				System.out.println("Client "+counter+": Applying for connection!");
				// Start a new thread for a connection
				Thread t = new Thread(() -> serveClient(client, false));
				t.start();
				
				connected = true;
				timeLimit = System.currentTimeMillis() + connectionIntervalLimit*1000;
			}				
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void serveClient(Socket client, Boolean isSecure) {
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
		    
		    // Receive more data..
		    boolean end=false;
		    boolean subscribeContinue=false;
		    while(true){
		    	if(input.available() > 0){
		    		// Attempt to convert read data to JSON
		    		JSONObject command = (JSONObject) parser.parse(input.readUTF());
		    		if(debug){
		    			log.debug("RECIEVED: "+command.toJSONString());
		    		}
		    		
		    		int category=0;
		    		if(command.containsKey("command")){
		    			switch((String)command.get("command")){
		    			case "PUBLISH":
		    			case "EXCHANGE":
		    			case "SHARE":
		    			case "REMOVE": category=1;break;
		    			case "QUERY":category=2;break;
		    			case "FETCH":category=3;break;
		    			case "SUBSCRIBE": category=4;break;
		    			case "UNSUBSCRIBE": category=5;break;
		    			default: break;
		    			}
		    		}	
		    		
		    		// start to check which thread it belongs to
		    		JSONArray result;
		    		if (isSecure) {
			    		result = SSLMath.parseCommand(command, output);

		    		} else {
			    		result = Math.parseCommand(command, output);

		    		}
		    		
		    		JSONObject subCommand=null;
		    		for(int i=0;i<result.size();i++){
			    		if(!result.get(i).toString().equals("{\"endOfTransmit\":true}")){
			    			JSONObject temp=(JSONObject) result.get(i);
			    			if(temp.containsKey("response")&&
			    					temp.get("response").equals("success")&&
			    					temp.containsKey("id")){
			    				subCommand=temp;
			    				
			    				subscribeContinue=true;
			    				System.out.println("INTO CONTINUE:"+subscribeContinue);
			    			}
			    			output.writeUTF(((JSONObject)result.get(i)).toJSONString());
			    			output.flush();	
			    			if(debug)log.debug("SENT: "+((JSONObject) result.get(i)).toJSONString());
			    		}
			    		else if(category==0 ||category==1||category==5){
			    			end=true;
			    			}else if(category==4&&subscribeContinue){
			    				//Thread !!!!!
			    				System.out.println("INTO if!!"+"continue:"+subscribeContinue);
			    				Thread Subscribe = new Thread(() -> {
			    					try {
			    						System.out.println("INTO Thread Subscribe!!");
			    						doSubscribe(output,command,isSecure);
			    					} catch (UnknownHostException e) {
			    						// TODO Auto-generated catch block
			    						e.printStackTrace();
			    					} catch (IOException e) {
			    						// TODO Auto-generated catch block
			    						e.printStackTrace();
			    					}
			    				});
			    				Subscribe.start();
			    			}
			    		}
		    		
		    		
		    		if(end) break;
		    	}
		    }
		    
		    output.close();
    		input.close();
		    
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}


	
	private static void doSubscribe(DataOutputStream output, JSONObject command,boolean secure) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("INTO Method doSubscribe!!");
		JSONObject raw;
		if(secure) raw=SSLMath.preProcess(command);
		else raw=Math.preProcess(command);
		String id=(String) raw.get("id");
		subscribeID.add(id);
		subscribedItem.put(id, (int)0);
		JSONObject result=new JSONObject();
		long startTime=System.currentTimeMillis();
		boolean updateTime=false;
		if(command.containsKey("relay")&&((boolean)command.get("relay"))==true){
			JSONObject relaycommand=new JSONObject(command);
			relaycommand.replace("name", "");
			relaycommand.replace("description", "");
			relaycommand.replace("relay", false);
		}
		if(secure){
		while(Server.subscribeID.contains(id)){
			long time1=System.currentTimeMillis();
			while(true){
				if(System.currentTimeMillis()>time1+1000)
					break;
			}
			updateTime=false;
			for(int i=0;i<Server.resourceList.size();i++){
				if(startTime<Server.resourceList.get(i).getTime()&&
						SSLMath.queryMatch(Server.resourceList.get(i),command)){
					output.writeUTF(      ( new Resource(Server.resourceList.get(i).getObj())    ).toJSON().toJSONString()     );
					output.flush();
					subscribedItem.replace(id, (int)subscribedItem.get(id)+1);
					updateTime=true;
				}
			}
			if(updateTime)
			startTime=System.currentTimeMillis();
		}
		
		}else{
			
			while(Server.subscribeID.contains(id)){
			//	System.out.println("templateTime:"+startTime);
				long time1=System.currentTimeMillis();
				while(true){
					if(System.currentTimeMillis()>time1+1000)
						break;
				}
				updateTime=false;
			for(int i=0;i<Server.resourceList.size();i++){
				if(Math.queryMatch(Server.resourceList.get(i),command)&&
						startTime<Server.resourceList.get(i).getTime()
						){
					output.writeUTF(      ( new Resource(Server.resourceList.get(i).getObj())    ).toJSON().toJSONString()     );
					output.flush();
					subscribedItem.replace(id, (int)subscribedItem.get(id)+1);
					updateTime=true;
					
				}
			}
			if(updateTime)
			startTime=System.currentTimeMillis();
			}
			
			
		}
		
		
		
	}


	////////to update
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
	
	
	private static void exchangeSecureServerRec() throws UnknownHostException, IOException {
		// create a thread that only res
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		Runnable periodicTask = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (secureServerRecords.size() == 0) {
					
				} else {
					
					int selectedIndex = (new Random()).nextInt(secureServerRecords.size());
				
					String host_ip = secureServerRecords.get(selectedIndex);
					String[] host_ip_arr = host_ip.split(":");
					String host_name = host_ip_arr[0];
					int ip_add = Integer.parseInt(host_ip_arr[1]);
					JSONObject exchangeCommand = new JSONObject();
					String records = "";
					for (int i = 0; i<secureServerRecords.size(); i++) {
						records += secureServerRecords.get(i) + ",";
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
								/////////debuging///////////
								String result = input.readUTF();
								System.out.println(result);
							}
						}
						if (!isReachable) {
							secureServerRecords.remove(selectedIndex);
							//System.out.println("Removed unreachable server-" + serverRecords.get(selectedIndex));
						}
						
					} catch (IOException e) {
						//e.printStackTrace();
						//System.out.println("Record invalid!" + serverRecords.size());
						secureServerRecords.remove(selectedIndex);
					}
				}
			}
			
		};
		executor.scheduleAtFixedRate(periodicTask, 5, exchangeInterval, TimeUnit.SECONDS);
	}
	
	//********************
	
	
	private static void exchangeServerRec() throws UnknownHostException, IOException {
		
		// Creates a socket for another server, the socket that will send msg to

		Timer timer = new Timer();
		TimerTask task = new TimerTask() {

			public void run() {
				// TODO Auto-generated method stub
				if (serverRecords.size() == 0) {
					
				} else {
					
					int selectedIndex = (new Random()).nextInt(serverRecords.size());
				
					String host_ip = serverRecords.get(selectedIndex);
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
							
							}
						}
						if (!isReachable) {
							serverRecords.remove(selectedIndex);
							//System.out.println("Removed unreachable server-" + serverRecords.get(selectedIndex));
						}
						
					} catch (IOException e) {
						//e.printStackTrace();
						//System.out.println("Record invalid!" + serverRecords.size());
						serverRecords.remove(selectedIndex);
					}
				}
				///
			}
			
		};
		timer.schedule(task, 0, exchangeInterval * 1000);
	}
	
	
	private static void secureExe() {
		if (secureServerRecords.size() == 0) {
			
		} else {
			
			int selectedIndex = (new Random()).nextInt(secureServerRecords.size());
		
			String host_ip = secureServerRecords.get(selectedIndex);
			String[] host_ip_arr = host_ip.split(":");
			String host_name = host_ip_arr[0];
			int ip_add = Integer.parseInt(host_ip_arr[1]);
			JSONObject exchangeCommand = new JSONObject();
			String records = "";
			for (int i = 0; i<secureServerRecords.size(); i++) {
				records += secureServerRecords.get(i) + ",";
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
					
					}
				}
				if (!isReachable) {
					secureServerRecords.remove(selectedIndex);
					//System.out.println("Removed unreachable server-" + serverRecords.get(selectedIndex));
				}
				
			} catch (IOException e) {
				//e.printStackTrace();
				//System.out.println("Record invalid!" + serverRecords.size());
				secureServerRecords.remove(selectedIndex);
			}
		}
	}
	
	
	private static void insecureExe() {
		if (serverRecords.size() == 0) {
			
		} else {
			
			int selectedIndex = (new Random()).nextInt(serverRecords.size());
		
			String host_ip = serverRecords.get(selectedIndex);
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
					
					}
				}
				if (!isReachable) {
					serverRecords.remove(selectedIndex);
					//System.out.println("Removed unreachable server-" + serverRecords.get(selectedIndex));
				}
				
			} catch (IOException e) {
				//e.printStackTrace();
				//System.out.println("Record invalid!" + serverRecords.size());
				serverRecords.remove(selectedIndex);
			}
		}
	}
}
