/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.core.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;

import dji.v5.utils.common.LogUtils;
import dji.v5.ux.core.base.SchedulerProvider;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Utility class for playing sounds.
 */
public final class AudioUtil {

    private static final float MIN_RATIO = 0.3f;
    private static MediaPlayer player;

    private AudioUtil() {
        // Util class
    }

    /**
     * Plays a sound.
     *
     * @param context A context object.
     * @param resID   The resource ID of the sound to play.
     */
    public static void playSound(Context context, int resID) {
        playSound(context, resID, false);
    }

    /**
     * Plays a sound.
     *
     * @param context        A context object.
     * @param resID          The resource ID of the sound to play.
     * @param ignoreWhenBusy If set to true, will do nothing if a sound is already being played.
     */
    public synchronized static void playSound(Context context, int resID, boolean ignoreWhenBusy) {
        try {
            if (player != null) {
                if (player.isPlaying()) {
                    if (ignoreWhenBusy) {
                        return;
                    }
                    player.stop();
                }
                player.release();
                player = null;
            }

            player = MediaPlayer.create(context, resID);
            player.setOnCompletionListener(mp -> {
                if (player != null) {
                    player.release();
                    player = null;
                }
            });
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            final float audioMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            final float audioCurrentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
            float volumeRatio = audioCurrentVolume / audioMaxVolume;
            if (volumeRatio < MIN_RATIO) {
                volumeRatio = MIN_RATIO;
            }
            player.setVolume(volumeRatio, volumeRatio);
            player.start();
        } catch (Exception e) {
            LogUtils.d("PlaySound", e.getMessage());
        }
    }

    /**
     * Plays a sound in the background.
     *
     * @param context A context object.
     * @param resID   The resource ID of the sound to play.
     */
    public static Disposable playSoundInBackground(final Context context, final int resID) {
        return Observable.just(true)
                .subscribeOn(SchedulerProvider.computation())
                .subscribe(aBoolean -> playSound(context, resID),
                        e -> LogUtils.d("PlaySound", e.getMessage()));
    }
}
