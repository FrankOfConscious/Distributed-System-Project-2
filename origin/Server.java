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
			try{
				if(Math.isPort(cmd.getOptionValue("port")))
					port = Integer.parseInt(cmd.getOptionValue("port"));
				else{
					System.out.println("Please provide valid port( integer:0~65535) arg.");
					System.exit(0);
				}
			}catch(Exception e){
				System.out.println("Please provide valid port( integer:0~65535) arg.");
				System.exit(0);
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

			
			//////////////////////////////////////////////////////////////
			ServerSocketFactory factory = ServerSocketFactory.getDefault();
			try(ServerSocket server = factory.createServerSocket(port)){
				log.info("Starting the EZshare Server");
				log.info("using secret: "+Server.secret);
				log.info("using advertiesd hostname: "+advertisedHostName);
				log.info("bound to port "+port);
				log.info("started");
				
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
					Thread t = new Thread(() -> serveClient(client));
					t.start();
					
					connected = true;
					timeLimit = System.currentTimeMillis() + connectionIntervalLimit*1000;
				}				
			} catch (IOException e) {
				e.printStackTrace();
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
			    
			    // Receive more data..
		
					
					
			    while(true){
			    	if(input.available() > 0){
			    		// Attempt to convert read data to JSON
			    		JSONObject command = (JSONObject) parser.parse(input.readUTF());
			    		if(debug){
			    			log.debug("RECIEVED: "+command.toJSONString());
			    		}
//			    		if(Server.debug){
//			    			log.debug("COMMAND RECEIVED: "+command.toJSONString());//////
//			    		}else{
//			    			System.out.println("COMMAND RECEIVED: "+command.toJSONString());
//			    		}
			    	
			    		JSONArray result = Math.parseCommand(command, output);
			    		for(int i=0;i<result.size();i++){
				    		
				    		output.writeUTF(((JSONObject)result.get(i)).toJSONString());
				    		output.flush();	
				    		if(debug&&!result.get(i).toString().equals("{\"endOfTransmit\":true}")){
				    			log.debug("SENT: "+((JSONObject) result.get(i)).toJSONString());
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


		
		////////to update
		public static void AddOptions(Options options) {
			options.addOption("debug", false, "Print debut information");
			options.addOption("secret", true, "Server secret");
			options.addOption("port", true, "server port, an integer");
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
				}
				
			};
			timer.schedule(task, 0, exchangeInterval * 1000);
		}

	}