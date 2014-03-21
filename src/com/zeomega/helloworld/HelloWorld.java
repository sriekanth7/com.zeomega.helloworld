package com.zeomega.helloworld;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.RpcClient;

@Path("/v1")
public class HelloWorld {
	
	private Connection connection;
  	private Channel channel;
  	private String requestQueueName;
  	private String replyQueueName;
  	private QueueingConsumer consumer;
  	static Logger logger = Logger.getLogger(HelloWorld.class);
  	private static int RPC_TIMEOUT_IN_MS = 50000; // Defaulting to 10 seconds for now
  			
	@Context
	UriInfo uriInfo;
			
    public String getSchemeHostPort() {
    	String schemeHostPort = this.uriInfo.getBaseUri() + ",v1";
    	return schemeHostPort;
    }

    protected void createAndSendRequest(String queueName) throws IOException {
	    ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost("localhost");
	    connection = factory.newConnection();
	    channel = connection.createChannel();

	    replyQueueName = channel.queueDeclare().getQueue(); 
	    consumer = new QueueingConsumer(channel);
	    channel.basicConsume(replyQueueName, true, consumer);    	
    }
    
    protected Response obtainAndReturnGETResponse(JSONObject obj) throws Exception {
	    String response = "";
	    try {
	    	
			response = this.call(obj.toString());	
			JSONObject json_response = new JSONObject(response);
    		Integer status = new Integer(json_response.getString("response_code"));
    		String message = json_response.getString("response_message");
    		return Response.status(status).type("application/json").entity(message).build();
	    } catch (TimeoutException timeOutException) {
    	    JSONObject error_object = new JSONObject();
    	    error_object.put("exception_class", timeOutException.getClass());
    	    error_object.put("exception_message", "The Queue Consumer may not be up. Please verify if the workers are up. Session timed out after " + RPC_TIMEOUT_IN_MS/1000 + " seconds");
	    	logger.error(error_object);
    		return Response.status(500).type("application/json").entity(error_object).build();
		} catch (Exception generalException) {
    	    JSONObject error_object = new JSONObject();
    	    error_object.put("exception_class", generalException.getClass());
    	    error_object.put("exception_message", generalException.getMessage());
	    	logger.error(error_object);
    		return Response.status(500).type("application/json").entity(error_object).build();
		}
	    finally {
	    	logger.debug("Closing channel ");
	    	this.close();
	    }
    }

    protected Response obtainAndReturnPOSTResponse(JSONObject obj) throws Exception {
    	String response="";
    	Integer status = null;
    	URI location_uri = null;
    	String message = "";
    	
    	try {
    		response = this.call(obj.toString());
    		JSONObject json_response = new JSONObject(response);
    		status = new Integer(json_response.getString("response_code"));
    		message = json_response.getString("response_message");
    		Boolean has_location_header = json_response.has("location_header"); 
    		if (has_location_header == true ){ 
    			location_uri = new URI(json_response.getString("location_header"));
    		}
    		
    		if (status == 201) {    		
    			return Response.created(location_uri).entity(message).build();
    		} else {
    			return Response.status(status).entity(message).build();
    		}
    	}catch (TimeoutException timeOutException) {
    	    JSONObject error_object = new JSONObject();
    	    error_object.put("exception_class", timeOutException.getClass());
    	    error_object.put("exception_message", "The Queue Consumer may not be up. Please verify if the workers are up." + timeOutException.getMessage());
	    	logger.error(error_object);
    		return Response.status(500).type("application/json").entity(error_object).build();
		} catch (Exception generalException) {
    	    JSONObject error_object = new JSONObject();
    	    error_object.put("exception_class", generalException.getClass());
    	    error_object.put("exception_message", generalException.getMessage());
	    	logger.error(error_object);
    		return Response.status(500).type("application/json").entity(error_object).build();
		}
	    finally {
	    	logger.debug("Closing channel ");
	    	this.close();
	    }

    }

    public String call(String message) throws TimeoutException, IOException {     
        String response = null;
        String[] messages = new String[1];
        messages[0] = message;
        logger.debug("Sending message to : " + requestQueueName + " , message : " + message);
    	RpcClient rpc_client = new RpcClient(channel, "", requestQueueName,RPC_TIMEOUT_IN_MS);
    	response = rpc_client.stringCall(message);
    	rpc_client.close();
    	logger.info("Obtained response from " + requestQueueName + response);
        return response; 
      }
      
    public void close() throws Exception {
        connection.close();
        logger.debug("Closed the rabbit connection");
      }
     
    /* ******************** Start Of My Helloworld Method *********************** */
    
    @GET @Path("helloworld/{message}")
    @Produces("application/json")
	public Response getHelloworldMessage(@PathParam("message") final String message) throws Exception {
    	Response response;
    	requestQueueName = "helloworld_get_56";
    	logger.info("Processing request for " + requestQueueName);
    	this.createAndSendRequest(requestQueueName);
   
	    JSONObject obj = new JSONObject();
	    obj.put("message", message);
	    JSONObject meta_information = new JSONObject();
	    meta_information.put("url", this.getSchemeHostPort());
	    meta_information.put("request", "GET");
	    meta_information.put("produces", "json");
	    meta_information.put("consumes", "");
	    obj.put("meta_info", meta_information);

	    response = this.obtainAndReturnGETResponse(obj);
	    logger.info("Returning response from " + requestQueueName + " " + response.getMetadata());
	    return response;    }
    
}
    
    /* ******************** End Of My Helloworld Method *********************** */
    
    
    /*
    @GET @Path("providers/{provider_id}")
    @Produces("application/json")
	public Response getProviderInformation(@PathParam("provider_id") final String provider_id) throws Exception {
    	Response response;
    	requestQueueName = "provider_get_56";
    	logger.info("Processing request for " + requestQueueName);
    	this.createAndSendRequest(requestQueueName);
   
	    JSONObject obj = new JSONObject();
	    obj.put("provider_id", provider_id);
	    JSONObject meta_information = new JSONObject();
	    meta_information.put("url", this.getSchemeHostPort());
	    meta_information.put("request", "GET");
	    meta_information.put("produces", "json");
	    meta_information.put("consumes", "");
	    obj.put("meta_info", meta_information);

	    response = this.obtainAndReturnGETResponse(obj);
	    logger.info("Returning response from " + requestQueueName + " " + response.getMetadata());
	    return response;    }
    

    @POST @Path("providers/")
    @Consumes("application/json")    
	public Response addProvider(String content) throws Exception{
    	Response response = null;

    	requestQueueName = "provider_post_56";
    	logger.info("Processing request for " + requestQueueName);
    	this.createAndSendRequest(requestQueueName);

	    JSONObject obj = new JSONObject();
	    obj.put("content", content);
	    JSONObject meta_information = new JSONObject();
	    meta_information.put("url", this.getSchemeHostPort());
	    meta_information.put("request", "POST");
	    meta_information.put("produces", "");
	    meta_information.put("consumes", "json");
	    obj.put("meta_info", meta_information);

	    response = this.obtainAndReturnPOSTResponse(obj);
	    logger.info("Returning response from " + requestQueueName);
		return response;
    }        

    @PUT @Path("providers/{provider_id}")
    @Consumes("application/json")    
	public Response updateProvider(@PathParam("provider_id") final String provider_id, String content) throws Exception{
    	Response response = null;

    	requestQueueName = "provider_put_56";
    	logger.info("Processing request for " + requestQueueName);
    	this.createAndSendRequest(requestQueueName);

	    JSONObject obj = new JSONObject();
	    obj.put("provider_id", provider_id);
	    obj.put("content", content);
	    JSONObject meta_information = new JSONObject();
	    meta_information.put("url", this.getSchemeHostPort());
	    meta_information.put("request", "PUT");
	    meta_information.put("produces", "");
	    meta_information.put("consumes", "json");
	    obj.put("meta_info", meta_information);

	    response = this.obtainAndReturnPOSTResponse(obj);
	    logger.info("Returning response from " + requestQueueName);
		return response;
    }        

    @DELETE @Path("providers/{provider_id}")
	public Response deleteProvider(@PathParam("provider_id") final String provider_id) throws Exception{
    	Response response = null;

    	requestQueueName = "provider_delete_56";
    	logger.info("Processing request for " + requestQueueName);
    	this.createAndSendRequest(requestQueueName);

	    JSONObject obj = new JSONObject();
	    obj.put("provider_id", provider_id);
	    JSONObject meta_information = new JSONObject();
	    meta_information.put("url", this.getSchemeHostPort());
	    meta_information.put("request", "DELETE");
	    meta_information.put("produces", "");
	    meta_information.put("consumes", "json");
	    obj.put("meta_info", meta_information);

	    response = this.obtainAndReturnPOSTResponse(obj);
	    logger.info("Returning response from " + requestQueueName);
		return response;
    }
    
    
    @GET @Path("members/{member_id}")
    @Produces("application/json")
	public Response getMemberInformation(@PathParam("member_id") final String member_id) throws Exception {
    	Response response;
    	requestQueueName = "member_get_56";
    	logger.info("Processing request for " + requestQueueName);
    	this.createAndSendRequest(requestQueueName);
   
	    JSONObject obj = new JSONObject();
	    obj.put("member_id", member_id);
	    JSONObject meta_information = new JSONObject();
	    meta_information.put("url", this.getSchemeHostPort());
	    meta_information.put("request", "GET");
	    meta_information.put("produces", "json");
	    meta_information.put("consumes", "");
	    obj.put("meta_info", meta_information);

	    response = this.obtainAndReturnGETResponse(obj);
	    logger.info("Returning response from " + requestQueueName + " " + response.getMetadata());
	    return response;    }
    
    @POST @Path("members/")
    @Consumes("application/json")    
	public Response addMember(String content) throws Exception{
    	Response response = null;

    	requestQueueName = "member_post_56";
    	logger.info("Processing request for " + requestQueueName);
    	this.createAndSendRequest(requestQueueName);

	    JSONObject obj = new JSONObject();
	    obj.put("content", content);
	    JSONObject meta_information = new JSONObject();
	    meta_information.put("url", this.getSchemeHostPort());
	    meta_information.put("request", "POST");
	    meta_information.put("produces", "");
	    meta_information.put("consumes", "json");
	    obj.put("meta_info", meta_information);

	    response = this.obtainAndReturnPOSTResponse(obj);
	    logger.info("Returning response from " + requestQueueName);
		return response;
    }
    
    @PUT @Path("members/{member_id}")
    @Consumes("application/json")    
	public Response updateMember(@PathParam("member_id") final String member_id, String content) throws Exception{
    	Response response = null;

    	requestQueueName = "member_put_56";
    	logger.info("Processing request for " + requestQueueName);
    	this.createAndSendRequest(requestQueueName);

	    JSONObject obj = new JSONObject();
	    obj.put("member_id", member_id);
	    obj.put("content", content);
	    JSONObject meta_information = new JSONObject();
	    meta_information.put("url", this.getSchemeHostPort());
	    meta_information.put("request", "PUT");
	    meta_information.put("produces", "");
	    meta_information.put("consumes", "json");
	    obj.put("meta_info", meta_information);

	    response = this.obtainAndReturnPOSTResponse(obj);
	    logger.info("Returning response from " + requestQueueName);
		return response;
    }
    
    @DELETE @Path("members/{member_id}")
	public Response deleteMember(@PathParam("member_id") final String member_id) throws Exception{
    	Response response = null;

    	requestQueueName = "member_delete_56";
    	logger.info("Processing request for " + requestQueueName);
    	this.createAndSendRequest(requestQueueName);

	    JSONObject obj = new JSONObject();
	    obj.put("member_id", member_id);
	    JSONObject meta_information = new JSONObject();
	    meta_information.put("url", this.getSchemeHostPort());
	    meta_information.put("request", "DELETE");
	    meta_information.put("produces", "");
	    meta_information.put("consumes", "json");
	    obj.put("meta_info", meta_information);

	    response = this.obtainAndReturnPOSTResponse(obj);
	    logger.info("Returning response from " + requestQueueName);
		return response;
    }
    
    @POST @Path("members/{member_id}/notes")
    @Consumes("application/json")    
	public Response addMemberNotes(@PathParam("member_id") final String member_id, String content) throws Exception{
    	Response response = null;

    	requestQueueName = "member_note_post_56";
    	logger.info("Processing request for " + requestQueueName);
    	this.createAndSendRequest(requestQueueName);

	    JSONObject obj = new JSONObject();
	    obj.put("member_id", member_id);
	    obj.put("content", content);
	    JSONObject meta_information = new JSONObject();
	    meta_information.put("url", this.getSchemeHostPort());
	    meta_information.put("request", "POST");
	    meta_information.put("produces", "");
	    meta_information.put("consumes", "json");
	    obj.put("meta_info", meta_information);

	    response = this.obtainAndReturnPOSTResponse(obj);
	    logger.info("Returning response from " + requestQueueName);
		return response;
    }
    
    @GET @Path("members/notes/{note_id}")
    @Produces("application/json")
	public Response getMemberNotesInformation(@PathParam("note_id") final String note_id) throws Exception {
    	Response response;
    	requestQueueName = "member_notes_get_56";
    	logger.info("Processing request for " + requestQueueName);
    	this.createAndSendRequest(requestQueueName);
   
	    JSONObject obj = new JSONObject();
	    obj.put("note_id", note_id);
	    JSONObject meta_information = new JSONObject();
	    meta_information.put("url", this.getSchemeHostPort());
	    meta_information.put("request", "GET");
	    meta_information.put("produces", "json");
	    meta_information.put("consumes", "");
	    obj.put("meta_info", meta_information);

	    response = this.obtainAndReturnGETResponse(obj);
	    logger.info("Returning response from " + requestQueueName + " " + response.getMetadata());
	    return response;    }

}
 */