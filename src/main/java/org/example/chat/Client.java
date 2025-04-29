/*
@author Emilija Sankauskaitė 5 grupė, VU programų sistemos
Jis prijungia vartotoją prie serverio,
leidžia siųsti ir gauti žinutes bei palaiko privačius pokalbius.
 */

package org.example.chat;

import java.io.*;
import java.net.Socket;

public class Client
{
    private String nickname;
    private String roomName;
    private ChatController controller;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public Client(String nickname, String roomName, ChatController controller)
    {
        this.nickname = nickname;
        this.roomName = roomName;
        this.controller = controller;

        try
        {
            socket = new Socket("localhost", 9999);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(nickname);
            out.println(roomName);

            //Sukuriama nauja gija, kurioje vykdomas metodas listenForMessages
            //Tai leidžia klientui nuolat klausytis gaunamų žinučių, neblokuojant pagrindinės veiklos.
            new Thread(this::listenForMessages).start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    //Klausytojas, kuris gauna žinutes iš serverio.
    //Siunčia žinutes į UI, kad vartotojas jas matytų.
    private void listenForMessages()
    {
        try
        {
            String message;
            while((message = in.readLine()) != null)
            {
                System.out.println("Gauta žinutė iš serverio: " + message);

                ChatController.appendMessage(message);
            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    //Vartotojo žinutė siunčiama serveriui
    public void sendMessage(String message)
    {
        if(out != null)
        {
            out.println(message);
        }
    }

    //Leidžia siųsti privačią žinutę
    public void sendPrivateMessage(String targetUser, String message)
    {
        if(out!=null)
        {
            out.println("/private " + targetUser + " " + message);
        }
    }
}