/*
@author Emilija Sankauskaitė 5 grupė, VU programų sistemos
Ši klasė yra vartotojo pokalbių valdymo centras.
 */

package org.example.chat;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.application.Platform;

import java.util.Set;

public class ChatController {

    @FXML private TextField nicknameField;
    @FXML private TextField roomField;
    @FXML private TextArea chatArea;
    @FXML private TextField inputField;
    @FXML private TextField switchRoomField;
    @FXML private TextField privateMessageField;
    @FXML private ListView<String> roomList;// Naujas laukelis privačioms žinutėms


    //Naudojamas instance, kad būtų galima kitoms klasėms pasiekti UI valdymą
    private static ChatController instance;
    private Client client;
    private String nickname;
    private String roomName;

    public ChatController()
    {
        instance = this;
    }

    //Metodas, kuris yra atsakingas už pokalbių sąsajos atnaujinimą.
    public static void appendMessage(String message)
    {

        //Jei egzistuojq objektas
        if(instance != null)
        {
            //JavaFX gija veikia atskirai nuo programos
            //Užtikrinama, kad atnaujinimas vyktų gijoje
            Platform.runLater(() -> {
                System.out.println("UI atnaujinamas su žinute: " + message);

                if (message.startsWith(instance.nickname + ":")) {
                    instance.chatArea.appendText("Me: " + message.substring(instance.nickname.length() + 1) + "\n");
                } else {
                    instance.chatArea.appendText(message + "\n");
                }
            });
        } else {
            System.err.println("ChatController instance is null!");
        }
    }


    @FXML
    private void setNickname() {
        nickname = nicknameField.getText();
        roomName = roomField.getText().trim();

        if(roomName.isEmpty())
        {
            roomName = "main";
        }

        if(!nickname.isEmpty())
        {
            client = new Client(nickname, roomName, this);

            chatArea.appendText("Prisijungėte kaip " + nickname + " į kambarį: " + roomName + "\n");

            nicknameField.setDisable(true);
            roomField.setDisable(true);
        }
    }

    @FXML
    private void sendMessage()
    {
        if(client != null)
        {
            String message = inputField.getText();
            if (!message.isEmpty()) {
                client.sendMessage(message);

                appendMessage(nickname + ": " + message);

                inputField.clear();
            }
        }
    }

    @FXML
    private void switchRoom()
    {
        if(client != null)
        {
            String newRoom = switchRoomField.getText();
            if (!newRoom.isEmpty())
            {
                client.sendMessage("/join " + newRoom);
                chatArea.clear();
                chatArea.appendText("Prisijungėte į kambarį: " + newRoom + "\n");
                switchRoomField.clear();
            }
        }
    }

    @FXML
    private void sendPrivateMessage()
    {
        if(client != null)
        {
            String[] parts = privateMessageField.getText().split(":", 2);
            if (parts.length < 2) {
                chatArea.appendText("Privati žinutė netinkamai suformatuota. Naudok: Vartotojas: Žinutė\n");
                return;
            }

            String targetUser = parts[0].trim();
            String message = parts[1].trim();

            if(!targetUser.isEmpty() && !message.isEmpty())
            {
                client.sendPrivateMessage(targetUser, message);
                appendMessage("Private message to " + targetUser + ": " + message);
                privateMessageField.clear();
            }
        }
    }

    @FXML
    private void requestRooms() {
        if (client != null) {
            client.sendMessage("/rooms");
        }
    }

    // Atnaujinti kambarių sąrašą
    public void updateRoomList(Set<String> rooms) {
        Platform.runLater(() -> {
            roomList.getItems().clear();
            roomList.getItems().addAll(rooms);
        });
    }

}