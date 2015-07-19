
import com.couchbase.client.CouchbaseClient;
import com.google.gson.Gson;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
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
public class StoreExample {
 
    
final String BUCKET = "test1";    
final String BUCKETPW = "test1";    

    
public StoreExample(){
    try {
        List<URI> nodes = Arrays.asList(new URI("http://127.0.0.1:8091/pools"));
        CouchbaseClient client = new CouchbaseClient(nodes, BUCKET, BUCKETPW);
        Logger.getLogger(StoreExample.class.getName()).log(Level.INFO, "Ready");
        
        
        Gson gson = new Gson();
        Rant rant = new Rant(
                UUID.randomUUID(),
                "JerryS",
                "Why do they call it Ovaltine? The mug is round. The jar is round. They shouldb call it Roundtine.",
                "rant");
        //gson.
        String jsonRant = gson.toJson(rant);
        String key = rant.getId().toString();
        Logger.getLogger(StoreExample.class.getName()).log(Level.INFO, "Writing key "+key+" doc "+jsonRant);  
        
        
       // Example to get result I.
       if(!client.set(key, jsonRant).get().booleanValue()){        
       Logger.getLogger(StoreExample.class.getName()).log(Level.SEVERE, "SET failed (1)");  
       }
        
       // Example to get result II.
       OperationFuture<Boolean> setResult = client.set(key, jsonRant);
        if(!setResult.get().booleanValue()){        
        Logger.getLogger(StoreExample.class.getName()).log(Level.SEVERE, "SET failed (2)");  
        }
        
        
        
        String getJson = (String)client.get(key);        
        Logger.getLogger(StoreExample.class.getName()).log(Level.INFO, "=> "+getJson);  
       
        //OperationFuture<Boolean> delete = client.delete("key");
        
        
    } catch (Exception ex) {
        Logger.getLogger(StoreExample.class.getName()).log(Level.SEVERE, null, ex);
    }
}    
public static void main(String[] args){
  new StoreExample();  
}   
}
