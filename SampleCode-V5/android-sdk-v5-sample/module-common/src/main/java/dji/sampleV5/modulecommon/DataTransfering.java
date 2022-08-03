package dji.sampleV5.modulecommon;

import android.util.Log;

import java.net.*;
import java.io.*;

public class DataTransfering {

    public static String DISCONNECT_MESSAGE = "!DISCONNECT";
    public static String SERVER = "192.168.9.1";
    public static int PORT = 2004;

    public static void Test(){

        Log.d("Sockets", "Server Attempting to connect: " + SERVER + " on Port: " + PORT);
/*
        try {
            Socket socket = new Socket(SERVER, PORT);

            PrintWriter pr = new PrintWriter(socket.getOutputStream());
            pr.println("Hello motherfucker");
            pr.flush();
        } catch (IOException e) {

            e.printStackTrace();
        }
*/

        try{
            Socket socket=new Socket(SERVER,PORT);

            DataOutputStream dout=new DataOutputStream(socket.getOutputStream());
            DataInputStream din=new DataInputStream(socket.getInputStream());


            dout.writeUTF("Hello");
            dout.flush();

            System.out.println("send first mess");
            String str = din.readUTF();//in.readLine();

            System.out.println("Message"+str);


            dout.close();
            din.close();
            socket.close();
        }

        catch(Exception e){
            e.printStackTrace();
        }


    }

}

