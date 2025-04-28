package org.example.chat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChatApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        AnchorPane root = FXMLLoader.load(getClass().getResource("ChatUI.fxml")); // Expect AnchorPane
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Chat Client");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
