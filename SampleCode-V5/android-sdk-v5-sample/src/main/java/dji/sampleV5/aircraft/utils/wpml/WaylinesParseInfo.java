package dji.sampleV5.aircraft.utils.wpml;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.wpmz.value.mission.Wayline;
import dji.sdk.wpmz.value.mission.WaylineExecuteMissionConfig;

/**
 * @author feel.feng
 * @time 2022/12/05 8:19 下午
 * @description:
 */
public class WaylinesParseInfo {


   private WaylineExecuteMissionConfig config;
   private List<Wayline> waylines ;

   public WaylinesParseInfo(){
      config = new WaylineExecuteMissionConfig();
      waylines = new ArrayList<>();
   }


   public WaylineExecuteMissionConfig getConfig() {
      return config;
   }

   public void setConfig(WaylineExecuteMissionConfig config) {
      this.config = config;
   }

   public List<Wayline> getWaylines() {
      return waylines;
   }

   public void setWaylines(List<Wayline> waylines) {
      this.waylines = waylines;
   }

}
