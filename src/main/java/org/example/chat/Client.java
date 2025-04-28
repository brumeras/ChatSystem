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
    private boolean done;
    private String nickname;

    public Client(String nickname) {
        this.nickname = nickname;
        try {
            client = new Socket("127.0.0.1", 9999);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            // Pirmiausia siunčiame nickname į serverį
            out.println(nickname);

            // Klausomės serverio pranešimų
            Thread listenerThread = new Thread(() -> {
                try {
                    String inMessage;
                    while ((inMessage = in.readLine()) != null) {
                        ChatController.appendMessage(inMessage);
                    }
                } catch (IOException e) {
                    shutdown();
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (Exception e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        if (out != null && message != null && !message.isEmpty()) {
            out.println(message);
        }
    }

    public void shutdown() {
        done = true;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (client != null && !client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            // Ignored
        }
    }
}
