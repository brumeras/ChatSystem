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
            if (message.startsWith(instance.nickname + ": ")) {
                instance.chatArea.appendText("Me: " + message.substring(instance.nickname.length() + 2) + "\n");
            } else {
                instance.chatArea.appendText(message + "\n");
            }
        }
    }

    
    @FXML
    private void setNickname() {
        String enteredNickname = nicknameField.getText().trim();

        // Patikrinimas: ar vardas tuščias arba jau naudojamas
        if (enteredNickname.isEmpty() || enteredNickname.contains(":")) {
            chatArea.appendText("Klaida: netinkamas vardas!\n");
            return;
        }

        nickname = enteredNickname;
        client = new Client(nickname, this);
        chatArea.appendText("Prisijungėte kaip " + nickname + "\n");
        nicknameField.setDisable(true);
    }

    @FXML
    private void sendMessage() {
        if (client != null) {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                // Užtikriname, kad žinutė rodomų be dubliuoto vardo
                String formattedMessage = nickname + ": " + message;
                client.sendMessage(formattedMessage);
                appendMessage(formattedMessage);
                inputField.clear();
            }
        }
    }
}
