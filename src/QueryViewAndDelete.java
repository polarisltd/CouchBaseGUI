
import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.internal.HttpFuture;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.spy.memcached.internal.OperationFuture;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/*
Views:

to return id only use this:

function (doc, meta) {
  emit(meta.id, null);
}

To return complete document use this:

function (doc, meta) {
  emit(meta.id, null);
}

To return single field use this:

function (doc, meta) {
  emit(meta.id, doc.username);
}



View can return not only whole document but list of columns, check out this:

function (doc, meta) {
  emit(meta.id, [doc.username,doc.type]);
}

it prints out:
INFO: key:value:doc73045e89-f977-4180-9490-2a7a3e034b1c:["JerryS","rant"]:{"id":"73045e89-f977-4180-9490-2a7a3e034b1c","username":"JerryS","ranttext":"Why do they call it Ovaltine? The mug is round. The jar is round. They shouldb call it Roundtine.","type":"rant"}

Possible is filtering:

function (doc) {
if(doc.jsonType == "active_user"){
emit(doc._id, [doc.username, doc.first_name, doc.last_name, doc.last_login]);
}


function (doc, meta) {
  if(doc.type && doc.type== "order"){
  emit(meta.id, [doc.name,null);
  }
}

following is ideal to implement lookups.
as key is used 3 field combination and as document is returned kind of columns in SQL meaning


function (doc) {
  emit(doc.id+';'+doc.type+';'+doc.username,[doc.id,doc.username,doc.ranttext,doc.type]);
}




*/


/*


To access documents create view:

function (doc, meta) {
  if (doc.type == "brewery" && doc.country){
   emit(doc.country);
  }
}




http://nitschinger.at/New-Features-in-the-Couchbase-Java-Client-1-1-dp4
=======================================================================

 New Features in the Couchbase Java Client 1.1-dp4
This post shows off some of the new features shipped with the new Couchbase Java Client 1.1 release.
Introduction
The latest Java Developer Preview (dp4) is hot off the press, and therefore I thought it would be a good idea to show you how to use some of the brand-new features that are now available. This post will show you how to use the new ComplexKey class for view queries and also how to create and delete buckets directly from your SDK.

First, we added a very flexible way of providing parameters to view queries. The idea is that, instead of having to deal with JSON strings on your own, you can pass in Java objects and the appropriate JSON string will be created for you. While it may not sound like a big deal first, this also frees you from the worries about encoding it properly so that it can be transferred over HTTP. Before we dig into the inner workings, lets first start with a basic example on how to create a view query.

Every time you query a view, you need to create a Query and a View object. The View both object defines the name of the design document and the view, and the Query object allows you to control the params that you can supply with the query.

View myview = client.getView("designdoc", "viewname");
Instead of creating the View object directly, you use the getView() method on the client object. It is important to know that the SDK actually fetches view information from the server and will throw an exception if either the design document name or the view name are not found. An exception typically looks like this:

com.couchbase.client.protocol.views.InvalidViewException: Could not load view "viewname" for design doc "designdoc"
Now that we have our view created, let's instantiate a query object:

Query myquery = new Query();
If you don't specify anything else, the query object will work with the default settings. This means that no reduce function will be used and the full documents are not included. If you want to change these settings (or others), the Query instance provides setter methods for all of them. If you want to find out more, look at the SDK documentation or at the Couchbase Server manual. Basically, you can control everything that you could too while querying a view from the Couchbase UI. Here are some examples:

// Include the full documents as well
myquery.setIncludeDocs(true);
 
// Enable the reduce function
myquery.setReduce(true);
 
// Set the group level
myquery.setGroupLevel(2);
Note that it doesn't make sense to include the full documents and run the reduce phase, since you don't have document references left after the reduce phase. If you try to set both to true, the SDK complains with the following exception:

// query.setReduce(true); query.setIncludeDocs(true);
java.lang.UnsupportedOperationException: Reduced views don't contain documents
Also, if your view doesn't contain a reduce function and you set reduce to true, you'll see the following:

// query.setReduce(true); on a view with no reduce function defined.
java.lang.RuntimeException: This view doesn't contain a reduce function
Querying with ComplexKeys
Enough with exceptions, here comes the fun part: some query setter methods not only accept boolean or string arguments, they also allow instances of ComplexKey. ComplexKey takes care of translating your Java objects (or primitives) to their appropriate JSON representation. Here is a list of the query methods who support it:

setKey(ComplexKey): Return only documents that match the specified key.
setKeys(ComplexKey): Return only documents that match each of keys specified within the given array.
setRange(ComplexKey, ComplexKey): Returns records in the given key range.
setRangeStart(ComplexKey): Return records with a value equal to or greater than the specified key.
setRangeEnd(ComplexKey): Stop returning records when the specified key is reached.
An instance of a ComplexKey object is obtained through the static of factory method. When the query object is accessed during the view query, the toJSON() method is called on it and as a result the JSON string is generated. Here are some examples:

// JSON Result: 100
ComplexKey.of(100);
 
// JSON Result: "Hello"
ComplexKey.of("Hello");
 
// JSON Result: ["Hello", "World"]
ComplexKey.of("Hello", "World");
 
// JSON Result: [1349360771542,1]
ComplexKey.of(new Date().getTime(), 1);
This means that you don't have to deal with building the proper JSON strings for yourself (and make sure the escaping is correct). All methods except setRange() expect exactly one instance of ComplexKey. For example, if you emit keys with a unix timestamp and you want to fetch all records until now, you could do it like so:

Query query = new Query();
query.setRangeEnd(ComplexKey.of(new Date().getTime()));
When converted to a string to send it over the wire, it would look like this: ?endkey=1349361137738. This makes it also very easy to create a full range query:

long now = new Date().getTime();
long tomorrow = now + 86400;
Query query = new Query();
query.setRange(ComplexKey.of(now), ComplexKey.of(tomorrow));
This converts to ?startkey=1349362647742&endkey=1349362734142 on the wire.

Bucket management through the ClusterManager
Until now, you'd have to create and delete buckets either manually through the Couchbase UI or by using the REST API directly. With the addition of the ClusterManager, it is now possible to mange your buckets directly through the SDK, be it for testing purposes or to automatically create them when your code is deployed to a new environment. This can be especially useful if you want to automate bucket management in a staging or CI environment.

The management API is not accessible through CouchbaseClient, but through the ClusterManager class. This is because the CouchbaseClient is bound to a specific bucket and was not designed for management purposes like this. Instead, you create a new instance of the ClusterManager class and pass it a list of URIs and the admin user name and password.

List<URI> uris = new LinkedList<URI>();
uris.add(URI.create("http://127.0.0.1:8091/pools"));
ClusterManager manager = new ClusterManager(uris, "Administrator", "password");
 
// .. your code
 
manager.shutdown();
You can now create and delete buckets (of type Couchbase or Memcached). You can also list all available buckets in the Couchbase cluster or flush the bucket (if flush is enabled). Here are some examples:

// Create the default bucket with a 100MB memory limit and 0 replicas.
manager.createDefaultBucket(BucketType.COUCHBASE, 100, 0);
 
// Create a bucket with authentication.
manager.createSaslBucket(BucketType.COUCHBASE, "saslbucket", 100, 0, "password");
 
// Get a list of all buckets in the cluster.
List<String> buckets = manager.listBuckets();
 
// Delete a bucket
manager.deleteBucket("saslbucket");
 
// Check if a bucket named "mybucket" exists in the cluster:
List<String> buckets = manager.listBuckets();
assertTrue(buckets.contains("mybucket"));
 
// Flush the "default" bucket if flush is enabled
manager.flushBucket("default");
Note that creating, deleting and flushing may take some time (from milliseconds to seconds, depending on the cluster load). You can either wait a few seconds before you move on (with a Thread.sleep()), or you can wait until listBuckets() contains your created bucket. There is also a callback-approach (which the client library uses internally to minimize waiting times during tests), but this involves more code on the application side.

Wrapping up
This post showed you two brand-new features shipped with the 1.1-dp4 release.

While you can still use string-based keys to run range or key queries, the ComplexKey class hopefully provides a convenient addition to your tool belt. While also useful for primitives and single objects, it makes it much more convenient to create JSON arrays or objects since you don't have to deal with brackets and escaping yourself.

The ClusterManager gives you the flexibility to manage your buckets directly from the SDK to avoid separate HTTP calls or even manual interaction through the UI.

The full release notes are available here. As always, we're curious what you say about this and if you encounter bugs or know how to enhance it even more, feel free to comment below, create a ticket or post in the forums!

*/



/**
 *
 * @author robertsp
 * http://www.slideshare.net/Couchbase/developing-with-java-and-couchbase-server-slides
 */
public class QueryViewAndDelete {
	final String CB_NODE = "http://10.251.13.248:8091/pools";	
final String BUCKET = "mapping";    
final String BUCKETPW = "";    
    
public QueryViewAndDelete(String desgnDoc,String vWname){    
// Accessing	  a	  View	  in	  Couchbase	  Server	  2.0	
try {
  List<URI> nodes = Arrays.asList(new URI(CB_NODE));
  CouchbaseClient client = new CouchbaseClient(nodes, BUCKET, BUCKETPW);
  Logger.getLogger(QueryViewAndDelete.class.getName()).log(Level.INFO, "Client instantiated!");
    
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
    OperationFuture<Boolean> delete = null;
    //if(id.length()<=10){
       delete = client.delete(id);
       if(!delete.get().booleanValue()){        
           Logger.getLogger(QueryViewAndDelete.class.getName()).log(Level.SEVERE, "Couchbase delete failed !");  
         }else Logger.getLogger(QueryViewAndDelete.class.getName()).log(Level.INFO, "key "+id+"removed"); 
           //}
    //if (ITEMS.containsKey(row.getId())) { 
    //    assert ITEMS.get(row.getId()).equals(row.getDocument()); 
    //} 
  }  
  Logger.getLogger(QueryViewAndDelete.class.getName()).log(Level.INFO, "Iterator completed!");
  client.shutdown();

 //client.shutdown();
}catch (Exception ex) {
        Logger.getLogger(QueryViewAndDelete.class.getName()).log(Level.SEVERE, "Exception caught: "+ex.getMessage(), ex);
}
 
   
}


public static void main(String[] args){
  //String designDoc = args[0];
  //String view = args[1];
  
  new QueryViewAndDelete("dev_lisa1","listOutputBits");  
} 


}
