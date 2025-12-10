module com.safmica {
  requires javafx.controls;
  requires javafx.fxml;
  requires transitive javafx.graphics;
  requires java.logging;
  requires com.google.gson;
  requires exp4j;
 
  opens com.safmica to javafx.fxml, com.google.gson;
  opens com.safmica.controllers to javafx.fxml;
  opens com.safmica.model to com.google.gson;
  
  exports com.safmica.controllers ;
  exports com.safmica.model ;
  exports com.safmica ;
  exports com.safmica.listener;
  exports com.safmica.network.server;
  exports com.safmica.network.client;
}
