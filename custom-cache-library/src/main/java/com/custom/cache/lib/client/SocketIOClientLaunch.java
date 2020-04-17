package com.custom.cache.lib.client;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.socket.client.IO;
import io.socket.client.Socket;

//Socket.io client to test custom cache
public class SocketIOClientLaunch {
	
	private static final Logger logger = LoggerFactory.getLogger(SocketIOClientLaunch.class);

    public static void main(String[] args) {
        // Server socket.io Connection Communication Address with custom cache server
        String url = "http://127.0.0.1:8959";
        try {
            IO.Options options = new IO.Options();
            options.transports = new String[]{"websocket"};
            options.reconnectionAttempts = 2;
            // Time interval for failed reconnection
            options.reconnectionDelay = 1000;
            // Connection timeout (ms)
            options.timeout = 500;
            // userId: Unique identity passed to the server-side store
            // many users can access centralised custom cache servers
            final Socket socket = IO.socket(url + "?userId=USR001", options);

            
            socket.on(Socket.EVENT_CONNECT, args1 -> socket.send("hello..."));

            // Custom Event`Connected` ->Receive Server Successful Connection Message
            socket.on("connected", objects -> logger.debug("connected Server:" + objects[0].toString()));

            socket.connect();
             
            //testing insert, get, remove and check cache size
            putMessage(socket, "A001", "Name:Ron;Age:20;Status:Maried");
            putMessage(socket, "A002", "Name:Richard;Age:19;Status:Maried");
            putMessage(socket, "A003", "Name:Elsa;Age:25;Status:Single");
            checkSizeMessage(socket);
            putMessage(socket, "A004", "Name:Sam;Age:28;Status:Single");
            putMessage(socket, "A005", "Name:Drew;Age:35;Status:Single");
            getMessage(socket, "A001");
            getMessage(socket, "A005");
            checkSizeMessage(socket);
            putMessage(socket, "A002", "Name:Tony;Age:16;Status:Maried");
            checkSizeMessage(socket);
            getMessage(socket, "A002");
            removeMessage(socket, "A005");
            removeMessage(socket, "A001");
            checkSizeMessage(socket);
            getMessage(socket, "A005");
            getMessage(socket, "A001");
            
            // Ping server every 30 second (greeting) to check if cache server still up and running
            new Thread(() -> {
                int i = 0;
                while (true) {
                    try {
                        // Send broadcast message every 3 seconds
                        Thread.sleep(30000);
                        //send ping event
                        socket.emit("ping_event", "Hello " + new Date());
                        //listener to incoming ping event
                        socket.on("ping_event", objects -> logger.debug("ping server result:" + objects[0].toString()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //fetch a record from cache
    private static void getMessage(Socket socket, String key) throws Exception {
    	Thread.sleep(3000);
    	//clear previous get event listener
    	socket.off("get_event");
    	socket.emit("get_event", key);
        socket.on("get_event", objects -> logger.debug("get key[" + key + "]  and value from server[" + objects[0] +"]"));
    }
    
    //insert a record into cache
    private static void putMessage(Socket socket, String key, String value) throws Exception{
    	Thread.sleep(3000);
    	//clear previous put event listener
    	socket.off("put_event");
    	socket.emit("put_event", key+"="+value);
        socket.on("put_event", objects -> logger.debug("put key["+key+ "] and value["+ value + "] result from server[" + objects[0]+"]"));
    }
    
    //remove a record from cache
    private static void removeMessage(Socket socket, String key) throws Exception{
    	Thread.sleep(3000);
    	//clear previous remove event listener
    	socket.off("remove_event");
    	socket.emit("remove_event", key);
        socket.on("remove_event", objects -> logger.debug("remove key["+ key + "]  result from server[" + objects[0] + "]"));
       
    }
    
    //check number of records inside cache
    private static void checkSizeMessage(Socket socket) throws Exception{
    	Thread.sleep(3000);
    	//clear previous check size event listener
    	socket.off("check_size_event");
    	socket.emit("check_size_event", "");
        socket.on("check_size_event", objects -> logger.debug("cache size[" + objects[0]+"]"));
    }

}