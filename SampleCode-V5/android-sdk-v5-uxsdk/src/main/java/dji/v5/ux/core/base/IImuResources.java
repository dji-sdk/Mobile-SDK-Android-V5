package dji.v5.ux.core.base;


import dji.v5.ux.R;

/**
 * Created by gashion.fang on 2016/7/11.
 */
public class IImuResources {

    private IImuResources(){}

    public static final int MAX_DESC_COUNT = 3;

    public static final int[] RESIDS_PREPARE_DESC = new int[]{
            R.string.uxsdk_setting_ui_imu_prepare_desc1, R.string.uxsdk_setting_ui_imu_prepare_desc2, R.string.uxsdk_setting_ui_imu_prepare_desc3
    };

    public static final int[] RESIDS_PREPARE_DESC_REMOVE_DESC1 = new int[]{
            R.string.uxsdk_setting_ui_imu_prepare_desc2, R.string.uxsdk_setting_ui_imu_prepare_desc3
    };

    public static final int[] RESIDS_PROCESS_DESC = new int[]{
            R.string.uxsdk_setting_ui_imu_process_desc1, R.string.uxsdk_setting_ui_imu_process_desc2
    };

    public static final int[] RESIDS_AIRCRAFT_P4 = new int[]{
            R.drawable.uxsdk_setting_ui_imu_front, R.drawable.uxsdk_setting_ui_imu_behind, R.drawable.uxsdk_setting_ui_imu_right, R.drawable.uxsdk_setting_ui_imu_left,
            R.drawable.uxsdk_setting_ui_imu_top, R.drawable.uxsdk_setting_ui_imu_under
    };

    public static final int[] RESIDS_AIRCRAFT_P4S = new int[]{
            R.drawable.uxsdk_setting_ui_imu_p4s_front, R.drawable.uxsdk_setting_ui_imu_p4s_behind, R.drawable.uxsdk_setting_ui_imu_right, R.drawable.uxsdk_setting_ui_imu_left,
            R.drawable.uxsdk_setting_ui_imu_top, R.drawable.uxsdk_setting_ui_imu_under
    };

    public static final int[] RESIDS_AIRCRAFT_KUMQUAT = new int[]{
            R.drawable.uxsdk_setting_ui_kumquat_imucali_front, R.drawable.uxsdk_setting_ui_kumquat_imucali_front, R.drawable.uxsdk_setting_ui_kumquat_imucali_right,
            R.drawable.uxsdk_setting_ui_kumquat_imucali_left, R.drawable.uxsdk_setting_ui_kumquat_imucali_top, R.drawable.uxsdk_setting_ui_kumquat_imucali_underside
    };

    public static final int[] RESIDS_AIRCRAFT_N3 = new int[]{
            R.drawable.uxsdk_setting_ui_imu_front_n3, R.drawable.uxsdk_setting_ui_imu_behind_n3, R.drawable.uxsdk_setting_ui_imu_right_n3,
            R.drawable.uxsdk_setting_ui_imu_left_n3, R.drawable.uxsdk_setting_ui_imu_top_n3, R.drawable.uxsdk_setting_ui_imu_under_n3
    };

    public static final int[] RESIDS_AIRCRAFT_M600 = new int[]{
            R.drawable.uxsdk_setting_ui_imu_front_m600, R.drawable.uxsdk_setting_ui_imu_below_m600, R.drawable.uxsdk_setting_ui_imu_right_m600,
            R.drawable.uxsdk_setting_ui_imu_left_m600, R.drawable.uxsdk_setting_ui_imu_top_m600, R.drawable.uxsdk_setting_ui_imu_top_m600};

    public static final int[] RESIDS_AIRCRAFT_M600_PRO = new int[]{
            R.drawable.uxsdk_setting_ui_imu_front_m600_pro, R.drawable.uxsdk_setting_ui_imu_below_m600_pro, R.drawable.uxsdk_setting_ui_imu_right_m600_pro,
            R.drawable.uxsdk_setting_ui_imu_left_m600_pro, R.drawable.uxsdk_setting_ui_imu_top_m600_pro, R.drawable.uxsdk_setting_ui_imu_top_m600_pro};

    public static final int[] RESIDS_AIRCRAFT_ORANGE2 = new int[]{
            R.drawable.uxsdk_setting_ui_imu_front_in2, R.drawable.uxsdk_setting_ui_imu_behind_in2, R.drawable.uxsdk_setting_ui_imu_right_in2,
            R.drawable.uxsdk_setting_ui_imu_left_in2, R.drawable.uxsdk_setting_ui_imu_top_in2, R.drawable.uxsdk_setting_ui_imu_top_in2
    };

    public static final int[] RESIDS_AIRCRAFT_M210 = new int[]{
        R.drawable.uxsdk_setting_ui_imu_front_m210, R.drawable.uxsdk_setting_ui_imu_behind_m210, R.drawable.uxsdk_setting_ui_imu_right_m210,
        R.drawable.uxsdk_setting_ui_imu_left_m210, R.drawable.uxsdk_setting_ui_imu_top_m210, R.drawable.uxsdk_setting_ui_imu_top_m210
    };

    public static final int[] RESIDS_AIRCRAFT_M300 = new int[] {
        R.drawable.uxsdk_setting_ui_imu_front_m300, R.drawable.uxsdk_setting_ui_imu_behind_m300, R.drawable.uxsdk_setting_ui_imu_right_m300,
        R.drawable.uxsdk_setting_ui_imu_left_m300, R.drawable.uxsdk_setting_ui_imu_top_m300, R.drawable.uxsdk_setting_ui_imu_top_m300
    };

    public static final int[] RESIDS_AIRCRAFT_M320 = new int[] {
            R.drawable.uxsdk_setting_ui_imu_front_m320, R.drawable.uxsdk_setting_ui_imu_behind_m320, R.drawable.uxsdk_setting_ui_imu_right_m320,
            R.drawable.uxsdk_setting_ui_imu_left_m320, R.drawable.uxsdk_setting_ui_imu_top_m320, R.drawable.uxsdk_setting_ui_imu_top_m320
    };

    public static final int[] RESIDS_AIRCRAFT_M350 = new int[] {
            R.drawable.uxsdk_setting_ui_imu_front_m350, R.drawable.uxsdk_setting_ui_imu_behind_m350, R.drawable.uxsdk_setting_ui_imu_right_m350,
            R.drawable.uxsdk_setting_ui_imu_left_m350, R.drawable.uxsdk_setting_ui_imu_top_m350, R.drawable.uxsdk_setting_ui_imu_top_m350
    };

    public static final int[] RESIDS_AIRCRAFT_M3E = new int[]{
            R.drawable.uxsdk_setting_ui_imucali_front_m3e,
            R.drawable.uxsdk_setting_ui_imucali_front_m3e,
            R.drawable.uxsdk_setting_ui_imucali_right_m3e,
            R.drawable.uxsdk_setting_ui_imucali_left_m3e,
            R.drawable.uxsdk_setting_ui_imucali_top_m3e,
            R.drawable.uxsdk_setting_ui_imucali_underside_m3e
    };

    public static final int[] RESIDS_AIRCRAFT_M3T = new int[]{
            R.drawable.uxsdk_setting_ui_imucali_front_m3t,
            R.drawable.uxsdk_setting_ui_imucali_front_m3t,
            R.drawable.uxsdk_setting_ui_imucali_right_m3t,
            R.drawable.uxsdk_setting_ui_imucali_left_m3t,
            R.drawable.uxsdk_setting_ui_imucali_top_m3t,
            R.drawable.uxsdk_setting_ui_imucali_underside_m3t
    };

    public static final int[] RESIDS_AIRCRAFT_M3M = new int[]{
            R.drawable.uxsdk_setting_ui_imucali_front_m3t,
            R.drawable.uxsdk_setting_ui_imucali_front_m3t,
            R.drawable.uxsdk_setting_ui_imucali_right_m3t,
            R.drawable.uxsdk_setting_ui_imucali_left_m3t,
            R.drawable.uxsdk_setting_ui_imucali_top_m3t,
            R.drawable.uxsdk_setting_ui_imucali_underside_m3t
    };

    public static final int INDEX_SIDE_FRONT = 0x00; // 前面朝下
    public static final int INDEX_SIDE_BEHIND = 0x01; // 后面朝下
    public static final int INDEX_SIDE_RIGHT = 0x02; // 右边朝下
    public static final int INDEX_SIDE_LEFT = 0x03; // 左边朝下
    public static final int INDEX_SIDE_TOP = 0x04; // 底部朝下
    public static final int INDEX_SIDE_UNDER = 0x05; // 顶部朝下
    public static final int INDEX_SIDE_NONE = -1; // 初始值

    // 校准面的顺序，由于飞控给出来的0-5不符合实际的校准面顺序
    public static final int[] SIDE_SEQUENCE = new int[]{
            INDEX_SIDE_TOP, INDEX_SIDE_RIGHT, INDEX_SIDE_LEFT, INDEX_SIDE_FRONT, INDEX_SIDE_BEHIND, INDEX_SIDE_UNDER
    };

    // M2E校准面的顺序，由于飞控给出来的0-4不符合实际的校准面顺序
    public static final int[] SIDE_SEQUENCE_M2E = new int[]{
        INDEX_SIDE_TOP, INDEX_SIDE_LEFT, INDEX_SIDE_RIGHT, INDEX_SIDE_BEHIND, INDEX_SIDE_UNDER
    };
}
