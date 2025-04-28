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
    private boolean done;
    private ExecutorService pool = Executors.newCachedThreadPool();
    private Map<String, List<String>> roomMessages = new HashMap<>(); // Kambarių žinučių saugojimas

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            System.out.println("Server started on port 9999!"); // Serverio paleidimas

            while (!done) {
                Socket client = server.accept();  // Priimame naują klientą
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            System.out.println("Error starting the server:");
            e.printStackTrace();
            shutdown();  // Uždarome serverį
        }
    }

    // Grąžina kambarių sąrašą
    public Set<String> listRooms() {
        Set<String> rooms = new HashSet<>();
        for (ConnectionHandler ch : connections) {
            rooms.add(ch.getRoomName());
        }
        return rooms;
    }

    // Serverio uždarymas
    public void shutdown() {
        done = true;
        try {
            if (server != null && !server.isClosed()) {
                server.close();  // Uždaryti serverio lizdą
            }
        } catch (IOException e) {
            // Ignore
        }
        for (ConnectionHandler ch : connections) {
            ch.shutdown();
        }
    }

    // Paskelbti žinutę visiems kambario dalyviams
    public void broadcast(String message, String roomName, ConnectionHandler sender) {
        for (ConnectionHandler ch : connections) {
            if (ch != sender && ch.getRoomName().equals(roomName)) {
                ch.sendMessage(message);
            }
        }
        // Saugojame žinutę į kambario žinučių sąrašą
        roomMessages.computeIfAbsent(roomName, k -> new ArrayList<>()).add(message);
    }

    // Siųsti privačią žinutę tik tam vartotojui
    public void sendPrivateMessage(String targetUser, String message) {
        for (ConnectionHandler ch : connections) {
            if (ch.getNickname().equals(targetUser)) {
                ch.sendMessage(message);
                break;
            }
        }
    }

    // Klasė, kuri priima ir apdoroja klientų užklausas
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

                // Gavome vartotojo vardą ir kambario pavadinimą
                nickname = in.readLine();
                roomName = in.readLine();

                if (roomName == null || roomName.trim().isEmpty()) {
                    roomName = "main";  // Jei kambarys nebuvo nurodytas, nustatome "main"
                }

                // Siunčiame žinutes, kurios jau buvo išsiųstos į šį kambarį
                List<String> previousMessages = roomMessages.getOrDefault(roomName, new ArrayList<>());
                for (String message : previousMessages) {
                    out.println(message);
                }

                broadcast(nickname + " joined the room!", roomName, this);  // Pranešame kitiems, kad vartotojas prisijungė

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("/quit")) {
                        shutdown();
                        break;
                    } else if (message.startsWith("/join ")) {
                        String newRoom = message.substring(6).trim();
                        if (!newRoom.isEmpty()) {
                            String oldRoom = roomName;
                            broadcast(nickname + " left the room.", oldRoom, this);  // Pranešame, kad paliko kambarį
                            roomName = newRoom;
                            out.println("You have joined the room: " + roomName);  // Informuojame, kad perejo į naują kambarį
                            broadcast(nickname + " joined the room.", roomName, this);  // Pranešame kitiems, kad prisijungė
                        }
                    } else if (message.startsWith("/private ")) {
                        String[] parts = message.split(" ", 3);  // /private <targetUser> <message>
                        if (parts.length > 2) {
                            String targetUser = parts[1];
                            String privateMessage = parts[2];
                            sendPrivateMessage(targetUser, nickname + " (private): " + privateMessage);  // Siunčiame privačią žinutę
                        }
                    } else {
                        broadcast(nickname + ": " + message, roomName, this);  // Paskelbti žinutę visiems kambario dalyviams
                    }
                }
            } catch (Exception e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);  // Siųsti žinutę klientui
        }

        public void shutdown() {
            try {
                connections.remove(this);  // Pašalinti šį klientą iš sąrašo
                if (nickname != null) {
                    broadcast(nickname + " left the room.", roomName, this);  // Informuoti kitus, kad vartotojas paliko kambarį
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
        new Thread(server).start();  // Paleisti serverį
    }
}
