package com.custom.cache.lib.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.custom.cache.lib.service.ISocketIOService;
import com.custom.cache.lib.service.CustomInMemoryCache;

import io.netty.util.internal.StringUtil;

//Why using socket.io to access the in-memory cache?
//1. Fast (faster compare to http protocol)
//2. Real time communication (most of online game use this protocol)
//3. Can be integrate easily with node.js
@Service(value = "socketIOService")
public class SocketIOServiceImpl implements ISocketIOService {

	private static final Logger logger = LoggerFactory.getLogger(ISocketIOService.class);
	
    /**
     * Store connected clients
     */
    private static Map<String, SocketIOClient> clientMap = new ConcurrentHashMap<>();
    
    /**
     * Custom Event`remove_event` for service side to client communication
     */
    private static final String REMOVE_EVENT = "remove_event";
    
    /**
     * Custom Event`put_event` for service side to client communication
     */
    private static final String PUT_EVENT = "put_event";

    /**
     * Custom Event`get_event` for service side to client communication
     */
    private static final String GET_EVENT = "get_event";
    
    /**
     * Custom Event`check_size_event` for service side to client communication
     */
    private static final String CHECK_SIZE_EVENT = "check_size_event";
    
    /**
     * Custom Event`ping_event` for service side to client communication
     */
    private static final String PING_EVENT = "ping_event";

    @Autowired
    private SocketIOServer socketIOServer;

    /**
     * Spring IoC After the container is created, start after loading the SocketIOServiceImpl Bean
     */
    @PostConstruct
    private void autoStartup() {
        start();
    }

    /**
     * Spring IoC Container closes before destroying SocketIOServiceImpl Bean to avoid restarting project service port occupancy
     */
    @PreDestroy
    private void autoStop() {
        stop();
    }

    @Override
    public void start() {
    	//this params can be configurable but for this sample, I'll just do the hardcoding
    	CustomInMemoryCache<String, String> cache = new CustomInMemoryCache<String, String>(200000, 500000, 1000);
    	
        // Listen for client connections
        socketIOServer.addConnectListener(client -> {
        	System.out.println("************ Client: " + getIpByClient(client) + " Connected ************");
//        	logger.debug("************ Client: " + getIpByClient(client) + " Connected ************");
            client.sendEvent("connected", "You're connected successfully...");
            String userId = getParamsByClient(client);
            if (userId != null) {
            	System.out.println("User id["+userId+"] Connected");
            	//logger.debug("User id["+userId+"] Connected");
                clientMap.put(userId, client);
            }
        });

        // Listening Client Disconnect
        socketIOServer.addDisconnectListener(client -> {
        	System.out.println("************ Client: " + getIpByClient(client) + " Disconnected ************");
//        	logger.debug("************ Client: " + getIpByClient(client) + " Disconnected ************");
            String userId = getParamsByClient(client);
            if (userId != null) {
            	System.out.println("User id["+userId+"] Disconnected");
//            	logger.debug("User id["+userId+"] Disconnected");
                clientMap.remove(userId);
                client.disconnect();
            }
        });
        
        // Start Services
        socketIOServer.start();
        
        // Custom Event`remove_event` ->Listen for client messages
        socketIOServer.addEventListener(REMOVE_EVENT, String.class, (client, data, ackSender) -> {            
            String clientIp = getIpByClient(client);
            String message = "";
            if(!StringUtil.isNullOrEmpty(data)) {
            	try {
            		if(!StringUtil.isNullOrEmpty(cache.get(data))) {
		            	cache.remove(data);
		            	message = "Remove record successfully into cache";
            		} else {
            			message = "Record does not exist inside cache";
            		}
            	} catch(Exception ex) {
            		ex.printStackTrace();
            		message = "Error removing data from cache "+ex.getMessage();
            	}
            } else {
            	message = "Invalid parameter";
            }
            System.out.println("message["+message+"]");
//            logger.debug("message["+message+"]");
            pushMessageToUser(REMOVE_EVENT, getParamsByClient(client), message);
            System.out.println(clientIp + " REMOVE_EVENT ************ key[" + data +"]");
//            logger.debug(clientIp + " REMOVE_EVENT ************ key[" + data +"]");
        });

        
        // Custom Event`put_event` ->Listen for client messages
        socketIOServer.addEventListener(PUT_EVENT, String.class, (client, data, ackSender) -> {           
            String clientIp = getIpByClient(client);
            String message = "";
            if(!StringUtil.isNullOrEmpty(data) && data.contains("=")) {
            	try {
	            	String[] strs = data.split("=");
	            	if(StringUtil.isNullOrEmpty(cache.get(strs[0]))) {
	            		cache.put(strs[0], strs[1]);
		            	message = "Insert record successfully into cache key["+strs[0]+"] value["+strs[1]+"]";
	            	} else {
	            		message = "Record exist inside cache";
	            	}
            	} catch(Exception ex) {
            		ex.printStackTrace();
            		message = "Error inserting into cache "+ex.getMessage();
            	}
            } else {
            	message = "Invalid parameter";
            }
            System.out.println("message["+message+"]");
//          logger.debug("message["+message+"]");
            pushMessageToUser(PUT_EVENT, getParamsByClient(client), message);
            System.out.println(clientIp + " PUT_EVENT ************ data[" + data +"]");
//          logger.debug(clientIp + " PUT_EVENT ************ data[" + data +"]");
        });

        // Custom Event`get_event` ->Listen for client messages
        socketIOServer.addEventListener(GET_EVENT, String.class, (client, data, ackSender) -> {
            String clientIp = getIpByClient(client);
            pushMessageToUser(GET_EVENT, getParamsByClient(client), cache.get(data));
            System.out.println(clientIp + " GET_EVENT ************ key[" + data+ "] value["+ cache.get(data)+ "]");
//            logger.debug(clientIp + " GET_EVENT ************ key[" + data+ "] value["+ cache.get(data)+ "]");
        });
        
        // Custom Event`check_size_event` ->Listen for client messages
        socketIOServer.addEventListener(CHECK_SIZE_EVENT, String.class, (client, data, ackSender) -> {
            String clientIp = getIpByClient(client);
            pushMessageToUser(CHECK_SIZE_EVENT, getParamsByClient(client), cache.size()+"");
            System.out.println(clientIp + " CHECK_SIZE_EVENT ************ key[" + data+ "] cache size["+ cache.size() + "]");
//          logger.debug(clientIp + " CHECK_SIZE_EVENT ************ key[" + data+ "] cache size["+ cache.size() + "]");
        });
        
        // Custom Event`ping_event` ->Listen for client messages
        // reply for a greeting from client
        socketIOServer.addEventListener(PING_EVENT, String.class, (client, data, ackSender) -> {
            String clientIp = getIpByClient(client);
            pushMessageToUser(PING_EVENT, getParamsByClient(client), "Greeting "+new Date());
            System.out.println(clientIp + " PING_EVENT ************ Receive ping message[" + data+"]");
//            logger.info(clientIp + " PING_EVENT ************ Receive ping message[" + data+"]");
        });

       

        // Broadcast: The default is to broadcast to all socket connections (
        /*
        new Thread(() -> {
            int i = 0;
            while (true) {
                try {
                    // Send broadcast message every 120 seconds
                    Thread.sleep(1200000);
                    socketIOServer.getBroadcastOperations().sendEvent("myBroadcast", "Greeting " + new Date());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        */
    }

    //Stop socket.io connection
    @Override
    public void stop() {
        if (socketIOServer != null) {
            socketIOServer.stop();
            socketIOServer = null;
        }
    }

    //Push message to client
    @Override
    public void pushMessageToUser(String event, String userId, String msgContent) {
        SocketIOClient client = clientMap.get(userId);
        if (client != null) {
            client.sendEvent(event, msgContent);
        }
    }

    /**
     * Get the userId parameter in the client url (modified here to suit individual needs and client side)
     *
     * @param client: Client
     * @return: java.lang.String
     */
    private String getParamsByClient(SocketIOClient client) {
        // Get the client url parameter (where userId is the unique identity)
        Map<String, List<String>> params = client.getHandshakeData().getUrlParams();
        List<String> userIdList = params.get("userId");
        if (!CollectionUtils.isEmpty(userIdList)) {
            return userIdList.get(0);
        }
        return null;
    }

    /**
     * Get the connected client ip address
     *
     * @param client: Client
     * @return: java.lang.String
     */
    private String getIpByClient(SocketIOClient client) {
        String sa = client.getRemoteAddress().toString();
        String clientIp = sa.substring(1, sa.indexOf(":"));
        return clientIp;
    }

}
