package org.cryse.widget.persistentsearch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

public abstract class VoiceRecognitionDelegate {
    public static final int DEFAULT_VOICE_REQUEST_CODE = 8185102;
    private int mVoiceRecognitionRequestCode;
    private Activity mActivity;
    private Fragment mSupportFragment;

    public VoiceRecognitionDelegate(Activity activity) {
        this(activity, DEFAULT_VOICE_REQUEST_CODE);
    }

    public VoiceRecognitionDelegate(Activity activity, int activityRequestCode) {
        this.mActivity = activity;
        this.mVoiceRecognitionRequestCode = activityRequestCode;
    }

    public VoiceRecognitionDelegate(Fragment supportFragment) {
        this(supportFragment, DEFAULT_VOICE_REQUEST_CODE);
    }

    public VoiceRecognitionDelegate(Fragment supportFragment, int activityRequestCode) {
        this.mSupportFragment = supportFragment;
        this.mVoiceRecognitionRequestCode = activityRequestCode;
    }

    public void onStartVoiceRecognition() {
        if (mActivity != null) {
            Intent intent = buildVoiceRecognitionIntent();
            mActivity.startActivityForResult(intent, mVoiceRecognitionRequestCode);
        } else if(mSupportFragment != null) {
            Intent intent = buildVoiceRecognitionIntent();
            mSupportFragment.startActivityForResult(intent, mVoiceRecognitionRequestCode);
        }
    }

    protected Context getContext() {
        if (mActivity != null) {
            return mActivity;
        } else if(mSupportFragment != null) {
            return mSupportFragment.getContext();
        } else {
            throw new IllegalStateException("Could not get context in VoiceRecognitionDelegate.");
        }
    }

    public abstract Intent buildVoiceRecognitionIntent();

    public abstract boolean isVoiceRecognitionAvailable();
}
