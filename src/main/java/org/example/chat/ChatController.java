package org.example.chat;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ChatController {

    @FXML private TextField nicknameField;
    @FXML private TextArea chatArea;
    @FXML private TextField inputField;

    private static ChatController instance;
    private Client client;
    private String nickname;

    public ChatController() {
        instance = this;
    }

    public static void appendMessage(String message) {
        if (instance != null) {
            instance.chatArea.appendText(message + "\n");
        }
    }

    @FXML
    private void setNickname() {
        nickname = nicknameField.getText();
        if (!nickname.isEmpty()) {
            client = new Client(nickname); // Sukuriame klientą su nickname
            chatArea.appendText("Prisijungėte kaip " + nickname + "\n");
            nicknameField.setDisable(true); // Užrakina lauką po įvedimo
        }
    }

    @FXML
    private void sendMessage() {
        if (client != null) {
            String message = inputField.getText();
            if (!message.isEmpty()) {
                client.sendMessage(message); // Call Client's sendMessage method
                chatArea.appendText(nickname + ": " + message + "\n"); // Show sender's nickname
                inputField.clear();
            }
        }
    }
}
