package org.example.chat;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server implements Runnable {

    private List<ConnectionHandler> connections = new CopyOnWriteArrayList<>();
    private Set<String> activeNicknames = new HashSet<>();
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
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message, ConnectionHandler sender) {
        for (ConnectionHandler ch : connections) {
            if (ch != sender) {
                ch.sendMessage(message);
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
            // Ignore errors
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

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                nickname = in.readLine().trim();

                // Patikriname, ar vardas jau naudojamas
                synchronized (activeNicknames) {
                    if (nickname.isEmpty() || activeNicknames.contains(nickname)) {
                        out.println("Error: Vardas jau naudojamas arba netinkamas.");
                        shutdown();
                        return;
                    }
                    activeNicknames.add(nickname);
                }

                connections.add(this);
                broadcast(nickname + " joined the chat!", this);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith(nickname + ": ")) {
                        sendMessage("Me: " + message.substring(nickname.length() + 2));
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
                synchronized (activeNicknames) {
                    activeNicknames.remove(nickname);
                }
                broadcast(nickname + " left the chat.", null);
                in.close();
                out.close();
                if (!client.isClosed()) {
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
