package org.example.chat;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ChatController {

    @FXML private TextField nicknameField;
    @FXML private TextField roomField;
    @FXML private TextArea chatArea;
    @FXML private TextField inputField;

    private static ChatController instance;
    private Client client;
    private String nickname;
    private String roomName;

    public ChatController() {
        instance = this;
    }

    public static void appendMessage(String message) {
        if (instance != null) {
            if (message.startsWith(instance.nickname + ":")) {
                instance.chatArea.appendText("Me: " + message.substring(instance.nickname.length() + 1) + "\n");
            } else {
                instance.chatArea.appendText(message + "\n");
            }
        }
    }

    @FXML
    private void setNickname() {
        nickname = nicknameField.getText();
        String roomName = roomField.getText(); // paimam kambario pavadinimą
        if (!nickname.isEmpty() && !roomName.isEmpty()) {
            client = new Client(nickname, roomName, this); // Dabar perduodam ir kambario pavadinimą
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
                client.sendMessage(message);
                appendMessage(nickname + ": " + message);
                inputField.clear();
            }
        }
    }
}
