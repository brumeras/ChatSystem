package org.example.chat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("ChatUI.fxml"));

        Scene scene = new Scene(root, 900, 600); // <-- čia nustatome pradinį dydį (plotis, aukštis)

        primaryStage.setScene(scene);
        primaryStage.setTitle("Chat Client");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
