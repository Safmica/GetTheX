package com.safmica.network;

public interface ClientConnectionListener {
    void onClientConnected(String clientId);
    void onClientDisconnected(String clientId);
}
