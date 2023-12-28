package dji.sampleV5.aircraft.utils.wpml;

import android.text.TextUtils;


import dji.sdk.wpmz.value.mission.WaylineExecuteAltitudeMode;
import dji.sdk.wpmz.value.mission.WaylineExitOnRCLostAction;
import dji.sdk.wpmz.value.mission.WaylineExitOnRCLostBehavior;
import dji.sdk.wpmz.value.mission.WaylineFinishedAction;
import dji.sdk.wpmz.value.mission.WaylineFlyToWaylineMode;
import dji.sdk.wpmz.value.mission.WaylineWaypointTurnMode;
import dji.sdk.wpmz.value.mission.WaylineWaypointYawMode;

/**
 * @author feel.feng
 * @time 2022/12/05 7:41 下午
 * @description: wayline中 枚举值与string之前的互相转换
 */
public class WPMLValueConverter {

    private WPMLValueConverter(){}


    public static boolean equals(String s1, String s2) {
        return !TextUtils.isEmpty(s1) && !TextUtils.isEmpty(s2) && s1.toLowerCase().equals(s2.toLowerCase());
    }


    public static WaylineFlyToWaylineMode getFlyToWaylineMode(String mode) {
        if (equals(mode , WPMLConstants.WAYLINE_MODE_SAFELY)) {
            return WaylineFlyToWaylineMode.SAFELY;
        }

        return WaylineFlyToWaylineMode.UNKNOWN;
    }

    public static WaylineFinishedAction getFinishAction(String action) {
        if (equals(action , WPMLConstants.FINISH_ACTION_GO_HOME)){
            return WaylineFinishedAction.GO_HOME;
        }
        return WaylineFinishedAction.UNKNOWN;
    }

    public static WaylineExitOnRCLostBehavior getRcLostBehavior(String lostBehavior) {
        if (equals(lostBehavior , WPMLConstants.RC_LOST_BEHAVIOR_VALUE_GO_CONTINUE)) {
            return WaylineExitOnRCLostBehavior.GO_ON;
        }

        return WaylineExitOnRCLostBehavior.UNKNOWN;
    }

    public static WaylineExitOnRCLostAction  getRcLostAction(String action) {
        if (equals(action , WPMLConstants.EXECUTION_RC_LOST_ACTION_VALUE_GO_BACK)) {
            return WaylineExitOnRCLostAction.GO_BACK;
        }
        return WaylineExitOnRCLostAction.GO_BACK;
    }

    public static WaylineExecuteAltitudeMode getExecuteAltitudeMode(String mode) {
        if (equals(mode , WPMLConstants.EXECUTE_HEIGHT_MODE_VALUE_RL_TO_START_POINT)) {
            return WaylineExecuteAltitudeMode.RELATIVE_TO_START_POINT;
        }
        return WaylineExecuteAltitudeMode.UNKNOWN;
    }


    public static WaylineWaypointTurnMode getWaypointTurnMode(String mode ) {
        if (equals(mode , WPMLConstants.WAYPOINT_TURN_MODE_VALUE_STOP_WITH_DIS_CURV)){
            return WaylineWaypointTurnMode.TO_POINT_AND_STOP_WITH_DISCONTINUITY_CURVATURE;
        }
        return WaylineWaypointTurnMode.UNKNOWN;
    }

    public static WaylineWaypointYawMode getWaypointHeadingMode(String mode){
        if (equals(mode , WPMLConstants.WAYPOINT_HEADING_MODE_VALUE_FOLLOW_WAYLINE)){
            return WaylineWaypointYawMode.FOLLOW_WAYLINE;
        }
        return WaylineWaypointYawMode.UNKNOWN;
    }


    public static int string2Int(String num) throws NumberFormatException {
        return Integer.parseInt(num);
    }

    public static double string2Double(String num) throws NumberFormatException {
        return Double.parseDouble(num);
    }

    public static float string2Float(String num) throws NumberFormatException {
        return Float.parseFloat(num);
    }


    public static String convert(WaylineFlyToWaylineMode mode) {
        switch (mode) {
            case SAFELY:
                return WPMLConstants.WAYLINE_MODE_SAFELY;
            default:
                return "";
        }
    }

    public static String convert(WaylineFinishedAction action) {
        switch (action) {
            case GO_HOME:
                return WPMLConstants.FINISH_ACTION_GO_HOME;
            default:
                return "";

        }
    }

    public static String convert(WaylineExitOnRCLostBehavior behavior) {
        switch (behavior){
            case GO_ON:
                return WPMLConstants.RC_LOST_BEHAVIOR_VALUE_GO_CONTINUE;
            default:
                return "";
        }
    }

    public static String convert(WaylineExecuteAltitudeMode mode) {
        switch (mode) {
            case RELATIVE_TO_START_POINT:
                return WPMLConstants.EXECUTE_HEIGHT_MODE_VALUE_RL_TO_START_POINT;
            default:
                return "";
        }
    }

    public static String convert(WaylineWaypointYawMode mode ) {
        switch (mode){
            case FOLLOW_WAYLINE:
                return WPMLConstants.WAYPOINT_HEADING_MODE_VALUE_FOLLOW_WAYLINE;
            default:
                return "";
        }
    }

    public static String convert(WaylineWaypointTurnMode mode ) {
        switch (mode) {
            case TO_POINT_AND_STOP_WITH_DISCONTINUITY_CURVATURE:
                return WPMLConstants.WAYPOINT_TURN_MODE_VALUE_STOP_WITH_DIS_CURV;
            default:
                return "";
        }
    }
}
