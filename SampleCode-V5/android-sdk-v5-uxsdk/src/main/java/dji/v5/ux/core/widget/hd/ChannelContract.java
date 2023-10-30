package dji.v5.ux.core.widget.hd;
import java.util.List;

public class ChannelContract {
    interface View {

        void updateSupportDataRates(List<String> bands);

        void initSlaveViews();

        void updateVideoCameras(boolean enable);

        void updateChannelMode(boolean auto);

    }
}
