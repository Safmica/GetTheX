package com.safmica.listener;

public interface ClientConnectionListener {
    void onClientConnected(String clientId);
    void onClientDisconnected(String clientId);
}
