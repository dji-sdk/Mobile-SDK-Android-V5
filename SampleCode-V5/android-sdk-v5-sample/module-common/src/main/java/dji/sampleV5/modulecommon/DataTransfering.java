package dji.sampleV5.modulecommon;

import android.util.Log;

import androidx.annotation.NonNull;

import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class DataTransfering{

    public static String DISCONNECT_MESSAGE = "!DISCONNECT";
    public static int headerLength = 64;
    public static Charset SELCHARSET = StandardCharsets.UTF_8;

    private Socket socket             = null;
    private DataInputStream  dIn      = null;
    private DataOutputStream dOut     = null;

    public static void SendMessageThread(String msg){

        Thread th = new Thread(() ->{
            SendMessageOld(msg);
        });
        th.start();
    }

    public DataTransfering(String serverIp, int port){
        Log.d("Sockets", "Server Attempting to connect: " + serverIp + " on Port: " + port);
        try{
            socket = new Socket(serverIp, port);
            Log.d("Sockets", "Connected!");

            dIn     = new DataInputStream(socket.getInputStream());
            dOut    = new DataOutputStream(socket.getOutputStream());

        } catch (IOException e) {
            Log.d("Sockets", e.toString());
        }

    }

    public void Disconnect(){
        SendMessage(DISCONNECT_MESSAGE);
        try
        {
            dIn.close();
            dOut.close();
            socket.close();
        }
        catch(IOException i)
        {
            Log.d("Sockets", i.toString());
        }

    }

    public static void SendMessageOld(String message){
        try {
            Socket socketStatic = new Socket("192.168.43.11", 2004);

            DataOutputStream dOut = new DataOutputStream(socketStatic.getOutputStream());
            DataInputStream dIn = new DataInputStream(socketStatic.getInputStream());

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

    public void SendMessage (String message){

        byte[] byteMessage = message.getBytes(SELCHARSET);
        byte[] headerBytes = getHeaderBytes(byteMessage);

        try {
            dOut.write(headerBytes);
            dOut.write(byteMessage);

            if(dIn.read() == 89){
                Log.d("Sockets", "Message received sucessfully");
            }

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
        for(int i = 0; i < headerLength; i++){
            spacesHeader += " ";
        }

        //Message length number is padded with empty UTF-8 (32 space) characters after number
        byte[] headerBytes = spacesHeader.getBytes(SELCHARSET);
        System.arraycopy(byteMsgLength, 0, headerBytes, 0, byteMsgLength.length);
        return headerBytes;
    }

}