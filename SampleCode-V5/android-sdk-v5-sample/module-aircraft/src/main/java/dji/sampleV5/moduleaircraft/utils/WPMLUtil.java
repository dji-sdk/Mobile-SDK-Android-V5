package dji.sampleV5.moduleaircraft.utils;

//import static dji.sampleV5.moduleaircraft.utils.wpml.WPMLConstants.PREFIX;
//import static dji.sampleV5.moduleaircraft.utils.wpml.WPMLValueConverter.convert;
//import static dji.sampleV5.moduleaircraft.utils.wpml.WPMLValueConverter.string2Double;
//import static dji.sampleV5.moduleaircraft.utils.wpml.WPMLValueConverter.string2Int;
//
//import android.content.Context;
//import android.text.TextUtils;
//
//import com.dji.industry.mission.DocumentsUtils;
//import com.dji.industry.mission.KMLZipHelper;
//
//
//
//import java.io.File;
//import java.io.IOException;
//import java.util.List;
//import java.util.regex.Pattern;
//
//import dji.sampleV5.moduleaircraft.utils.wpml.WPMLConstants;
//import dji.sampleV5.moduleaircraft.utils.wpml.WPMLException;
//import dji.sampleV5.moduleaircraft.utils.wpml.WPMLValueConverter;
//import dji.sampleV5.moduleaircraft.utils.wpml.WaylinesParseInfo;
//import dji.sdk.keyvalue.value.mission.WaypointMissionHeadingMode;
//import dji.sdk.keyvalue.value.payload.PayloadInfo;
//import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource;
//import dji.sdk.wpmz.value.mission.Wayline;
//import dji.sdk.wpmz.value.mission.WaylineDroneInfo;
//import dji.sdk.wpmz.value.mission.WaylineDroneType;
//import dji.sdk.wpmz.value.mission.WaylineExecuteMissionConfig;
//import dji.sdk.wpmz.value.mission.WaylineExecuteWaypoint;
//import dji.sdk.wpmz.value.mission.WaylineLocationCoordinate2D;
//import dji.sdk.wpmz.value.mission.WaylineLocationCoordinate3D;
//import dji.sdk.wpmz.value.mission.WaylinePayloadInfo;
//import dji.sdk.wpmz.value.mission.WaylinePayloadType;
//import dji.sdk.wpmz.value.mission.WaylineWaypointTurnParam;
//import dji.sdk.wpmz.value.mission.WaylineWaypointYawParam;
//import dji.v5.utils.common.ContextUtil;
//import dji.v5.utils.common.FileUtils;
//import dji.v5.utils.common.LogUtils;


/**
 * @author feel.feng
 * @time 2022/12/05 7:28 下午
 * @description:
 */
public class WPMLUtil {

    private WPMLUtil(){
    }
    //非法字符
    //public final static String ILLEGAL_EX = "[ _`~!@#$%^&*()+=|{}':;',\\[\\]\\\\.<>/?~！@#￥%……&*（）\"——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
    //public static final Pattern ILLEGAL_PATTERN = Pattern.compile(ILLEGAL_EX);
    /**
     * 导入KML文件
     */
//    public static WaylinesParseInfo importMission(Context context, String path) throws WPMLException, DocumentException {
//        int suffixIndex = path.lastIndexOf('.');
//        if (suffixIndex < 0) {
//            throw new WPMLException(WPMLConstants.NON_KML_FILE);
//        }
//
//        SAXReader saxReader = new SAXReader();
//        File f = new File(path);
//        //kmz
//        if (WPMLValueConverter.equals(path.substring(path.lastIndexOf('.')), WPMLConstants.KMZ_SUFFIX)) {
//            File tempUnzipPath = new File(f.getParent(), WPMLConstants.TEMP_FILE_DIR);
//            if (tempUnzipPath.exists()) {
//                FileUtils.delFile(tempUnzipPath);
//            }
//            tempUnzipPath.mkdirs();
//
//            KMLZipHelper.unZipFolder(context, path, tempUnzipPath.getAbsolutePath(), true);
//            File[] files = tempUnzipPath.listFiles();
//            for (File file : files) {
//                if (file.getName().endsWith(WPMLConstants.SUFFIX)) {
//                    f = file;
//                    path = f.getAbsolutePath();
//                } else if (file.getName().endsWith("jpg")) {
//                   // ignore
//                }
//            }
//        }
//
//        if (WPMLValueConverter.equals(path.substring(path.lastIndexOf('.')), WPMLConstants.SUFFIX)) {
//            String name = f.getName().substring(0, f.getName().lastIndexOf("."));
//            name = name.replaceAll(ILLEGAL_EX, "");
//
//            return parseWaypointKML(saxReader.read(f), name);
//        } else {
//            throw new WPMLException(WPMLConstants.NON_KML_FILE);
//        }
//    }
//
//
//    public static void exportMission(WaylinesParseInfo mission) {
//        Document kml  = createKML(mission);
//        OutputFormat format = OutputFormat.createPrettyPrint();
//        format.setEncoding(WPMLConstants.ENCODING);
//        String fileFullPath = "/sdcard/FEEL/waylines1.wpml";
//
//        try {
//            XMLWriter writer = new XMLWriter(DocumentsUtils.getOutputStream(ContextUtil.getContext(), new File(fileFullPath)), format);
//            writer.write(kml);
//            writer.flush();
//            writer.close();
//        }catch (Exception e ) {
//            LogUtils.e("WPMLUtil" , e.getMessage());
//        }
//
//
//    }
//
//
//    /**
//     * 创建航线对应的 KML 文件
//     *
//     * @param mission 需要导出的Mission
//     */
//    private static Document createKML(WaylinesParseInfo mission) {
//        Document doc = DocumentHelper.createDocument();
//        // root
//        Element kml = doc.addElement(WPMLConstants.ROOT);
//        kml.addNamespace("", WPMLConstants.NAMESPACE);
//        kml.addNamespace("wpml" , WPMLConstants.NAME_SPACE_EX);
//
//        Element document = kml.addElement(WPMLConstants.DOCUMENT);
//
//
//        //missionconfig
//        Element missionConfig = document.addElement(PREFIX + WPMLConstants.MISSION_CONFIG);
//        missionConfig.addElement(PREFIX+ WPMLConstants.FLY_TO_WAYLINE_MODE).addText(convert(mission.getConfig().getFlyToWaylineMode()));
//        missionConfig.addElement(PREFIX+ WPMLConstants.FINISH_ACTION).addText(convert(mission.getConfig().getFinishAction()));
//        missionConfig.addElement(PREFIX + WPMLConstants.EXIT_ON_RC_LOST ).addText(convert(mission.getConfig().getExitOnRCLostBehavior()));
//        missionConfig.addElement(PREFIX + WPMLConstants.TAKE_OFF_SECURITY_HEIGHT).addText(String.valueOf(mission.getConfig().getSecurityTakeOffHeight()));
//        missionConfig.addElement(PREFIX + WPMLConstants.GLOBAL_TRANSITION_SPEED).addText(String.valueOf(mission.getConfig().getGlobalTransitionalSpeed()));
//
//        //missionconfig droneInfo
//        Element droneInfoElement = missionConfig.addElement(PREFIX + WPMLConstants.DRONE_INFO);
//        droneInfoElement.addElement(PREFIX + WPMLConstants.DRONE_ENUM_VALUE).addText(String.valueOf(mission.getConfig().getDroneInfo().getDroneType().value()));
//        droneInfoElement.addElement(PREFIX + WPMLConstants.DRONE_SUB_ENUM_VALUE).addText(mission.getConfig().getDroneInfo().getDroneSubType().toString());
//
//        //missionconfig payloadInfo
//        for (WaylinePayloadInfo payloadInfo : mission.getConfig().getPayloadInfo()) {
//            Element payloadElement = missionConfig.addElement(PREFIX + WPMLConstants.PAYLOAD_INFO);
//            payloadElement.addElement(PREFIX + WPMLConstants.PAYLOAD_ENUM_VALUE).addText(String.valueOf(payloadInfo.getPayloadType().value()));
//            payloadElement.addElement(PREFIX + WPMLConstants.PAYLOAD_SUB_ENUM_VALUE).addText(payloadInfo.getPayloadSubType().toString());
//            payloadElement.addElement(PREFIX + WPMLConstants.PAYLOAD_POSITION_INDEX).addText(payloadInfo.getPayloadPositionIndex().toString());
//
//        }
//
//        for (Wayline wayline : mission.getWaylines()) {
//            Element folder = document.addElement(WPMLConstants.FOLDER);
//            folder.addElement(PREFIX + WPMLConstants.TEMPLATE_ID).addText(wayline.getTemplateId().toString());
//            folder.addElement(PREFIX + WPMLConstants.EXECUTE_HEIGHT_MODE).addText(convert(wayline.getMode()));
//            folder.addElement(PREFIX + WPMLConstants.WAYLINE_ID).addText(wayline.getWaylineId().toString());
//            folder.addElement(PREFIX + WPMLConstants.DISTANCE).addText(String.valueOf(wayline.getDistance()));
//            folder.addElement(PREFIX + WPMLConstants.DURATION).addText(String.valueOf(wayline.getDuration()));
//            folder.addElement(PREFIX + WPMLConstants.AUTO_FIGHT_SPEED).addText(String.valueOf(wayline.getAutoFlightSpeed()));
//
//
//           for (WaylineExecuteWaypoint waypoint : wayline.getWaypoints()) {
//               Element placeMarker = folder.addElement(  WPMLConstants.PLACE_MARK);
//               placeMarker.addElement(WPMLConstants.POINT).addElement(WPMLConstants.COORDINATES).addText(String.valueOf(waypoint.getLocation().getLongitude()) + ','
//                       + String.valueOf(waypoint.getLocation().getLatitude()));
//
//               placeMarker.addElement(PREFIX + WPMLConstants.POINT_INDEX).addText(String.valueOf(waypoint.getWaypointIndex()));
//               placeMarker.addElement(PREFIX + WPMLConstants.EXECUTE_HEIGHT).addText(String.valueOf(waypoint.getExecuteHeight()));
//               placeMarker.addElement(PREFIX + WPMLConstants.WAYPOINT_SPEED).addText(String.valueOf(waypoint.getSpeed()));
//
//               //headingParam
//               Element headingParam = placeMarker.addElement(PREFIX + WPMLConstants.WAYPOINT_HEADING_PARAM);
//               headingParam.addElement(PREFIX + WPMLConstants.WAYPOINT_HEADING_MODE).addText(convert(waypoint.getYawParam().getYawMode()));
//               headingParam.addElement(PREFIX + WPMLConstants.WAYPOINT_HEADING_ANGLE).addText(String.valueOf(waypoint.getYawParam().getYawAngle()));
//               headingParam.addElement(PREFIX + WPMLConstants.WAYPOINT_POI_POINT).addText(String.valueOf(waypoint.getYawParam().getPoiLocation().getLongitude()) + ','
//                       + String.valueOf(waypoint.getYawParam().getPoiLocation().getLatitude()) + ','
//                       + String.valueOf(waypoint.getYawParam().getPoiLocation().getAltitude()));
//
//               headingParam.addElement(PREFIX + WPMLConstants.WAYPOINT_HEADING_ANGLE_ENABLE).addText(waypoint.getYawParam().getEnableYawAngle() ? "1":"0");
//
//               //turnParam
//               Element turnParam = placeMarker.addElement(PREFIX + WPMLConstants.WAYPOINT_TURN_PARAM);
//               turnParam.addElement(PREFIX + WPMLConstants.WAYPOINT_TURN_MODE).addText(convert(waypoint.getTurnParam().getTurnMode()));
//               turnParam.addElement(PREFIX + WPMLConstants.WAYPOINT_TURN_DIST).addText(String.valueOf(waypoint.getTurnParam().getTurnDampingDistance()));
//
//
//               //useStraightLine
//               placeMarker.addElement(PREFIX + WPMLConstants.USE_STRIGHT_LINE).addText(waypoint.getUseStraightLine() ? "1" : "0");
//
//           }
//
//        }
//
//
//
//
//        return doc;
//    }
//
//
//    private static WaylinesParseInfo parseWaypointKML(Document kml, String name) throws WPMLException {
//        Element document = kml.getRootElement().element(WPMLConstants.DOCUMENT);
//        if (document == null ) {
//            throw new WPMLException(WPMLConstants.MISSION_TYPE_ILLEGAL);
//        }
//        if (name.length() > WPMLConstants.MAX_MISSION_NAME_LENGTH) {
//            name = name.substring(0, WPMLConstants.MAX_MISSION_NAME_LENGTH);
//        }
//        // mission data to be set
//        WaylinesParseInfo waypointMissionModel = new WaylinesParseInfo();
//
//
//
//        List<Element> docElements = document.elements();
//        for (Element element : docElements) {
//            // MissionInfo 的扩展数据
//            switch (element.getName()) {
//                case WPMLConstants.MISSION_CONFIG:
//                    parseMissionConfig(element , waypointMissionModel.getConfig());
//                    break;
//                case WPMLConstants.FOLDER:
//                    waypointMissionModel.getWaylines().add( parseFolder(element));
//                    break;
//                default:
//            }
//        }
//
//        return waypointMissionModel;
//    }
//
//    private static Wayline parseFolder(Element element ){
//        Wayline wayline = new Wayline();
//        for (Element missionData : element.elements()) {
//            String elementName = missionData.getName();
//            String value = missionData.getTextTrim();
//            switch (elementName) {
//                case WPMLConstants.TEMPLATE_ID:
//                    wayline.setTemplateId(string2Int(value));
//                    break;
//                case WPMLConstants.EXECUTE_HEIGHT_MODE:
//                    wayline.setMode(WPMLValueConverter.getExecuteAltitudeMode(value));
//                    break;
//                case WPMLConstants.WAYLINE_ID:
//                    wayline.setWaylineId(string2Int(value));
//                    break;
//                case WPMLConstants.DISTANCE:
//                    wayline.setDistance(string2Double(value));
//                    break;
//                case WPMLConstants.DURATION:
//                    wayline.setDuration(string2Double(value));
//                    break;
//                case WPMLConstants.AUTO_FIGHT_SPEED:
//                    wayline.setAutoFlightSpeed(string2Double(value));
//                    break;
//                case WPMLConstants.PLACE_MARK:
//                    parsePlaceMark(missionData , wayline);
//                    break;
//                default:
//                    break;
//            }
//
//        }
//
//        return wayline;
//
//    }
//
//    private static void parsePlaceMark(Element element , Wayline wayline) {
//        WaylineExecuteWaypoint waypoint = new WaylineExecuteWaypoint();
//        for (Element missionData : element.elements()) {
//            String elementName = missionData.getName();
//            String value = missionData.getTextTrim();
//
//            switch (elementName) {
//                case WPMLConstants.POINT:
//                    parsePointData(missionData , waypoint);
//                    break;
//                case WPMLConstants.POINT_INDEX:
//                    waypoint.setWaypointIndex(string2Int(value));
//                    break;
//                case WPMLConstants.EXECUTE_HEIGHT:
//                    waypoint.setExecuteHeight(string2Double(value));
//                    break;
//                case WPMLConstants.WAYPOINT_SPEED:
//                    waypoint.setSpeed(string2Double(value));
//                    break;
//                case WPMLConstants.USE_STRIGHT_LINE:
//                    waypoint.setUseStraightLine(string2Int(value) == 1);
//                    break;
//                case WPMLConstants.WAYPOINT_HEADING_PARAM:
//                    parseHeadingParam(missionData , waypoint);
//                    break;
//
//                case WPMLConstants.WAYPOINT_TURN_PARAM:
//                    parseTurnParam(missionData , waypoint);
//                    break;
//                default:
//                    break;
//            }
//
//
//        }
//
//        wayline.getWaypoints().add(waypoint);
//
//
//    }
//
//    private static void parseTurnParam(Element element, WaylineExecuteWaypoint waypoint) {
//
//        WaylineWaypointTurnParam turnParam = new WaylineWaypointTurnParam();
//        waypoint.setTurnParam(turnParam);
//        for (Element missionData : element.elements()) {
//            String elementName = missionData.getName();
//            String value = missionData.getTextTrim();
//            switch (elementName) {
//                case WPMLConstants.WAYPOINT_TURN_MODE:
//                    turnParam.setTurnMode(WPMLValueConverter.getWaypointTurnMode(value));
//                    break;
//                case WPMLConstants.WAYPOINT_TURN_DIST:
//                    turnParam.setTurnDampingDistance(string2Double(value));
//                    break;
//                default:
//                    break;
//            }
//        }
//    }
//
//    private static void parseHeadingParam(Element element, WaylineExecuteWaypoint waypoint) {
//        WaylineWaypointYawParam headingParam = new WaylineWaypointYawParam();
//        waypoint.setYawParam(headingParam);
//        for (Element missionData : element.elements()) {
//            String elementName = missionData.getName();
//            String value = missionData.getTextTrim();
//            switch (elementName) {
//                case  WPMLConstants.WAYPOINT_HEADING_MODE:
//                    headingParam.setYawMode(WPMLValueConverter.getWaypointHeadingMode(value));
//                    break;
//                case WPMLConstants.WAYPOINT_HEADING_ANGLE:
//                    headingParam.setYawAngle(string2Double(value));
//                    break;
//                case WPMLConstants.WAYPOINT_POI_POINT:
//
//                    headingParam.setPoiLocation(parsePoiPointData(missionData));
//                    break;
//                case WPMLConstants.WAYPOINT_HEADING_ANGLE_ENABLE:
//                    headingParam.setEnableYawAngle(string2Int(value) == 1);
//                    break;
//                default:
//                    break;
//
//            }
//
//        }
//    }
//
//    private static void parsePointData(Element element, WaylineExecuteWaypoint waypoint) {
//        if (element == null) {
//            return;
//        }
//        if (element != null) {
//            Element poiCoor = element.element(WPMLConstants.COORDINATES);
//            if (poiCoor != null && !TextUtils.isEmpty(poiCoor.getTextTrim())) {
//                String[] coordinates = poiCoor.getTextTrim().split(",");
//                if (coordinates.length == 3 || coordinates.length == 2) {
//                    try {
//                        double longitude = WPMLValueConverter.string2Double(coordinates[0]);
//                        double latitude = WPMLValueConverter.string2Double(coordinates[1]);
//                        waypoint.setLocation(new WaylineLocationCoordinate2D(latitude , longitude));
//                    } catch (NumberFormatException e) {
//                      LogUtils.e("WPMLUTIL" , e.getMessage());
//                    }
//                }
//            }
//        }
//    }
//
//    private static WaylineLocationCoordinate3D parsePoiPointData(Element element) {
//
//
//        if (element != null && !TextUtils.isEmpty(element.getTextTrim())) {
//            String[] coordinates = element.getTextTrim().split(",");
//            if (coordinates.length == 3) {
//                try {
//                    double longitude = WPMLValueConverter.string2Double(coordinates[0]);
//                    double latitude = WPMLValueConverter.string2Double(coordinates[1]);
//                    double height = WPMLValueConverter.string2Double(coordinates[2]);
//                    return new WaylineLocationCoordinate3D(latitude, longitude, height);
//                } catch (NumberFormatException e) {
//                    LogUtils.e("WPMLUTIL", e.getMessage());
//                }
//            }
//        }
//
//        return null;
//    }
//
//    private static void parseMissionConfig(Element element, WaylineExecuteMissionConfig config){
//        if (element == null) {
//            return;
//        }
//
//        for (Element missionData : element.elements()) {
//            String elementName = missionData.getName();
//            String value = missionData.getTextTrim();
//            if (elementName.equals(WPMLConstants.FLY_TO_WAYLINE_MODE)) {
//                config.setFlyToWaylineMode(WPMLValueConverter.getFlyToWaylineMode(value));
//            } else if (WPMLConstants.FINISH_ACTION.equals(elementName)) {
//               config.setFinishAction(WPMLValueConverter.getFinishAction(value));
//            } else if (WPMLConstants.EXIT_ON_RC_LOST.equals(elementName)) {
//                config.setExitOnRCLostBehavior(WPMLValueConverter.getRcLostBehavior(value));
//            } else if (WPMLConstants.EXECUTION_RC_LOST_ACTION.equals(elementName)) {
//                config.setExitOnRCLostType(WPMLValueConverter.getRcLostAction(value));
//            } else if (WPMLConstants.TAKE_OFF_SECURITY_HEIGHT.equals(elementName)) {
//                config.setSecurityTakeOffHeight(WPMLValueConverter.string2Double(value));
//            } else if (WPMLConstants.GLOBAL_TRANSITION_SPEED.equals(elementName)) {
//                config.setGlobalTransitionalSpeed(WPMLValueConverter.string2Double(value));
//            } else if (WPMLConstants.DRONE_INFO.equals(elementName)) {
//
//                parseDroneInfo(missionData , config);
//            } else if (WPMLConstants.PAYLOAD_INFO.equals(elementName)) {
//                config.getPayloadInfo().add( parsePayloadInfo(missionData));
//
//            }
//
//        }
//    }
//
//    private static WaylinePayloadInfo parsePayloadInfo(Element missionData) {
//        WaylinePayloadInfo payloadInfo = new WaylinePayloadInfo();
//        for (Element infoData: missionData.elements()) {
//            String elementName = infoData.getName();
//            String value = infoData.getTextTrim();
//            switch (elementName) {
//                case WPMLConstants.PAYLOAD_ENUM_VALUE:
//                    payloadInfo.setPayloadType(WaylinePayloadType.find(string2Int(value)));
//                    break;
//                case WPMLConstants.PAYLOAD_SUB_ENUM_VALUE:
//                    payloadInfo.setPayloadSubType(string2Int(value));
//                    break;
//                case WPMLConstants.PAYLOAD_POSITION_INDEX:
//                    payloadInfo.setPayloadPositionIndex(string2Int(value));
//                    break;
//                default:
//                    break;
//            }
//        }
//        return payloadInfo;
//
//    }
//
//    private static void parseDroneInfo(Element missionData, WaylineExecuteMissionConfig config) {
//        WaylineDroneInfo droneInfo = new WaylineDroneInfo();
//        config.setDroneInfo(droneInfo);
//        for (Element infoData: missionData.elements()) {
//            String elementName = infoData.getName();
//            String value = infoData.getTextTrim();
//            switch (elementName) {
//                case WPMLConstants.DRONE_ENUM_VALUE:
//                    droneInfo.setDroneType(WaylineDroneType.find(string2Int(value)));
//                    break;
//                case WPMLConstants.DRONE_SUB_ENUM_VALUE:
//                    droneInfo.setDroneSubType((string2Int(value)));
//                    break;
//                default:
//                    break;
//            }
//        }
//    }
}
