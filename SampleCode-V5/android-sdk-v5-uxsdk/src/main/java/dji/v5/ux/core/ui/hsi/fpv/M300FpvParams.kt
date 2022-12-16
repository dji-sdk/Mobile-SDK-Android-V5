
import dji.v5.ux.core.ui.hsi.fpv.IFPVParams

object M300FpvParams : IFPVParams {
    private const val FPV_FOCUS = 1716.12f
    private const val FPV_CENTER_X = 2028f
    private const val FPV_CENTER_Y = 1520f
    override fun getFocusX(): Float {
        return FPV_FOCUS
    }

    override fun getFocusY(): Float {
        return FPV_FOCUS
    }

    override fun getCenterX(): Float {
        return FPV_CENTER_X
    }

    override fun getCenterY(): Float {
        return FPV_CENTER_Y
    }
}