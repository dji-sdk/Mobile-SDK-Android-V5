package dji.sampleV5.modulecommon;

import android.provider.ContactsContract;

public class DataStreamingController {

    public static String serverIp = "192.168.1.11";
    public static int port = 2004;

    public static void SendTestData() throws InterruptedException {

        DataTransfering.Connect(serverIp, port);

        for (int i = 0; i <= 5; i++){
            Thread th = new Thread(() ->{
                DataTransfering.SendMessage("Hello world!");
            });
            th.start();
            th.join();
            Thread.sleep(2000);
        }
        DataTransfering.Disconnect();
    }

}
