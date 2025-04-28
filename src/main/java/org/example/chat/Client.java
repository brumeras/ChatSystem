package org.example.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname;
    private String roomName;
    private boolean done = false;
    private ChatController chatController;

    public Client(String nickname, String roomName, ChatController chatController) {
        this.nickname = nickname;
        this.chatController = chatController;
        try {
            client = new Socket("127.0.0.1", 9999);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            out.println(nickname); // Pirmas dalykas - siunčiam nickname
            out.println(roomName); // Antras dalykas - siunčiam room name

            // Klausytojas
            Thread listenerThread = new Thread(() -> {
                try {
                    String inMessage;
                    while ((inMessage = in.readLine()) != null && !done) {
                        chatController.appendMessage(inMessage);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                    shutdown();
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (IOException e) {
            System.out.println("Error: Unable to connect to server.");
        }
    }

    public void sendMessage(String message) {
        if (out != null && message != null && !message.isEmpty()) {
            out.println(message);
            if (message.equalsIgnoreCase("/quit")) {
                shutdown();
            }
        }
    }

    public void shutdown() {
        done = true;
        try {
            if (out != null) {
                out.println("/quit");
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (client != null && !client.isClosed()) {
                client.close();
            }
            System.out.println("Client disconnected.");
        } catch (IOException e) {
            System.out.println("Error shutting down client.");
        }
    }
}
