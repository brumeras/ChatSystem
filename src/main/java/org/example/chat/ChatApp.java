/*
@author Emilija Sankauskaitė 5 grupė, VU Programų sistemos
Klasė, kuri leidžia pridėti naują klientą.
 */

package org.example.chat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatApp extends Application
{

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        Parent root = FXMLLoader.load(getClass().getResource("ChatUI.fxml"));
        Scene scene = new Scene(root, 600, 600);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Emilijos susirašinėjimo programa.");
        primaryStage.show();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}