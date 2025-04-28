package org.example.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//Su žodžiu runnable šita klasė gali būti perduodama kaip thread.
//Turėsime serverį, kuris ,,klausys" connections.
public class Server implements Runnable{

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    //istraukia gija
    private ExecutorService pool;

    public Server()
    {
        connections = new ArrayList<>();
        done = false;
    }
    //Kai perduosim šią klasę metodas run bus panaudotas kitur.
    @Override
    public void run()
    {
        try {

            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while(!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                //kiekviena karta, kai pridedam new connection
                pool.execute(handler);
            }


        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message)
    {
        for( ConnectionHandler ch : connections)
        {
            if(ch != null)
            {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown()
    {
        try{
            done = true;
            if(!server.isClosed())
            {
                server.close();
            }}
        catch (IOException e)
        {
            //ignore it
        }
        for(ConnectionHandler ch : connections)
        {
            ch.shutdown();
        }
    }


    class ConnectionHandler implements Runnable{

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client)
        {
            this.client = client;

        }

        @Override
        public void run() {
            try
            {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                out.println("Please enter your nickname:");
                nickname = in.readLine();
                //Galima tikrinti nickname
                System.out.println(nickname + " connected!");
                //Norime, kad tai rodytu kitiems klientams tai mums reikia list

                broadcast(nickname + " joined the chat!");
                String message;

                while((message = in.readLine()) != null)
                {
                    if(message.startsWith("/nick "))
                    {
                        String[] messageSplit = message.split(" ", 2);
                        if(messageSplit.length == 2)
                        {
                            broadcast(nickname + " renamed themselves to " + messageSplit[1]);
                            System.out.println(nickname + " renamed themselves to " + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Succesfully changed nickname to " + nickname);

                        }
                        else
                        {
                            out.println("No nickname provided!");
                        }
                    }
                    else if (message.startsWith("/quit"))
                    {
                        broadcast(nickname + " left the chat!");
                        shutdown();

                    }
                    else {
                        broadcast(nickname + " : " + message);
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
            try{
                in.close();
                out.close();
                if(!server.isClosed())
                {
                    client.close();

                }}
            catch(IOException e)
            {
                //ignore
            }
        }
    }
    public static void main(String[] args)
    {
        Server server = new Server();
        server.run();
    }
}
