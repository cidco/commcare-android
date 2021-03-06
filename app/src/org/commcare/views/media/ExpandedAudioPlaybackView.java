package org.commcare.views.media;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.commcare.dalvik.R;

import java.util.concurrent.TimeUnit;

/**
 * Audio playback widget with clickable horizontal progress bar
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public class ExpandedAudioPlaybackView extends AudioPlaybackButtonBase {
    private ProgressBar seekBar;
    private ObjectAnimator animation;
    private Handler handler;
    private TextView progressText;
    private int playbackDurationMillis;

    /**
     * Used by media inflater.
     */
    public ExpandedAudioPlaybackView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public ExpandedAudioPlaybackView(Context context, String URI, int questionIndex) {
        super(context, URI, ViewId.buildListViewId(questionIndex), true);
    }

    @Override
    protected void setupView(Context context) {
        super.setupView(context);
        progressText = (TextView)findViewById(R.id.duration_info);

        setupProgressBar();
        setupProgressAnimation();
    }

    private void setupProgressBar() {
        seekBar = (ProgressBar)findViewById(R.id.seek_bar);
        seekBar.setEnabled(true);
        seekBar.setOnTouchListener(
                new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return performProgressBarTouch(v, event);
                    }
                });
    }

    private void setupProgressAnimation() {
        final int startPosition = 0;
        animation = ObjectAnimator.ofInt(seekBar, "progress", startPosition, seekBar.getMax());
        animation.setInterpolator(new LinearInterpolator());
    }

    @Override
    protected int getLayout() {
        return R.layout.expanded_audio_playback;
    }

    @Override
    protected void startProgressBar(int currentPositionMillis, int milliDuration) {
        playbackDurationMillis = milliDuration;
        animation.setCurrentPlayTime(currentPositionMillis);
        animation.setDuration(playbackDurationMillis);
        animation.start();
        launchElapseTextUpdaterThread();
    }

    private boolean performProgressBarTouch(View v, MotionEvent event) {
        int progress = (int)(playbackDurationMillis * (event.getX() / v.getWidth()));
        animation.setCurrentPlayTime(progress);
        updateProgressText(progress, playbackDurationMillis);
        if (AudioController.INSTANCE.doesCurrentMediaCorrespondToButton(ExpandedAudioPlaybackView.this)) {
            AudioController.INSTANCE.seekTo(progress);
        }
        return true;
    }

    private void launchElapseTextUpdaterThread() {
        handler = new Handler();
        this.post(new Runnable() {

            @Override
            public void run() {
                // make sure we are playing this audio
                if (AudioController.INSTANCE.doesCurrentMediaCorrespondToButton(ExpandedAudioPlaybackView.this)) {
                    int pos = AudioController.INSTANCE.getCurrentPosition();
                    updateProgressText(pos, playbackDurationMillis);
                    handler.postDelayed(this, 20);
                }
            }
        });
    }

    @Override
    protected void resetProgressBar() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }

        if (animation != null) {
            animation.removeAllListeners();
            animation.end();
            animation.cancel();
        }

        if (seekBar != null) {
            seekBar.clearAnimation();
            seekBar.setProgress(0);
            updateProgressText(0, seekBar.getMax());
        }
    }

    @Override
    protected void pauseProgressBar() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        animation.pause();
    }

    private void updateProgressText(int progress, int max) {
        progressText.setText(milliToHumanReadable(progress) + " / " + milliToHumanReadable(max));
    }

    private static String milliToHumanReadable(int millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }
}
