package dji.sampleV5.aircraft.cable;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.sdk.keyvalue.value.flightassistant.CableInspectionErrCode;
import dji.sdk.keyvalue.value.flightcontroller.CableArInfo;
import dji.v5.common.callback.CommonCallbacks;

/**
 * 导线巡检管理器接口。
 * <p>
 * 封装导线巡检相关的所有原生 Key 操作、业务逻辑和计算。
 * 参照 ICameraStreamManager 的设计模式：
 * <ul>
 *   <li>接口定义公开契约，实现类通过单例提供</li>
 *   <li>所有硬件 Key 订阅在 init / destroy 中统一管理</li>
 *   <li>支持检查相关 Key（飞控版本、相机型号/固件、地区码）全部收敛在内部，
 *       仅通过 {@link CableInspectionListener#onSupportStatusChanged} 对外暴露综合结果</li>
 *   <li>通过 {@link CableInspectionListener} 回调驱动上层，不依赖 LiveData</li>
 * </ul>
 */
public interface ICableInspectionManager {

    // ══════════════════════════════════════════════════════════════════
    // 生命周期
    // ══════════════════════════════════════════════════════════════════

    /**
     * 初始化 Manager，注册所有导线巡检相关的 Key 订阅。
     * 应在进入导线巡检页面时调用，通常对应 ViewModel.initListener()。
     * 内部按顺序执行：启动清理 CMD_EXIT → 启用电线 AR → 注册导线数据/状态/位置/支持检查 Key 订阅。
     */
    void init();

    /**
     * 销毁 Manager，注销所有 Key 订阅并执行兜底清理。
     * 应在 ViewModel.onCleared() 中调用。
     * 清理顺序：注销 Key 订阅 → CMD_EXIT → 关闭电线 AR → 重置内部状态。
     */
    void destroy();

    // ══════════════════════════════════════════════════════════════════
    // 命令方法
    // ══════════════════════════════════════════════════════════════════

    /**
     * 发送 CMD_EXIT，退出当前导线巡检任务。
     * 飞控处于 STANDBY 时跳过发送，直接调用 onSuccess。可安全重复调用。
     *
     * @param callback 命令结果回调，可为 null
     */
    void enterExitMissionState(@Nullable CommonCallbacks.CompletionCallback callback);

    /**
     * 发送 SELECT_CABLE 命令，进入选线识别状态。
     *
     * @param callback 命令结果回调，可为 null
     */
    void enterSelectCableState(@Nullable CommonCallbacks.CompletionCallback callback);

    /**
     * 发送 ADJUST_POS 命令，启动导线巡检第一阶段（飞向起始点）。
     * currentSelectedCableId < 0 时直接调用 onFailure 并返回。
     *
     * @param callback 命令结果回调，可为 null
     */
    void enterAdjustPosState(@Nullable CommonCallbacks.CompletionCallback callback);

    /**
     * 发送 BEGIN_FOLLOW 命令，启动导线巡检第二阶段（咬线跟随）。
     *
     * @param callback 命令结果回调，可为 null
     */
    void enterBeginFollowState(@Nullable CommonCallbacks.CompletionCallback callback);

    /**
     * 准备返回方向选择页。
     * 内部预设状态为 DIR（防止飞控 STANDBY 回推误触发异常退出）、重置巡检方向，
     * 然后发送 CMD_EXIT。调用方在回调后切换 UI 即可。
     *
     * @param callback CMD_EXIT 命令结果回调，可为 null
     */
    void prepareGoBack(@Nullable CommonCallbacks.CompletionCallback callback);

    // ══════════════════════════════════════════════════════════════════
    // 计算方法
    // ══════════════════════════════════════════════════════════════════

    /**
     * 动态计算推荐变焦倍率。
     *
     * @param flightDistance 飞行距离（m）
     * @return 推荐变焦倍率，默认 5.0
     */
    double calculateDefaultZoom(double flightDistance);

    /**
     * 计算拍照间隔。当间隔小于 200ms 时，返回结果中 {@link PhotoIntervalResult#speedAdjusted}
     * 为 true，{@link PhotoIntervalResult#adjustedSpeed} 为建议降速值，
     * 调用方可据此决定是否更新巡检速度。
     *
     * @param flightDistance  飞行距离（m）
     * @param zoomLevel       变焦倍率
     * @param inspectionSpeed 巡检速度（m/s）
     * @return 拍照间隔计算结果
     */
    @NonNull
    PhotoIntervalResult calculatePhotoInterval(double flightDistance, double zoomLevel, double inspectionSpeed);

    // ══════════════════════════════════════════════════════════════════
    // Listener
    // ══════════════════════════════════════════════════════════════════

    /** 添加导线巡检事件订阅 */
    void addCableInspectionListener(@NonNull CableInspectionListener listener);
    /** 移除导线巡检事件订阅 */
    void removeCableInspectionListener(@NonNull CableInspectionListener listener);

    /**
     * 导线巡检统一事件订阅接口。
     * 所有回调方法提供默认空实现，调用方只需 override 关心的回调。
     */
    interface CableInspectionListener {
        /**
         * App 侧巡检状态发生变化时回调。
         *
         * @param oldState 变化前的状态
         * @param newState 变化后的状态
         * @param errCode  伴随的飞控错误码，正常状态转换时为 null
         */
        default void onStateChanged(@NonNull InspectionState oldState,
                                    @NonNull InspectionState newState,
                                    @Nullable CableInspectionErrCode errCode) {}

        /**
         * 收到新的导线 AR 数据时回调。
         *
         * @param cables        当前识别到的导线列表
         * @param nearestCableId Manager 自动计算的距飞机最近导线 ID，无有效导线时为 -1
         */
        default void onCableArInfoUpdated(@NonNull List<CableArInfo> cables,
                                          int nearestCableId) {}

        /**
         * 功能支持状态发生变化时回调。
         *
         * @param detail 包含完整判断依据和最终结果
         */
        default void onSupportStatusChanged(@NonNull SupportDetail detail) {}

        /**
         * 收到飞控错误状态时回调（OK 和 USER_REQ_EXIT 不触发此回调）。
         *
         * @param errCode 飞控错误码枚举
         * @param message 对应的中文描述
         */
        default void onError(@NonNull CableInspectionErrCode errCode,
                             @NonNull String message) {}
    }

}
