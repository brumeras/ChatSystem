package org.example.chat;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.application.Platform;

public class ChatController {

    @FXML private TextField nicknameField;
    @FXML private TextField roomField;
    @FXML private TextArea chatArea;
    @FXML private TextField inputField;
    @FXML private TextField switchRoomField;
    @FXML private TextField privateMessageField;  // Naujas laukelis privačioms žinutėms

    private static ChatController instance;
    private Client client;
    private String nickname;
    private String roomName;

    public ChatController() {
        instance = this;
    }

    public static void appendMessage(String message) {
        if (instance != null) {
            // Užtikriname, kad atnaujinimas būtų vykdomas JavaFX UI thread
            Platform.runLater(() -> {
                // Jei žinutė priklauso šiam vartotojui, parodome ją kaip "Me:"
                if (message.startsWith(instance.nickname + ":")) {
                    instance.chatArea.appendText("Me: " + message.substring(instance.nickname.length() + 1) + "\n");
                } else {
                    instance.chatArea.appendText(message + "\n");
                }
            });
        }
    }

    @FXML
    private void setNickname() {
        nickname = nicknameField.getText();
        roomName = roomField.getText().trim(); // Paimam kambario pavadinimą ir pašalinam nereikalingus tarpus

        if (roomName.isEmpty()) {
            roomName = "main";
        }

        if (!nickname.isEmpty()) {
            client = new Client(nickname, roomName, this);

            chatArea.appendText("Prisijungėte kaip " + nickname + " į kambarį: " + roomName + "\n");

            nicknameField.setDisable(true);
            roomField.setDisable(true);
        }
    }

    @FXML
    private void sendMessage() {
        if (client != null) {
            String message = inputField.getText();
            if (!message.isEmpty()) {
                client.sendMessage(message); // Siunčiame žinutę klientui

                appendMessage(nickname + ": " + message); // Rodyti žinutę pačiame sąsajoje

                inputField.clear(); // Išvalome įvedimo lauką
            }
        }
    }

    @FXML
    private void switchRoom() {
        if (client != null) {
            String newRoom = switchRoomField.getText();
            if (!newRoom.isEmpty()) {
                client.sendMessage("/join " + newRoom); // Pakeičiame kambarį
                chatArea.clear(); // Išvalome seną chatą
                chatArea.appendText("Prisijungėte į kambarį: " + newRoom + "\n");
                switchRoomField.clear();
            }
        }
    }

    // Naujas metodas privačioms žinutėms
    @FXML
    private void sendPrivateMessage() {
        if (client != null) {
            String targetUser = privateMessageField.getText().split(":")[0].trim(); // Paimti tik vartotojo vardą
            String message = privateMessageField.getText().split(":")[1].trim(); // Paimti pranešimo tekstą
            if (!targetUser.isEmpty() && !message.isEmpty()) {
                // Siunčiame privačią žinutę
                client.sendMessage("/private " + targetUser + " " + message);

                appendMessage("Private message " + targetUser + ": " + message); // Rodyti privačią žinutę
                privateMessageField.clear(); // Išvalome laukelį
            }
        }
    }
}
