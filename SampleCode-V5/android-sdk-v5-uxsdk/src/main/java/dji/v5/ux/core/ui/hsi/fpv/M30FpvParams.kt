import dji.v5.ux.core.ui.hsi.fpv.IFPVParams

object M30FpvParams : IFPVParams {
    private const val FPV_FOCUS_X = 915.221845859551f * 0.75f
    private const val FPV_FOCUS_Y = 901.869063410212f * 0.75f
    private const val FPV_CENTER_X = 2560f / 2 * 0.75f
    private const val FPV_CENTER_Y = 1440f / 2 * 0.75f
    override fun getFocusX(): Float {
        return FPV_FOCUS_X
    }

    override fun getFocusY(): Float {
        return FPV_FOCUS_Y
    }

    override fun getCenterX(): Float {
        return FPV_CENTER_X
    }

    override fun getCenterY(): Float {
        return FPV_CENTER_Y
    }
}