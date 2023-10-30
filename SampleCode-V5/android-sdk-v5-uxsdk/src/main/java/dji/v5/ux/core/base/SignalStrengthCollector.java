package dji.v5.ux.core.base;

public interface SignalStrengthCollector {
    public void onCreate();

    public void onDestroy();

    public void setSignalStrengthReceiver(ISignalStrengthReceiver receiver);
}
