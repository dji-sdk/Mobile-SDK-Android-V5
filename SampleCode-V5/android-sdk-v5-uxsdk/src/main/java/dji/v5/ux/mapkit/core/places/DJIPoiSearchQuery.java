package dji.v5.ux.mapkit.core.places;

import androidx.annotation.NonNull;

/**
 * Created by joeyang on 10/10/17.
 * POI检索条件的builder类
 */
public class DJIPoiSearchQuery {

    private final String keyWord;

    public DJIPoiSearchQuery(Builder builder) {
        this.keyWord = builder.keyWord;
    }

    public String keyWord() {
        return keyWord;
    }

    public static class Builder {

        private String keyWord = "";

        public Builder keyWord(@NonNull String keyWord) {
            this.keyWord = keyWord;
            return this;
        }

        public DJIPoiSearchQuery build() {
            return new DJIPoiSearchQuery(this);
        }
    }
}
