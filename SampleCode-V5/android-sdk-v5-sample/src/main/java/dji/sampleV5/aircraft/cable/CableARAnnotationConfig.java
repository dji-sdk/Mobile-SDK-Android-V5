package dji.sampleV5.aircraft.cable;

import androidx.annotation.NonNull;

import dji.open.pilot.base.manager.ar.ARFontGravity;

/**
 * 导线 AR 标注的可视化配置。
 */
public class CableARAnnotationConfig {
    // ── 文字标注（运行时可修改，修改后需调用 refreshCableARAnnotations 生效）──
    @NonNull public String highlightText;
    public int textColor;
    // ── 文字标注（静态配置）──
    public final float textSize;
    @NonNull public final ARFontGravity textGravity;
    public final boolean textShadow;
    public final int textBgColor;
    public final float textIconPx;
    // ── 方向箭头 ──
    public final int arrowColor;
    public final float arrowRealSizeM;
    public final float arrowMinPx;
    public final float arrowMaxPx;

    /** 使用全部默认值构造 */
    public CableARAnnotationConfig() {
        this("当前选中导线", 0xFF1E90FF, 36f, ARFontGravity.TOP, true,
                0x88141414, 30f, 0xFF1E90FF, 2.0f, 40f, 100f);
    }

    public CableARAnnotationConfig(@NonNull String highlightText, int textColor, float textSize,
                                   @NonNull ARFontGravity textGravity, boolean textShadow,
                                   int textBgColor, float textIconPx,
                                   int arrowColor, float arrowRealSizeM,
                                   float arrowMinPx, float arrowMaxPx) {
        this.highlightText = highlightText;
        this.textColor = textColor;
        this.textSize = textSize;
        this.textGravity = textGravity;
        this.textShadow = textShadow;
        this.textBgColor = textBgColor;
        this.textIconPx = textIconPx;
        this.arrowColor = arrowColor;
        this.arrowRealSizeM = arrowRealSizeM;
        this.arrowMinPx = arrowMinPx;
        this.arrowMaxPx = arrowMaxPx;
    }
}
