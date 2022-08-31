package dji.sampleV5.modulecommon;

import static dji.sdk.keyvalue.value.common.LogModuleTag.FlightController;
import static dji.sdk.keyvalue.value.common.LogModuleTag.FlightHub;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.telecom.Call;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dji.sampleV5.modulecommon.R;
import dji.sampleV5.modulecommon.keyvalue.*;
import dji.sampleV5.modulecommon.keyvalue.KeyItemHelper;
import dji.sampleV5.modulecommon.models.KeyValueVM;
import dji.sampleV5.modulecommon.util.ToastUtils;
import dji.sampleV5.modulecommon.util.Util;
import dji.sdk.keyvalue.converter.EmptyValueConverter;

/*
https://developer.dji.com/api-reference/android-api/Components/KeyManager/DJIKey.html
 */
import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.BatteryKey;

//ieskau camera import, bet siti kazkode neveik, nenutuokiu kolkas kas kaip su kuom.
//import dji.sdk.camera
//import dji.sdk.camera.VideoFeed
//import dji.sdk.camera.VideoFeeder
//import dji.common.airlink
//import com.dji.sdk.sample.demo.camera;
//import dji.v5.manager.interfaces;
import dji.sdk.keyvalue.value.airlink.VideoSourceEntity;
import dji.v5.manager.video.channel.BaseVideoChannel;

import dji.sdk.keyvalue.value.airlink.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.v5.common.callback.CommonCallbacks.CompletionCallback;
import dji.v5.common.video.channel.VideoChannelType;
import dji.v5.common.video.interfaces.IVideoChannel;
import dji.v5.common.video.stream.StreamSource;
import dji.v5.manager.datacenter.video.StreamSourceListener;
import java.util.List;

import androidx.lifecycle.MutableLiveData;
import dji.sampleV5.modulecommon.data.VideoChannelInfo;
import dji.v5.common.video.interfaces.IVideoChannel;
import dji.v5.common.video.channel.VideoChannelState;
import dji.v5.common.video.interfaces.VideoChannelStateChangeListener;
import dji.v5.common.video.channel.VideoChannelType;
import dji.v5.manager.datacenter.MediaDataCenter;

import dji.v5.manager.interfaces.IVideoStreamManager;
import dji.v5.manager.video.DualChannelVideoStreamManagerDelegate;
import dji.v5.manager.video.channel.BaseVideoChannel;
import dji.v5.manager.interfaces.IMediaManager;

import dji.sdk.keyvalue.value.common.LocationCoordinate3D;
import dji.sdk.keyvalue.value.common.Velocity3D;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.v5.manager.KeyManager;
import dji.v5.manager.capability.CapabilityManager;
import dji.v5.utils.common.DjiSharedPreferencesManager;
import dji.v5.utils.common.LogUtils;
import dji.sdk.keyvalue.key.KeyTools;
import java.text.MessageFormat;

import dji.sampleV5.modulecommon.models.LiveStreamVM;
import dji.v5.common.callback.CommonCallbacks;
import androidx.lifecycle.MutableLiveData;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.common.video.channel.VideoChannelState;
import dji.v5.common.video.channel.VideoChannelType;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.manager.interfaces.ILiveStreamManager;
import dji.v5.manager.datacenter.livestream.*;
import dji.v5.manager.datacenter.livestream.settings.AgoraSettings;
import dji.v5.manager.datacenter.livestream.settings.GB28181Settings;
import dji.v5.manager.datacenter.livestream.settings.RtmpSettings;
import dji.v5.manager.datacenter.livestream.settings.RtspSettings;
import dji.v5.manager.datacenter.video.VideoStreamManager;
import dji.v5.utils.common.ContextUtil;
import dji.v5.utils.common.DjiSharedPreferencesManager;
import dji.sampleV5.modulecommon.models.VideoPlayVM;


import dji.v5.manager.datacenter.MediaDataCenter;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.*;
import android.view.*;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import dji.sampleV5.modulecommon.R;
import dji.sampleV5.modulecommon.models.MultiVideoChannelVM;
import dji.sampleV5.modulecommon.models.VideoChannelVM;
import dji.sampleV5.modulecommon.models.VideoChannelVMFactory;
import dji.sampleV5.modulecommon.util.ToastUtils;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.common.video.channel.VideoChannelState;
import dji.v5.common.video.channel.VideoChannelType;
import dji.v5.common.video.decoder.*;
import dji.v5.common.video.interfaces.*;
import dji.v5.common.video.stream.StreamSource;
import dji.v5.utils.common.ContextUtil;
import dji.v5.utils.common.DiskUtil;
import dji.v5.utils.common.LogUtils;
//import dji.sampleV5.modulecommon.pages;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.*;
import android.view.*;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import dji.sampleV5.modulecommon.R;
import dji.sampleV5.modulecommon.models.MultiVideoChannelVM;
import dji.sampleV5.modulecommon.models.VideoChannelVM;
import dji.sampleV5.modulecommon.models.VideoChannelVMFactory;
import dji.sampleV5.modulecommon.util.ToastUtils;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.common.video.channel.VideoChannelState;
import dji.v5.common.video.channel.VideoChannelType;
import dji.v5.common.video.decoder.*;
import dji.v5.common.video.interfaces.*;
import dji.v5.common.video.stream.StreamSource;
import dji.v5.utils.common.ContextUtil;
import dji.v5.utils.common.DiskUtil;
import dji.v5.utils.common.LogUtils;

import android.os.Bundle;
import android.view.*;
import android.widget.SeekBar;
import dji.sampleV5.modulecommon.R;
import dji.sampleV5.modulecommon.models.VideoPlayVM;
import dji.sampleV5.modulecommon.util.ToastUtils;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.common.video.channel.VideoChannelType;
import dji.v5.common.video.decoder.*;
import dji.v5.common.video.interfaces.*;
import dji.v5.manager.datacenter.media.MediaFile;
import dji.v5.manager.datacenter.media.MediaManager;
import dji.v5.manager.datacenter.media.VideoPlayState;
import java.io.*;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import dji.sampleV5.modulecommon.R;
import dji.sampleV5.modulecommon.models.MultiVideoChannelVM;
import dji.sampleV5.modulecommon.util.ToastUtils;
import dji.v5.common.video.interfaces.IVideoFrame;



public class DataStreamingController {

    public static String serverIp = "192.168.43.11";
    public static int port = 2004;

    private static DataTransfering dataTransfer = null;

    public static void SendTestDataTry() {
        try {
            DataStreamingController.SendTestData();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void ExecuteRepeaded() throws InterruptedException{
        for (int i = 0; i < 50; i++) {
            Thread thMessage = new Thread(() -> {
                getAircraftLocation();
                //dataTransfer.SendMessage(getAircraftLocation());
                //GetAndSendData();
            });
            thMessage.start();
            thMessage.join();
            Thread.sleep(1000);
        }
    }

    public static void SendTestData() throws InterruptedException {

        Thread thConnect = new Thread(() -> {
            dataTransfer = new DataTransfering(serverIp, port);
        });
        thConnect.start();
        thConnect.join();

        for (int i = 0; i < 50; i++) {
            Thread thMessage = new Thread(() -> {
                //dataTransfer.SendMessage(getAircraftLocation());
                //GetAndSendData();
            });
            thMessage.start();
            thMessage.join();
            Thread.sleep(1000);
        }
        Thread thDisconnect = new Thread(() -> {
            dataTransfer.Disconnect();
        });
        thDisconnect.start();
    }

    private static void GetAndSendData() {


/*
        KeyManager.getInstance().getValue(
                KeyTools.createKey()
                KeyTools.createKey(
                        CameraKey.KeyCameraType,
                        CapabilityManager.getInstance().componentIndex
                )
        );
*/

        //public LocationCoordinate3D getAircraftLocation ()


        dataTransfer.SendMessage("Hello world!");
    }


    public static void TestCamera(){
        //StreamSource.
        //Video
        //MediaDataCenter.getInstance().videoStreamManager.addStreamSourcesListener(streamSourcesListener);
        List<StreamSource> sources = MediaDataCenter.getInstance().getVideoStreamManager().getAvailableStreamSources();
        List<IVideoChannel> videoChannels = MediaDataCenter.getInstance().getVideoStreamManager().getAvailableVideoChannels();
        videoChannels.get(0).startChannel(sources.get(0), new CompletionCallback() {
            @Override
            public void onSuccess() {
                Log.d("VideoStream", "sucess");
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                Log.d("VideoStream", error.toString());
            }
        });

        VideoDecoder decoder = new VideoDecoder(
                null,
                VideoChannelType.PRIMARY_STREAM_CHANNEL,
                DecoderOutputMode.SURFACE_MODE, null
        );
        Log.d("stream", String.valueOf(decoder.getVideoHeight()));

        //IVideoFrame frame = IVideoFram

        //videoChannels.get(0).addStreamDataListener();
/*
        VideoDecoder decoder = new  VideoDecoder {
            this @VideoPlayFragment.context,
            VideoChannelType.PRIMARY_STREAM_CHANNEL,
                    DecoderOutputMode.SURFACE_MODE,
                    surfaceView.holder
        }
 */
        //VideoDecoder(Context context, VideoChannelType channelType, DecoderOutputMode outputMode, Object outputSurface, int width, int height, boolean isForLiveStream)

        //After decoder possible to get a frame
    }


    public static void TestStream(){
        //LiveStreamVM stream = new LiveStreamVM();
        ILiveStreamManager stream = LiveStreamManager.getInstance();

        RtmpSettings.Builder rtmpBuild = new RtmpSettings.Builder();
        rtmpBuild.setUrl("rtmp://192.168.43.11:1935"); // cia gal galima ir kitaip
        RtmpSettings rtmpSettings = rtmpBuild.build(); // build funkcijos gal reik gal ne, nezinau ar ji po setingu ar pries turi eit

        LiveStreamSettings.Builder liveStreamSettingsBuild = new LiveStreamSettings.Builder();
        liveStreamSettingsBuild.setLiveStreamType(LiveStreamType.RTMP);
        liveStreamSettingsBuild.setRtmpSettings(rtmpSettings);
        LiveStreamSettings liveStreamSettings = liveStreamSettingsBuild.build();

        stream.setLiveStreamSettings(liveStreamSettings);
        //stream.se("rtmp://192.168.43.11:1935");

        stream.startStream(new CompletionCallback() {
            @Override
            public void onSuccess() {
                Log.d("RTMPTransmission", "Transmitting via rtmp://192.168.43.11:1935");
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                Log.d("RTMPTransmission", error.toString());
            }
        });
    }


    public static String getAircraftLocation() {

        double heading = (double) KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyCompassHeading));
        double altitude = (double) KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyAltitude));

        Log.d("keyValueTesting", "H: " + String.valueOf(heading) + " A: " + String.valueOf(altitude));

        Velocity3D vel = (Velocity3D) KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity));
        if(vel != null){
            Log.d("keyValueTesting", vel.x.toString() + " " + vel.y.toString() + " " + vel.z.toString() +"\n");
        }


        //https://developer.dji.com/api-reference/android-api/Components/FlightController/DJIFlightController_DJILocationCoordinate3D.html#djiflightcontroller_djilocationcoordinate3d_constructor_inline
        LocationCoordinate3D loc = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D));

        if(loc != null){
            Log.d("keyValueTesting", loc.latitude.toString() + " " + loc.longitude + " " + loc.altitude + "\n");
        }



        //double lo = (double) KeyManager.getInstance().getValue(FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE));
        //double altitude = (double) KeyManager.getInstance().getValue(FlightControllerKey.create(FlightControllerKey.ALTITUDE));
        //return new LocationCoordinate3D(la, lo, altitude);

        /*String message = MessageFormat.format("Heading: {0} \n" +
                                                     "Altitude: {1} \n" +
                                                     "Lat: {2} \n" +
                                                     "Lon: {3}", heading, altitude, loc.latitude, loc.longitude);

        Log.d("messageTest", message);
        */

        return "";
    }
}
