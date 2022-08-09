package dji.sampleV5.modulecommon;

import android.provider.ContactsContract;

public class DataStreamingController {

    public static String serverIp = "192.168.43.11";
    public static int port = 2004;

    private static DataTransfering dataTransfer = null;

    public static void SendTestData() throws InterruptedException {

        Thread thConnect = new Thread(() ->{
            dataTransfer = new DataTransfering(serverIp, port);
        });
        thConnect.start();
        thConnect.join();

        for (int i = 0; i < 5; i++){
            Thread thMessage = new Thread(() ->{
                dataTransfer.SendMessage("Hello world!");
            });
            thMessage.start();
            thMessage.join();
            Thread.sleep(2000);
        }
        Thread thDisconnect = new Thread(() ->{
            dataTransfer.Disconnect();
        });
        thDisconnect.start();
    }

}
