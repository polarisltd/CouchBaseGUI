import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.ObservedException;
import com.couchbase.client.ObservedModifiedException;
import com.couchbase.client.ObservedTimeoutException;
import com.couchbase.client.internal.HttpFuture;
import com.couchbase.client.protocol.views.DesignDocument;
import com.couchbase.client.protocol.views.Paginator;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewDesign;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Observer;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;

import net.spy.memcached.PersistTo;
import net.spy.memcached.ReplicateTo;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.OperationStatus;

import java.util.Observer;

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
 
final int SCROLL_SIZE = 1024;
String cbURL;
String cbBucket;
Paginator scroll;


private CouchbaseClient client;    
Logger logger = java.util.logging.Logger.getLogger(this.getClass().getName());	
    
public CBDocUtil(String cbUrl,String bucket,String bucketPW){
    try {
    	// "http://127.0.0.1:8091/pools"
    	logger.log(Level.SEVERE, "About to start");
        List<URI> nodes = Arrays.asList(new URI(cbUrl));
        client = new CouchbaseClient(nodes, bucket, bucketPW);
    	logger.log(Level.SEVERE, "cbClient created");
    	this.cbURL = cbUrl;
    	this.cbBucket = bucket;
    } catch (Exception ex) {
        logger.log(Level.SEVERE, null, ex);
    }
}    
    

public String getBucketName(){
	return this.cbBucket;
}
public String getServerUrl(){
	return this.cbURL;
}


public File download(String docKey){    
	Path out = Paths.get(docKey+".cb");        
	try {        

		logger.log(Level.SEVERE, "Output file: "+out.toFile().getAbsolutePath());  

		Object cbDoc = client.get(docKey);   
		//logger.log(Level.INFO, "document "+docKey+" is "+cbDoc.getClass().getName());  

		String json;

		if(cbDoc.getClass().getName().contains("[B")){  // bytearray

			json = new String((byte[])cbDoc);
			//Files.write(out, (byte[])cbDoc);

		}else{   // otherwise it is string     

			json = (String) cbDoc;

			//Files.write(out, ((String) cbDoc).getBytes());


		}


		json = prettyPrint(json); // no need to catch exception as this function do instead!
		logger.log(Level.INFO, "Prettyprinted "+docKey+" = "+json);

		Files.write(out, json.getBytes());




	} catch (Exception ex) {
		logger.log(Level.SEVERE, ex.getMessage(), ex);
	}
	return out.toFile(); 
} 


public void downloadDir(Enumeration keys, String outputDir) throws Exception{    	//Path out = Paths.get(docKey+".cb");        
		
	while(keys.hasMoreElements()){

		String docKey = (String) keys.nextElement();

		String filename = docKey+".cb";
		filename = filename.replaceAll("\\*", "x");
		if(!isFilenameValid(filename)){
			logger.log(Level.SEVERE, "File skipped as filename is not valid "+filename);  	
		}else{
			Path out = Paths.get(outputDir,filename);			

			String json;
			Object cbDoc = client.get(docKey);   
			if(cbDoc.getClass().getName().contains("[B")){  // bytearray
				json = new String((byte[])cbDoc);
			}else{   // otherwise it is string     
				json = (String) cbDoc;
			}	  		  
			json = prettyPrint(json);  // no need to catch exception as this function do instead!
	  		logger.log(Level.INFO, "Prettyprinted "+docKey+" = "+json);
			Files.write(out, json.getBytes());
			// logger.log(Level.INFO, "File written "+out.toFile().getAbsolutePath());  
		}
		    
	   }
	
}	
		
		
		
		
		
		
/////////////////////


public String QueryView(String desgnDoc,String vWname,String key) throws Exception{    
	  StringBuffer sb = new StringBuffer(); 
	  Query query = new Query(); 
	  query.setReduce(false); 
	  query.setIncludeDocs(true); 
	  query.setKey(key);
	  //Logger.getLogger(QueryViewAndDelete.class.getName()).log(Level.INFO, "About to query DesignDoc:View"+desgnDoc+":"+vWname);
	  View view = client.getView(desgnDoc, vWname); 
	  
	  HttpFuture<ViewResponse> future = client.asyncQuery(view, query); 
	  ViewResponse response = future.get(); 
	  if (!future.getStatus().isSuccess()) { 
	    Logger.getLogger(QueryViewAndDelete.class.getName()).log(Level.SEVERE, "Query failed!");
	    throw new Exception("Query Failed");
	  } 

	  Iterator<ViewRow> itr = response.iterator(); 
	  int cnt = 0;
	  while (itr.hasNext()) { 
		cnt++;
	    ViewRow row = itr.next(); 
	    //String id = row.getId();
	    String viewDocument = row.getValue();
	    viewDocument = prettyPrint(viewDocument); // no need to catch exception as this function do instead!
	    sb.append(viewDocument);
	    //Object o = row.getDocument(); // row.getDocument() retrieves complete document so thats not a view part but get operation on data executed.
	  }  
	  logger.log(Level.INFO, "Query response received! items iterated "+cnt);
  
	  return sb.toString();
	   
	}







// return all views found in design document


public String[] listDesgnDocViews(String desgnDocName){
//	client.
DesignDocument dc = client.getDesignDoc(desgnDocName);

List<ViewDesign> views = (List<ViewDesign>) dc.getViews();
Vector<String> items = new Vector<String>();
for (ViewDesign view : views)
{
	items.add(view.getName());
  // process view data
}
logger.log(Level.INFO, "views : "+items+" cls "+items.toArray().getClass().getName());  
//return (String[])items.toArray();
return items.toArray(new String[items.size()]);
}


public String getDocument(String docKey){  
	String json=null;
    try {        

    	
        Object cbDoc = client.get(docKey);   
    	logger.log(Level.INFO, "document "+docKey+" is "+cbDoc.getClass().getName());  


    	
        if(cbDoc.getClass().getName().contains("[B")){  // bytearray
        
        	json = new String((byte[])cbDoc);
            //Files.write(out, (byte[])cbDoc);
        	
        }else{   // otherwise it is string     
        
        	json = (String) cbDoc;
        	
        	//Files.write(out, ((String) cbDoc).getBytes());
        	
        	
        }
       
        
        json = prettyPrint(json); // no need to catch exception as this function do instead!
        logger.log(Level.INFO, "prettyprinted json :"+docKey+" = "+json);
       
        
 
    } catch (Exception ex) {
        logger.log(Level.SEVERE, null, ex);
    }
    return json; 
} 


public void uploadDir(String directory){
	String filePattern = ".cb";
	List<File> files = fileList(directory,filePattern);
	logger.log(Level.SEVERE, "Directory listing "+files);
	for(File file: files){
		upload(file);
		
	}
	
}


String prettyPrint(String uglyJSONString){
String prettyJsonString = uglyJSONString;	
try{
  Gson gson = new GsonBuilder().setPrettyPrinting().create();
  JsonParser jp = new JsonParser();
  JsonElement je = jp.parse(uglyJSONString);
  prettyJsonString = gson.toJson(je);
}catch(Exception e1){
  logger.log(java.util.logging.Level.INFO, 
         "Excepton  {0} {1} ", new Object[]{e1.getMessage(), getPrintStackTrace(e1)});

}




return prettyJsonString;

}

public void upload(File dossierFile){
        String filename = dossierFile.getName();
		String key=filename.substring(0,filename.indexOf(".cb"));
		try{	
		   logger.log(Level.INFO, "Reading and storing file into couchbase w persist/replic options file:key "+dossierFile.getAbsolutePath()+":"+key);
		   Path in = Paths.get(dossierFile.getAbsolutePath());
		   byte[] doc=Files.readAllBytes(in) ;


	       // Example to get result II.
	       OperationFuture<Boolean> setResult = client.set(key, 0, doc, PersistTo.MASTER,ReplicateTo.ONE);
	       Boolean lr = !setResult.get();
	       if(!lr){ 
	          logger.log(Level.SEVERE, "Doc update failed (get()=="+lr+") key:"+key);  
	          logger.log(Level.SEVERE, "setResult.getStatus().getMessage() : "+setResult.getStatus().getMessage());
	       }
		
		} catch (Exception ex) {
	          logger.log(Level.SEVERE, "Exception! ", ex);
	    }
		

	
}



public void upload(String key, String doc){
//	try{	
//	   logger.log(Level.INFO, "Storing document into couchbase w persist/replic options "+key);

       // Example to get result II.
/* 
* 
* http://docs.couchbase.com/couchbase-sdk-java-1.1/
* 
* 
Persistence and Replication¶

By default, the OperationFutures return when Couchbase Server has accepted the command and stored it in memory (disk persistence and replication is handled asynchronously by the cluster). That’s one of the reason why it’s so fast. 
For most use-cases, that’s the behavior that you need. 
Sometimes though, you want to trade in performance for data-safety and wait until the document has been saved to disk and/or replicated to other hosts.

The Java SDK provides overloaded commands for all necessary operations (that is set(), add(), replace(), cas() and delete() ). 
They all accept a combindation of PersistTo and ReplicateTo enums, 
which define on how many other servers you want it to persist/replicate. 
Here is an example on how to make sure that the document has been persisted on its master node, 
but also replicated to at least one of its replicas.

OperationFuture<Boolean> oper = client.set(
  "important",
  0,
  "document",
  PersistTo.MASTER,
  ReplicateTo.ONE
);
Boolean success = oper.get();
if(success == false) {
  System.err.println(oper.getStatus().getMessage());
}	   
	   
	   
	   
When you are writing unit tests and when you absolutely need 100% accurate data sets in your views you not only need to query with Stale.FALSE, but also write the data with a persistence constraint of PersistTo.MASTER. This is because the indexer picks up the data from disk, and the only way to make sure it has received the write is to wait until it actually has been persisted to disk.

	   
	   
	   
OperationFuture<Boolean> setResult = cbc.set("foo", 0, "bar");

if (setResult.get()) {
  Observer watchit = new Observer(setResult.getStatus());
else {
  // do something because the set failed
}	   
	   
// now that the observer knows what to observe...
ObserveBoolean observeSuccess = watchit.blockForReplication(1); // this will block
if (!observeSuccess) {
  // handle whatever we want to do for this change
  switch (observeSuccess.getStatus()) {
    case OBS_TIMEDOUT:
      // do something for the timeout case
      break;
    case OBS_MODIFIED:
      // key was either modified by someone else or failover occurred
      break;
  }
}
	   
	   
	   */
	OperationFuture<Boolean> setResult = null;
	try{
       setResult = client.set(key, 0, doc.getBytes(), PersistTo.MASTER,ReplicateTo.THREE);
       Boolean lr = !setResult.get();
       if(!lr){ 
          logger.log(Level.SEVERE, "Doc update failed (get()=="+lr+") key:"+key);  
          logger.log(Level.SEVERE, "setResult.getStatus().getMessage() : "+setResult.getStatus().getMessage());
       }else{
    	   logger.log(Level.SEVERE, "Doc update OK");  
       }
    } catch (ObservedException e) {
    	logger.log(Level.SEVERE, "Doc update failed ObservedException "+key+""+e.getMessage()); 
	} catch (ObservedTimeoutException e) {
		logger.log(Level.SEVERE, "Doc update failed ObservedTimeoutException "+key+""+e.getMessage()); 
	} catch (ObservedModifiedException e) {
		logger.log(Level.SEVERE, "Doc update failed ObservedModifiedException "+key+""+e.getMessage()); 
	} catch (Exception e) {
		logger.log(Level.SEVERE, "Doc update failed Exception "+key+""+e.getMessage()); 	
	}       
       

}



public void stop(){
	try{
		if (client!=null)
        client.shutdown();
	}catch(Exception e){
		e.printStackTrace();
	}
	
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



public void paginatedQueryCreate(String desgnDoc,String vWname, int scrollSize,DefaultListModel<String> viewResultModel) throws Exception{  
    viewResultModel.clear();  

	// Accessing	  a	  View	  in	  Couchbase	  Server	  2.0	
	  Query query = new Query(); 
	  query.setIncludeDocs(false); 
	  query.setDebug(true); 	  
	  query.setSkip(0);
	  query.setLimit(scrollSize);
	  //query.setStale( Stale.FALSE );
	 	  
	  logger.log(Level.INFO, "About to query DesignDoc:View "+desgnDoc+":"+vWname);
	  View view = client.getView(desgnDoc, vWname); 
      //query.setStale(Stale.OK);
      scroll = client.paginatedQuery(view, query, scrollSize);
}


public boolean paginatedQueryResult(DefaultListModel<String> viewResultModel) throws Exception{    

		  int docsFromScrollCnt = 0;
		  if(scroll==null || !scroll.hasNext()){
			  scroll = null;
			  logger.log(Level.INFO, "Query retrieval completed or query not active! ");
			  return true; 
		  }else{
		    ViewResponse response = scroll.next();
		    docsFromScrollCnt += response.size();
		    StringBuffer str = new StringBuffer();
			logger.log(Level.INFO, "Query page retrieved, size "+response.size()+" docs retrieved: "+docsFromScrollCnt);
			  Iterator<ViewRow> itr = response.iterator(); 
			  while (itr.hasNext()) { 
			    ViewRow row = itr.next(); 
			    String id = row.getId();
			    viewResultModel.addElement(id);
			    str.append(id+";");
			    // we said dont retrieve doc.
			    //String v = row.getValue();
			    //Object o = row.getDocument(); // row.getDocument() retrieves complete document so thats not a view part but get operation on data executed.
			  } 
			  logger.log(Level.INFO, str.toString());

			  return false;
			}		  

		  
		  
	  }
	  
	  
	  
	  
public boolean paginatedQueryResult2(DefaultListModel<String> viewResultModel) throws Exception{    

	  int docsFromScrollCnt = 0;
	  if(scroll==null || !scroll.hasNext()){
		  scroll = null;
		  logger.log(Level.INFO, "Query retrieval completed or query not active! ");
		  return true; 
	  }else{
		  
		    StringBuffer str = new StringBuffer();
  
			    ViewResponse response = scroll.next();
				logger.log(Level.INFO, "Query page retrieved, size "+response.size()+" docs retrieved: "+docsFromScrollCnt);
			    
			    for (ViewRow row : response) {
			    	String id = row.getId();
			        System.out.println(id);
				    viewResultModel.addElement(id);
				    str.append(id+";");
				    

			    }
					  
		  logger.log(Level.INFO, str.toString());

		  return false;
		  
	  }		  
		  
		  
		  
		  
	  
	  
}
	  
	  
	  




String getPrintStackTrace(Exception ex){	
	StringWriter w = new StringWriter();
	ex.printStackTrace(new PrintWriter(w));
	return w.toString();		
}

public static boolean isFilenameValid(String file) {
    File f = new File(file);
    try {
       f.getCanonicalPath();
       return true;
    }
    catch (IOException e) {
       return false;
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
