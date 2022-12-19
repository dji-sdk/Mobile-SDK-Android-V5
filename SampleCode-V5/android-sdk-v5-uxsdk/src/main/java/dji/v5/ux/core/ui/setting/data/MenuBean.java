package dji.v5.ux.core.ui.setting.data;

/**
 * 设置页的图标类
 */
public class MenuBean {
    private int selectIcon;
    private int normalIcon;

    public int getSelectIcon() {
        return selectIcon;
    }

    public void setSelectIcon(int selectIcon) {
        this.selectIcon = selectIcon;
    }

    public int getNormalIcon() {
        return normalIcon;
    }

    public void setNormalIcon(int normalIcon) {
        this.normalIcon = normalIcon;
    }

    public MenuBean(int selectIcon, int normalIcon) {
        this.selectIcon = selectIcon;
        this.normalIcon = normalIcon;
    }
}