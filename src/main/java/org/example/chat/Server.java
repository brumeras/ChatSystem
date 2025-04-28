package org.example.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private List<ConnectionHandler> connections = new CopyOnWriteArrayList<>();
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool = Executors.newCachedThreadPool();

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            System.out.println("Server started on port 9999!");

            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message, ConnectionHandler sender) {
        for (ConnectionHandler ch : connections) {
            if (ch != sender && ch.getRoomName().equals(sender.getRoomName())) {
                ch.sendMessage(message);
            }
        }
    }

    public Set<String> listRooms() {
        Set<String> rooms = new HashSet<>();
        for (ConnectionHandler ch : connections) {
            rooms.add(ch.getRoomName());
        }
        return rooms;
    }

    public void shutdown() {
        done = true;
        try {
            if (server != null && !server.isClosed()) {
                server.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        for (ConnectionHandler ch : connections) {
            ch.shutdown();
        }
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        private String roomName;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        public String getRoomName() {
            return roomName;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                nickname = in.readLine();
                roomName = in.readLine();

                if (nickname != null && !nickname.trim().isEmpty()) {
                    System.out.println(nickname + " joined room " + roomName);
                    broadcast(nickname + " joined the room!", this);
                }

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("/quit")) {
                        shutdown();
                        break;
                    } else if (message.equalsIgnoreCase("/list")) {
                        Set<String> rooms = listRooms();
                        out.println("Available rooms: " + String.join(", ", rooms));
                    } else {
                        broadcast(nickname + ": " + message, this);
                    }
                }
            } catch (Exception e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                connections.remove(this);
                if (nickname != null) {
                    broadcast(nickname + " left the room.", this);
                }
                if (in != null) in.close();
                if (out != null) out.close();
                if (client != null && !client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        new Thread(server).start();
    }
}
