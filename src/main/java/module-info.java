module com.safmica {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.logging;

  opens com.safmica to javafx.fxml;
  opens com.safmica.controllers to javafx.fxml;
  exports com.safmica.controllers ;
  exports com.safmica ;
}
