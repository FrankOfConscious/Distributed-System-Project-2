package EZShare2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.*;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;



@SuppressWarnings("unused")
class Client {
	private static String ip;
	private static int port;
	private static boolean debug = false;	
	private static final Logger log = Logger.getLogger(Logger.class);
	
	
	public static void main(String[] args) {

		// Parse CMD options
		Options options = new Options();
		AddOptions(options);

		// accept args from CMD
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println("Command is invalid or not found. \nPlease check your command and try again.");
			System.exit(0);
		}
		try{
			port = Integer.parseInt(cmd.getOptionValue("port"));
			if(port>65535||port<0){
				System.out.println("Port or IP is invalid.\nPlease provid valid port and ip args.");
				System.exit(0);
			}
			
			ip = cmd.getOptionValue("host");
			
		}catch(Exception e){
			log.warn("Port or IP is invalid.\nPlease provid valid port and ip args.");
			System.exit(0);
		}
		
		if(cmd.hasOption("debug")) {
			debug = true;
		}
		log.info("Client has started.");
		//-----------
		
		if(cmd.hasOption("secure")){
			//Location of the Java keystore file containing the collection of 
			//certificates trusted by this application (trust store).
			System.setProperty("javax.net.ssl.trustStore", "client/client.jks");
		    
		    //Test
		    //the keystore file contains an application's own certificate and private key
	        System.setProperty("javax.net.ssl.keyStore","client/client.jks");
	        //Password to access the private key from the keystore file
	        System.setProperty("javax.net.ssl.keyStorePassword","poiuytre");
			
			
			System.setProperty("javax.net.debug","all");
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			try {
				SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(ip, port);
				DataInputStream input = new DataInputStream(sslsocket.getInputStream());
				DataOutputStream output = new DataOutputStream(sslsocket.getOutputStream());
				JSONObject raw = autoFill(cmd);
				boolean exit=false;
				String tag;
				if (!raw.containsKey("command")){
					try{
						output.writeUTF(raw.toJSONString());
						output.flush();
						if(debug){
							log.info("SENT: "+raw.toJSONString());	
						}
					}catch(IOException e){
						e.printStackTrace();
						System.exit(0);			
					}
					try {
						while(true){
							//ssl readutf
							if(input.readUTF() != null){
								String message = input.readUTF();
								JSONParser parser1=new JSONParser();
								if(debug){
									log.info("RECEIVED: " + message);}
								else{
									log.info(message);}
								if(((JSONObject) parser1.parse(message)).containsKey("errorMessage")){
									
									exit=true; break;
								}else break;
													
							}
						}
						input.close();
						output.close();
					} catch (IOException e) {
							log.warn("Server seems to have closed connection.");
							System.exit(0);
						}
					
				}
				tag=(String) raw.get("command");
				switch(tag){
				case "PUBLISH":
				case "REMOVE":
				case "SHARE":
				case "EXCHANGE":
					try{
						output.writeUTF(raw.toJSONString());
						output.flush();
						if(debug){
							log.info("SENT: "+raw.toJSONString());	
						}
					}catch(IOException e){
						e.printStackTrace();
						System.exit(0);			
					}
					
					try {
						while(true){
							//ssl readutf
							if(input.readUTF() != null){
								String message = input.readUTF();
								JSONParser parser1=new JSONParser();
								if(debug){
									log.info("RECEIVED: " + message);}
								else{
									log.info(message);}
								if(((JSONObject) parser1.parse(message)).containsKey("errorMessage")){
									
									exit=true; break;
								}else if(((JSONObject) parser1.parse(message)).get("response").equals("success")){
									exit=true;break;
								}					
							}
						}
						input.close();
						output.close();
					} catch (IOException e) {
							log.warn("Server seems to have closed connection.");
							System.exit(0);
						}
					break;
				case "FETCH"://////////////////////
					try{
						output.writeUTF(raw.toJSONString());
						output.flush();
						if(debug){
							log.info("SENT: "+raw.toJSONString());	
						}
					}catch(IOException e){
						e.printStackTrace();
						System.exit(0);			
					}
					boolean fetchFlag=false;
					try {
						String fetchMessage = null;
						while(true){						
							if(input.available()>0){
								String message = input.readUTF();
								JSONParser parser1=new JSONParser();
								JSONObject temp=(JSONObject) parser1.parse(message);
								
								if(debug){
									log.info("RECEIVED: " + message);}
								else{
									log.info(message);}
								if(temp.containsKey("errorMessage")) {exit=true;break;}
								else if((int)temp.get("resultSize")==0){exit=true;break;}
								else
								if(((JSONObject) parser1.parse(message)).containsKey("resourceSize"))
									fetchMessage=message;
								break;
							}
						}
						if(fetchMessage!=null) doFetch(fetchMessage,input);
						input.close();
						output.close();
					} catch (IOException e) {
							log.warn("Server seems to have closed connection.");
							System.exit(0);
						}
					break;
				
				
				case "QUERY":
					try{
						output.writeUTF(raw.toJSONString());
						output.flush();
						if(debug){
							log.info("SENT: "+raw.toJSONString());	
						}
					}catch(IOException e){
						e.printStackTrace();
						System.exit(0);			
					}
		//			boolean fetchFlag=false;
					try {
						String fetchMessage = null;
						while(true){						
							if(input.available()>0){
								String message = input.readUTF();
								JSONParser parser1=new JSONParser();
								JSONObject temp=(JSONObject) parser1.parse(message);
								if(debug){
									log.info("RECEIVED: " + message);}
								else{
									log.info(message);}
								if(temp.containsKey("errorMessage")) {exit=true;break;}
								else if(temp.containsKey("resultSize")){exit=true;break;}
							}
						}
						input.close();
						output.close();
					} catch (IOException e) {
							log.warn("Server seems to have closed connection.");
							System.exit(0);
						}
				case "SUBSCRIBE":
					try{
						output.writeUTF(raw.toJSONString());
						output.flush();
						if(debug){
							log.info("SENT: "+raw.toJSONString());	
						}
					}catch(IOException e){
						e.printStackTrace();
						System.exit(0);			
					}
		//			boolean fetchFlag=false;
					try {
						String fetchMessage = null;
						while(true){						
							if(input.available()>0){
								String message = input.readUTF();
								JSONParser parser1=new JSONParser();
								JSONObject temp=(JSONObject) parser1.parse(message);
								if(debug){
									log.info("RECEIVED: " + message);}
								else{
									log.info(message);}
								if(temp.containsKey("errorMessage")) {exit=true;break;}
								else if(temp.containsKey("response")&& ((String)temp.get("response")).equals("success")){
									final String id=(String) temp.get("id");
									Thread t2 = new Thread(() -> {
										try {
											listenToEnter(output,id);
										} catch (UnknownHostException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									});
									t2.start();
									}
							}
						}
						input.close();
						output.close();
					} catch (IOException e) {
							log.warn("Server seems to have closed connection.");
							System.exit(0);
						}
					break;
				
					
				default: break;
					
				
				}
			}catch(Exception e){
				System.exit(0);
			}

						
		}
		else{		
			// connect to a server socket
		try (Socket socket = new Socket(ip, port)) {
			// Get I/O streams for connection
			DataInputStream input = new DataInputStream(socket.getInputStream());
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			//**LIAM**
			JSONObject raw = autoFill(cmd);
			boolean exit=false;
			String tag;
			if (!raw.containsKey("command")){
				try{
					output.writeUTF(raw.toJSONString());
					output.flush();
					if(debug){
						log.info("SENT: "+raw.toJSONString());	
					}
				}catch(IOException e){
					e.printStackTrace();
					System.exit(0);			
				}
				try {
					while(true){
						//ssl readutf
						if(input.available()>0){
							String message = input.readUTF();
							JSONParser parser1=new JSONParser();
							if(debug){
								log.info("RECEIVED: " + message);}
							else{
								log.info(message);}
							if(((JSONObject) parser1.parse(message)).containsKey("errorMessage")){
								
								exit=true; break;
							}else break;
												
						}
					}
					input.close();
					output.close();
				} catch (IOException e) {
						log.warn("Server seems to have closed connection.");
						System.exit(0);
					}
				
			}
			tag=(String) raw.get("command");
			switch(tag){
			case "PUBLISH":
			case "REMOVE":
			case "SHARE":
			case "EXCHANGE":
				try{
					output.writeUTF(raw.toJSONString());
					output.flush();
					if(debug){
						log.info("SENT: "+raw.toJSONString());	
					}
				}catch(IOException e){
					e.printStackTrace();
					System.exit(0);			
				}
				
				try {
					while(true){
						if(input.available()>0){
							String message = input.readUTF();
							JSONParser parser1=new JSONParser();
							if(debug){
								log.info("RECEIVED: " + message);}
							else{
								log.info(message);}
							if(((JSONObject) parser1.parse(message)).containsKey("errorMessage")){
								
								exit=true; break;
							}else if(((JSONObject) parser1.parse(message)).get("response").equals("success")){
								exit=true;break;
							}					
						}
					}
					input.close();
					output.close();
				} catch (IOException e) {
						log.warn("Server seems to have closed connection.");
						System.exit(0);
					}
				break;
			case "FETCH"://////////////////////
				try{
					output.writeUTF(raw.toJSONString());
					output.flush();
					if(debug){
						log.info("SENT: "+raw.toJSONString());	
					}
				}catch(IOException e){
					e.printStackTrace();
					System.exit(0);			
				}
				boolean fetchFlag=false;
				try {
					String fetchMessage = null;
					while(true){						
						if(input.available()>0){
							String message = input.readUTF();
							JSONParser parser1=new JSONParser();
							JSONObject temp=(JSONObject) parser1.parse(message);
							
							if(debug){
								log.info("RECEIVED: " + message);}
							else{
								log.info(message);}
							if(temp.containsKey("errorMessage")) {exit=true;break;}
							else if((int)temp.get("resultSize")==0){exit=true;break;}
							else
							if(((JSONObject) parser1.parse(message)).containsKey("resourceSize"))
								fetchMessage=message;
							break;
						}
					}
					if(fetchMessage!=null) doFetch(fetchMessage,input);
					input.close();
					output.close();
				} catch (IOException e) {
						log.warn("Server seems to have closed connection.");
						System.exit(0);
					}
				break;
			
			
			case "QUERY":
				try{
					output.writeUTF(raw.toJSONString());
					output.flush();
					if(debug){
						log.info("SENT: "+raw.toJSONString());	
					}
				}catch(IOException e){
					e.printStackTrace();
					System.exit(0);			
				}
	//			boolean fetchFlag=false;
				try {
					String fetchMessage = null;
					while(true){						
						if(input.available()>0){
							String message = input.readUTF();
							JSONParser parser1=new JSONParser();
							JSONObject temp=(JSONObject) parser1.parse(message);
							if(debug){
								log.info("RECEIVED: " + message);}
							else{
								log.info(message);}
							if(temp.containsKey("errorMessage")) {exit=true;break;}
							else if(temp.containsKey("resultSize")){exit=true;break;}
						}
					}
					input.close();
					output.close();
				} catch (IOException e) {
						log.warn("Server seems to have closed connection.");
						System.exit(0);
					}
			case "SUBSCRIBE":
				try{
					output.writeUTF(raw.toJSONString());
					output.flush();
					if(debug){
						log.info("SENT: "+raw.toJSONString());	
					}
				}catch(IOException e){
					e.printStackTrace();
					System.exit(0);			
				}
	//			boolean fetchFlag=false;
				try {
					String fetchMessage = null;
					while(true){						
						if(input.available()>0){
							String message = input.readUTF();
							JSONParser parser1=new JSONParser();
							JSONObject temp=(JSONObject) parser1.parse(message);
							if(debug){
								log.info("RECEIVED: " + message);}
							else{
								log.info(message);}
							if(temp.containsKey("errorMessage")||temp.containsKey("resultSize")) {exit=true;break;}
							else if(temp.containsKey("response")&& ((String)temp.get("response")).equals("success")){
								final String id=(String) temp.get("id");
								Thread t2 = new Thread(() -> {
									try {
										listenToEnter(output,id);
									} catch (UnknownHostException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								});
								t2.start();
								}
						}
					}
					input.close();
					output.close();
				} catch (IOException e) {
						log.warn("Server seems to have closed connection.");
						System.exit(0);
					}
				break;
				
			default: break;
				
				
			
			
			
			}
		}catch(Exception e){
			System.exit(0);
		}
			
		}
		
		
		

//			try{
//				output.writeUTF(raw.toJSONString());
//				output.flush();
//				if(debug){
//					log.info("SENT: "+raw.toJSONString());	
//				}
//			}catch(IOException e){
//				e.printStackTrace();
//				System.exit(0);			
//			}
//			boolean fetchFlag=false;
//			try {
//				long startTime=System.currentTimeMillis();
////				boolean timeoutFlag=true;
//				String fetchMessage = null;
//				while(true){
//					
//					if(input.available()>0){
//						startTime=System.currentTimeMillis();
////						timeoutFlag=false;
//						String message = input.readUTF();
//						if(message.equals("{\"endOfTransmit\":true}")) break;
//						JSONParser parser1=new JSONParser();
//						if(debug){
//							log.info("RECEIVED: " + message);}
//						else{
//							log.info(message);}
//						if(((JSONObject) parser1.parse(message)).containsKey("resourceSize"))
//							fetchMessage=message;
//							
////						if(debug){log.info("RECEIVED: " + message);}
////						else{
////							log.info(message);
////						}
//					}
//					// Timeout of client
//					if((System.currentTimeMillis()-startTime)/1000>=10){
//						System.out.println("Time out.\nClient has exited.");
//						System.exit(0);
//					}
//				}
//				if(fetchMessage!=null) doFetch(fetchMessage,input);
//				input.close();
//				output.close();
//			} catch (IOException e) {
//					log.warn("Server seems to have closed connection.");
//					System.exit(0);
//				}
//		} catch (Exception e) {
//			log.warn("Server seems to have closed connection.");
//			System.exit(0);
//			//e.printStackTrace();			
//		}
	}
	
	private static void doFetch(String result, DataInputStream input) {
		// TODO Auto-generated method stub
				JSONObject cmd = new JSONObject();
				try {
					JSONParser parser = new JSONParser();;
					cmd = (JSONObject) parser.parse(result);
					// Create a RandomAccessFile to read and write the
					// output file.
					String uriStr = (String)cmd.get("uri");
					String fileName = uriStr.substring( uriStr.lastIndexOf('/')+1, uriStr.length() );
					RandomAccessFile downloadingFile = null; 
					try {
						downloadingFile = new RandomAccessFile(fileName, "rw");
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// Find out how much size is remaining to get from the server
					long fileSizeRemaining = (Long) cmd.get("resourceSize");
					int chunkSize = setChunkSize(fileSizeRemaining);
					// Represents the receiving buffer
					byte[] receiveBuffer = new byte[chunkSize];
					// Variable used to read if there are remaining size
					// left to read.
					int num;
					try {
						while ((num = input.read(receiveBuffer)) > 0) {
							// Write the received bytes into the
							// RandomAccessFile
							downloadingFile.write(Arrays.copyOf(receiveBuffer, num));

							// Reduce the file size left to read..
							fileSizeRemaining -= num;

							// Set the chunkSize again
							chunkSize = setChunkSize(fileSizeRemaining);
							receiveBuffer = new byte[chunkSize];

							// If you're done then break
							if (fileSizeRemaining == 0) {
								break;
							}
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						downloadingFile.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (org.json.simple.parser.ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
		
		
	}

	public static int setChunkSize(long fileSizeRemaining) {
		// Determine the chunkSize
		int chunkSize = 1024 * 1024;
		// If the file size remaining is less than the chunk size
		// then set the chunk size to be equal to the file size.
		if (fileSizeRemaining < chunkSize) {
			chunkSize = (int) fileSizeRemaining;
		}
		return chunkSize;
	}

	public static void AddOptions(Options options) {
		options.addOption("port", true, "Server port");
		options.addOption("host", true, "Server IP address");
		options.addOption("channel", true, "channel");
		options.addOption("debug", false, "print debug information");
		options.addOption("description", true, "resource description");
		options.addOption("exchange", false, "exchange server list with server");
		options.addOption("fetch", false, "fetch resources from server");
		options.addOption("name", true, "resource name");
		options.addOption("owner", true, "owner");
		options.addOption("publish", false, "publish resource on server");
		options.addOption("query", false, "query for resources from server");
		options.addOption("remove", false, "remove resource from server");
		options.addOption("secret", true, "secret");
		options.addOption("servers", true, "server list");
		options.addOption("share", false, "share resource on server");
		options.addOption("tags", true, "resource tags");
		options.addOption("uri", true, "resource URI");
		options.addOption("relay", true, "query relay");
		options.addOption("subscribe",false,"subscribe resource on server");
		options.addOption("unsubscribe",false,"unsubscribe resource on server");
		options.addOption("secure",false,"secure connections");
	}
	
	static JSONObject autoFill(CommandLine cmd) {
		JSONObject raw = new JSONObject();
		if(cmd.hasOption("secure")){
			raw = securesocketFill(cmd);
			return raw;	
		}
		else{
			raw = socketFill(cmd);
			return raw;
		}
	}
	
	@SuppressWarnings("unchecked")
	static JSONObject securesocketFill(CommandLine cmd){
		JSONObject raw = new JSONObject();
		if (cmd.hasOption("publish")) {
			raw.put("command", "PUBLISH");
			String name = "";
			if (cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}
			
			ArrayList<String> tags=new ArrayList<String>();
			if (cmd.hasOption("tags")) {
				String[] str=cmd.getOptionValue("tags").split(",");
				for(int i=0;i<str.length;i++){
					tags.add(str[i]);
				}
				//tags = cmd.getOptionValue("tags");
			}
			
			String des = "";
			if (cmd.hasOption("des")) {
				des = cmd.getOptionValue("description");
			}
			String uri = "";
			if (cmd.hasOption("uri")) {
				uri = cmd.getOptionValue("uri");
			}
			String channel = "";
			if (cmd.hasOption("channel")) {
				channel = cmd.getOptionValue("channel");
			}
			String owner = "";
			if (cmd.hasOption("owner")) {
				owner = cmd.getOptionValue("owner");
			}
			JSONObject pubRes = new JSONObject();
			pubRes.put("name", name);
			pubRes.put("tags", tags);
			pubRes.put("description", des);
			pubRes.put("uri", uri);
			pubRes.put("channel", channel);
			pubRes.put("owner", owner);
			pubRes.put("ezserver", null);
			pubRes.put("secure", true);		
			raw.put("resource", pubRes);
			return raw;
		}

		if (cmd.hasOption("remove")) {
			raw.put("command", "REMOVE");

			String name = "";
			if (cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}

			ArrayList<String> tags=new ArrayList<String>();
			if (cmd.hasOption("tags")) {
				String[] str=cmd.getOptionValue("tags").split(",");
				for(int i=0;i<str.length;i++){
					tags.add(str[i]);
				}
				//tags = cmd.getOptionValue("tags");
			} 

			String des = "";
			if (cmd.hasOption("description")) {
				des = cmd.getOptionValue("description");
			}

			String uri = "";
			if (cmd.hasOption("uri")) {
				uri = cmd.getOptionValue("uri");
			}

			String channel = "";
			if (cmd.hasOption("channel")) {
				channel = cmd.getOptionValue("channel");
			}

			String owner = "";
			if (cmd.hasOption("owner")) {
				owner = cmd.getOptionValue("owner");
			}
			JSONObject remRes = new JSONObject();
			remRes.put("name", name);
			remRes.put("tags", tags);
			remRes.put("description", des);
			remRes.put("uri", uri);
			remRes.put("channel", channel);
			remRes.put("owner", owner);
			remRes.put("ezserver", null);
			remRes.put("secure", true);

			raw.put("resource", remRes);
			return raw;
		}

		if (cmd.hasOption("share")) {
			raw.put("command", "SHARE");

			String secret = "";
			if (cmd.hasOption("secret")) {
				secret = cmd.getOptionValue("secret");
			}
			raw.put("secret", secret);

			String name = "";
			if (cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}

			ArrayList<String> tags=new ArrayList<String>();
			if (cmd.hasOption("tags")) {
				String[] str=cmd.getOptionValue("tags").split(",");
				for(int i=0;i<str.length;i++){
					tags.add(str[i]);
				}
				//tags = cmd.getOptionValue("tags");
			}

			String des = "";
			if (cmd.hasOption("description")) {
				des = cmd.getOptionValue("description");
			}

			String uri = "";
			if (cmd.hasOption("uri")) {
				uri = cmd.getOptionValue("uri");
			}

			String channel = "";
			if (cmd.hasOption("channel")) {
				channel = cmd.getOptionValue("channel");
			}

			String owner = "";
			if (cmd.hasOption("owner")) {
				owner = cmd.getOptionValue("owner");
			}

			JSONObject shaRes = new JSONObject();
			shaRes.put("name", name);
			shaRes.put("tags", tags);
			shaRes.put("description", des);
			shaRes.put("uri", uri);
			shaRes.put("channel", channel);
			shaRes.put("owner", owner);
			shaRes.put("ezserver", null);
			shaRes.put("secure", true);

			raw.put("resource", shaRes);
			return raw;
		}

		if (cmd.hasOption("query")) {
			raw.put("command", "QUERY");

			boolean relay = false;
			if (cmd.hasOption("relay")) {
				relay = Boolean.parseBoolean(cmd.getOptionValue("relay"));
			}
			raw.put("relay", relay);

			String name = "";
			if (cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}

			ArrayList<String> tags=new ArrayList<String>();
			if (cmd.hasOption("tags")) {
				String[] str=cmd.getOptionValue("tags").split(",");
				for(int i=0;i<str.length;i++){
					tags.add(str[i]);
				}
				//tags = cmd.getOptionValue("tags");
			}
			String des = "";
			if (cmd.hasOption("description")) {
				des = cmd.getOptionValue("description");
			}

			String uri = "";
			if (cmd.hasOption("uri")) {
				uri = cmd.getOptionValue("uri");
			}

			String channel = "";
			if (cmd.hasOption("channel")) {
				channel = cmd.getOptionValue("channel");
			}

			String owner = "";
			if (cmd.hasOption("owner")) {
				owner = cmd.getOptionValue("owner");
			}

			JSONObject queRes = new JSONObject();

			queRes.put("name", name);
			queRes.put("tags", tags);
			queRes.put("description", des);
			queRes.put("uri", uri);
			queRes.put("channel", channel);
			queRes.put("owner", owner);
			queRes.put("ezserver", null);
			queRes.put("secure", true);

			raw.put("resourceTemplate", queRes);
			return raw;
		}
		if (cmd.hasOption("subscribe")) {
			raw.put("command", "SUBSCRIBE");

			boolean relay = false;
			if (cmd.hasOption("relay")) {
				relay = Boolean.parseBoolean(cmd.getOptionValue("relay"));
			}
			raw.put("relay", relay);
			Random rand = new Random();
			raw.put("id", getRandomString(rand.nextInt(5)+5));

			String name = "";
			if (cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}

			ArrayList<String> tags=new ArrayList<String>();
			if (cmd.hasOption("tags")) {
				String[] str=cmd.getOptionValue("tags").split(",");
				for(int i=0;i<str.length;i++){
					tags.add(str[i]);
				}
				//tags = cmd.getOptionValue("tags");
			}

			String des = "";
			if (cmd.hasOption("description")) {
				des = cmd.getOptionValue("description");
			}

			String uri = "";
			if (cmd.hasOption("uri")) {
				uri = cmd.getOptionValue("uri");
			}

			String channel = "";
			if (cmd.hasOption("channel")) {
				channel = cmd.getOptionValue("channel");
			}

			String owner = "";
			if (cmd.hasOption("owner")) {
				owner = cmd.getOptionValue("owner");
			}

			JSONObject queRes = new JSONObject();

			queRes.put("name", name);
			queRes.put("tags", tags);
			queRes.put("description", des);
			queRes.put("uri", uri);
			queRes.put("channel", channel);
			queRes.put("owner", owner);
			queRes.put("ezserver", null);
			queRes.put("secure", true);

			raw.put("resourceTemplate", queRes);
			return raw;
		}
		
		if (cmd.hasOption("fetch")) {
			raw.put("command", "FETCH");

			String name = "";
			if (cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}

			ArrayList<String> tags=new ArrayList<String>();
			if (cmd.hasOption("tags")) {
				String[] str=cmd.getOptionValue("tags").split(",");
				for(int i=0;i<str.length;i++){
					tags.add(str[i]);
				}
				//tags = cmd.getOptionValue("tags");
			}

			String des = "";
			if (cmd.hasOption("description")) {
				des = cmd.getOptionValue("description");
			}

			String uri = "";
			if (cmd.hasOption("uri")) {
				uri = cmd.getOptionValue("uri");
			}

			String channel = "";
			if (cmd.hasOption("channel")) {
				channel = cmd.getOptionValue("channel");
			}

			String owner = "";
			if (cmd.hasOption("owner")) {
				owner = cmd.getOptionValue("owner");
			}

			JSONObject fetRes = new JSONObject();
			fetRes.put("name", name);
			fetRes.put("tags", tags);
			fetRes.put("description", des);
			fetRes.put("uri", uri);
			fetRes.put("channel", channel);
			fetRes.put("owner",owner);
			fetRes.put("ezserver", null);
			fetRes.put("secure", true);
			
			raw.put("resourceTemplate", fetRes);
			return raw;
		}

		if (cmd.hasOption("exchange")) {
			raw.put("command", "EXCHANGE");
			raw.put("secure", true);

			String serverList = "";
			if (cmd.hasOption("servers")) {
				serverList = cmd.getOptionValue("servers");
			}

			raw.put("serverList", serverList);
			return raw;
		}
		

			boolean relay = false;
			if (cmd.hasOption("relay")) {
				relay = Boolean.parseBoolean(cmd.getOptionValue("relay"));
			}
			raw.put("relay", relay);

			String name = "";
			if (cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}

			ArrayList<String> tags=new ArrayList<String>();
			if (cmd.hasOption("tags")) {
				String[] str=cmd.getOptionValue("tags").split(",");
				for(int i=0;i<str.length;i++){
					tags.add(str[i]);
				}
				//tags = cmd.getOptionValue("tags");
			}
			String des = "";
			if (cmd.hasOption("description")) {
				des = cmd.getOptionValue("description");
			}

			String uri = "";
			if (cmd.hasOption("uri")) {
				uri = cmd.getOptionValue("uri");
			}

			String channel = "";
			if (cmd.hasOption("channel")) {
				channel = cmd.getOptionValue("channel");
			}

			String owner = "";
			if (cmd.hasOption("owner")) {
				owner = cmd.getOptionValue("owner");
			}

			JSONObject queRes = new JSONObject();

			queRes.put("name", name);
			queRes.put("tags", tags);
			queRes.put("description", des);
			queRes.put("uri", uri);
			queRes.put("channel", channel);
			queRes.put("owner", owner);
			queRes.put("ezserver", null);
			queRes.put("secure", true);

			raw.put("resourceTemplate", queRes);
			return raw;
		
	

	
	}
	
	@SuppressWarnings("unchecked")
	static JSONObject socketFill(CommandLine cmd){
		JSONObject raw = new JSONObject();
		if (cmd.hasOption("publish")) {
			raw.put("command", "PUBLISH");
			String name = "";
			if (cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}
			
			ArrayList<String> tags=new ArrayList<String>();
			if (cmd.hasOption("tags")) {
				String[] str=cmd.getOptionValue("tags").split(",");
				for(int i=0;i<str.length;i++){
					tags.add(str[i]);
				}
				//tags = cmd.getOptionValue("tags");
			}
			
			String des = "";
			if (cmd.hasOption("des")) {
				des = cmd.getOptionValue("description");
			}
			String uri = "";
			if (cmd.hasOption("uri")) {
				uri = cmd.getOptionValue("uri");
			}
			String channel = "";
			if (cmd.hasOption("channel")) {
				channel = cmd.getOptionValue("channel");
			}
			String owner = "";
			if (cmd.hasOption("owner")) {
				owner = cmd.getOptionValue("owner");
			}
			JSONObject pubRes = new JSONObject();
			pubRes.put("name", name);
			pubRes.put("tags", tags);
			pubRes.put("description", des);
			pubRes.put("uri", uri);
			pubRes.put("channel", channel);
			pubRes.put("owner", owner);
			pubRes.put("ezserver", null);
			pubRes.put("secure", false);
			
			raw.put("resource", pubRes);
			return raw;
		}

		if (cmd.hasOption("remove")) {
			raw.put("command", "REMOVE");

			String name = "";
			if (cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}

			ArrayList<String> tags=new ArrayList<String>();
			if (cmd.hasOption("tags")) {
				String[] str=cmd.getOptionValue("tags").split(",");
				for(int i=0;i<str.length;i++){
					tags.add(str[i]);
				}
				//tags = cmd.getOptionValue("tags");
			} 

			String des = "";
			if (cmd.hasOption("description")) {
				des = cmd.getOptionValue("description");
			}

			String uri = "";
			if (cmd.hasOption("uri")) {
				uri = cmd.getOptionValue("uri");
			}

			String channel = "";
			if (cmd.hasOption("channel")) {
				channel = cmd.getOptionValue("channel");
			}

			String owner = "";
			if (cmd.hasOption("owner")) {
				owner = cmd.getOptionValue("owner");
			}
			JSONObject remRes = new JSONObject();
			remRes.put("name", name);
			remRes.put("tags", tags);
			remRes.put("description", des);
			remRes.put("uri", uri);
			remRes.put("channel", channel);
			remRes.put("owner", owner);
			remRes.put("ezserver", null);
			remRes.put("secure", false);

			raw.put("resource", remRes);
			return raw;
		}

		if (cmd.hasOption("share")) {
			raw.put("command", "SHARE");

			String secret = "";
			if (cmd.hasOption("secret")) {
				secret = cmd.getOptionValue("secret");
			}
			raw.put("secret", secret);

			String name = "";
			if (cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}

			ArrayList<String> tags=new ArrayList<String>();
			if (cmd.hasOption("tags")) {
				String[] str=cmd.getOptionValue("tags").split(",");
				for(int i=0;i<str.length;i++){
					tags.add(str[i]);
				}
				//tags = cmd.getOptionValue("tags");
			}

			String des = "";
			if (cmd.hasOption("description")) {
				des = cmd.getOptionValue("description");
			}

			String uri = "";
			if (cmd.hasOption("uri")) {
				uri = cmd.getOptionValue("uri");
			}

			String channel = "";
			if (cmd.hasOption("channel")) {
				channel = cmd.getOptionValue("channel");
			}

			String owner = "";
			if (cmd.hasOption("owner")) {
				owner = cmd.getOptionValue("owner");
			}

			JSONObject shaRes = new JSONObject();
			shaRes.put("name", name);
			shaRes.put("tags", tags);
			shaRes.put("description", des);
			shaRes.put("uri", uri);
			shaRes.put("channel", channel);
			shaRes.put("owner", owner);
			shaRes.put("ezserver", null);
			shaRes.put("secure", false);

			raw.put("resource", shaRes);
			return raw;
		}

		if (cmd.hasOption("query")) {
			raw.put("command", "QUERY");

			boolean relay = false;
			if (cmd.hasOption("relay")) {
				relay = Boolean.parseBoolean(cmd.getOptionValue("relay"));
			}
			raw.put("relay", relay);

			String name = "";
			if (cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}

			ArrayList<String> tags=new ArrayList<String>();
			if (cmd.hasOption("tags")) {
				String[] str=cmd.getOptionValue("tags").split(",");
				for(int i=0;i<str.length;i++){
					tags.add(str[i]);
				}
				//tags = cmd.getOptionValue("tags");
			}
			String des = "";
			if (cmd.hasOption("description")) {
				des = cmd.getOptionValue("description");
			}

			String uri = "";
			if (cmd.hasOption("uri")) {
				uri = cmd.getOptionValue("uri");
			}

			String channel = "";
			if (cmd.hasOption("channel")) {
				channel = cmd.getOptionValue("channel");
			}

			String owner = "";
			if (cmd.hasOption("owner")) {
				owner = cmd.getOptionValue("owner");
			}

			JSONObject queRes = new JSONObject();

			queRes.put("name", name);
			queRes.put("tags", tags);
			queRes.put("description", des);
			queRes.put("uri", uri);
			queRes.put("channel", channel);
			queRes.put("owner", owner);
			queRes.put("ezserver", null);
			queRes.put("secure", false);

			raw.put("resourceTemplate", queRes);
			return raw;
		}
		if (cmd.hasOption("subscribe")) {
			raw.put("command", "SUBSCRIBE");

			boolean relay = false;
			if (cmd.hasOption("relay")) {
				relay = Boolean.parseBoolean(cmd.getOptionValue("relay"));
			}
			raw.put("relay", relay);
			Random rand = new Random();
			raw.put("id", getRandomString(rand.nextInt(5)+5));

			String name = "";
			if (cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}

			ArrayList<String> tags=new ArrayList<String>();
			if (cmd.hasOption("tags")) {
				String[] str=cmd.getOptionValue("tags").split(",");
				for(int i=0;i<str.length;i++){
					tags.add(str[i]);
				}
				//tags = cmd.getOptionValue("tags");
			}

			String des = "";
			if (cmd.hasOption("description")) {
				des = cmd.getOptionValue("description");
			}

			String uri = "";
			if (cmd.hasOption("uri")) {
				uri = cmd.getOptionValue("uri");
			}

			String channel = "";
			if (cmd.hasOption("channel")) {
				channel = cmd.getOptionValue("channel");
			}

			String owner = "";
			if (cmd.hasOption("owner")) {
				owner = cmd.getOptionValue("owner");
			}

			JSONObject queRes = new JSONObject();

			queRes.put("name", name);
			queRes.put("tags", tags);
			queRes.put("description", des);
			queRes.put("uri", uri);
			queRes.put("channel", channel);
			queRes.put("owner", owner);
			queRes.put("ezserver", null);
			queRes.put("secure", false);

			raw.put("resourceTemplate", queRes);
			return raw;
		}
		
		if (cmd.hasOption("fetch")) {
			raw.put("command", "FETCH");

			String name = "";
			if (cmd.hasOption("name")) {
				name = cmd.getOptionValue("name");
			}

			ArrayList<String> tags=new ArrayList<String>();
			if (cmd.hasOption("tags")) {
				String[] str=cmd.getOptionValue("tags").split(",");
				for(int i=0;i<str.length;i++){
					tags.add(str[i]);
				}
				//tags = cmd.getOptionValue("tags");
			}

			String des = "";
			if (cmd.hasOption("description")) {
				des = cmd.getOptionValue("description");
			}

			String uri = "";
			if (cmd.hasOption("uri")) {
				uri = cmd.getOptionValue("uri");
			}

			String channel = "";
			if (cmd.hasOption("channel")) {
				channel = cmd.getOptionValue("channel");
			}

			String owner = "";
			if (cmd.hasOption("owner")) {
				owner = cmd.getOptionValue("owner");
			}

			JSONObject fetRes = new JSONObject();
			fetRes.put("name", name);
			fetRes.put("tags", tags);
			fetRes.put("description", des);
			fetRes.put("uri", uri);
			fetRes.put("channel", channel);
			fetRes.put("owner",owner);
			fetRes.put("ezserver", null);
			fetRes.put("secure", false);

			raw.put("resourceTemplate", fetRes);
			return raw;
		}

		if (cmd.hasOption("exchange")) {
			raw.put("command", "EXCHANGE");
			raw.put("secure", false);

			String serverList = "";
			if (cmd.hasOption("servers")) {
				serverList = cmd.getOptionValue("servers");
			}

			raw.put("serverList", serverList);
			return raw;
		}

		boolean relay = false;
		if (cmd.hasOption("relay")) {
			relay = Boolean.parseBoolean(cmd.getOptionValue("relay"));
		}
		raw.put("relay", relay);

		String name = "";
		if (cmd.hasOption("name")) {
			name = cmd.getOptionValue("name");
		}

		ArrayList<String> tags=new ArrayList<String>();
		if (cmd.hasOption("tags")) {
			String[] str=cmd.getOptionValue("tags").split(",");
			for(int i=0;i<str.length;i++){
				tags.add(str[i]);
			}
			//tags = cmd.getOptionValue("tags");
		}
		String des = "";
		if (cmd.hasOption("description")) {
			des = cmd.getOptionValue("description");
		}

		String uri = "";
		if (cmd.hasOption("uri")) {
			uri = cmd.getOptionValue("uri");
		}

		String channel = "";
		if (cmd.hasOption("channel")) {
			channel = cmd.getOptionValue("channel");
		}

		String owner = "";
		if (cmd.hasOption("owner")) {
			owner = cmd.getOptionValue("owner");
		}

		JSONObject queRes = new JSONObject();

		queRes.put("name", name);
		queRes.put("tags", tags);
		queRes.put("description", des);
		queRes.put("uri", uri);
		queRes.put("channel", channel);
		queRes.put("owner", owner);
		queRes.put("ezserver", null);
		queRes.put("secure", true);

		raw.put("resourceTemplate", queRes);
		return raw;
	

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
	
	@SuppressWarnings("unchecked")
	private static void listenToEnter(DataOutputStream output, String id) throws IOException {
		// TODO Auto-generated method stub
		while(true){
		 if (new InputStreamReader(System.in).read() == 13)
		  {
			 JSONObject command=new JSONObject();
			 command.put("command", "UNSUBSCRIBE");
			 command.put("id", id);
			 output.writeUTF(command.toJSONString());
		//     System.out.println("chenggong!!!");
		     break;
		  }
		}

	}
	
}
