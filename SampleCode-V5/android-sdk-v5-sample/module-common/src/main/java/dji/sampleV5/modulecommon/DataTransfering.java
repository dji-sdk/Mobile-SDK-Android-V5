package dji.sampleV5.modulecommon;

import android.util.Log;

import androidx.annotation.NonNull;

import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class DataTransfering{

    public static String DISCONNECT_MESSAGE = "!DISCONNECT";
    public static Socket socket = null;
    public static int headerLength = 64;
    public static Charset SELCHARSET = StandardCharsets.UTF_8;

    public static void SendMessageThread(String msg){

        Thread th = new Thread(() ->{
            DataTransfering.SendMessage(msg);
        });
        th.start();
    }

    public static void Connect(String serverIp, int port){
        Log.d("Sockets", "Server Attempting to connect: " + serverIp + " on Port: " + port);
        try{
            socket = new Socket(serverIp, port);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public static void Disconnect(){
        SendMessageThread(DISCONNECT_MESSAGE);
        try{
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void SendMessage (String message){

        try {
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
            DataInputStream dIn = new DataInputStream(socket.getInputStream());

            byte[] byteMessage = message.getBytes(SELCHARSET);
            byte[] headerBytes = getHeaderBytes(byteMessage);

            dOut.write(headerBytes);
            dOut.write(byteMessage);

            if(dIn.read() == 89){
                Log.d("Sockets", "Message received sucessfully");
            }

            dOut.close();
            dIn.close();

        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    @NonNull
    private static byte[] getHeaderBytes(byte[] byteMessage) {

        //Geting the length of the message
        int messageLength = byteMessage.length;
        byte[] byteMsgLength = String.valueOf(messageLength).getBytes(SELCHARSET);

        //Creates bytes of UTF-8 (space = 32) array of 64
        String spacesHeader = "";
        for(int i = 0; i <= headerLength-1; i++){
            spacesHeader += " ";
        }

        //Message length number is padded with empty UTF-8 (32 space) characters after number
        byte[] headerBytes = spacesHeader.getBytes(SELCHARSET);
        System.arraycopy(byteMsgLength, 0, headerBytes, 0, byteMsgLength.length);
        return headerBytes;
    }

}

