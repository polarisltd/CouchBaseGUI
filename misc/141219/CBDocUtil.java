import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.internal.HttpFuture;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.spy.memcached.internal.OperationFuture;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author robertsp
 */
public class CBDocUtil {
 
    
private CouchbaseClient client;    

    
public CBDocUtil(String cbUrl,String bucket,String bucketPW){
    try {
    	// "http://127.0.0.1:8091/pools"
    	Logger.getLogger(StoreExample.class.getName()).log(Level.SEVERE, "About to start");
        List<URI> nodes = Arrays.asList(new URI(cbUrl));
        client = new CouchbaseClient(nodes, bucket, bucketPW);
    	Logger.getLogger(StoreExample.class.getName()).log(Level.SEVERE, "cbClient created");
    } catch (Exception ex) {
        Logger.getLogger(StoreExample.class.getName()).log(Level.SEVERE, null, ex);
    }
}    
    
public File download(String docKey){    
	Path out = Paths.get(docKey+".cb");        
    try {        

    	Logger.getLogger(StoreExample.class.getName()).log(Level.SEVERE, "Output file: "+out.toFile().getAbsolutePath());  
    	
        Object cbDoc = client.get(docKey);   
    	Logger.getLogger(StoreExample.class.getName()).log(Level.INFO, "document "+docKey+" is "+cbDoc.getClass().getName());  

    	String json;
    	
        if(cbDoc.getClass().getName().contains("[B")){  // bytearray
        
        	json = new String((byte[])cbDoc);
            //Files.write(out, (byte[])cbDoc);
        	
        }else{   // otherwise it is string     
        
        	json = (String) cbDoc;
        	
        	//Files.write(out, ((String) cbDoc).getBytes());
        	
        	
        }
       
        
        Logger.getLogger(StoreExample.class.getName()).log(Level.INFO, "json to prettyprint :"+json);
        json = prettyPrint(json);
        Logger.getLogger(StoreExample.class.getName()).log(Level.INFO, "prettyprinted json :"+json);
       
        Files.write(out, json.getBytes());
        
        
        //String getJsonStr = (String)getJson;
        //Logger.getLogger(StoreExample.class.getName()).log(Level.INFO, "=> (String) "+getJsonStr);  
       
        //OperationFuture<Boolean> delete = client.delete("key");

 
    } catch (Exception ex) {
        Logger.getLogger(StoreExample.class.getName()).log(Level.SEVERE, null, ex);
    }
    return out.toFile(); 
} 


public void uploadDir(String directory){
	String filePattern = ".cb";
	List<File> files = fileList(directory,filePattern);
	Logger.getLogger(StoreExample.class.getName()).log(Level.SEVERE, "Directory listing "+files);
	for(File file: files){
		upload(file);
		
	}
	
}


String prettyPrint(String uglyJSONString){
Gson gson = new GsonBuilder().setPrettyPrinting().create();
JsonParser jp = new JsonParser();
JsonElement je = jp.parse(uglyJSONString);
String prettyJsonString = gson.toJson(je);
return prettyJsonString;
}

public void upload(File dossierFile){
        String filename = dossierFile.getName();
		String key=filename.substring(0,filename.indexOf(".cb"));
		try{	
		   Logger.getLogger(StoreExample.class.getName()).log(Level.INFO, "Reading and storing file into couchbase file:key "+dossierFile.getAbsolutePath()+":"+key);
		   Path in = Paths.get(dossierFile.getAbsolutePath());
		   byte[] doc=Files.readAllBytes(in) ;


	       // Example to get result II.
	       OperationFuture<Boolean> setResult = client.set(key, doc);
	       if(!setResult.get().booleanValue()){        
	          Logger.getLogger(StoreExample.class.getName()).log(Level.SEVERE, "Doc update failed "+key);  
	       }
		
		} catch (Exception ex) {
	          Logger.getLogger(StoreExample.class.getName()).log(Level.SEVERE, "Exception! ", ex);
	    }
		

	
}



public void upload(String key, String doc){
	try{	
	   Logger.getLogger(StoreExample.class.getName()).log(Level.INFO, "Storing document into couchbase "+key);

       // Example to get result II.
       OperationFuture<Boolean> setResult = client.set(key, doc.getBytes());
       if(!setResult.get().booleanValue()){        
          Logger.getLogger(StoreExample.class.getName()).log(Level.SEVERE, "Doc update failed "+key);  
       }
	
	} catch (Exception ex) {
          Logger.getLogger(StoreExample.class.getName()).log(Level.SEVERE, "Exception! ", ex);
    }
	


}



public void stop(){
    client.shutdown();
	
}





public static List<File> fileList(String directory,String filePattern) {
    List<File> fileNames = new ArrayList<>();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory))) {
        for (Path path : directoryStream) {
        	if (path.toString().contains(filePattern))
        	     // path.toString()
                 fileNames.add(path.toFile());
        }
    } catch (IOException ex) {}
    return fileNames;
}


public void queryView(String desgnDoc,String vWname){    
	// Accessing	  a	  View	  in	  Couchbase	  Server	  2.0	
	try {
	    
	  Query query = new Query(); 
	  query.setReduce(false); 
	  query.setIncludeDocs(true); 
	  Logger.getLogger(QueryViewAndDelete.class.getName()).log(Level.INFO, "About to query DesignDoc:View"+desgnDoc+":"+vWname);
	  View view = client.getView(desgnDoc, vWname); 
	  HttpFuture<ViewResponse> future = client.asyncQuery(view, query); 
	  ViewResponse response = future.get(); 
	  Logger.getLogger(QueryViewAndDelete.class.getName()).log(Level.INFO, "Query response received!");
	  if (!future.getStatus().isSuccess()) { 
	    Logger.getLogger(QueryViewAndDelete.class.getName()).log(Level.SEVERE, "Query failed!");
	    throw new Exception("Query Failed");
	  } 

	  Iterator<ViewRow> itr = response.iterator(); 
	  while (itr.hasNext()) { 
	    ViewRow row = itr.next(); 
	    String id = row.getId();
	    String v = row.getValue();
	    Object o = row.getDocument(); // row.getDocument() retrieves complete document so thats not a view part but get operation on data executed.
	        // LOOKS IT TRIES TO DESERIALIZE INTO OBJECT!!!!
	        // 2014-09-28 09:55:47.005 WARN net.spy.memcached.transcoders.SerializingTranscoder:  Caught CNFE decoding 92 bytes of data
	        //java.lang.ClassNotFoundException: Person
	        // if following is used query.setIncludeDocs(false); no above exception is thrown!
	    Logger.getLogger(QueryViewAndDelete.class.getName()).log(Level.INFO, "key:value:doc "+id+":"+v+":"+o);
	      
	  }  
	  Logger.getLogger(QueryViewAndDelete.class.getName()).log(Level.INFO, "Iterator completed!");

	}catch (Exception ex) {
	        Logger.getLogger(QueryViewAndDelete.class.getName()).log(Level.SEVERE, "Exception caught: "+ex.getMessage(), ex);
	}
	 
	   
	}




public static void main(String[] args){
	CBDocUtil util = new CBDocUtil("http://10.251.12.48:8091/pools","LFP_Dossiers",""); 
	util.download("0714000904");
	util.download("0012001057");
	util.uploadDir("G:\\java\\eclipsews\\CouchbaseUtilities");
	util.stop();
}   
}
