package silas;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.grailrtls.libworldmodel.client.ClientWorldConnection;
import org.grailrtls.libworldmodel.client.StepResponse;
import org.grailrtls.libworldmodel.client.Response;
import org.grailrtls.libworldmodel.client.WorldState;
import org.grailrtls.libworldmodel.client.protocol.messages.Attribute;
import org.grailrtls.libworldmodel.types.StringConverter;
import org.grailrtls.libworldmodel.types.IntegerConverter;
import org.grailrtls.libworldmodel.types.BooleanConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

public class PowerDriver {
	private static final Logger log = LoggerFactory
	.getLogger(PowerDriver.class);
	public static void main(String[] args) {
		
		
	    if (args.length < 2) {
	      printUsageInfo();
	      return;
	    }

	    
	    // Create a connection to the World Model as a client
	    final ClientWorldConnection wmc = new ClientWorldConnection();
	    log.info("Connecting to {}", wmc);
	    wmc.setHost(args[0]);
	    wmc.setPort(Integer.parseInt(args[1]));
	    log.debug("Connected.");

	    // Attempt to connect and exit if it fails
	    if (!wmc.connect()) {
	      log.error("Couldn't connect to the world model!  Check your connection parameters.");
	      return;
	    }
	    Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				log.info("Disconnecting from {}", wmc);
				wmc.disconnect();
			}
		});
		// Set up streaming request
	    long now = System.currentTimeMillis();
	    long oneSecond = 1000;
	    log.info("Requesting from {} every {} ms." , new Date(now), oneSecond);
	    StepResponse response = wmc.getStreamRequest(".*powerswitch.*", now, oneSecond,"on");		
	    WorldState state = null;
	    // Streaming request loop
	    while (!response.isComplete()) {
	      try {
	        state = response.next();
	        log.debug("Recieved response");
	      } catch(Exception e){
	        log.error("Error occured during request.", e);
	        
	        // Restart request if there is an error
	        now = System.currentTimeMillis();
	        response = wmc.getStreamRequest(".*powerswitch.*", now, oneSecond,"on");
	        log.info("Restarting request. Requesting from " + new Date(now) + " every " + oneSecond + " ms.");
	        state = null;
	        continue;
	      }
	      Collection<String> uris = state.getURIs();
	      for(String uri : uris) {
	          log.debug("URI: {}", uri);
	          Collection<Attribute> attribs = state.getState(uri);
	          for (Attribute att : attribs) {
	        	  boolean onoff=BooleanConverter.CONVERTER.decode(att.getData());
	        	  log.info("{} is {}", uri, onoff ? "on" : "off");
	        	  WebPowerSwitchIII(getStringAttribute(uri, wmc, "target"), getIntAttribute(uri, wmc, "outlet"), onoff, getStringAttribute(uri, wmc, "username"), getStringAttribute(uri, wmc, "password"));
	          }
	      }
	    }
	}
	public static void printUsageInfo() {
		StringBuffer sb = new StringBuffer(
				"Usage: <World Model Host> <World Model Port>" +
				"\n");
		System.err.println(sb.toString());
	}
	private static String getStringAttribute(String queryuri, ClientWorldConnection wmc, String attribute){
		// Query should be specific; this method only returns the first string found.
		try {
		      WorldState state1 = wmc.getSnapshot(queryuri, 0l, 0l, attribute).get();
		      Collection<String> uris1 = state1.getURIs();
		      // Gets the first URI
		      Iterator<String> iter = uris1.iterator();
		      String uri = iter.next();
		      // Gets the first matching attribute
		      Iterator<Attribute> attribs = state1.getState(uri).iterator();
		      String answer = StringConverter.CONVERTER.decode(attribs.next().getData());
		      //String answer =  (String)DataConverter.decodeUri("room", attribs.next().getData());
//		      String answer = new String()
		      log.info("Decoded {}: {}", attribute, answer);
		      return answer;
		    } catch (Exception e) {
		      log.error("Exception thrown while retrieving "+attribute+" from world model " ,e);
		    }
		return null;
	}
	private static int getIntAttribute(String queryuri, ClientWorldConnection wmc, String attribute){
		// Query should be specific; this method only returns the first string found.
		try {
		      WorldState state1 = wmc.getSnapshot(queryuri, 0l, 0l, attribute).get();
		      Collection<String> uris1 = state1.getURIs();
		      // Gets the first URI
		      Iterator<String> iter = uris1.iterator();
		      String uri = iter.next();
		      // Gets the first matching attribute
		      Iterator<Attribute> attribs = state1.getState(uri).iterator();
		      int answer =  IntegerConverter.CONVERTER.decode(attribs.next().getData());
		      log.info("Decoded answer: {}", answer);
		      return answer;
			} catch (Exception e) {
		      log.error("Exception thrown while retrieving "+attribute+" from world model: ", e);
		    }
		return -1;
	}
	private static void WebPowerSwitchIII(String target, int outletnum, boolean on, String username, String password){
		{
			int numOutlets = 8;
/*			if(dargs.length<3){
				System.err.println("Needs 5 arguments: deviceIP:port, outlet number, boolean for on/off, username, and password");
				return;
			}*/
			log.debug("Outlet number {}/{}", outletnum,numOutlets-1);
			if(outletnum>numOutlets||outletnum<0){
				log.warn("Invalid outlet: {}", outletnum);
				return;
			}
			// Establish an HTTP client for connections
			HttpClient httpclient = new DefaultHttpClient();
			// Define the GET request
			// Format: "http://192.168.200.34:7005/outlet?8=ON"
			if (!target.startsWith("http://"))
				target="http://"+target;
			if (!target.endsWith("/"))
				target = target+"/";
			target = target+"outlet?"+outletnum;
			if (on)
				target = target+"=ON";
			else
				target = target+"=OFF";
			log.info("Target URL: {}",target);
			HttpGet httpget = new HttpGet(target);
			// Add authentication to the GET request
			try {
				UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
				BasicScheme scheme = new BasicScheme();
				Header authorizationHeader = scheme.authenticate(credentials, httpget);
				httpget.addHeader(authorizationHeader); 
			} catch (AuthenticationException e) {
				e.printStackTrace();
				return;
			}
			
			try {
				// Send the request to the WebPowerSwitch, store the response
				HttpResponse response = httpclient.execute(httpget);
				
				// Examine the response status
				log.info("Status line: {}", response.getStatusLine());
			    
			    
			    // Print out all the headers in the response
			   
			    HeaderIterator it = response.headerIterator();
			    while (it.hasNext()) {
			        log.debug("Response header: " + it.next());
			    }
			    // Get hold of the response entity
			    HttpEntity entity = response.getEntity();
			 
				// If the response does not enclose an entity, there is no need
				// to worry about connection release
				if (entity != null) 
				{
					InputStream instream = entity.getContent();
				    try {
				        BufferedReader reader = new BufferedReader(
				        		new InputStreamReader(instream));
				        
				        // do something useful with the response content
				        
					    String str = null;
				        while ((str = reader.readLine()) != null) {
				        	log.info("Response line: " + str);
				        }
				    } catch (IOException ex) {
				        // In case of an IOException the connection will be released
				    	// back to the connection manager automatically
				        throw ex;
				    } catch (RuntimeException ex) {
				    	// In case of an unexpected exception you may want to abort
				    	// the HTTP request in order to shut down the underlying 
				    	// connection and release it back to the connection manager.
				        httpget.abort();
				        throw ex;
				    } finally {
				        // Closing the input stream will trigger connection release
				    	instream.close();
				    }
			 
				    // When HttpClient instance is no longer needed, 
				    // shut down the connection manager to ensure
				    // immediate deallocation of all system resources
				    httpclient.getConnectionManager().shutdown();        
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}