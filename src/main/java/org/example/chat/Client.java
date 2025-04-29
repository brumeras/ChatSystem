package org.example.chat;

import java.io.*;
import java.net.Socket;

public class Client {
    private String nickname;
    private String roomName;
    private ChatController controller;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public Client(String nickname, String roomName, ChatController controller) {
        this.nickname = nickname;
        this.roomName = roomName;
        this.controller = controller;

        try {
            socket = new Socket("localhost", 9999);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(nickname);
            out.println(roomName);

            new Thread(this::listenForMessages).start(); // Paleidžiame žinučių klausymą atskiroje gijoje
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Klausytųjas, kuris gauna žinutes iš serverio ir jas atnaujina UI
    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Gauta žinutė iš serverio: " + message); // Atspausdiname gautą žinutę

                ChatController.appendMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Siųsti žinutę serveriui
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // Siųsti privačią žinutę
    public void sendPrivateMessage(String targetUser, String message) {
        if (out != null) {
            out.println("/private " + targetUser + " " + message);
        }
    }
}