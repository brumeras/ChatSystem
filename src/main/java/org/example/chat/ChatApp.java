package org.example.chat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Paleisti serverį atskirame thread
        startServer();

        // Įkeliame FXML ir sukuriame sceną
        Parent root = FXMLLoader.load(getClass().getResource("ChatUI.fxml"));
        Scene scene = new Scene(root, 600, 600);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Chat Client");
        primaryStage.show();
    }

    private void startServer() {
        // Paleisti serverį kitame thread
        new Thread(() -> {
            try {
                Server server = new Server();
                new Thread(server).start(); // Serverį paleidžiame atskirame thread

                System.out.println("Server started successfully."); // Informuoti apie serverio pradžią
            } catch (Exception e) {
                System.err.println("Failed to start the server: " + e.getMessage());
                // Rodo klaidos pranešimą, jei serveris nepavyko užsikrauti
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
