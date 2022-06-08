# Important Steps Before Run MSDK V5 On Your Own Project

## Step 1 : check packagingOptions on your project app build.gradle
Please compare with the file in Sample, like this : https://github.com/dji-sdk/Mobile-SDK-Android-V5/blob/dev-sdk-main/SampleCode-V5/android-sdk-v5-sample/app-aircraft/build.gradle .Then,check your packagingOptions with sample build.gradle's packagingOptions,rigth now it will look like this:

```groovy
packagingOptions {
    doNotStrip "*/*/libconstants.so"
    doNotStrip "*/*/libdji_innertools.so"
    doNotStrip "*/*/libdjibase.so"
    doNotStrip "*/*/libDJICSDKCommon.so"
    doNotStrip "*/*/libDJIFlySafeCore-CSDK.so"
    doNotStrip "*/*/libdjifs_jni-CSDK.so"
    doNotStrip "*/*/libDJIRegister.so"
    doNotStrip "*/*/libdjisdk_jni.so"
    doNotStrip "*/*/libDJIUpgradeCore.so"
    doNotStrip "*/*/libDJIUpgradeJNI.so"
    doNotStrip "*/*/libDJIWaypointV2Core-CSDK.so"
    doNotStrip "*/*/libdjiwpv2-CSDK.so"
    doNotStrip "*/*/libffmpeg.so"
    doNotStrip "*/*/libFlightRecordEngine.so"
    doNotStrip "*/*/libvideo-framing.so"
    doNotStrip "*/*/libwaes.so"
    doNotStrip "*/*/libagora-rtsa-sdk.so"
    doNotStrip "*/*/libc++.so"
    doNotStrip "*/*/libc++_shared.so"
    doNotStrip "*/*/libmrtc_28181.so"
    doNotStrip "*/*/libmrtc_agora.so"
    doNotStrip "*/*/libmrtc_core.so"
    doNotStrip "*/*/libmrtc_core_jni.so"
    doNotStrip "*/*/libmrtc_data.so"
    doNotStrip "*/*/libmrtc_log.so"
    doNotStrip "*/*/libmrtc_onvif.so"
    doNotStrip "*/*/libmrtc_rtmp.so"
    doNotStrip "*/*/libmrtc_rtsp.so"
}
```


## Step 2 : check proguard file on your app
All your need is in sample file :https://github.com/dji-sdk/Mobile-SDK-Android-V5/blob/dev-sdk-main/SampleCode-V5/android-sdk-v5-as/proguard-relative/sample.pro . Your should copy it to your own project.

## Step 3 : make sure use com.secneo.sdk.Helper.install(this)
Yous must add this code `com.secneo.sdk.Helper.install(this)` on your own `Application`. Your can refer to this : https://github.com/dji-sdk/Mobile-SDK-Android-V5/blob/dev-sdk-main/SampleCode-V5/android-sdk-v5-sample/app-aircraft/src/main/java/dji/sampleV5/aircraft/DJIAircraftApplication.kt .
