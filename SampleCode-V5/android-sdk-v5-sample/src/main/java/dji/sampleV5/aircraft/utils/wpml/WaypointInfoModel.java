package dji.sampleV5.aircraft.utils.wpml;

import java.util.List;

import dji.sdk.wpmz.value.mission.WaylineActionInfo;
import dji.sdk.wpmz.value.mission.WaylineWaypoint;

/**
 * @author feel.feng
 * @time 2023/07/05 5:31 下午
 * @description:
 */
public class WaypointInfoModel {

    WaylineWaypoint waylineWaypoint;
    List<WaylineActionInfo> actionInfos;

    public WaylineWaypoint getWaylineWaypoint() {
        return waylineWaypoint;
    }

    public void setWaylineWaypoint(WaylineWaypoint waylineWaypoint) {
        this.waylineWaypoint = waylineWaypoint;
    }

    public List<WaylineActionInfo> getActionInfos() {
        return actionInfos;
    }

    public void setActionInfos(List<WaylineActionInfo> actionInfos) {
        this.actionInfos = actionInfos;
    }

}
