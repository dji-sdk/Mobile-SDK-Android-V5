package dji.sampleV5.aircraft.utils.wpml;

import java.util.List;

import dji.sdk.wpmz.value.mission.WaylineActionInfo;
import dji.sdk.wpmz.value.mission.WaylineExecuteWaypoint;

/**
 * @author feel.feng
 * @time 2023/07/06 1:00 下午
 * @description:
 */
public class WaypointExecuteInfoModel {


    WaylineExecuteWaypoint waylineExecuteWaypoint;
    List<WaylineActionInfo> actionInfos;

    public WaylineExecuteWaypoint getWaylineWaypoint() {
        return waylineExecuteWaypoint;
    }

    public void setWaylineWaypoint(WaylineExecuteWaypoint waylineWaypoint) {
        this.waylineExecuteWaypoint = waylineWaypoint;
    }

    public List<WaylineActionInfo> getActionInfos() {
        return actionInfos;
    }

    public void setActionInfos(List<WaylineActionInfo> actionInfos) {
        this.actionInfos = actionInfos;
    }
}
