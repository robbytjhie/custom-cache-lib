package com.custom.cache.lib.service;

public interface ISocketIOService {
    /**
     * Start Services
     */
    void start();

    /**
     * Out of Service
     */
    void stop();

    /**
     * Push information to specified client
     *
     * @param userId:     Client Unique Identification
     * @param msgContent: Message Content
     */
    void pushMessageToUser(String event, String userId, String msgContent);
}