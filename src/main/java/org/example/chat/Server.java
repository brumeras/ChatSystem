/*
@author Emilija Sankauskaitė, 5 grupė, VU Programų sistemos
Ši klasė implementuoja interfeisą Runnable, kuris yra atsakingas
už naujos gijos sukūrimą.
Ši klasė klausosi naujų prisijungusių users ir priima juos.
Socket- inicijuoja ryšį su klientu.
*/
package org.example.chat;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable
{
    //Saugo visus prisijungusius userius, užtikrina duomenų keitimąsi tarp users.
    private List<ConnectionHandler> connections = new CopyOnWriteArrayList<>();

    //Objektas, kuris atlieka serverio vaidmenį
    private ServerSocket server;

    //Leidžia sustabdyti serverio veiklą.
    private boolean done = false;
    private ExecutorService pool = Executors.newCachedThreadPool();

    //Masyvas, kuriame saugomos žinutės pagal kambarį.
    private Map<String, List<String>> roomMessages = new HashMap<>();


    //Kadangi implementuoja runnable, tai šitas metodas yra privalomas.
    //Jis paleidžia patį serverį.
    @Override
    public void run()
    {
        try
        {
            //Sukuriamas prievadas.
            server = new ServerSocket(9999);
            System.out.println("Server started on port 9999!");

            //Ciklas, kuris priima klientus.
            while (!done)
            {
                //Sukuriamas naujas Socket objektas, kuris priima naują klientą.
                Socket client = server.accept();
                System.out.println("New client connected: " + client.getInetAddress());

                //Sukuriamas naujas connectionHandler objektas, kuris atsakingas už komunikaciją.
                ConnectionHandler handler = new ConnectionHandler(client);
                //Vėliau pridedamas į connections.
                connections.add(handler);

                //Kliento paleidimas atskiroje gijoje
                //Nauja gija handler objektą vykdo atskirai.
                //Leidžia klientui turėti nepriklausomą komunikacijos procesą.
                new Thread(handler).start();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            //Jei klaida, serveris uždaromas.
            shutdown();
        }
    }


    //Išsiunčia pranešimą kambaryje, išskyrus siuntėją.
    public void broadcast(String message, String roomName, ConnectionHandler sender)
    {
        //Cikle eina per visus vartotojus
        for(ConnectionHandler ch : connections)
        {
            //Neleidžia siuntėjui gauti savo paties žinutės.
            //Siunčia pagal kambarį.
            if(ch != sender && ch.getRoomName().equals(roomName))
            {
                ch.sendMessage(message);
            }
        }
        roomMessages.computeIfAbsent(roomName, k -> new ArrayList<>()).add(message);
        saveMessageToFile(roomName, sender.getNickname(), message);
    }

    public void sendPrivateMessage(String senderName, String targetUser, String message)
    {
        for(ConnectionHandler ch : connections)
        {
            if(ch.getNickname().equals(targetUser))
            {
                ch.sendMessage(senderName + " (private): " + message);
                saveMessageToFile("private", senderName, message);
                break;
            }
        }
    }

    //Metodas,kuris yra atsakingas už serverio atjungimą.
    public void shutdown()
    {

        done=true;
        try
        {
            if (server != null && !server.isClosed())
            {
                server.close();
            }
        }
        catch (IOException e)
        {
            //ignore
        }
        for(ConnectionHandler ch : connections)
        {
            ch.shutdown();
        }
    }


    public Set<String> listRooms()
    {
        //Naudojamas HashSet, kad kambariai nesidubliuotų.
        Set<String> rooms = new HashSet<>();
        for(ConnectionHandler ch : connections)
        {
            rooms.add(ch.getRoomName());
        }
        return rooms;
    }


    //Synchronized, nes taip yra apsaugotas nuo kelių užrašymų keliuose gijose
    private synchronized void saveMessageToFile(String room, String sender, String message)
    {
        try(PrintWriter writer = new PrintWriter(new FileOutputStream(new File("messages.txt"), true)))
        {
            writer.println(room + ";" + sender + ";" + message);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private synchronized void saveUserToFile(String nickname, String room)
    {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(new File("users.txt"), true)))
        {
            writer.println(nickname + ";" + room);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public class ConnectionHandler implements Runnable
    {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        private String roomName;

        public ConnectionHandler(Socket client)
        {
            this.client = client;
        }

        public String getNickname()
        {
            return nickname;
        }

        public String getRoomName()
        {
            return roomName;
        }

        @Override
        public void run()
        {
            try {
                //Siųsti žinutėms.
                out = new PrintWriter(client.getOutputStream(), true);
                //Gauti žinutes.
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                nickname = in.readLine();
                roomName = in.readLine();

                if (roomName == null || roomName.trim().isEmpty()) {
                    roomName = "main";
                }

                saveUserToFile(nickname, roomName);


                List<String> previousMessages = roomMessages.getOrDefault(roomName, new ArrayList<>());

                for(String message : previousMessages)
                {
                    out.println(message);
                }

                broadcast(nickname + " joined the room!", roomName, this);

                String message;

                while((message = in.readLine()) != null)
                {
                    if (message.equalsIgnoreCase("/quit"))
                    {
                        shutdown();
                        break;
                    }
                    else if(message.startsWith("/join "))
                    {
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
                    }
                    else if (message.startsWith("/private "))
                    {
                        String[] parts = message.split(" ", 3);
                        if (parts.length > 2) {
                            String targetUser = parts[1];
                            String privateMessage = parts[2];
                            sendPrivateMessage(nickname, targetUser, privateMessage);
                        }
                    }
                    else if (message.equalsIgnoreCase("/rooms")) {
                        out.println("Active rooms: " + String.join(", ", listRooms()));}

                    else
                    {
                        broadcast(nickname + ": " + message, roomName, this);
                    }
                }
            } catch (Exception e) {
                shutdown();
            }
        }

        public void sendMessage(String message)
        {
            out.println(message);
        }

        public void shutdown()
        {
            try
            {
                connections.remove(this);

                if(nickname!=null)
                {
                    broadcast(nickname + " left the room.", roomName, this);
                }
                if (in != null) in.close();
                if (out != null) out.close();
                if (client != null && !client.isClosed())
                {
                    client.close();
                }
            }
            catch (IOException e)
            {
                // Ignore
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        new Thread(server).start();
    }
}