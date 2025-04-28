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
    private Map<String, List<String>> roomMessages = new HashMap<>(); // Kambarių žinučių saugojimas

    @Override
    public void run() {
        try {
            // Bandom užkurti serverį, klausyti prievado 9999
            server = new ServerSocket(9999);
            System.out.println("Server started on port 9999!"); // Pranešimas apie serverio paleidimą

            while (!done) {
                // Priimame naujus klientus
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            System.out.println("Error starting the server:");
            e.printStackTrace(); // Jei įvyksta klaida, išvedame išsamią klaidą
            shutdown();
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
                server.close();
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
            // Siųsti tik tiems vartotojams, kurie yra tame pačiame kambaryje
            if (ch != sender && ch.getRoomName().equals(roomName)) {
                ch.sendMessage(message);
            }
        }

        // Saugojame žinutę į kambario žinučių sąrašą
        roomMessages.computeIfAbsent(roomName, k -> new ArrayList<>()).add(message);
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
                    roomName = "main";
                }

                // Siunčiame žinutes, kurios jau buvo išsiųstos į šį kambarį
                List<String> previousMessages = roomMessages.getOrDefault(roomName, new ArrayList<>());
                for (String message : previousMessages) {
                    out.println(message);
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
                            broadcast(nickname + " joined the room.", roomName, this);
                        }
                    } else {
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
        new Thread(server).start();  // Paleisti serverį
    }
}
