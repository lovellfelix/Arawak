package com.lovellfelix.caribbeannewsstand.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.lovellfelix.caribbeannewsstand.Constants;
import com.lovellfelix.caribbeannewsstand.R;
import com.lovellfelix.caribbeannewsstand.utils.UiUtils;

public abstract class BaseActivity extends Activity {

    public interface OnFullScreenListener {
        public void onFullScreenEnabled(boolean isImmersive);

        public void onFullScreenDisabled();
    }

    private static final String STATE_IS_FULLSCREEN = "STATE_IS_FULLSCREEN";

    private ProgressBar mProgressBar;
    private boolean mIsFullScreen;
    private View mDecorView;

    private OnFullScreenListener mFullScreenListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDecorView = getWindow().getDecorView();
        mDecorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) { // We are not un fullscreen mode

                            if (mIsFullScreen) { // It was fullscreen, we need to change it
                                toggleFullScreen();
                                mIsFullScreen = false;

                                if (mFullScreenListener != null) {
                                    mFullScreenListener.onFullScreenDisabled();
                                }
                            }
                        } else { // We are now in fullscreen mode
                            if (!mIsFullScreen) { // It was not-fullscreen, we need to change it
                                mIsFullScreen = true;

                                if (mFullScreenListener != null) {
                                    mFullScreenListener.onFullScreenEnabled(android.os.Build.VERSION.SDK_INT >= 19);
                                }
                            }
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        if (Constants.NOTIF_MGR != null) {
            Constants.NOTIF_MGR.cancel(0);
        }

        if (mIsFullScreen && getActionBar().isShowing()) { // This is needed for the immersive mode
            mIsFullScreen = false;
            toggleFullScreen();
        }

        super.onResume();
    }

    @Override
    public void setContentView(View view) {
        init().addView(view);
    }

    @Override
    public void setContentView(int layoutResID) {
        getLayoutInflater().inflate(layoutResID, init(), true);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        init().addView(view, params);
    }

    private ViewGroup init() {
        super.setContentView(R.layout.activity_progress);
        mProgressBar = (ProgressBar) findViewById(R.id.activity_bar);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            // Fix a display bug
            ((FrameLayout.LayoutParams) mProgressBar.getLayoutParams()).setMargins(0, UiUtils.dpToPixel(-4), 0, 0);
        }
        return (ViewGroup) findViewById(R.id.activity_frame);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_IS_FULLSCREEN, mIsFullScreen);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_IS_FULLSCREEN)) {
            toggleFullScreen();
        }

        super.onRestoreInstanceState(savedInstanceState);
    }

    public void setOnFullscreenListener(OnFullScreenListener listener) {
        mFullScreenListener = listener;
    }

    public ProgressBar getProgressBar() {
        return mProgressBar;
    }

    public boolean isFullScreen() {
        return mIsFullScreen;
    }

    @SuppressLint("InlinedApi")
    public void toggleFullScreen() {
        if (!mIsFullScreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            } else {
                mIsFullScreen = true;

                getActionBar().hide();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

                if (mFullScreenListener != null) {
                    mFullScreenListener.onFullScreenEnabled(false);
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else {
                mIsFullScreen = false;

                getActionBar().show();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                if (mFullScreenListener != null) {
                    mFullScreenListener.onFullScreenDisabled();
                }
            }
        }
    }
}