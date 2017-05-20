package EZShare;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;

class Resource {
	private String owner="";
	private String channel="";
	private String uri="";
	private String name="";
	private String description="";
	private String[] tags=null;
	private String ezserver;
	private static ArrayList<Resource> resourceList = new ArrayList<Resource>();
	
	public Resource(String name, String description, String[] tags, String uri, String channel, String owner, String ezserver){
		this.name=checker(name);
		this.description=checker(description);
		int num=tags.length;
		this.tags=new String[num];
		this.tags=tags;
		this.uri=checker(uri);
		this.channel=checker(channel);
		this.owner=checker(owner);
		this.ezserver=checker(ezserver);	
	}
	
	public Resource(Resource obj){
		this.name=obj.get("name");
		this.description=obj.get("description");
		this.tags=obj.getTags();
		this.uri=obj.get("uri");
		this.channel=obj.get("channel");
		if(obj.get("owner").equals("")){
		this.owner=obj.get("owner");}
		else this.owner="*";
		this.ezserver=Server.advertisedHostName+":"+Server.port;
	}
	
	JSONObject toJSON(){
    	JSONObject obj=new JSONObject();
    	obj.put("name", this.get("name"));      	
    	ArrayList<String> tagArray=new ArrayList<String>();
    	if(!(this.getTags()==null)){
    		for(int i=0;i<this.getTags().length;i++){
    			tagArray.add(this.getTags()[i]);
    		}
    	}
    	obj.put("tags", tagArray);
    	obj.put("description", this.get("description"));
    	obj.put("uri", this.get("uri"));
    	obj.put("channel", this.get("channel"));
    	obj.put("owner", this.get("owner"));
    	obj.put("ezserver", Server.advertisedHostName+":"+Server.port);
    	return obj;
	}
	
	public Resource(JSONObject json) {
		this.name = checker((String) ((HashMap) json.get("resource")).get("name"));
		if(((HashMap) json.get("resource")).get("tags").equals("")) this.tags=null;
		else this.tags = (String[]) ((String) ((HashMap) json.get("resource")).get("tags")).split(",");
		this.description = checker((String) ((HashMap) json.get("resource")).get("description"));
		this.uri= checker((String) ((HashMap) json.get("resource")).get("uri"));
		this.channel = checker((String) ((HashMap) json.get("resource")).get("channel"));
		this.owner = checker((String) ((HashMap) json.get("resource")).get("owner"));
		this.ezserver=Server.advertisedHostName+":"+Server.port;		
	}
	
	public static void createResource(JSONObject command) {
		resourceList.add(new Resource(command));
	}
	
	private static String checker(String input){
		String b=input.replaceAll("\\s*", "");
		 b=b.replace("\0", "");
		return b;	
	}
	
	public static void main(String[] args) {
		System.out.println(checker("   abcd  "));	
	}
		
	String get(String str){
		switch(str){
			case "owner": return owner;
			case "channel": return channel;
			case "uri":return uri;
			case "name":return name;
			case "description":return description;
			case "ezserver": return ezserver; 
			default: return null;			
		}		
	}
	
	String[] getTags(){
		return this.tags;
	}
	
	

}
