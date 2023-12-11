package dji.sampleV5.aircraft.utils.wpml;

/**
 * KML 文件的一些常量，包括元素、属性、和一些值
 *
 * @author bryan.jia
 */
public final class WPMLConstants {

    private WPMLConstants(){

    }
    // File related
    public final static String SUFFIX = ".wpml";
    public final static String DIR = "/KML/";
    public final static String ZIP_SUFFIX = ".zip";
    public final static String KMZ_SUFFIX = ".kmz";
    public final static String TEMP_FILE_DIR = "KMLTempFiles";
    public final static String ENCODING = "UTF-8";
    public final static int REQUEST_CODE = 1;

    // KML related
    public final static int DEFAULT_POI_HEIGHT = 50;
    public final static int MAX_MISSION_NAME_LENGTH = 60;
    public final static String ROOT = "kml";
    public final static String NAMESPACE = "http://www.opengis.net/kml/2.2";
    public final static String DOCUMENT = "Document";
    public final static String NAME = "name";
    public final static String OPEN = "open";
    public final static String DESCRIPTION = "description";
    public final static String ID = "id";



    public final static String MISSION_CONFIG = "missionConfig";
    public final static String POLYGON = "Polygon";
    public final static String OUTER_BOUNDARY_IS = "outerBoundaryIs";
    public final static String LINEAR_RING = "LinearRing";

    // Style
    public final static String STYLE = "Style";
    public final static String COLOR = "color";
    public final static String WIDTH = "width";
    public final static String LINE_STYLE = "LineStyle";
    public final static String ICON_STYLE = "IconStyle";
    public final static String POLY_STYLE = "PolyStyle";
    public final static String ICON = "Icon";
    public final static String HREF = "href";
    public final static String STYLE_URL = "styleUrl";
    public final static String STYLE_MARK = "#";

    // Values
    public final static String FOLDER_WAYPOINT_NAME = "Waypoints";
    public final static String FOLDER_WAYPOINT_DESC = "Waypoints in the Mission.";
    public final static String WAYPOINT_STYLE = "waypointStyle";
    public final static String WAYPOINT_HREF = "https://cdnen.dji-flighthub.com/static/app/images/point.png";
    public final static String WAYPOINT_DESC = "Waypoint";
    public final static String POI_DESC = "Poi";
    public final static String WAYLINE_STYLE = "waylineGreenPoly";
    public final static String WAYLINE_COLOR = "FF0AEE8B";
    public final static String WAYLINE_WIDTH = "6";
    public final static String WAYLINE_DESC = "Wayline";
    public final static String FIELD_STYLE = "fieldStyle";
    public final static String FIELD_STROKE_COLOR = "FF1FA3F6";
    public final static String FIELD_FILL_COLOR = "4D1088F2";
    public final static String FIELD_DESC = "Field";

    // Extended DATA,  MissionConfig


    public final static String EXTENDED_NAMESPACE = "www.dji.com";
    public final static String EXTENDED_NAMESPACE_PREFIX = "mis";
    public final static String MISSION_TYPE = "type";
    public final static String MISSION_TYPE_WAYPOINT = "Waypoint";
    public final static String MISSION_TYPE_MAPPING_2D = "Mapping2D";
    public final static String MISSION_TYPE_MAPPING_3D = "Mapping3D";
    public final static String MISSION_TYPE_STRIP = "MappingStrip";




    // Element
    // Waypoint Related


    // Wayline Related
    public final static String ALTITUDE = "altitude";
    public final static String TAKEOFF_SPEED = "takeoffSpeed";

    public final static String ACTION_ON_FINISH = "actionOnFinish";
    public final static String HEADING_MODE = "headingMode";
    public final static String GIMBAL_PITCH_MODE = "gimbalPitchMode";
    public final static String MAX_FLIGHT_SPEED = "maxFlightSpeed";
    public final static String GOTO_FIRST_POINT_MODE = "gotoFirstPointMode";
    public final static String FLIGHT_PATH_MODE = "flightPathMode";

    public final static String REPEAT_TIMES = "repeatTimes";
    public final static String USE_POINT_SETTING = "UsePointSetting";
    public final static String CONTROLLED_BY_RC = "ControlledByRC";
    public final static String USING_INITIAL_DIRECTION = "UsingInitialDirection";
    public final static String FINISHED_ACTION_HOVER = "Hover";
    public final static String FINISHED_ACTION_GO_HOME = "GoHome";
    public final static String FINISHED_ACTION_AUTO_LAND = "AutoLand";
    public final static String FINISHED_ACTION_GO_FIRST_POINT = "GoFirstPoint";
    public final static String HEADING_AUTO = "Auto";
    public final static String HEADING_TOWARDS_POI = "TowardsPoi";
    public final static String WAYLINE_WAYPOINT_TYPE = "waypointType";
    public final static String POWER_SAVE_MODE = "powerSaveMode";
    public final static String FLIGHT_CALI = "flightCali";


    public final static String DRONE_TYPE = "droneType";
    public final static String ADVANCE_SETTINGS = "advanceSettings";
    public final static String DRONE_CAMERAS = "droneCameras";
    public final static String DRONE_HEIGHT_MODE = "droneHeight";
    public final static String DRONE_CAMERA_INFO = "camera";
    public final static String DRONE_CAMERA_INDEX = "cameraIndex";
    public final static String DRONE_CAMERA_TYPE = "cameraType";
    public final static String DRONE_PAYLOAD_CAMERA_TYPE = "payloadCameraType";
    public final static String DRONE_PAYLOAD_CAMERA_CONFIG = "payloadCameraConfigInfo";
    public final static String DRONE_CAMERA_NAME = "cameraName";

    public final static String PAYLOAD_NAME = "payloadName";
    public final static String PAYLOAD_WIDGET = "payloadWidget";
    public final static String PAYLOAD_WIDGET_INFO = "payloadWidgetInfo";
    public final static String PAYLOAD_WIDGET_INDEX = "index";
    public final static String PAYLOAD_WIDGET_NAME = "name";
    public final static String PAYLOAD_WIDGET_TYPE = "type";
    public final static String PAYLOAD_WIDGET_VALUE = "value";
    public final static String PAYLOAD_WIDGET_VALUE_MIN = "minValue";
    public final static String PAYLOAD_WIDGET_VLAUE_MAX = "maxValue";
    public final static String DRONE_HEIGHT_USE_ABSOLUTE = "useAbsolute";
    public final static String DRONE_HEIGHT_HAS_TAKEOFF_HEIGHT = "hasTakeoffHeight";
    public final static String DRONE_HEIGHT_TAKEOFF_HEIGHT = "takeoffHeight";

    // Mapping Related
    public final static String DIRECTION = "direction";
    public final static String MARGIN = "margin";
    public final static String OVERLAP_W = "overlapW";
    public final static String OVERLAP_H = "overlapH";
    public final static String CAMERA_TYPE = "cameraType";
    public final static String FOCAL_LENGTH = "focalLength";
    public final static String SENSOR_W = "sensorW";
    public final static String SENSOR_H = "sensorH";
    public final static String IMAGE_W = "imageW";
    public final static String IMAGE_H = "imageH";
    public final static String SHOT_INTERVAL = "shotInterval";

    public final static String INCLINE_SPEED = "inclineSpeed";
    public final static String INCLINE_OVERLAP_W = "inclineOverlapW";
    public final static String INCLINE_OVERLAP_H = "inclineOverlapH";
    public final static String CUSTOMIZE_CAMERA = "other";
    public final static String ELEVATION_OPTI = "elevationOptimize";
    public final static String PHOTO_MODE = "photoMode";
    public final static String DEWARPING = "dewarping";
    public final static String FOCUS_MODE = "focusMode";
    public final static String LIDAR_SCAN_MODE = "lidarScanMode";
    public final static String LIDAR_ECHO_MODE = "lidarEchoMode";
    public final static String LIDAR_SAMPLE_RATE_MODE = "lidarSampleRateMode";
    public final static String LIDAR_NEED_VARIEGATION_MODE = "LidarNeedVariegationMode";
    public final static String METERING_MODE = "meteringMode";
    public final static String MAPPING_ALTITUDE_MODE = "mappingAltitudeMode";
    public final static String RELATIVE_DISTANCE = "relativeDistance";
    public final static String ENABLE_CALIBRATE = "enableCalibrate";
    public final static String LEFT_EXTAND_DISTANCE = "leftExtandDistance";
    public final static String RIGHT_EXTAND_DISTANCE = "rightExtandDistance";
    public final static String CUT_DISTANCE = "cutDistance";
    public final static String INCLUDE_CENTER_LINE = "includeCenterLine";
    public final static String PLAN_MODE = "planMode";
    public final static String ENABLE_SINGLE_LINE = "enableSingleLine";
    public final static String ENABLE_FIVE_WAY_POSE = "enableFiveWayPose";
    public final static String FIVE_WAY_POSE_WITH_GIMBAL_PITCH = "fiveWayPoseWithGimbalPitch";

    // Actions
    public final static String ACTIONS = "actions";
    public final static String PARAM = "param";
    public final static String ACTION_ACCURACY = "accuracy";
    public final static String ACTION_CAMERA_INDEX = "cameraIndex";
    public final static String ACTION_PAYLOAD_TYPE = "payloadType";
    public final static String ACTION_PAYLOAD_INDEX = "payloadIndex";
    public final static String ACTION_PRECISE_INFO_NAME = "preciseInfoName";
    public final static String ACTION_MEDIA_PATH_NAME = "mediaPathName";


    // Exceptions
    public final static String DOCUMENT_ELEMENT_NOT_EXIST = "Document element not exits";
    public final static String NAME_ELEMENT_NOT_EXIST = "Name element not exits";
    public final static String NAME_ELEMENT_HAS_NO_VALUE = "Name has no value";
    public final static String WAYPOINT_LOCATION_INVALID = "Waypoint location param is invalid";

    // 只有下面这些String会作为异常抛出去，需和资源ID一致
    public final static String PARAM_ILLEGAL = "mission_kml_invalid_format";
    public final static String INVALID_MISSION = "mission_kml_invalid_mission";
    public final static String NON_KML_FILE = "mission_kml_non_kml_file";
    public final static String MISSION_TYPE_ILLEGAL = "mission_list_type_wrong";
    public final static String LOCATION_ILLEGAL = "mission_list_points_loc_error";
    public final static String NO_WRITABLE_PERMISSION = "chooser_storage_not_writable";


    // LOG EXCEPTION 记录log用
    public final static String DIRECTION_PARAM_ILLEGAL = "DIRECTION param is illegal.";
    public final static String MARGIN_PARAM_ILLEGAL = "MARGIN param is illegal.";
    public final static String OVERLAP_H_PARAM_ILLEGAL = "OVERLAP_H param is illegal.";
    public final static String OVERLAP_W_PARAM_ILLEGAL = "OVERLAP_W param is illegal.";
    public final static String AUTO_FLIGHT_SPEED_PARAM_ILLEGAL = "SPEED param is illegal.";
    public final static String CAMERA_PARAM_ILLEGAL = "CAMERA params is illegal.";
    public final static String ALTITUDE_PARAM_ILLEGAL = "ALTITUDE param is illegal.";
    public final static String INCLINE_SPEED_PARAM_ILLEGAL = "INCLINE_SPEED param is illegal.";
    public final static String INCLINE_OVERLAP_H_PARAM_ILLEGAL = "INCLINE_OVERLAP_H param is illegal.";
    public final static String INCLINE_OVERLAP_W_PARAM_ILLEGAL = "INCLINE_OVERLAP_W param is illegal.";
    public final static String EDGE_POINTS_LOCATION_ILLEGAL = "EDGE_POINTS_LOCATION params is illegal.";

    public final static String TEXT_ACTION_SHOOT_PHOTO = "ShootPhoto";
    public final static String TEXT_ACTION_START_RECORDING = "StartRecording";
    public final static String TEXT_ACTION_STOP_RECORDING = "StopRecording";
    public final static String TEXT_ACTION_HOVERING = "Hovering";
    public final static String TEXT_ACTION_GIMBAL_PITCH = "GimbalPitch";
    public final static String TEXT_ACTION_AIRCRAFT_YAW = "AircraftYaw";
    public final static String TEXT_ACTION_GIMBAL_YAW = "GimbalYaw";
    public final static String TEXT_ACTION_CAMERA_ZOOM = "CameraZoom";
    public final static String TEXT_ACTION_CAMERA_FOCUS = "CameraFocus";
    public final static String TEXT_ACTION_TIME_INTERVAL_SHOT = "TimeIntervalShot";
    public final static String TEXT_ACTION_DISTANCE_INTERVAL_SHOT = "DistanceIntervalShot";
    public final static String TEXT_ACTION_STOP_INTERVAL_SHOT = "StopIntervalShot";
    public final static String TEXT_ACTION_PRECISE_SHOT = "PreciseShot";
    public final static String TEXT_ACTION_PAYLOAD_BUTTON = "PayloadButton";
    public final static String TEXT_ACTION_PAYLOAD_SWITCH_ON = "PayloadSwitchOn";
    public final static String TEXT_ACTION_PAYLOAD_SWITCH_OFF = "PayloadSwitchOff";
    public final static String TEXT_ACTION_PAYLOAD_SEEK = "PayloadSeek";
    public final static String TEXT_ACTION_START_POINT_CLOUD = "StartPointCloud";
    public final static String TEXT_ACTION_PAUSE_POINT_CLOUD = "PausePointCloud";
    public final static String TEXT_ACTION_CONTINUE_POINT_CLOUD = "ContinuePointCloud";
    public final static String TEXT_ACTION_FINISH_POINT_CLOUD = "FinishPointCloud";
    public final static String TEXT_ACTION_CAMERA_MKDIR = "CameraMkdir";

    public final static String TEXT_WAYPOINT_TYPE_CURVATURE_PASSED = "CurvaturePassed";
    public final static String TEXT_WAYPOINT_TYPE_CURVATURE_STOP = "CurvatureStop";
    public final static String TEXT_WAYPOINT_TYPE_LINE_STOP = "LineStop";
    public final static String TEXT_WAYPOINT_TYPE_COORDINATE_TURNING = "CoordinateTurning";
    public final static String TEXT_WAYPOINT_TYPE_STRAIGHT_IN = "StraightIn";
    public final static String TEXT_WAYPOINT_TYPE_STRAIGHT_OUT = "StraightOut";

    public final static String TEXT_DRONE_TYPE_P4R = "P4R";
    public final static String TEXT_DRONE_TYPE_COMMON = "COMMON";
    public static final String LABEL_STYLE = "LabelStyle";
    public static final String CLAMP_TO_GROUND = "ClampToGroud";
    public static final String RELATIVE_TO_SEA_FLOOR = "RelativeToSeaFloor";
    public static final String ALTITUDE_MODE_ABSOLUTE = "absolute";


    //--new
    public final static String NAME_SPACE_EX = "http://www.dji.com/wpmz/1.0.2";
    public final static String PREFIX = "wpml:";

    public final static String FLY_TO_WAYLINE_MODE = "flyToWaylineMode";
    public final static String FINISH_ACTION = "finishAction";
    public final static String EXIT_ON_RC_LOST = "exitOnRCLost"; //behavior
    public final static String EXECUTION_RC_LOST_ACTION = "executeRCLostAction";
    public final static String TAKE_OFF_SECURITY_HEIGHT = "takeOffSecurityHeight";
    public final static String GLOBAL_TRANSITION_SPEED = "globalTransitionalSpeed";
    public final static String DRONE_INFO = "droneInfo";
    public final static String DRONE_ENUM_VALUE = "droneEnumValue";
    public final static String DRONE_SUB_ENUM_VALUE = "droneSubEnumValue";

    public final static String PAYLOAD_INFO = "payloadInfo";
    public final static String PAYLOAD_ENUM_VALUE = "payloadEnumValue";
    public final static String PAYLOAD_SUB_ENUM_VALUE = "payloadSubEnumValue";
    public final static String PAYLOAD_POSITION_INDEX = "payloadPositionIndex";

    public final static String TEMPLATE_ID = "templateId";
    public final static String EXECUTE_HEIGHT_MODE = "executeHeightMode";
    public final static String WAYLINE_ID = "waylineId";
    public final static String DISTANCE = "distance";
    public final static String DURATION = "duration";
    public final static String AUTO_FIGHT_SPEED = "autoFlightSpeed";

    public final static String PLACE_MARK = "Placemark";
    public final static String FOLDER = "Folder";

    public final static String POINT = "Point";
    public final static String COORDINATES = "coordinates";
    public final static String POINT_INDEX = "index";
    public final static String EXECUTE_HEIGHT = "executeHeight";
    public final static String WAYPOINT_SPEED = "waypointSpeed";
    public final static String USE_STRIGHT_LINE = "useStraightLine";

    public final static String WAYPOINT_HEADING_PARAM = "waypointHeadingParam";
    public final static String WAYPOINT_HEADING_MODE = "waypointHeadingMode";
    public final static String WAYPOINT_HEADING_ANGLE = "waypointHeadingAngle";
    public final static String WAYPOINT_POI_POINT = "waypointPoiPoint";
    public final static String WAYPOINT_HEADING_ANGLE_ENABLE = "waypointHeadingAngleEnable";

    public final static String WAYPOINT_TURN_PARAM = "waypointTurnParam";
    public final static String WAYPOINT_TURN_MODE = "waypointTurnMode";
    public final static String WAYPOINT_TURN_DIST = "waypointTurnDampingDist";



    //value
    public final static String WAYLINE_MODE_SAFELY = "safely";
    public final static String FINISH_ACTION_GO_HOME = "goHome";
    public final static String RC_LOST_BEHAVIRO_LOST_ACTION = "executeLostAction";
    public final static String RC_LOST_BEHAVIOR_VALUE_GO_CONTINUE = "goContinue";

    public final static String EXECUTION_RC_LOST_ACTION_VALUE_GO_BACK = "goBack";

    public final static String EXECUTE_HEIGHT_MODE_VALUE_RL_TO_START_POINT = "relativeToStartPoint";

    //toPointAndStopWithDiscontinuityCurvature
    public final static String WAYPOINT_TURN_MODE_VALUE_STOP_WITH_DIS_CURV = "toPointAndStopWithDiscontinuityCurvature";
    public final static String WAYPOINT_HEADING_MODE_VALUE_FOLLOW_WAYLINE = "followWayline";


}
