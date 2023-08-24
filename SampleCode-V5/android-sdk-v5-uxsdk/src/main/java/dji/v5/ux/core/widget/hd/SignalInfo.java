package dji.v5.ux.core.widget.hd;

import java.util.Locale;

import dji.sdk.keyvalue.value.airlink.Bandwidth;
import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;

public class SignalInfo {
    private static final String FORMAT_STR = "%.2fMbps";
    public float strength;
    public Bandwidth bandwidth;
    public float dataRate;

    @Override
    public String toString() {

        String strengthStr = strength + "dBm";
        String bandwidthStr = "";
        if(bandwidth == Bandwidth.BANDWIDTH_10MHZ) {
            bandwidthStr = "10MHz";
        } else if(bandwidth == Bandwidth.BANDWIDTH_20MHZ) {
            bandwidthStr = "20MHZ";
        } else if(bandwidth == Bandwidth.BANDWIDTH_40MHZ) {
            bandwidthStr = "40MHZ";
        }
        String rateStr = String.format(Locale.US, FORMAT_STR, dataRate);

        return AndUtil.getResString(R.string.uxsdk_setting_ui_hd_sdr_channel_state_summary,strengthStr,bandwidthStr,rateStr);
    }
}
