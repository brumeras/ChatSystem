module org.example.chat {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.chat to javafx.fxml;
    exports org.example.chat;
}