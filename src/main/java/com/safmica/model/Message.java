package com.safmica.model;

public class Message<T> {
    public String type;
    public T data;

    public Message() {}
    public Message(String type, T data) {
        this.type = type;
        this.data = data;
    }
}
