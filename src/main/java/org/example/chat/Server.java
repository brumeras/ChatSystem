package org.example.chat;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private List<ConnectionHandler> connections = new CopyOnWriteArrayList<>();
    private ServerSocket server;
    private boolean done = false;
    private ExecutorService pool = Executors.newCachedThreadPool();
    private Map<String, List<String>> roomMessages = new HashMap<>(); // Žinutės pagal kambarius

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            System.out.println("Server started on port 9999!");

            while (!done) {
                Socket client = server.accept();
                System.out.println("New client connected: " + client.getInetAddress());

                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);

                new Thread(handler).start(); // Čia paleidžiame kiekvieną klientą atskiroje gijoje
            }
        } catch (IOException e) {
            e.printStackTrace();
            shutdown();
        }
    }


    public void broadcast(String message, String roomName, ConnectionHandler sender) {
        for (ConnectionHandler ch : connections) {
            if (ch != sender && ch.getRoomName().equals(roomName)) {
                ch.sendMessage(message);
            }
        }
        roomMessages.computeIfAbsent(roomName, k -> new ArrayList<>()).add(message);
        saveMessageToFile(roomName, sender.getNickname(), message);
    }

    public void sendPrivateMessage(String senderName, String targetUser, String message) {
        for (ConnectionHandler ch : connections) {
            if (ch.getNickname().equals(targetUser)) {
                ch.sendMessage(senderName + " (private): " + message);

                // Išsaugom privačią žinutę į failą (naudojam specialų "private" kambarį)
                saveMessageToFile("private", senderName, message);
                break;
            }
        }
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
    public Set<String> listRooms() {
        Set<String> rooms = new HashSet<>();
        for (ConnectionHandler ch : connections) {
            rooms.add(ch.getRoomName());
        }
        return rooms;
    }

    private synchronized void saveMessageToFile(String room, String sender, String message) {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(new File("messages.txt"), true))) {
            writer.println(room + ";" + sender + ";" + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void saveUserToFile(String nickname, String room) {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(new File("users.txt"), true))) {
            writer.println(nickname + ";" + room);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class ConnectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        private String roomName;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        public String getNickname() {
            return nickname;
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

                if (roomName == null || roomName.trim().isEmpty()) {
                    roomName = "main"; // Numatytasis kambarys
                }

                saveUserToFile(nickname, roomName);

                // **Kai keičiam kambarį, parodome senas žinutes**
                List<String> previousMessages = roomMessages.getOrDefault(roomName, new ArrayList<>());
                for (String message : previousMessages) {
                    out.println(message); // Siunčiame senas žinutes vartotojui
                }

                broadcast(nickname + " joined the room!", roomName, this);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("/quit")) {
                        shutdown();
                        break;
                    } else if (message.startsWith("/join ")) {
                        String newRoom = message.substring(6).trim();
                        if (!newRoom.isEmpty()) {
                            String oldRoom = roomName;
                            broadcast(nickname + " left the room.", oldRoom, this);
                            roomName = newRoom;
                            out.println("You have joined the room: " + roomName);

                            List<String> newRoomMessages = roomMessages.getOrDefault(roomName, new ArrayList<>());
                            for (String msg : newRoomMessages) {
                                out.println(msg);
                            }
                            broadcast(nickname + " joined the room.", roomName, this);
                        }
                    } else if (message.startsWith("/private ")) {
                        String[] parts = message.split(" ", 3);
                        if (parts.length > 2) {
                            String targetUser = parts[1];
                            String privateMessage = parts[2];
                            sendPrivateMessage(nickname, targetUser, privateMessage);
                        }
                    }
                    else if (message.equalsIgnoreCase("/rooms")) {
                        out.println("Active rooms: " + String.join(", ", listRooms()));}

                    else {
                        broadcast(nickname + ": " + message, roomName, this);
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
                    broadcast(nickname + " left the room.", roomName, this);
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