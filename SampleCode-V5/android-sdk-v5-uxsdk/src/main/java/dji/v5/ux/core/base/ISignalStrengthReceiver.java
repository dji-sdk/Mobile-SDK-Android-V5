package dji.v5.ux.core.base;

import dji.sdk.keyvalue.value.mobilenetwork.LinkType;
import dji.v5.ux.core.widget.hd.SignalInfo;

public interface ISignalStrengthReceiver {
    void updateSignal(LinkType link, SignalInfo info);
}
