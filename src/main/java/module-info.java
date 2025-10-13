module com.safmica {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.safmica to javafx.fxml;
    exports com.safmica;
}
