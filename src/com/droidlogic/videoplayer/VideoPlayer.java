package com.droidlogic.videoplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.Metadata;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
//import android.media.MediaPlayerAmlogic;
import android.net.Uri;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Message;
//import android.os.PowerManager;
//import android.os.PowerManager.WakeLock;
//import android.os.RemoteException;
//import android.os.ServiceManager;
import android.os.*;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.IWindowManager;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerPolicy;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.droidlogic.app.SubtitleManager;
import com.droidlogic.app.SystemControlManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import java.util.Random;

public class VideoPlayer extends Activity {
        private static String TAG = "VideoPlayer";
        private boolean DEBUG = false;
        private Context mContext;

        PowerManager.WakeLock mScreenLock = null;

        private boolean backToOtherAPK = true;
        private Uri mUri = null;
        private Map<String, String> mHeaders;
        private boolean mHdmiPlugged;

        private LinearLayout ctlbar = null; //for OSD bar layer 1; controller bar
        private LinearLayout optbar = null; //for OSD bar layer 2; option bar
        private LinearLayout subwidget = null; //for subtitle switch
        private LinearLayout otherwidget = null; //for audio track, resume play on/off, repeat mode, display mode
        private LinearLayout infowidget = null; //for video infomation showing

        private SeekBar progressBar; // all the follow for OSD bar layer 1
        private TextView curTimeTx = null;
        private TextView totalTimeTx = null;
        private ImageButton browserBtn = null;
        private ImageButton preBtn = null;
        private ImageButton fastreverseBtn = null;
        private ImageButton playBtn = null;
        private ImageButton nextBtn = null;
        private ImageButton fastforwordBtn = null;
        private ImageButton optBtn = null;

        private ImageButton ctlBtn = null; // all the follow for OSD bar layer 2
        private ImageButton resumeModeBtn = null;
        private ImageButton repeatModeBtn = null;
        private ImageButton audiooptionBtn = null;
        private ImageButton subtitleSwitchBtn = null;
        private ImageButton displayModeBtn = null;
        private ImageButton brigtnessBtn = null;
        private ImageButton fileinfoBtn = null;
        private ImageButton play3dBtn = null;
        private TextView otherwidgetTitleTx = null;
        private boolean progressBarSeekFlag = false;

        //certification view
        private ImageView certificationDoblyView = null;
        private ImageView certificationDoblyPlusView = null;
        private ImageView certificationDTSView = null;
        private ImageView certificationDTSExpressView = null;
        private ImageView certificationDTSHDMasterAudioView = null;

        //store index of file postion for back to file list
        private int item_position_selected; // for back to file list view
        private int item_position_first;
        private int fromtop_piexl;
        private int item_position_selected_init;
        private boolean item_init_flag = true;
        private ArrayList<Integer> fileDirectory_position_selected = new ArrayList<Integer>();
        private ArrayList<Integer> fileDirectory_position_piexl = new ArrayList<Integer>();

        // All the stuff we need for playing and showing a video
        private VideoView mVideoView;
        private View mVideoViewRoot;
        private SurfaceHolder mSurfaceHolder = null;
        private boolean surfaceDestroyedFlag = true;
        private int totaltime = 0;
        private int curtime = 0;
        //@@private int mVideoWidth;
        //@@private int mVideoHeight;
        //@@private int mSurfaceWidth;
        //@@private int mSurfaceHeight;
        private OnCompletionListener mOnCompletionListener;

        Option mOption = null;
        Bookmark mBookmark = null;
        ResumePlay mResumePlay = null;
        PlayList mPlayList = null;
        MediaInfo mMediaInfo = null;
        ErrorInfo mErrorInfo = null;

        private boolean browserBackDoing = false;
        private boolean browserBackInvokeFromOnPause = false; //browserBack invoked by back keyevent and browserBack button as usually, if invoked from OnPause suppose to meaning HOME key pressed
        private boolean playmode_switch = true;

        private float mTransitionAnimationScale = 1.0f;
        private long mResumeableTime = Long.MAX_VALUE;
        private int mVideoPosition = 0;
        private boolean mHasPaused = false;
        private boolean intouch_flag = false;
        private boolean set_3d_flag = false;

        private AudioManager mAudioManager;
        private boolean mAudioFocused = false;

        private SubtitleManager mSubtitleManager;
        private SystemControlManager mSystemControl;

        @Override
        public void onCreate (Bundle savedInstanceState) {
            super.onCreate (savedInstanceState);
            mSystemControl = new SystemControlManager(this);
            LOGI (TAG, "[onCreate]");
            setContentView (R.layout.control_bar);
            setTitle (null);
            mAudioManager = (AudioManager) this.getSystemService (Context.AUDIO_SERVICE);
            mScreenLock = ( (PowerManager) this.getSystemService (Context.POWER_SERVICE)).newWakeLock (
                              PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
            //uncaughtException execute
            /*Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                public void uncaughtException(Thread thread, Throwable ex) {
                    VideoPlayer.this.sendBroadcast(new Intent("com.droidlogic.videoplayer.PLAYER_CRASHED"));
                    LOGI(TAG,"----------------uncaughtException--------------------");
                    showSystemUi(true);
                    stop();
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            });*/
            init();
            if (0 != checkUri()) { return; }
            storeFilePos();
            ////showCtlBar();
        }

        @Override
        public void onResume() {
            super.onResume();
            LOGI (TAG, "[onResume]mResumePlay.getEnable():" + mResumePlay.getEnable() + ",isHdmiPlugged:" + isHdmiPlugged);
            //close transition animation
            // shield for google tv 20140929 , opened for bug 101311 20141224
            mTransitionAnimationScale = Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.TRANSITION_ANIMATION_SCALE, mTransitionAnimationScale);
            IWindowManager iWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            try {
                iWindowManager.setAnimationScale(1, 0.0f);
            }
            catch (RemoteException e) {
                LOGE(TAG, "[onResume]RemoteException e:" + e);
            }

            browserBackDoing = false;
            browserBackInvokeFromOnPause = false;
            //WakeLock acquire
            closeScreenOffTimeout();
            // Tell the music playback service to pause
            Intent intent = new Intent ("com.android.music.musicservicecommand");
            intent.putExtra ("command", "pause");
            mContext.sendBroadcast (intent);
            //init time store
            mErrorTime = java.lang.System.currentTimeMillis();
            mErrorTimeBac = java.lang.System.currentTimeMillis();
            if (mResumePlay != null && mPlayList != null) {
                if (true == mResumePlay.getEnable()) {
                    if (isHdmiPlugged == true) {
                        browserBack();
                        return;
                    }
                    String path = mResumePlay.getFilepath();
                    for (int i = 0; i < mPlayList.getSize(); i++) {
                        String tempP = mPlayList.get (i);
                        if (tempP.equals (path)) {
                            // find the same file in the list and play
                            LOGI (TAG, "[onResume] start resume play, path:" + path + ",surfaceDestroyedFlag:" + surfaceDestroyedFlag);
                            if (new File (path).exists()) {
                                LOGI (TAG, "[onResume] resume play file exists,  path:" + path);
                                if (surfaceDestroyedFlag) { //add for press power key quickly
                                    initVideoView(); //file play will do in surface create
                                }
                                else {
                                    //browserBack();
                                    initPlayer();
                                    mPath = path;
                                    sendPlayFileMsg();
                                }
                            }
                            else {
                                /*if(mContext != null)
                                    Toast.makeText(mContext,mContext.getText(R.string.str_no_file),Toast.LENGTH_SHORT).show();
                                browserBack();*/
                                retryPlay();
                            }
                            break;
                        }
                    }
                }
            }
            registerHdmiReceiver();
            registerMountReceiver();
            registerPowerReceiver();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            LOGI (TAG, "[onDestroy]");
            if (mResumePlay != null) {
                mResumePlay.setEnable (false); //disable resume play
            }
            release();
            surfaceDestroyedFlag = true;
            LOGI (TAG, "[onDestroy] surfaceDestroyedFlag:" + surfaceDestroyedFlag);
        }

        @Override
        public void onPause() {
            super.onPause();
            LOGI (TAG, "[onPause] curtime:" + curtime);
            mErrorTime = 0;
            mErrorTimeBac = 0;
            if (randomSeekEnable()) { // random seek for test
                randomSeekTestFlag = true;
            }
            //close certification
            closeCertification();
            //set book mark
            if (mBookmark != null) {
                if (confirm_dialog != null && confirm_dialog.isShowing() && exitAbort == false) {
                    bmPos = 0;
                    exitAbort = true;
                    confirm_dialog.dismiss();
                    mBookmark.set (mPlayList.getcur(), 0);
                }
                else {
                    mBookmark.set (mPlayList.getcur(), curtime);
                }
            }
            if (progressBar != null) { //add for focus changed to highlight playing item in file list
                progressBar.requestFocus();
            }
            resetVariate();
            openScreenOffTimeout();
            unregisterHdmiReceiver();
            unregisterMountReceiver();
            unregisterPowerReceiver();
            if (mHandler != null) {
                mHandler.removeMessages (MSG_UPDATE_PROGRESS);
                mHandler.removeMessages (MSG_PLAY);
                mHandler.removeMessages (MSG_STOP);
                mHandler.removeMessages (MSG_RETRY_PLAY);
                mHandler.removeMessages (MSG_RETRY_END);
            }
            // shield for google tv 20140929, opened for bug 101311 20141224
            IWindowManager iWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            try {
                iWindowManager.setAnimationScale(1, mTransitionAnimationScale);
            }
            catch (RemoteException e) {
                LOGE(TAG, "[onPause]RemoteException e:" + e);
            }

            if (mResumePlay != null) {
                if (mContext != null) {
                    boolean resumeEnable = true;
                    if (mSystemControl.getPropertyBoolean("ro.platform.has.mbxuimode", false)) {
                        resumeEnable = mContext.getResources().getBoolean (R.bool.config_resume_play_enable);
                    }
                    LOGI (TAG, "[onPause] resumeEnable:" + resumeEnable);
                    if (resumeEnable == true) {
                        mResumePlay.setEnable (true); //resume play function ON/OFF
                        if (true == mResumePlay.getEnable()) {
                            mResumePlay.set (mPlayList.getcur(), curtime);
                            LOGI (TAG, "[onPause]mStateBac:" + mState);
                            mStateBac = mState;
                            sendStopMsg();
                        }
                    }
                    else {
                        browserBackInvokeFromOnPause = true;
                        browserBack();
                    }
                }
            }
        }

        //@@--------this part for message handle---------------------------------------------------------------------
        private static final long MSG_SEND_DELAY = 0; //1000;//1s
        private static final int MSG_UPDATE_PROGRESS = 0xF1;//random value
        private static final int MSG_PLAY = 0xF2;
        private static final int MSG_STOP = 0xF3;
        private static final int MSG_RETRY_PLAY = 0xF4;
        private static final int MSG_RETRY_END = 0xF5;
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage (Message msg) {
                int pos;
                //LOGI(TAG,"[handleMessage]msg:"+msg);
                switch (msg.what) {
                    case MSG_UPDATE_PROGRESS:
                        //LOGI(TAG,"[handleMessage]MSG_UPDATE_PROGRESS mState:"+mState+",mSeekState:"+mSeekState);
                        if ( (mState == STATE_PLAYING
                                || mState == STATE_PAUSED
                        || mState == STATE_SEARCHING)  && (mSeekState == SEEK_END) /*&& osdisshowing*/) {
                            pos = getCurrentPosition();
                            updateProgressbar();
                            msg = obtainMessage (MSG_UPDATE_PROGRESS);
                            sendMessageDelayed (msg, 1000 - (pos % 1000));
                        }
                        break;
                    case MSG_PLAY:
                        LOGI (TAG, "[handleMessage]resume mode:" + mOption.getResumeMode() + ",mPath:" + mPath);
                        if (mOption != null && mPath != null) {
                            resetVariate();
                            showOsdView();
                            if (mResumePlay.getEnable() == true) {
                                setVideoPath (mPath);
                            }
                            else {
                                if (mOption.getResumeMode() == true) {
                                    bmPlay (mPath);
                                }
                                else {
                                    setVideoPath (mPath);
                                }
                            }
                        }
                        break;
                    case MSG_STOP:
                        stop();
                        break;
                    case MSG_RETRY_PLAY:
                        LOGI (TAG, "[handleMessage]MSG_RETRY_PLAY");
                        String path = mResumePlay.getFilepath();
                        if (new File (path).exists()) {
                            if (surfaceDestroyedFlag) { //add for press power key quickly
                                initVideoView();
                            }
                            else {
                                //browserBack();
                                initPlayer();
                                mPath = path;
                                sendPlayFileMsg();
                            }
                        }
                        else {
                            LOGI (TAG, "retry fail, retry again.");
                            retryPlay();
                        }
                        break;
                    case MSG_RETRY_END:
                        LOGI (TAG, "[handleMessage]MSG_RETRY_END");
                        if (mContext != null) {
                            Toast.makeText (mContext, mContext.getText (R.string.str_no_file), Toast.LENGTH_SHORT).show();
                        }
                        browserBack();
                        break;
                }
            }
        };

        private void updateProgressbar() {
            if ( (mState >= STATE_PREPARED) && (mState != STATE_PLAY_COMPLETED) && (mState <= STATE_SEARCHING)) { //avoid error (-38, 0), caused by getDuration before prepared
                curtime = getCurrentPosition();
                totaltime = getDuration();
                // add for seeking to head
                /*if(curtime <= 1000) { //current time is equal or smaller than 1S stop fw/fb
                    stopFWFB();
                    mState = STATE_PLAYING;
                    updateIconResource();
                }*/
                LOGI (TAG, "[updateProgressbar]curtime:" + curtime + ",totaltime:" + totaltime + ",progressBar:" + progressBar + ",ctlbar:" + ctlbar + ",ctlbar.getVisibility():" + ctlbar.getVisibility());
                if (curTimeTx != null && totalTimeTx != null && progressBar != null) {
                    int flag = getCurOsdViewFlag();
                    if ( (OSD_CTL_BAR == flag) && (null != ctlbar) && (View.VISIBLE == ctlbar.getVisibility())) { // check control bar is showing
                        curTimeTx.setText (secToTime (curtime / 1000));
                        totalTimeTx.setText (secToTime (totaltime / 1000));
                        if (totaltime != 0) {
                            //curtime = curtime/1000;
                            //totaltime = totaltime/1000;
                            progressBar.setProgress (curtime * 100 / totaltime);
                        }
                    }
                }
            }
        }

        private boolean isDemoVersion = false;
        private boolean isDemoVersion() {
            boolean ret = mSystemControl.getPropertyBoolean("sys.videoplayer.demon", false);
            ret = (ret || isDemoVersion);
            return ret;
        }

        private boolean getPlayIgnoreHdmiEnable() {
            boolean ret = mSystemControl.getPropertyBoolean("ro.videoignorehdmi.enable", true);
            return ret;
        }

        private boolean getImgSubRatioEnable() {
            boolean ret = mSystemControl.getPropertyBoolean("sys.imgsubratio.enable", true);
            return ret;
        }

        private boolean getDebugEnable() {
            boolean ret = mSystemControl.getPropertyBoolean("sys.videoplayer.debug", false);
            return ret;
        }

        private void LOGI (String tag, String msg) {
            if (DEBUG || getDebugEnable()) { Log.i (tag, msg); }
        }

        private void LOGD (String tag, String msg) {
            if (DEBUG || getDebugEnable()) { Log.d (tag, msg); }
        }

        private void LOGE (String tag, String msg) {
            /*if(DEBUG)*/ Log.e (tag, msg);
        }

        protected void closeScreenOffTimeout() {
            if (mScreenLock.isHeld() == false) {
                mScreenLock.acquire();
            }
        }

        protected void openScreenOffTimeout() {
            if (mScreenLock.isHeld() == true) {
                mScreenLock.release();
            }
        }

        private void init() {
            initView();
            initOption();
            initBookmark();
            initResumePlay();
            initPlayList();
            initErrorInfo();
        }

        private void initOption() {
            mOption = new Option (VideoPlayer.this);
        }

        private void initBookmark() {
            mBookmark = new Bookmark (VideoPlayer.this);
        }

        private void initResumePlay() {
            mResumePlay = new ResumePlay (VideoPlayer.this);
        }

        private void initPlayList() {
            mPlayList = PlayList.getinstance();
        }

        private void initMediaInfo() {
            LOGI (TAG, "[initMediaInfo] mMediaPlayer:"+mMediaPlayer);
            mMediaInfo = new MediaInfo (mMediaPlayer, VideoPlayer.this);
            mMediaInfo.initMediaInfo();
            //prepare for audio track
            if (mMediaInfo != null) {
                int audio_init_list_idx = mMediaInfo.getCurAudioIdx();
                int audio_total_num = mMediaInfo.getAudioTotalNum();
                if ( (mMediaInfo != null) && (audio_init_list_idx >= audio_total_num)) {
                    audio_init_list_idx = audio_total_num - 1;
                }
                mOption.setAudioTrack (audio_init_list_idx);
                mOption.setAudioDtsAsset (0); //dts test // default 0, should get current asset from player core 20140717
            }
        }

        private void initErrorInfo() {
            mErrorInfo = new ErrorInfo (VideoPlayer.this);
        }

        private void initView() {
            LOGI (TAG, "initView");
            mContext = this.getApplicationContext();
            //videView
            initVideoView();
            ff_fb = Toast.makeText (VideoPlayer.this, "", Toast.LENGTH_SHORT);
            ff_fb.setGravity (Gravity.TOP | Gravity.RIGHT, 10, 10);
            ff_fb.setDuration (0x00000001);
            //certification image
            certificationDoblyView = (ImageView) findViewById (R.id.CertificationDobly);
            certificationDoblyPlusView = (ImageView) findViewById (R.id.CertificationDoblyPlus);
            certificationDTSView = (ImageView) findViewById (R.id.CertificationDTS);
            certificationDTSExpressView = (ImageView) findViewById (R.id.CertificationDTSExpress);
            certificationDTSHDMasterAudioView = (ImageView) findViewById (R.id.CertificationDTSHDMasterAudio);
            certificationDoblyView.setVisibility (View.GONE);
            certificationDoblyPlusView.setVisibility (View.GONE);
            certificationDTSView.setVisibility (View.GONE);
            certificationDTSExpressView.setVisibility (View.GONE);
            certificationDTSHDMasterAudioView.setVisibility (View.GONE);
            ctlbar = (LinearLayout) findViewById (R.id.infobarLayout);
            optbar = (LinearLayout) findViewById (R.id.morebarLayout);
            subwidget = (LinearLayout) findViewById (R.id.LinearLayout_sub);
            otherwidget = (LinearLayout) findViewById (R.id.LinearLayout_other);
            infowidget = (LinearLayout) findViewById (R.id.dialog_layout);
            ctlbar.setVisibility (View.GONE);
            optbar.setVisibility (View.GONE);
            subwidget.setVisibility (View.GONE);
            otherwidget.setVisibility (View.GONE);
            infowidget.setVisibility (View.GONE);
            //layer 1
            progressBar = (SeekBar) findViewById (R.id.SeekBar);
            curTimeTx = (TextView) findViewById (R.id.CurTime);
            totalTimeTx = (TextView) findViewById (R.id.TotalTime);
            curTimeTx.setText (secToTime (curtime));
            totalTimeTx.setText (secToTime (totaltime));
            browserBtn = (ImageButton) findViewById (R.id.BrowserBtn);
            preBtn = (ImageButton) findViewById (R.id.PreBtn);
            fastforwordBtn = (ImageButton) findViewById (R.id.FastForwardBtn);
            playBtn = (ImageButton) findViewById (R.id.PlayBtn);
            fastreverseBtn = (ImageButton) findViewById (R.id.FastReverseBtn);
            nextBtn = (ImageButton) findViewById (R.id.NextBtn);
            optBtn = (ImageButton) findViewById (R.id.moreBtn);
            //layer 2
            ctlBtn = (ImageButton) findViewById (R.id.BackBtn);
            resumeModeBtn = (ImageButton) findViewById (R.id.ResumeBtn);
            repeatModeBtn = (ImageButton) findViewById (R.id.PlaymodeBtn);
            audiooptionBtn = (ImageButton) findViewById (R.id.ChangetrackBtn);
            subtitleSwitchBtn = (ImageButton) findViewById (R.id.SubtitleBtn);
            displayModeBtn = (ImageButton) findViewById (R.id.DisplayBtn);
            brigtnessBtn = (ImageButton) findViewById (R.id.BrightnessBtn);
            fileinfoBtn = (ImageButton) findViewById (R.id.InfoBtn);
            play3dBtn = (ImageButton) findViewById (R.id.Play3DBtn);
            otherwidgetTitleTx = (TextView) findViewById (R.id.more_title);

            if (mSystemControl.getPropertyBoolean("ro.platform.has.mbxuimode", false)) {
                brigtnessBtn.setVisibility (View.GONE);
            }
            else {
                play3dBtn.setVisibility (View.GONE);
            }

            //layer 1
            progressBar.setOnSeekBarChangeListener (new SeekBar.OnSeekBarChangeListener() {
                public void onStopTrackingTouch (SeekBar seekBar) {
                    progressBarSeekFlag = false;
                    progressBar.requestFocusFromTouch();
                    startOsdTimeout();
                }
                public void onStartTrackingTouch (SeekBar seekBar) {
                    progressBarSeekFlag = true;
                }
                public void onProgressChanged (SeekBar seekBar, int progress, boolean fromUser) {
                    if (totaltime == -1) {
                        return;
                    }
                    if (fromUser == true) {
                        progressBarSeekFlag = true;
                        seekByProgressBar();
                    }
                }
            });
            browserBtn.setOnClickListener (new ImageButton.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "browserBtn onClick");
                    browserBack();
                }
            });
            preBtn.setOnClickListener (new Button.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "preBtn onClick");
                    playPrev();
                }
            });
            fastforwordBtn.setOnClickListener (new Button.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "fastforwordBtn onClick");
                    if (mCanSeek) {
                        fastForward();
                    }
                }
            });
            playBtn.setOnClickListener (new Button.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "playBtn onClick");
                    playPause();
                }
            });
            fastreverseBtn.setOnClickListener (new Button.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "fastreverseBtn onClick");
                    if (mCanSeek) {
                        fastBackward();
                    }
                }
            });
            nextBtn.setOnClickListener (new Button.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "nextBtn onClick");
                    playNext();
                }
            });
            optBtn.setOnClickListener (new ImageButton.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "optBtn onClick");
                    switchOsdView();
                }
            });
            //layer 2
            ctlBtn.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "ctlBtn onClick");
                    switchOsdView();
                }
            });
            resumeModeBtn.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "resumeModeBtn onClick");
                    resumeSelect();
                }
            });
            if (playmode_switch) {
                repeatModeBtn.setOnClickListener (new View.OnClickListener() {
                    public void onClick (View v) {
                        LOGI (TAG, "repeatModeBtn onClick");
                        repeatSelect();
                    }
                });
            }
            else {
                repeatModeBtn.setImageDrawable (getResources().getDrawable (R.drawable.mode_disable));
            }
            audiooptionBtn.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "audiooptionBtn onClick");
                    audioOption();
                }
            });
            subtitleSwitchBtn.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "subtitleSwitchBtn onClick");
                    subtitleSelect();
                }
            });
            displayModeBtn.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "displayModeBtn onClick");
                    displayModeSelect();
                }
            });
            brigtnessBtn.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "brigtnessBtn onClick");
                    brightnessSelect();
                }
            });
            fileinfoBtn.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "fileinfoBtn onClick");
                    if (randomSeekEnable()) { // random seek, shield for default
                        randomSeekTest();
                    }
                    else {
                        fileinfoShow();
                    }
                }
            });
            play3dBtn.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    LOGI (TAG, "play3dBtn onClick");
                    /*Toast toast =Toast.makeText(VideoPlayer.this, "this function is not opened right now",Toast.LENGTH_SHORT );
                    toast.setGravity(Gravity.BOTTOM,0,0);
                    toast.setDuration(0x00000001);
                    toast.show();
                    startOsdTimeout();*/
                    play3DSelect();
                }
            });
        }

        private void initVideoView() {
            LOGI (TAG, "[initVideoView]");
            mVideoViewRoot = findViewById (R.id.VideoViewRoot);
            mVideoView = (VideoView) mVideoViewRoot.findViewById (R.id.VideoView);
            setOnSystemUiVisibilityChangeListener(); // TODO:ATTENTION: this is very import to keep osd bar show or hide synchronize with touch event, bug86905
            //showSystemUi(false);
            //getHolder().setFormat(PixelFormat.VIDEO_HOLE_REAL);
            if (mVideoView != null) {
                mVideoView.getHolder().addCallback (mSHCallback);
                mVideoView.getHolder().setType (SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                //@@mVideoView.addOnLayoutChangeListener(this);
                mVideoView.setFocusable (true);
                mVideoView.setFocusableInTouchMode (true);
                mVideoView.requestFocus();
                //@@mVideoWidth = 0;
                //@@mVideoHeight = 0;
                //@@mCurrentState = STATE_IDLE;
                //@@mTargetState  = STATE_IDLE;
            }
        }

        private int getCurDirFile (Uri uri, List<String> list) {
            LOGI (TAG, "[getCurDirFile]uri:" + uri);
            String path = uri.getPath();
            int pos = -1;
            list.clear();
            if (null != path) {
                String dir = null;
                int index = -1;
                index = path.lastIndexOf ("/");
                if (index >= 0) {
                    dir = path.substring (0, index);
                }
                File dirFile = new File (dir);
                if (dirFile.exists() && dirFile.isDirectory() && dirFile.listFiles() != null && dirFile.listFiles().length > 0) {
                for (File file : dirFile.listFiles()) {
                        String pathFull = file.getAbsolutePath();
                        String name = (new File (pathFull)).getName();
                        String ext = name.substring (name.lastIndexOf (".") + 1, name.length()).toLowerCase();
                        if (ext.equals ("rm") || ext.equals ("rmvb") || ext.equals ("avi") || ext.equals ("mkv") ||
                                ext.equals ("mp4") || ext.equals ("wmv") || ext.equals ("mov") || ext.equals ("flv") ||
                                ext.equals ("asf") || ext.equals ("3gp") || ext.equals ("mpg") || ext.equals ("mvc") ||
                                ext.equals ("m2ts") || ext.equals ("ts") || ext.equals ("swf") || ext.equals ("mlv") ||
                                ext.equals ("divx") || ext.equals ("3gp2") || ext.equals ("3gpp") || ext.equals ("h265") ||
                                ext.equals ("m4v") || ext.equals ("mts") || ext.equals ("tp") || ext.equals ("bit") ||
                                ext.equals ("webm") || ext.equals ("3g2") || ext.equals ("f4v") || ext.equals ("pmp") ||
                                ext.equals ("mpeg") || ext.equals ("vob") || ext.equals ("dat") || ext.equals ("m2v") ||
                                ext.equals ("iso") || ext.equals ("vp9") || ext.equals ("trp") || ext.equals ("hm10")) {
                            list.add (pathFull);
                        }
                    }
                }
                for (int i = 0; i < list.size(); i++) {
                    String tempP = list.get (i);
                    if (tempP.equals (path)) {
                        pos = i;
                    }
                }
            }
            return pos;
        }

        private int checkUri() {
            // TODO: should check mUri=null
            LOGI (TAG, "[checkUri]");
            Intent it = getIntent();
            mUri = it.getData();
            LOGI (TAG, "[checkUri]mUri:" + mUri);
            if (it.getData() != null) {
                if (it.getData().getScheme() != null && it.getData().getScheme().equals ("file")) {
                    List<String> paths = new ArrayList<String>();
                    int pos = getCurDirFile (mUri, paths);
                    //paths.add(it.getData().getPath());
                    if (pos != -1) {
                        mPlayList.setlist (paths, pos);
                        mPlayList.rootPath = null;
                        backToOtherAPK = true;
                    }
                    else {
                        return -1;
                    }
                }
                else {
                    Cursor cursor = managedQuery (it.getData(), null, null, null, null);
                    if (cursor != null) {
                        cursor.moveToFirst();
                        int index = cursor.getColumnIndex (MediaStore.Video.Media.DATA);
                        if ( (index == -1) || (cursor.getCount() <= 0)) {
                            LOGE (TAG, "Cursor empty or failed\n");
                        }
                        else {
                            List<String> paths = new ArrayList<String>();
                            cursor.moveToFirst();
                            paths.add (cursor.getString (index));
                            mPlayList.setlist (paths, 0);
                        }
                    }
                    else {
                        // unsupported mUri, exit directly
                        android.os.Process.killProcess (android.os.Process.myPid());
                        return -1;
                    }
                }
            }
            return 0;
        }

        private void storeFilePos() {
            Bundle bundle = new Bundle();
            try {
                bundle = VideoPlayer.this.getIntent().getExtras();
                if (bundle != null) {
                    item_position_selected = bundle.getInt ("item_position_selected");
                    item_position_first = bundle.getInt ("item_position_first");
                    fromtop_piexl = bundle.getInt ("fromtop_piexl");
                    fileDirectory_position_selected = bundle.getIntegerArrayList ("fileDirectory_position_selected");
                    fileDirectory_position_piexl = bundle.getIntegerArrayList ("fileDirectory_position_piexl");
                    backToOtherAPK = bundle.getBoolean ("backToOtherAPK", true);
                    if (item_init_flag) {
                        item_position_selected_init = item_position_selected - mPlayList.getindex();
                        item_init_flag = false;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        private Bundle getFilePos() {
            Bundle bundle = new Bundle();
            try {
                if (bundle != null) {
                    bundle.putInt ("item_position_selected", item_position_selected);
                    bundle.putInt ("item_position_first", item_position_first);
                    bundle.putInt ("fromtop_piexl", fromtop_piexl);
                    bundle.putIntegerArrayList ("fileDirectory_position_selected", fileDirectory_position_selected);
                    bundle.putIntegerArrayList ("fileDirectory_position_piexl", fileDirectory_position_piexl);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return bundle;
        }

        private String secToTime (int i) {
            String retStr = null;
            int hour = 0;
            int minute = 0;
            int second = 0;
            if (i <= 0) {
                return "00:00:00";
            }
            else {
                minute = i / 60;
                if (minute < 60) {
                    second = i % 60;
                    retStr = "00:" + unitFormat (minute) + ":" + unitFormat (second);
                }
                else {
                    hour = minute / 60;
                    if (hour > 99) {
                        return "99:59:59";
                    }
                    minute = minute % 60;
                    second = i % 60;
                    retStr = unitFormat (hour) + ":" + unitFormat (minute) + ":" + unitFormat (second);
                }
            }
            return retStr;
        }

        private String unitFormat (int i) {
            String retStr = null;
            if (i >= 0 && i < 10) {
                retStr = "0" + Integer.toString (i);
            }
            else {
                retStr = Integer.toString (i);
            }
            return retStr;
        }

        SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {
            public void surfaceChanged (SurfaceHolder holder, int format, int w, int h) {
                LOGI (TAG, "[surfaceChanged]format:" + format + ",w:" + w + ",h:" + h);
                if (mVideoView != null) {
                    displayModeImpl();
                    //mVideoView.requestLayout();
                    //mVideoView.invalidate();
                }
                //@@
                /*mSurfaceWidth = w;
                mSurfaceHeight = h;
                boolean isValidState =  (mTargetState == STATE_PLAYING);
                boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
                if (mMediaPlayer != null && isValidState && hasValidSize) {
                    if (mSeekWhenPrepared != 0) {
                        seekTo(mSeekWhenPrepared);
                    }
                    start();
                }*/
            }
            public void surfaceCreated (SurfaceHolder holder) {
                LOGI (TAG, "[surfaceCreated]");
                mSurfaceHolder = holder;
                surfaceDestroyedFlag = false;
                initPlayer();
                LOGI (TAG, "[surfaceCreated]mResumePlay:" + mResumePlay + ",surfaceDestroyedFlag:" + surfaceDestroyedFlag);
                if (mResumePlay != null) {
                    LOGI (TAG, "[surfaceCreated]mResumePlay.getEnable():" + mResumePlay.getEnable());
                    if (mResumePlay.getEnable() == true) {
                        LOGI (TAG, "[surfaceCreated] mResumePlay.getFilepath():" + mResumePlay.getFilepath());
                        String path = mResumePlay.getFilepath();
                        if (mPlayList != null) {
                            if (mPlayList.getindex() < 0) {
                                List<String> paths = new ArrayList<String>();
                                Uri uri = Uri.parse (path);
                                if (uri != null) {
                                    int pos = getCurDirFile (uri, paths);
                                    if (pos != -1) {
                                        mPlayList.setlist (paths, pos);
                                    }
                                }
                            }
                            path = mPlayList.getcur();
                            LOGI (TAG, "[surfaceCreated]mResumePlay prepare path:" + path);
                            if (path != null) {
                                mPath = path;
                                sendPlayFileMsg();
                            }
                            else {
                                browserBack();
                            }
                        }
                        else {
                            browserBack(); // mPlayList is null, resume play function error, and then back to file list
                        }
                    }
                    else {
                        LOGI (TAG, "[surfaceCreated]0path:" + mPlayList.getcur());
                        mPath = mPlayList.getcur();
                        sendPlayFileMsg();
                    }
                }
                else {
                    LOGI (TAG, "[surfaceCreated]1path:" + mPlayList.getcur());
                    mPath = mPlayList.getcur();
                    sendPlayFileMsg();
                }
            }
            public void surfaceDestroyed (SurfaceHolder holder) {
                LOGI (TAG, "[surfaceDestroyed]");
                mSurfaceHolder = null;
                mVideoView = null;
                release();
                surfaceDestroyedFlag = true;
                LOGI (TAG, "[surfaceDestroyed]surfaceDestroyedFlag:" + surfaceDestroyedFlag);
            }
        };

        public void onConfigurationChanged (Configuration config) {
            super.onConfigurationChanged (config);
            if (mVideoView != null) {
                displayModeImpl();
                mVideoView.requestLayout();
                mVideoView.invalidate();
            }
        }
        //@@--------this part for broadcast receiver-------------------------------------------------------------------------------------
        private final String POWER_KEY_SUSPEND_ACTION = "com.amlogic.vplayer.powerkey";
        private boolean isEjectOrUnmoutProcessed = false;
        private boolean isHdmiPluggedbac = false;
        private boolean isHdmiPlugged = false;

        private BroadcastReceiver mHdmiReceiver = new BroadcastReceiver() {
            public void onReceive (Context context, Intent intent) {
                isHdmiPlugged = intent.getBooleanExtra (WindowManagerPolicy.EXTRA_HDMI_PLUGGED_STATE, false);
                if ( (isHdmiPluggedbac != isHdmiPlugged) && (isHdmiPlugged == false)) {
                    if (mState == STATE_PLAYING) {
                        if (!getPlayIgnoreHdmiEnable()) {
                            pause();
                            //close 3D
                            close3D();
                        }
                    }
                    startOsdTimeout();
                }
                isHdmiPluggedbac = isHdmiPlugged;
            }
        };

        private BroadcastReceiver mMountReceiver = new BroadcastReceiver() {
            public void onReceive (Context context, Intent intent) {
                String action = intent.getAction();
                Uri uri = intent.getData();
                String path = uri.getPath();
                LOGI (TAG, "[mMountReceiver] action=" + action + ",uri=" + uri + ",path=" + path + ", mRetrying:" + mRetrying);
                if (action == null || path == null) {
                    return;
                }
                if (mRetrying == true) {
                    return;
                }
                if ( (action.equals (Intent.ACTION_MEDIA_EJECT)) || (action.equals (Intent.ACTION_MEDIA_UNMOUNTED))) {
                    if (mPlayList.getcur() != null) {
                        if (mPlayList.getcur().startsWith (path)) {
                            if (isEjectOrUnmoutProcessed) {
                                return;
                            }
                            else {
                                isEjectOrUnmoutProcessed = true;
                            }
                            browserBack();
                        }
                    }
                }
                else if (action.equals (Intent.ACTION_MEDIA_MOUNTED)) {
                    isEjectOrUnmoutProcessed = false;
                    // Nothing
                }
            }
        };

        private BroadcastReceiver mPowerReceiver = new BroadcastReceiver() {
            public void onReceive (Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                if (action.equals (POWER_KEY_SUSPEND_ACTION)) {
                    if (mResumePlay != null) {
                        mResumePlay.setEnable (true);
                    }
                }
            }
        };

        private void registerHdmiReceiver() {
            IntentFilter intentFilter = new IntentFilter (WindowManagerPolicy.ACTION_HDMI_PLUGGED);
            Intent intent = registerReceiver (mHdmiReceiver, intentFilter);
            if (intent != null) {
                mHdmiPlugged = intent.getBooleanExtra (WindowManagerPolicy.EXTRA_HDMI_PLUGGED_STATE, false);
            }
            LOGI (TAG, "[registerHdmiReceiver]mHdmiReceiver:" + mHdmiReceiver);
        }

        private void registerMountReceiver() {
            IntentFilter intentFilter = new IntentFilter (Intent.ACTION_MEDIA_MOUNTED);
            intentFilter.addAction (Intent.ACTION_MEDIA_EJECT);
            intentFilter.addAction (Intent.ACTION_MEDIA_UNMOUNTED);
            intentFilter.addDataScheme ("file");
            registerReceiver (mMountReceiver, intentFilter);
            LOGI (TAG, "[registerMountReceiver]mMountReceiver:" + mMountReceiver);
        }

        private void registerPowerReceiver() {
            IntentFilter intentFilter = new IntentFilter (POWER_KEY_SUSPEND_ACTION);
            registerReceiver (mPowerReceiver, intentFilter);
            LOGI (TAG, "[registerPowerReceiver]mPowerReceiver:" + mPowerReceiver);
        }

        private void unregisterHdmiReceiver() {
            LOGI (TAG, "[unregisterHdmiReceiver]mHdmiReceiver:" + mHdmiReceiver);
            if (mHdmiReceiver != null) {
                unregisterReceiver (mHdmiReceiver);
                mHdmiReceiver = null;
            }
        }

        private void unregisterMountReceiver() {
            LOGI (TAG, "[unregisterMountReceiver]mMountReceiver:" + mMountReceiver);
            if (mMountReceiver != null) {
                unregisterReceiver (mMountReceiver);
                isEjectOrUnmoutProcessed = false;
                mMountReceiver = null;
            }
        }

        private void unregisterPowerReceiver() {
            LOGI (TAG, "[unregisterPowerReceiver]mPowerReceiver:" + mPowerReceiver);
            if (mPowerReceiver != null) {
                unregisterReceiver (mPowerReceiver);
                mPowerReceiver = null;
            }
        }

        //@@--------this part for option function implement------------------------------------------------------------------------------
        private void resumeSelect() {
            LOGI (TAG, "[resumeSelect]");
            ListView listView = (ListView) findViewById (R.id.ListView);
            listView.setAdapter (getMorebarListAdapter (RESUME_MODE, mOption.getResumeMode() ? 0 : 1));
            listView.setOnItemClickListener (new AdapterView.OnItemClickListener() {
                public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        mOption.setResumeMode (true);
                    }
                    else if (position == 1) {
                        mOption.setResumeMode (false);
                    }
                    exitOtherWidget (resumeModeBtn);
                }
            });
            showOtherWidget (R.string.setting_resume);
        }

        private void repeatSelect() {
            LOGI (TAG, "[repeatSelect]");
            ListView listView = (ListView) findViewById (R.id.ListView);
            listView.setAdapter (getMorebarListAdapter (REPEAT_MODE, mOption.getRepeatMode()));
            listView.setOnItemClickListener (new AdapterView.OnItemClickListener() {
                public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        mOption.setRepeatMode (0);
                    }
                    else if (position == 1) {
                        mOption.setRepeatMode (1);
                    }
                    exitOtherWidget (repeatModeBtn);
                }
            });
            showOtherWidget (R.string.setting_playmode);
        }

        private void audioOption() {
            LOGI (TAG, "[audioOption] mMediaInfo:" + mMediaInfo);
            if (mMediaInfo != null) {
                LOGI (TAG, "[audioOption] mMediaInfo.getAudioTotalNum():" + mMediaInfo.getAudioTotalNum());
                if (/*(audio_flag == Errorno.PLAYER_NO_AUDIO) || */ (mMediaInfo.getAudioTotalNum() <= 0)) {
                    Toast toast = Toast.makeText (VideoPlayer.this, R.string.file_have_no_audio, Toast.LENGTH_SHORT);
                    toast.setGravity (Gravity.BOTTOM,/*110*/0, 0);
                    toast.setDuration (0x00000001);
                    toast.show();
                    startOsdTimeout();
                    return;
                }
            }
            if (mState == STATE_SEARCHING) {
                Toast toast_track_switch = Toast.makeText (VideoPlayer.this, R.string.cannot_switch_track, Toast.LENGTH_SHORT);
                toast_track_switch.show();
                return;
            }
            SimpleAdapter audiooptionarray = getMorebarListAdapter (AUDIO_OPTION, 0);
            ListView listView = (ListView) findViewById (R.id.ListView);
            listView.setAdapter (audiooptionarray);
            listView.setOnItemClickListener (new AdapterView.OnItemClickListener() {
                public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        audiotrackSelect();
                    }
                    else if (position == 1) {
                        soundtrackSelect();
                    }
                }
            });
            showOtherWidget (R.string.setting_audiooption);
        }

        private void audiotrackSelect() {
            LOGI (TAG, "[audiotrackSelect]");
            SimpleAdapter audioarray = getMorebarListAdapter (AUDIO_TRACK, mOption.getAudioTrack());
            ListView listView = (ListView) findViewById (R.id.ListView);
            listView.setAdapter (audioarray);
            listView.setOnItemClickListener (new AdapterView.OnItemClickListener() {
                public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                    boolean ret = mMediaInfo.checkAudioisDTS (mMediaInfo.getAudioFormat (position));
                    if (ret) {
                        showDtsAseetFromInfoLis = false;
                        audioDtsAseetSelect();
                    }
                    else {
                        exitOtherWidget (audiooptionBtn);
                    }
                    mOption.setAudioTrack (position);
                    audioTrackImpl (position);
                    mDtsType = DTS_NOR;
                    showCertification(); //update certification status and icon
                }
            });
            showOtherWidget (R.string.setting_audiotrack);
        }

        private void soundtrackSelect() {
            LOGI (TAG, "[soundtrackSelect]");
            SimpleAdapter soundarray = getMorebarListAdapter (SOUND_TRACK, mOption.getSoundTrack());
            ListView listView = (ListView) findViewById (R.id.ListView);
            listView.setAdapter (soundarray);
            listView.setOnItemClickListener (new AdapterView.OnItemClickListener() {
                public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                    mOption.setSoundTrack (position);
                    soundTrackImpl (position);
                    exitOtherWidget (audiooptionBtn);
                }
            });
            showOtherWidget (R.string.setting_soundtrack);
        }

        private void audioDtsAseetSelect() {
            SimpleAdapter audiodtsAssetarray = getMorebarListAdapter (AUDIO_DTS_ASSET, mOption.getAudioDtsAsset());
            ListView listView = (ListView) findViewById (R.id.ListView);
            listView.setAdapter (audiodtsAssetarray);
            listView.setOnItemClickListener (new AdapterView.OnItemClickListener() {
                public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                    mOption.setAudioDtsAsset (position);
                    audioDtsAseetImpl (position);
                    exitOtherWidget (audiooptionBtn);
                    showCertification(); //update certification status and icon
                }
            });
            showOtherWidget (R.string.setting_audiodtsasset);
            if (getDtsAssetTotalNum() == 0) { // dts test
                exitOtherWidget (audiooptionBtn);
            }
            if (showDtsAseetFromInfoLis) {
                startOsdTimeout();
            }
        }

        private void play3DSelect() {
            LOGI (TAG, "[play3DSelect]");
            SimpleAdapter _3darray = getMorebarListAdapter (PLAY3D, mOption.get3DMode());
            ListView listView = (ListView) findViewById (R.id.ListView);
            listView.setAdapter (_3darray);
            listView.setOnItemClickListener (new AdapterView.OnItemClickListener() {
                public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                    mOption.set3DMode (position);
                    play3DImpl (position);
                    exitOtherWidget (play3dBtn);
                }
            });
            showOtherWidget (R.string.setting_3d_mode);
        }

        private void subtitleSelect() {
            /*Toast toast =Toast.makeText(VideoPlayer.this, "this function is not opened right now",Toast.LENGTH_SHORT );
            toast.setGravity(Gravity.BOTTOM,110,0);
            toast.setDuration(0x00000001);
            toast.show();
            startOsdTimeout();
            return;*/
            subtitle_prepare();
            LOGI (TAG, "[subtitleSelect] sub_para.totalnum:" + sub_para.totalnum);
            if (sub_para.totalnum <= 0) {
                Toast toast = Toast.makeText (VideoPlayer.this, R.string.sub_no_subtitle, Toast.LENGTH_SHORT);
                toast.setGravity (Gravity.BOTTOM,/*110*/0, 0);
                toast.setDuration (0x00000001);
                toast.show();
                startOsdTimeout();
                return;
            }
            showSubWidget (R.string.setting_subtitle);
            subtitle_control();
        }

        private void displayModeSelect() {
            LOGI (TAG, "[displayModeSelect]");
            // TODO: check 3D
            if (mMediaInfo != null) {
                int videoNum = mMediaInfo.getVideoTotalNum();
                if (videoNum <= 0) {
                    Toast toast = Toast.makeText (VideoPlayer.this, R.string.file_have_no_video, Toast.LENGTH_SHORT);
                    toast.setGravity (Gravity.BOTTOM,/*110*/0, 0);
                    toast.setDuration (0x00000001);
                    toast.show();
                    startOsdTimeout();
                    return;
                }
            }
            ListView listView = (ListView) findViewById (R.id.ListView);
            listView.setAdapter (getMorebarListAdapter (DISPLAY_MODE, mOption.getDisplayMode()));
            listView.setOnItemClickListener (new AdapterView.OnItemClickListener() {
                public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                    switch (position) {
                        case 0://mOption.DISP_MODE_NORMAL:
                                mOption.setDisplayMode (mOption.DISP_MODE_NORMAL);
                            break;
                        case 1://mOption.DISP_MODE_FULLSTRETCH:
                            mOption.setDisplayMode (mOption.DISP_MODE_FULLSTRETCH);
                            break;
                        case 2://mOption.DISP_MODE_RATIO4_3:
                            mOption.setDisplayMode (mOption.DISP_MODE_RATIO4_3);
                            break;
                        case 3://mOption.DISP_MODE_RATIO16_9:
                            mOption.setDisplayMode (mOption.DISP_MODE_RATIO16_9);
                            break;
                        default:
                            break;
                    }
                    displayModeImpl();
                    exitOtherWidget (displayModeBtn);
                }
            });
            showOtherWidget (R.string.setting_displaymode);
        }

        private void brightnessSelect() {
            LOGI (TAG, "[brightnessSelect]");
            ListView listView = (ListView) findViewById (R.id.ListView);
            int mBrightness = 0;
            try {
                mBrightness = Settings.System.getInt (VideoPlayer.this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            }
            catch (SettingNotFoundException e) {
                e.printStackTrace();
            }
            int item;
            if (mBrightness <= (/*android.os.PowerManager.BRIGHTNESS_OFF*/20 + 10)) {
                item = 0;
            }
            else if (mBrightness <= (android.os.PowerManager.BRIGHTNESS_ON * 0.2f)) {
                item = 1;
            }
            else if (mBrightness <= (android.os.PowerManager.BRIGHTNESS_ON * 0.4f)) {
                item = 2;
            }
            else if (mBrightness <= (android.os.PowerManager.BRIGHTNESS_ON * 0.6f)) {
                item = 3;
            }
            else if (mBrightness <= (android.os.PowerManager.BRIGHTNESS_ON * 0.8f)) {
                item = 4;
            }
            else {
                item = 5;
            }
            listView.setAdapter (getMorebarListAdapter (BRIGHTNESS, item));
            listView.setOnItemClickListener (new AdapterView.OnItemClickListener() {
                public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                    int brightness;
                    switch (position) {
                        case 0:
                            brightness = /*android.os.PowerManager.BRIGHTNESS_OFF*/20 + 10;
                            break;
                        case 1:
                            brightness = (int) (android.os.PowerManager.BRIGHTNESS_ON * 0.2f);
                            break;
                        case 2:
                            brightness = (int) (android.os.PowerManager.BRIGHTNESS_ON * 0.4f);
                            break;
                        case 3:
                            brightness = (int) (android.os.PowerManager.BRIGHTNESS_ON * 0.6f);
                            break;
                        case 4:
                            brightness = (int) (android.os.PowerManager.BRIGHTNESS_ON * 0.8f);
                            break;
                        case 5:
                            brightness = android.os.PowerManager.BRIGHTNESS_ON;
                            break;
                        default:
                            brightness = /*android.os.PowerManager.BRIGHTNESS_OFF*/20 + 30;
                            break;
                    }
                    try {
                        IPowerManager power = IPowerManager.Stub.asInterface (ServiceManager.getService ("power"));
                        if (power != null) {
                            //power.setBacklightBrightness(brightness);
                            Settings.System.putInt (VideoPlayer.this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
                            power.setTemporaryScreenBrightnessSettingOverride (brightness);
                        }
                    }
                    catch (RemoteException doe) {
                    }
                    exitOtherWidget (brigtnessBtn);
                }
            });
            showOtherWidget (R.string.setting_brightness);
        }


        private void fileinfoShow() {
            LOGI (TAG, "[fileinfoShow]");
            showInfoWidget (R.string.str_file_name);
            String fileinf = null;
            TextView filename = (TextView) findViewById (R.id.filename);
            fileinf = VideoPlayer.this.getResources().getString (R.string.str_file_name)
                      + "\t: " + mMediaInfo.getFileName (mPlayList.getcur());
            filename.setText (fileinf);
            TextView filetype = (TextView) findViewById (R.id.filetype);
            fileinf = VideoPlayer.this.getResources().getString (R.string.str_file_format)
                      //+ "\t: " + mMediaInfo.getFileType();
                      + "\t: " + mMediaInfo.getFileType (mPlayList.getcur());
            filetype.setText (fileinf);
            TextView filesize = (TextView) findViewById (R.id.filesize);
            fileinf = VideoPlayer.this.getResources().getString (R.string.str_file_size)
                      + "\t: " + mMediaInfo.getFileSize();
            filesize.setText (fileinf);
            TextView resolution = (TextView) findViewById (R.id.resolution);
            fileinf = VideoPlayer.this.getResources().getString (R.string.str_file_resolution)
                      + "\t: " + mMediaInfo.getResolution();
            resolution.setText (fileinf);
            TextView duration = (TextView) findViewById (R.id.duration);
            fileinf = VideoPlayer.this.getResources().getString (R.string.str_file_duration)
                      + "\t: " + secToTime (mMediaInfo.getDuration());
            duration.setText (fileinf);
            Button ok = (Button) findViewById (R.id.info_ok);
            ok.setText ("OK");
            ok.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    exitInfoWidget (fileinfoBtn);
                }
            });
        }

        private void displayModeImpl () {
            int videoNum = -1;
            int videoWidth = -1;
            int videoHeight = -1;
            int dispWidth = -1;
            int dispHeight = -1;
            int frameWidth = -1;
            int frameHeight = -1;
            int width = -1;
            int height = -1;

            DisplayMetrics dm = new DisplayMetrics();
            this.getWindowManager().getDefaultDisplay().getRealMetrics (dm);
            frameWidth = dm.widthPixels;
            frameHeight = dm.heightPixels;
            dispWidth = frameWidth;
            dispHeight = frameHeight;

            LOGI (TAG, "[displayModeImpl]dispWidth:" + dispWidth + ",dispHeight:" + dispHeight);

            if (mMediaInfo != null) {
                videoNum = mMediaInfo.getVideoTotalNum();
                if (videoNum <= 0) {
                    return;
                }
            }
            if (mMediaPlayer != null && mOption != null && mVideoView != null) {
                LOGI (TAG, "[displayModeImpl]mode:" + mOption.getDisplayMode());
                //ViewGroup.LayoutParams lp = mVideoView.getLayoutParams();
                videoWidth = mMediaPlayer.getVideoWidth();
                videoHeight = mMediaPlayer.getVideoHeight();
                LOGI (TAG, "[displayModeImpl]videoWidth:" + videoWidth + ",videoHeight:" + videoHeight);
                if (mOption.getDisplayMode() == 0) { // normal
                    if (videoWidth * dispHeight < dispWidth * videoHeight) {
                        //image too wide
                        width = dispHeight * videoWidth / videoHeight;
                        height = dispHeight;
                    }
                    else if (videoWidth * dispHeight > dispWidth * videoHeight) {
                        //image too tall
                        width = dispWidth;
                        height = dispWidth * videoHeight / videoWidth;
                    }
                    else {
                        width = dispWidth;
                        height = dispHeight;
                    }
                }
                else if (mOption.getDisplayMode() == 1) { // full screen
                    width = dispWidth;
                    height = dispHeight;
                }
                else if (mOption.getDisplayMode() == 2) { // 4:3
                    videoWidth = 4 * videoHeight / 3;
                    if (videoWidth * dispHeight < dispWidth * videoHeight) {
                        //image too wide
                        width = dispHeight * videoWidth / videoHeight;
                        height = dispHeight;
                    }
                    else if (videoWidth * dispHeight > dispWidth * videoHeight) {
                        //image too tall
                        width = dispWidth;
                        height = dispWidth * videoHeight / videoWidth;
                    }
                    else {
                        width = dispWidth;
                        height = dispHeight;
                    }
                }
                else if (mOption.getDisplayMode() == 3) { // 16:9
                    videoWidth = 16 * videoHeight / 9;
                    if (videoWidth * dispHeight < dispWidth * videoHeight) {
                        //image too wide
                        width = dispHeight * videoWidth / videoHeight;
                        height = dispHeight;
                    }
                    else if (videoWidth * dispHeight > dispWidth * videoHeight) {
                        //image too tall
                        width = dispWidth;
                        height = dispWidth * videoHeight / videoWidth;
                    }
                    else {
                        width = dispWidth;
                        height = dispHeight;
                    }
                }

                LOGI (TAG, "[displayModeImpl]width:" + width + ",height:" + height);
                if (getImgSubRatioEnable() && dispWidth != 0 && dispHeight != 0) {
                    width = width * frameWidth / dispWidth;
                    height = height * frameHeight / dispHeight;
                    float ratioW = 1.000f;
                    float ratioH = 1.000f;
                    float ratioMax = 2.000f;
                    int maxW = dispWidth;
                    int maxH = dispHeight;
                    if (videoWidth != 0 & videoHeight != 0) {
                        ratioW = ((float)width) / videoWidth;
                        ratioH = ((float)height) / videoHeight;
                        if (ratioW > ratioMax || ratioH > ratioMax) {
                            ratioW = ratioMax;
                            ratioH = ratioMax;
                        }
                        mSubtitleManager.setImgSubRatio(ratioW, ratioH, maxW, maxH);
                    }
                    LOGI(TAG,"[displayModeImpl]after change width:" + width + ",height:" + height + ", ratioW:" + ratioW + ",ratioH:" + ratioH +", maxW:" + maxW + ",maxH:" + maxH);
                }

                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams (width, height);
                lp.addRule (RelativeLayout.CENTER_IN_PARENT);
                mVideoViewRoot.setLayoutParams (lp);
                //mVideoViewRoot.requestLayout();
            }
        }

        private void audioTrackImpl (int idx) {
            if (mMediaPlayer != null && mMediaInfo != null) {
                LOGI (TAG, "[audioTrackImpl]idx:" + idx);
                int id = mMediaInfo.getAudioIdx (idx);
                String str = Integer.toString (id);
                StringBuilder builder = new StringBuilder();
                builder.append ("aid:" + str);
                LOGI (TAG, "[audioTrackImpl]" + builder.toString());
                mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_SWITCH_AUDIO_TRACK,builder.toString());
            }
        }

        private void soundTrackImpl (int idx) {
            if (mMediaPlayer != null && mMediaInfo != null) {
                LOGI (TAG, "[soundTrackImpl]idx:" + idx);
                String soundTrackStr = "stereo";
                if (idx == 0) {
                    soundTrackStr = "stereo";
                }
                else if (idx == 1) {
                    soundTrackStr = "lmono";
                }
                else if (idx == 2) {
                    soundTrackStr = "rmono";
                }
                else if (idx == 3) {
                    soundTrackStr = "lrmix";
                }
                LOGI (TAG, "[soundTrackImpl]soundTrackStr:" + soundTrackStr);
                mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_SWITCH_SOUND_TRACK, soundTrackStr);
            }
        }

        private void audioDtsAseetImpl (int idx) {
            if (mMediaPlayer != null && mMediaInfo != null) {
                String str = Integer.toString (idx);
                StringBuilder builder = new StringBuilder();
                builder.append ("dtsAsset:" + str);
                LOGI (TAG, "[audioDtsAseetImpl]" + builder.toString());
                mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_SET_DTS_ASSET, builder.toString());
            }
        }

        private int getDtsAssetTotalNum() {
            int num = 0;
            if (mMediaPlayer != null) {
                num = mMediaPlayer.getIntParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_GET_DTS_ASSET_TOTAL);
            }
            LOGI (TAG, "[getDtsAssetTotalNum] num:" + num);
            return num;
        }
        private void play3DImpl (int idx) {
            LOGI (TAG, "[play3DSelect]idx:" + idx);
            boolean ret = false;
            if (mMediaPlayer != null) {
                // TODO: should open after 3d function debug ok
                ret = mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_SET_DISPLAY_MODE,idx);
                if (idx > 0) {
                    set_3d_flag = true;
                }
                else {
                    set_3d_flag = false;
                }
                LOGI (TAG, "[play3DSelect]ret:" + ret);
                if (!ret) {
                    if (mOption != null) {
                        mOption.set3DMode (0);
                    }
                    set_3d_flag = false;
                    Toast toast = Toast.makeText (VideoPlayer.this, getResources().getString (R.string.not_support_3d), Toast.LENGTH_SHORT);
                    toast.setGravity (Gravity.BOTTOM,/*110*/0, 0);
                    toast.setDuration (0x00000001);
                    toast.show();
                    startOsdTimeout();
                }
            }
        }

        private void close3D() {
            LOGI (TAG, "[close3D]mMediaPlayer:" + mMediaPlayer + ",mOption:" + mOption);
            if (mMediaPlayer != null && mOption != null) {
                if (set_3d_flag) {
                    mOption.set3DMode (0);
                    set_3d_flag = false;
                    mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_SET_DISPLAY_MODE,0);
                }
            }
        }

        //@@--------random seek function-------------------------------------------------------------------------------------------
        private boolean randomSeekTestFlag = false;
        private Random r = new Random (99);
        private boolean randomSeekEnable() {
            boolean ret = mSystemControl.getPropertyBoolean("sys.vprandomseek.enable", false);
            return ret;
        }
        private void randomSeekTest() {
            if (!randomSeekEnable()) {
                return;
            }
            if (r == null) {
                r = new Random();
            }
            int i = r.nextInt();
            LOGI (TAG, "[randomSeekTest]0i:" + i);
            if (i < 0) {
                i = i * -1;
            }
            i = i % 80;
            LOGI (TAG, "[randomSeekTest]1i:" + i);
            int pos = totaltime * (i + 1) / 100;
            randomSeekTestFlag = true;
            //check for small stream while seeking
            int pos_check = totaltime * (i + 1) - pos * 100;
            if (pos_check > 0) {
                pos += 1;
            }
            if (pos >= totaltime) {
                pos = totaltime;
            }
            LOGI (TAG, "[randomSeekTest]seekTo:" + pos);
            seekTo (pos);
        }

        //@@--------this part for play function implement--------------------------------------------------------------------------------
        // The ffmpeg step is 2*step
        private Toast ff_fb = null;
        private boolean FF_FLAG = false;
        private boolean FB_FLAG = false;
        private int FF_LEVEL = 0;
        private int FB_LEVEL = 0;
        private static int FF_MAX = 5;
        private static int FB_MAX = 5;
        private static int FF_SPEED[] = {0, 2, 4, 8, 16, 32};
        private static int FB_SPEED[] = {0, 2, 4, 8, 16, 32};
        private static int FF_STEP[] =  {0, 1, 2, 4, 8, 16};
        private static int FB_STEP[] =  {0, 1, 2, 4, 8, 16};
        private static int mRetryTimesMax = 5; // retry play after volume unmounted
        private static int mRetryTimes = mRetryTimesMax;
        private static int mRetryStep = 1000; //1000ms
        private boolean mRetrying = false;
        private Timer retryTimer = new Timer();
        private String mPath;

        private void updateIconResource() {
            if ( (progressBar == null) || (fastforwordBtn == null) || (fastreverseBtn == null) || (playBtn == null)) {
                return;
            }
            if (mState == STATE_PLAYING) {
                playBtn.setImageResource (R.drawable.pause);
            }
            else if (mState == STATE_PAUSED) {
                playBtn.setImageResource (R.drawable.play);
            }
            else if (mState == STATE_SEARCHING) {
                playBtn.setImageResource (R.drawable.play);
            }
            if (mCanSeek) {
                progressBar.setEnabled (true);
                if (mMediaPlayer != null) {
                    /*
                    String playerTypeStr = mMediaPlayer.getStringParameter(mMediaPlayer.KEY_PARAMETER_AML_PLAYER_TYPE_STR);
                    if ((playerTypeStr != null) && (playerTypeStr.equals("AMLOGIC_PLAYER"))) {
                      fastforwordBtn.setEnabled(true);
                      fastreverseBtn.setEnabled(true);
                      fastforwordBtn.setImageResource(R.drawable.ff);
                      fastreverseBtn.setImageResource(R.drawable.rewind);
                    }
                    else {
                      fastforwordBtn.setEnabled(false);
                      fastreverseBtn.setEnabled(false);
                      fastforwordBtn.setImageResource(R.drawable.ff_disable);
                      fastreverseBtn.setImageResource(R.drawable.rewind_disable);
                    }
                    */
                    fastforwordBtn.setEnabled (true);
                    fastreverseBtn.setEnabled (true);
                    fastforwordBtn.setImageResource (R.drawable.ff);
                    fastreverseBtn.setImageResource (R.drawable.rewind);
                }
                else {
                    fastforwordBtn.setEnabled (false);
                    fastreverseBtn.setEnabled (false);
                    fastforwordBtn.setImageResource (R.drawable.ff_disable);
                    fastreverseBtn.setImageResource (R.drawable.rewind_disable);
                }
            }
            else {
                progressBar.setEnabled (false);
                fastforwordBtn.setEnabled (false);
                fastreverseBtn.setEnabled (false);
                fastforwordBtn.setImageResource (R.drawable.ff_disable);
                fastreverseBtn.setImageResource (R.drawable.rewind_disable);
            }
        }

        private void resetVariate() {
            showDtsAseetFromInfoLis = false;
            progressBarSeekFlag = false;
            haveTried = false;
            mRetrying = false;
            mRetryTimes = mRetryTimesMax;
        }

        private void sendPlayFileMsg() {
            showOsdView();
            if (mHandler != null) {
                Message msg = mHandler.obtainMessage (MSG_PLAY);
                mHandler.sendMessageDelayed (msg, MSG_SEND_DELAY);
                LOGI (TAG, "[sendPlayFileMsg]sendMessageDelayed MSG_PLAY");
            }
        }

        private void playFile (String path) {
            LOGI (TAG, "[playFile]resume mode:" + mOption.getResumeMode() + ",path:" + path);
            if (mOption == null) {
                return;
            }
            resetVariate();
            if (mResumePlay.getEnable() == true) {
                setVideoPath (path);
                //showCtlBar();
                showOsdView();
                return;
            }
            if (mOption.getResumeMode() == true) {
                bmPlay (path);
            }
            else {
                setVideoPath (path);
            }
            //showCtlBar();
            showOsdView();
        }

        private void retryPlay() {
            LOGI (TAG, "[retryPlay]mRetryTimes:" + mRetryTimes + ",mRetryStep:" + mRetryStep + ",mResumePlay:" + mResumePlay);
            if (mResumePlay == null) {
                browserBack(); // no need to retry, back to file list
                return;
            }
            LOGI (TAG, "[retryPlay]mResumePlay.getEnable():" + mResumePlay.getEnable());
            if (false == mResumePlay.getEnable()) {
                browserBack(); // no need to retry, back to file list
                return;
            }
            mRetrying = true;
            TimerTask task = new TimerTask() {
                public void run() {
                    LOGI (TAG, "[retryPlay]TimerTask run mRetryTimes:" + mRetryTimes);
                    if (mRetryTimes > 0) {
                        mRetryTimes--;
                        if (mHandler != null) {
                            Message msg = mHandler.obtainMessage (MSG_RETRY_PLAY);
                            mHandler.sendMessageDelayed (msg, MSG_SEND_DELAY);
                            LOGI (TAG, "[retryPlay]sendMessageDelayed MSG_RETRY_PLAY");
                        }
                    }
                    else {
                        retryTimer.cancel();
                        retryTimer = null;
                        mRetrying = false;
                        if (mHandler != null) {
                            Message msg = mHandler.obtainMessage (MSG_RETRY_END);
                            mHandler.sendMessageDelayed (msg, MSG_SEND_DELAY);
                            LOGI (TAG, "[retryPlay]sendMessageDelayed MSG_RETRY_END");
                        }
                    }
                }
            };
            retryTimer = new Timer();
            retryTimer.schedule (task, mRetryStep);
        }

        private void browserBack() {
            LOGI (TAG, "[browserBack]backToOtherAPK:" + backToOtherAPK + ",browserBackDoing:" + browserBackDoing);
            if (browserBackDoing == true) {
                return;
            }
            item_position_selected = item_position_selected_init + mPlayList.getindex();
            browserBackDoing = true;
            if (browserBackInvokeFromOnPause == true) {
                mPlayList.rootPath = null;
            }
            else {
                if (!backToOtherAPK) {
                    Intent selectFileIntent = new Intent();
                    Bundle bundle = getFilePos();
                    selectFileIntent.setClass (VideoPlayer.this, FileList.class);
                    selectFileIntent.putExtras (bundle);
                    startActivity (selectFileIntent);
                }
            }
            showNoOsdView();
            sendStopMsg();
            finish();
        }

        private void playPause() {
            LOGI (TAG, "[playPause]mState:" + mState);
            if (mState == STATE_PLAYING) {
                pause();
            }
            else if (mState == STATE_PAUSED) {
                start();
            }
            else if (mState == STATE_SEARCHING) {
                stopFWFB();
                start();
            }
            startOsdTimeout();
        }

        private void playPrev() {
            LOGI (TAG, "[playPrev]mState:" + mState);
            stopOsdTimeout();
            if (mState != STATE_PREPARING) { // avoid status error for preparing
                close3D();
                stopFWFB();
                sendStopMsg();
                mBookmark.set (mPlayList.getcur(), curtime);
                mStateBac = STATE_STOP;
                mPath = mPlayList.moveprev();
                sendPlayFileMsg();
            }
            else {
                LOGI (TAG, "[playPrev]mState=STATE_PREPARING, error status do nothing only waitting");
            }
        }

        private void playNext() {
            LOGI (TAG, "[playNext]mState:" + mState);
            stopOsdTimeout();
            if (mState != STATE_PREPARING) { // avoid status error for preparing
                close3D();
                stopFWFB();
                sendStopMsg();
                mBookmark.set (mPlayList.getcur(), curtime);
                mStateBac = STATE_STOP;
                mPath = mPlayList.movenext();
                sendPlayFileMsg();
            }
            else {
                LOGI (TAG, "[playNext]mState=STATE_PREPARING, error status do nothing only waitting");
            }
        }

        private void playCur() {
            LOGI (TAG, "[playCur]");
            stopOsdTimeout();
            stopFWFB();
            sendStopMsg();
            curtime = 0;
            totaltime = 0;
            mBookmark.set (mPlayList.getcur(), curtime);
            mStateBac = STATE_STOP;
            mPath = mPlayList.getcur();
            sendPlayFileMsg();
        }

        private void fastForward() {
            LOGI (TAG, "[fastForward]mState:" + mState + ",FF_FLAG:" + FF_FLAG + ",FF_LEVEL:" + FF_LEVEL + ",FB_FLAG:" + FB_FLAG + ",FB_LEVEL:" + FB_LEVEL);
            progressBarSeekFlag = false;
            if (mState == STATE_SEARCHING) {
                if (FF_FLAG) {
                    if (FF_LEVEL < FF_MAX) {
                        FF_LEVEL = FF_LEVEL + 1;
                    }
                    else {
                        FF_LEVEL = 0;
                    }
                    FFimpl (FF_STEP[FF_LEVEL]);
                    if (FF_LEVEL == 0) {
                        ff_fb.cancel();
                        FF_FLAG = false;
                        start();
                    }
                    else {
                        ff_fb.setText (new String ("FF x" + Integer.toString (FF_SPEED[FF_LEVEL])));
                        ff_fb.show();
                    }
                }
                if (FB_FLAG) {
                    if (FB_LEVEL > 0) {
                        FB_LEVEL = FB_LEVEL - 1;
                    }
                    else {
                        FB_LEVEL = 0;
                    }
                    FBimpl (FB_STEP[FB_LEVEL]);
                    if (FB_LEVEL == 0) {
                        ff_fb.cancel();
                        FB_FLAG = false;
                        start();
                    }
                    else {
                        ff_fb.setText (new String ("FB x" + Integer.toString (FB_SPEED[FB_LEVEL])));
                        ff_fb.show();
                    }
                }
            }
            else {
                FFimpl (FF_STEP[1]);
                FF_FLAG = true;
                FF_LEVEL = 1;
                ff_fb.setText (new String ("FF x" + FF_SPEED[FF_LEVEL]));
                ff_fb.show();
            }
            startOsdTimeout();
        }

        private void fastBackward() {
            progressBarSeekFlag = false;
            if (mState == STATE_SEARCHING) {
                if (FB_FLAG) {
                    if (FB_LEVEL < FB_MAX) {
                        FB_LEVEL = FB_LEVEL + 1;
                    }
                    else {
                        FB_LEVEL = 0;
                    }
                    FBimpl (FB_STEP[FB_LEVEL]);
                    if (FB_LEVEL == 0) {
                        ff_fb.cancel();
                        FB_FLAG = false;
                        start();
                    }
                    else {
                        ff_fb.setText (new String ("FB x" + Integer.toString (FB_SPEED[FB_LEVEL])));
                        ff_fb.show();
                    }
                }
                if (FF_FLAG) {
                    if (FF_LEVEL > 0) {
                        FF_LEVEL = FF_LEVEL - 1;
                    }
                    else {
                        FF_LEVEL = 0;
                    }
                    FFimpl (FF_STEP[FF_LEVEL]);
                    if (FF_LEVEL == 0) {
                        ff_fb.cancel();
                        FF_FLAG = false;
                        start();
                    }
                    else {
                        ff_fb.setText (new String ("FF x" + Integer.toString (FF_SPEED[FF_LEVEL])));
                        ff_fb.show();
                    }
                }
            }
            else {
                FBimpl (FB_STEP[1]);
                FB_FLAG = true;
                FB_LEVEL = 1;
                ff_fb.setText (new String ("FB x" + FB_SPEED[FB_LEVEL]));
                ff_fb.show();
            }
            startOsdTimeout();
        }

        private void stopFWFB() {
            if (ff_fb != null) {
                ff_fb.cancel();
            }
            if (FF_FLAG) {
                FFimpl (0);
            }
            if (FB_FLAG) {
                FBimpl (0);
            }
            FF_FLAG = false;
            FB_FLAG = false;
            FF_LEVEL = 0;
            FB_LEVEL = 0;
        }

        private void FFimpl (int para) {
            if (isDemoVersion()) {
                Toast toast = Toast.makeText (VideoPlayer.this, "this function is not opened for demo version", Toast.LENGTH_SHORT);
                toast.setGravity (Gravity.BOTTOM, 0, 0);
                toast.setDuration (0x00000001);
                toast.show();
                return;
            }
            if (mMediaPlayer != null) {
                LOGI (TAG, "[FFimpl]para:" + para);
                if (para > 0) {
                    mState = STATE_SEARCHING;
                }
                else if (para == 0) {
                    mState = STATE_PLAYING;
                }
                updateIconResource();
                /*
                            String str = Integer.toString(para);
                            StringBuilder builder = new StringBuilder();
                            builder.append("forward:"+str);
                            LOGI(TAG,"[FFimpl]"+builder.toString());
                            mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_TRICKPLAY_FORWARD,builder.toString());
                */
                mMediaPlayer.fastForward (para);
            }
        }

        private void FBimpl (int para) {
            if (isDemoVersion()) {
                Toast toast = Toast.makeText (VideoPlayer.this, "this function is not opened for demo version", Toast.LENGTH_SHORT);
                toast.setGravity (Gravity.BOTTOM, 0, 0);
                toast.setDuration (0x00000001);
                toast.show();
                return;
            }
            if (mMediaPlayer != null) {
                LOGI (TAG, "[FBimpl]para:" + para);
                if (para > 0) {
                    mState = STATE_SEARCHING;
                    mStateBac = STATE_PLAYING; // add to update icon resource and status for FB to head
                }
                else if (para == 0) {
                    mState = STATE_PLAYING;
                }
                updateIconResource();
                /*
                            String str = Integer.toString(para);
                            StringBuilder builder = new StringBuilder();
                            builder.append("backward:"+str);
                            LOGI(TAG,"[FBimpl]"+builder.toString());
                            mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_TRICKPLAY_BACKWARD,builder.toString());
                */
                mMediaPlayer.fastBackward (para);
            }
        }

        private void seekByProgressBar() {
            int dest = progressBar.getProgress();
            int pos = totaltime * (dest + 1) / 100;
            //check for small stream while seeking
            int pos_check = totaltime * (dest + 1) - pos * 100;
            if (pos_check > 0) {
                pos += 1;
            }
            if (pos >= totaltime) {
                pos = totaltime;
            }
            if (dest <= 1) {
                pos = 0;
            }
            LOGI (TAG, "[seekByProgressBar]seekTo:" + pos);
            seekTo (pos);
            stopOsdTimeout();
            //curtime=pos;
        }

        //@@--------this part for play control------------------------------------------------------------------------------------------
        private MediaPlayerAmlogic mMediaPlayer = null;
        private static final int STATE_ERROR = -1;
        private static final int STATE_STOP = 0;
        private static final int STATE_PREPARING = 1;
        private static final int STATE_PREPARED = 2;
        private static final int STATE_PLAYING = 3;
        private static final int STATE_PAUSED = 4;
        private static final int STATE_PLAY_COMPLETED = 5;
        private static final int STATE_SEARCHING = 6;
        private int mState = STATE_STOP;
        private int mStateBac = STATE_STOP;
        private static final int SEEK_START = 0;//flag for seek stability to stop update progressbar
        private static final int SEEK_END = 1;
        private int mSeekState = SEEK_END;

        //@@private int mCurrentState = STATE_IDLE;
        //@@private int mTargetState  = STATE_IDLE;

        private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
            public void onAudioFocusChange (int focusChange) {
                Log.d (TAG, "onAudioFocusChange, focusChange: " + focusChange);
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        if (mMediaPlayer != null) {
                            Log.d (TAG, "onAudioFocusChange, setVolume 0");
                            mMediaPlayer.setVolume (0.0f);
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        if (mMediaPlayer != null) {
                            Log.d (TAG, "onAudioFocusChange, setVolume 1");
                            mMediaPlayer.setVolume (1.0f);
                        }
                        break;
                }
            }
        };

        private void sendStopMsg() {
            if (mHandler != null) {
                Message msg = mHandler.obtainMessage (MSG_STOP);
                mHandler.sendMessageDelayed (msg, MSG_SEND_DELAY);
                LOGI (TAG, "[sendStopMsg]sendMessageDelayed MSG_STOP");
            }
        }

        private void start() {
            LOGI (TAG, "[start]mMediaPlayer:" + mMediaPlayer);
            if (mMediaPlayer != null) {
                if (!mAudioFocused) {
                    mAudioFocused = true;
                    Log.d (TAG, "start, requestAudioFocus");
                    mAudioManager.requestAudioFocus (mAudioFocusListener, AudioManager.STREAM_MUSIC,
                                                     AudioManager.AUDIOFOCUS_GAIN);
                }
                mMediaPlayer.start();
                mSubtitleManager.start();
                mState = STATE_PLAYING;
                updateIconResource();
                if (mHandler != null) {
                    Message msg = mHandler.obtainMessage (MSG_UPDATE_PROGRESS);
                    mHandler.sendMessageDelayed (msg, MSG_SEND_DELAY);
                }
            }
        }

        private void pause() {
            LOGI (TAG, "[pause]mMediaPlayer:" + mMediaPlayer);
            if (mMediaPlayer != null) {
                if (/*isPlaying()*/mState == STATE_PLAYING) {
                    mMediaPlayer.pause();
                    mState = STATE_PAUSED;
                    updateIconResource();
                }
            }
        }

        private void stop() {
            LOGI (TAG, "[stop]mMediaPlayer:" + mMediaPlayer + ",mState:" + mState);
            if (mMediaPlayer != null && mState != STATE_STOP) {
                //close 3D
                close3D();

                mMediaPlayer.stop();
                mMediaPlayer.reset();
                mSubtitleManager.close();
                mState = STATE_STOP;
            }
        }

        private void release() {
            LOGI (TAG, "[release]mMediaPlayer:" + mMediaPlayer);
            if (mMediaPlayer != null) {
                mMediaPlayer.reset();
                mMediaPlayer.release();
                ///mMediaPlayer.setIgnoreSubtitle(false); //should sync with MediaPlayer.java
                mMediaPlayer = null;
                mSubtitleManager.release();
                mSubtitleManager = null;
                mState = STATE_STOP;
                //mStateBac = STATE_STOP; //shield for resume play while is in pause status
                mSeekState = SEEK_END;
                if (mAudioFocused) {
                    Log.d (TAG, "release, abandonAudioFocus");
                    mAudioManager.abandonAudioFocus (mAudioFocusListener);
                    mAudioFocused = false;
                }
            }
        }

        private int getDuration() {
            //LOGI(TAG,"[getDuration]mMediaPlayer:"+mMediaPlayer);
            if (mMediaPlayer != null) {
                return mMediaPlayer.getDuration();
            }
            return 0;
        }

        private int getCurrentPosition() {
            //LOGI(TAG,"[getCurrentPosition]mMediaPlayer:"+mMediaPlayer);
            if (mMediaPlayer != null) {
                return mMediaPlayer.getCurrentPosition();
            }
            return 0;
        }

        public boolean isPlaying() {
            //LOGI(TAG,"[isPlaying]mMediaPlayer:"+mMediaPlayer);
            boolean ret = false;
            if (mMediaPlayer != null) {
                if (mState != STATE_ERROR &&
                        mState != STATE_STOP &&
                        mState != STATE_PREPARING) {
                    ret = mMediaPlayer.isPlaying();
                }
            }
            return ret;
        }

        private void seekTo (int msec) {
            LOGI (TAG, "[seekTo]msec:" + msec + ",mState:" + mState);
            if (mMediaPlayer != null && mCanSeek == true) {
                // stop update progress bar
                mSeekState = SEEK_START;
                if (mHandler != null) {
                    mHandler.removeMessages (MSG_UPDATE_PROGRESS);
                }
                if (mState == STATE_SEARCHING) {
                    mStateBac = STATE_PLAYING;
                }
                else if (mState == STATE_PLAYING || mState == STATE_PAUSED) {
                    mStateBac = mState;
                }
                else {
                    mStateBac = STATE_ERROR;
                    LOGI (TAG, "[seekTo]state error for seek, state:" + mState);
                    return;
                }
                stopFWFB();
                mMediaPlayer.seekTo (msec);
                //mState = STATE_SEARCHING;
                //updateIconResource();
            }
        }

        private void setVideoPath (String path) {
            //LOGI(TAG,"[setVideoPath]path:"+path);
            /*Uri uri = null;
            String[] cols = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA
            };

            if (mContext == null) {
                LOGE(TAG,"[setVideoPath]mContext=null error!!!");
                return;
            }

            //change path to uri such as content://media/external/video/media/8206
            ContentResolver resolver = mContext.getContentResolver();
            String where = MediaStore.Video.Media.DATA + "=?" + path;
            Cursor cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols, where , null, null);
            if (cursor != null && cursor.getCount() == 1) {
                int colidx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                cursor.moveToFirst();
                int id = cursor.getInt(colidx);
                uri = MediaStore.Video.Media.getContentUri("external");
                String uriStr = uri.toString() + "/" + Integer.toString(id);
                uri = Uri.parse(uriStr);
                LOGI(TAG,"[setVideoPath]uri:"+uri.toString());
            }

            if (uri == null) {
                LOGE(TAG,"[setVideoPath]uri=null error!!!");
                return;
            }
            setVideoURI(uri);*/
            /*LOGI(TAG,"[setVideoPath]Uri.parse(path):"+Uri.parse(path));
            Uri uri = Uri.parse(path);
            AssetFileDescriptor fd = null;
            try {
                ContentResolver resolver = mContext.getContentResolver();
                fd = resolver.openAssetFileDescriptor(uri, "r");
                if (fd == null) {
                    LOGE(TAG,"[setVideoPath]fd =null error!!!");
                    return;
                }
                if (fd.getDeclaredLength() < 0) {
                    mMediaPlayer.setDataSource(fd.getFileDescriptor());
                } else {
                    mMediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getDeclaredLength());
                }
                mMediaPlayer.prepare();
                return;
            } catch (SecurityException ex) {
                LOGE(TAG, "[SecurityException]Unable to open content: " + mUri+",ex:"+ex);
                mState = STATE_ERROR;
                mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            } catch (IOException ex) {
                LOGE(TAG, "[IOException]Unable to open content: " + mUri+",ex:"+ex);
                mState = STATE_ERROR;
                mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            } catch (IllegalArgumentException ex) {
                LOGE(TAG, "[IllegalArgumentException]Unable to open content: " + mUri+",ex:"+ex);
                mState = STATE_ERROR;
                mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            }finally {
                if (fd != null) {
                    try {
                        fd.close();
                    } catch (IOException ex) {}
                }
            }*/
            LOGI (TAG, "[setVideoPath]Uri.parse(path):" + Uri.parse (path));
            setVideoURI (Uri.parse (path), path); //add path to resolve special character for uri, such as  ";" | "/" | "?" | ":" | "@" | "&" | "=" | "+" |"$" | ","
        }

        private void setVideoURI (Uri uri, String path) {
            LOGI (TAG, "[setVideoURI]uri:" + uri + ",path:" + path);
            setVideoURI (uri, null, path);
        }

        private void setVideoURI (Uri uri, Map<String, String> headers, String path) {
            LOGI (TAG, "[setVideoURI]uri:" + uri + ",headers:" + headers + ",mState:" + mState);
            mUri = uri;
            mHeaders = headers;
            try {
                mMediaPlayer.setDataSource (mContext, mUri, mHeaders);
                mSubtitleManager.setSource (mContext, mUri);
                mState = STATE_PREPARING;
                mMediaPlayer.prepare();
            }
            catch (IOException ex) {
                LOGE (TAG, "Unable to open content: " + mUri + ",ex:" + ex);
                if (haveTried == false) {
                    haveTried = true;
                    trySetVideoPathAgain (uri, headers, path);
                }
                else {
                    mState = STATE_ERROR;
                    mErrorListener.onError (mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
                }
            }
            catch (IllegalArgumentException ex) {
                LOGE (TAG, "Unable to open content: " + mUri + ",ex:" + ex);
                mState = STATE_ERROR;
                mErrorListener.onError (mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            }
            //requestLayout();
            //invalidate();
        }

        private boolean haveTried = false;
        private void trySetVideoURIAgain (Uri uri, Map<String, String> headers, String paramPath) {
            if (uri == null) {
                LOGE (TAG, "[trySetVideoURIAgain]init uri=null error!!!");
                return;
            }
            if (mContext == null) {
                LOGE (TAG, "[trySetVideoURIAgain]mContext=null error!!!");
                return;
            }
            LOGI (TAG, "[trySetVideoURIAgain]path:" + uri.getPath());
            Uri uriTmp = null;
            String path = uri.getPath();
            String[] cols = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA
            };
            //change path to uri such as content://media/external/video/media/8206
            ContentResolver resolver = mContext.getContentResolver();
            Cursor cursor = resolver.query (MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                int destIdx = -1;
                int len = cursor.getCount();
                LOGI (TAG, "[trySetVideoURIAgain]len:" + len);
                String [] pathList = new String[len];
                cursor.moveToFirst();
                int dataIdx = cursor.getColumnIndexOrThrow (MediaStore.Video.Media.DATA);
                int idIdx = cursor.getColumnIndexOrThrow (MediaStore.Video.Media._ID);
                for (int i = 0; i < len; i++) {
                    LOGI (TAG, "[trySetVideoURIAgain]cursor.getString(dataIdx):" + cursor.getString (dataIdx));
                    if ( (cursor.getString (dataIdx)).startsWith (path)) {
                        destIdx = cursor.getInt (idIdx);
                        LOGI (TAG, "[trySetVideoURIAgain]destIdx:" + destIdx);
                        break;
                    }
                    else {
                        cursor.moveToNext();
                    }
                }
                if (destIdx >= 0) {
                    uriTmp = MediaStore.Video.Media.getContentUri ("external");
                    String uriStr = uriTmp.toString() + "/" + Integer.toString (destIdx);
                    uriTmp = Uri.parse (uriStr);
                    LOGI (TAG, "[trySetVideoURIAgain]uriTmp:" + uriTmp.toString());
                }
            }
            cursor.close();
            if (uriTmp == null) {
                LOGE (TAG, "[trySetVideoURIAgain]uriTmp=null error!!!");
                Toast.makeText (mContext, mContext.getText (R.string.wait_for_scan), Toast.LENGTH_SHORT).show();
                browserBack();
                return;
            }
            LOGI (TAG, "[trySetVideoURIAgain]setVideoURI uriTmp:" + uriTmp);
            setVideoURI (uriTmp, paramPath);
        }

        private void trySetVideoPathAgain (Uri uri, Map<String, String> headers, String path) {
            LOGI (TAG, "[trySetVideoPathAgain]path:" + path);
            try {
                mMediaPlayer.setDataSource (path);
                mSubtitleManager.setSource (path);
                mState = STATE_PREPARING;
                mMediaPlayer.prepare();
            }
            catch (IOException ex) {
                LOGE (TAG, "[trySetVideoPathAgain] Unable to open content: " + path + ",ex:" + ex);
                trySetVideoURIAgain (uri, headers, path); // should debug, maybe some error
                mState = STATE_ERROR;
                mErrorListener.onError (mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            }
            catch (IllegalArgumentException ex) {
                LOGE (TAG, "[trySetVideoPathAgain] Unable to open content: " + path + ",ex:" + ex);
                mState = STATE_ERROR;
                mErrorListener.onError (mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            }
        }

        private void initPlayer() {
            LOGI (TAG, "[initPlayer]mSurfaceHolder:" + mSurfaceHolder);
            if (mSurfaceHolder == null) {
                return;
            }
            release();
            mMediaPlayer = new MediaPlayerAmlogic();
            mSubtitleManager = new SubtitleManager (mMediaPlayer);
            ///mMediaPlayer.setIgnoreSubtitle(true); //should sync with MediaPlayer.java
            mMediaPlayer.setOnPreparedListener (mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener (mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener (mCompletionListener);
            mMediaPlayer.setOnSeekCompleteListener (mSeekCompleteListener);
            mMediaPlayer.setOnErrorListener (mErrorListener);
            mMediaPlayer.setOnInfoListener (mInfoListener);
            mMediaPlayer.setDisplay (mSurfaceHolder);
        }

        //@@--------this part for listener----------------------------------------------------------------------------------------------
        private boolean mCanPause;
        private boolean mCanSeek;
        private boolean mCanSeekBack;
        private boolean mCanSeekForward;
        private boolean showDtsAseetFromInfoLis;
        private long mErrorTime = 0;
        private long mErrorTimeBac = 0;
        MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
        new MediaPlayer.OnVideoSizeChangedListener() {
            public void onVideoSizeChanged (MediaPlayer mp, int width, int height) {
                LOGI (TAG, "[onVideoSizeChanged]");
                displayModeImpl();
                if (mMediaPlayer != null && mVideoView != null) {
                    int videoWidth = mMediaPlayer.getVideoWidth();
                    int videoHeight = mMediaPlayer.getVideoHeight();
                    LOGI (TAG, "[onVideoSizeChanged]videoWidth:" + videoWidth + ",videoHeight:" + videoHeight);
                    if (videoWidth != 0 && videoHeight != 0) {
                        displayModeImpl();
                        mVideoView.requestLayout();
                        /*mVideoView.getHolder().setFixedSize(videoWidth, videoHeight);
                        mVideoView.requestLayout();*/
                    }
                }
            }
        };

        MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
            public void onPrepared (MediaPlayer mp) {
                LOGI (TAG, "[mPreparedListener]onPrepared mp:" + mp);
                mState = STATE_PREPARED;
                Metadata data = mp.getMetadata (MediaPlayer.METADATA_ALL, MediaPlayer.BYPASS_METADATA_FILTER);
                if (data != null) {
                    mCanPause = !data.has (Metadata.PAUSE_AVAILABLE)
                                || data.getBoolean (Metadata.PAUSE_AVAILABLE);
                    mCanSeekBack = !data.has (Metadata.SEEK_BACKWARD_AVAILABLE)
                                   || data.getBoolean (Metadata.SEEK_BACKWARD_AVAILABLE);
                    mCanSeekForward = !data.has (Metadata.SEEK_FORWARD_AVAILABLE)
                                      || data.getBoolean (Metadata.SEEK_FORWARD_AVAILABLE);
                    mCanSeek = mCanSeekBack && mCanSeekForward;
                    LOGI (TAG, "[mPreparedListener]mCanSeek:" + mCanSeek);
                }
                else {
                    mCanPause = mCanSeek = mCanSeekBack = mCanSeekForward = true;
                }

                data.recycleParcel();
                /*
                MediaPlayer.TrackInfo[] trackInfo = mp.getTrackInfo();
                if (trackInfo != null) {
                    LOGI(TAG,"[mPreparedListener]trackInfo.length:"+trackInfo.length);
                    for (int i = 0; i < trackInfo.length; i++) {
                        int trackType = trackInfo[i].getTrackType();
                        LOGI(TAG,"[mPreparedListener]trackInfo["+i+"].trackType:"+trackType);
                    }
                }*/
                if (mStateBac != STATE_PAUSED) {
                    start();
                }
                if (!isDemoVersion()) {
                    initSubtitle();
                    initMediaInfo();
                }
                displayModeImpl(); // init display mode //useless because it will reset when start playing, it should set after the moment playing
                showCertification(); // show certification
                if (mResumePlay.getEnable() == true) {
                    mResumePlay.setEnable (false);
                    int targetState = mStateBac; //get mStateBac first for seekTo will change mStateBac
                    mState = mStateBac; //prepare mState before seekTo
                    seekTo (mResumePlay.getTime());
                    LOGI (TAG, "[mPreparedListener]targetState:" + targetState);
                    if (targetState == STATE_PAUSED) {
                        start();
                        pause();
                    }
                    return;
                }
                if (mOption.getResumeMode() == true) {
                    if (bmPos > 0) {
                        seekTo (bmPos);
                    }
                }
            }
        };

        private MediaPlayer.OnCompletionListener mCompletionListener =
        new MediaPlayer.OnCompletionListener() {
            public void onCompletion (MediaPlayer mp) {
                LOGI (TAG, "[onCompletion] mOption.getRepeatMode():" + mOption.getRepeatMode());
                mState = STATE_PLAY_COMPLETED;
                curtime = 0; // reset current time
                curTimeTx.setText (secToTime (curtime / 1000));
                progressBar.setProgress (0);
                if (mOption.getRepeatMode() == mOption.REPEATONE) {
                    playCur();
                }
                else if (mOption.getRepeatMode() == mOption.REPEATLIST) {
                    playNext();
                }
                else {
                    LOGE (TAG, "[onCompletion] Wrong mOption.getRepeatMode():" + mOption.getRepeatMode());
                }
                /*mCurrentState = STATE_PLAYBACK_COMPLETED;
                mTargetState = STATE_PLAYBACK_COMPLETED;
                if (mMediaController != null) {
                    mMediaController.hide();
                }
                if (mOnCompletionListener != null) {
                    mOnCompletionListener.onCompletion(mMediaPlayer);
                }*/
            }
        };

        private MediaPlayer.OnSeekCompleteListener mSeekCompleteListener =
        new MediaPlayer.OnSeekCompleteListener() {
            public void onSeekComplete (MediaPlayer mp) {
                LOGI (TAG, "[onSeekComplete] progressBarSeekFlag:" + progressBarSeekFlag + ",mStateBac:" + mStateBac);
                if (!isDemoVersion()) {
                    if (mSubtitleManager != null) {
                        mSubtitleManager.resetForSeek();
                    }
                }
                if (progressBarSeekFlag == false) { //onStopTrackingTouch
                    stopFWFB(); //reset fw/fb status
                    if (mStateBac == STATE_PLAYING) {
                        start();
                    }
                    else if (mStateBac == STATE_PAUSED) {
                        pause();
                    }
                    else if (mStateBac == STATE_SEARCHING) {
                        // do nothing
                    }
                    else {
                        mStateBac = STATE_ERROR;
                        LOGI (TAG, "[onSeekComplete]mStateBac = STATE_ERROR.");
                    }
                    mStateBac = STATE_STOP;
                }
                //start update progress bar
                mSeekState = SEEK_END;
                if (mHandler != null) {
                    Message msg = mHandler.obtainMessage (MSG_UPDATE_PROGRESS);
                    mHandler.sendMessageDelayed (msg, MSG_SEND_DELAY + 1000);
                }
                if (randomSeekEnable()) {
                    if (randomSeekTestFlag) {
                        randomSeekTest();
                    }
                }
            }
        };

        private MediaPlayer.OnErrorListener mErrorListener =
        new MediaPlayer.OnErrorListener() {
            public boolean onError (MediaPlayer mp, int what, int extra) {
                Log.e (TAG, "Error: " + what + "," + extra);
                mErrorTime = java.lang.System.currentTimeMillis();
                int offset = (int) (mErrorTime - mErrorTimeBac);
                //Log.e(TAG, "[onError]mErrorTime:" + mErrorTime + ",mErrorTimeBac:" + mErrorTimeBac + ", offset:" + offset);
                if (offset > 300) {
                    mState = STATE_ERROR;
                    mErrorTimeBac = mErrorTime;
                    String infoStr = mErrorInfo.getErrorInfo (what, mPlayList.getcur());
                    Toast toast = Toast.makeText (VideoPlayer.this, "Status Error:" + infoStr, Toast.LENGTH_SHORT);
                    toast.setGravity (Gravity.BOTTOM,/*110*/0, 0);
                    toast.setDuration (0x00000001);
                    toast.show();
                    if (mOption.getRepeatMode() == mOption.REPEATLIST) {
                        playNext();
                    }
                    else {
                        browserBack();
                    }
                }
                return true;
            }
        };

        private final int DTS_NOR = 0;
        private final int DTS_EXPRESS = 1;
        private final int DTS_HD_MASTER_AUDIO = 2;
        private int mDtsType = DTS_NOR;
        private MediaPlayer.OnInfoListener mInfoListener =
        new MediaPlayer.OnInfoListener() {
            public  boolean onInfo (MediaPlayer mp, int arg1, int arg2) {
                LOGI (TAG, "[onInfo] mp: " + mp + ",arg1:" + arg1 + ",arg2:" + arg2);
                if (mp != null) {
                    boolean needShow = MediaInfo.needShowOnUI (arg1);
                    LOGI (TAG, "[onInfo] needShow: " + needShow);
                    if (needShow == true) {
                        String infoStr = MediaInfo.getInfo (arg1, VideoPlayer.this);
                        LOGI (TAG, "[onInfo] infoStr: " + infoStr);
                        Toast toast = Toast.makeText (VideoPlayer.this, /*"Media Info:"+*/infoStr, Toast.LENGTH_SHORT);
                        toast.setGravity (Gravity.BOTTOM,/*110*/0, 0);
                        toast.setDuration (0x00000001);
                        toast.show();
                    }
                    if (arg1 == mMediaInfo.MEDIA_INFO_AMLOGIC_SHOW_DTS_ASSET) {
                        if (getDtsAssetTotalNum() > 0) {
                            showDtsAseetFromInfoLis = true;
                            audioDtsAseetSelect();
                        }
                    }
                    else if (arg1 == mMediaInfo.MEDIA_INFO_AMLOGIC_SHOW_DTS_EXPRESS) {
                        mDtsType = DTS_EXPRESS;
                        showCertification();
                    }
                    else if (arg1 == mMediaInfo.MEDIA_INFO_AMLOGIC_SHOW_DTS_HD_MASTER_AUDIO) {
                        mDtsType = DTS_HD_MASTER_AUDIO;
                        showCertification();
                    }
                }
                return true;
            }
        };

        //@@--------this part for book mark play-------------------------------------------------------------------
        private AlertDialog confirm_dialog = null;
        private int bmPos = 0; // book mark postion
        private int resumeSecondMax = 8; //resume max second 8s
        private int resumeSecond = resumeSecondMax;
        private static final int MSG_COUNT_DOWN = 0xE1;//random value
        private boolean exitAbort = false; //indicate exit with abort
        private int bmPlay (String path) {
            bmPos = 0; //reset value for bmPos
            final int pos = mBookmark.get (path);
            if (pos > 0) {
                confirm_dialog = new AlertDialog.Builder (this)
                .setTitle (R.string.setting_resume)
                .setMessage (R.string.str_resume_play)
                .setPositiveButton (R.string.str_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick (DialogInterface dialog, int whichButton) {
                        bmPos = pos;
                    }
                })
                .setNegativeButton (VideoPlayer.this.getResources().getString (R.string.str_cancel) + " ( " + resumeSecond + " )",
                new DialogInterface.OnClickListener() {
                    public void onClick (DialogInterface dialog, int whichButton) {
                        bmPos = 0;
                    }
                })
                .show();
                confirm_dialog.setOnDismissListener (new confirmDismissListener());
                ResumeCountdown();
                return pos;
            }
            else {
                setVideoPath (path);
            }
            LOGI (TAG, "[bmPlay]pos is :" + pos);
            return pos;
        }

        protected void ResumeCountdown() {
            final Handler handler = new Handler() {
                public void handleMessage (Message msg) {
                    switch (msg.what) {
                        case MSG_COUNT_DOWN:
                            if (confirm_dialog.isShowing()) {
                                if (resumeSecond > 0) {
                                    String cancel = VideoPlayer.this.getResources().getString (R.string.str_cancel);
                                    confirm_dialog.getButton (AlertDialog.BUTTON_NEGATIVE)
                                    .setText (cancel + " ( " + (--resumeSecond) + " )");
                                    ResumeCountdown();
                                }
                                else {
                                    bmPos = 0;
                                    confirm_dialog.dismiss();
                                    resumeSecond = resumeSecondMax;
                                }
                            }
                            break;
                    }
                    super.handleMessage (msg);
                }
            };
            TimerTask task = new TimerTask() {
                public void run() {
                    Message message = Message.obtain();
                    message.what = MSG_COUNT_DOWN;
                    handler.sendMessage (message);
                }
            };
            Timer resumeTimer = new Timer();
            resumeTimer.schedule (task, 1000);
        }

        private class confirmDismissListener implements DialogInterface.OnDismissListener {
                public void onDismiss (DialogInterface arg0) {
                    if (!exitAbort) {
                        setVideoPath (mPlayList.getcur());
                        resumeSecond = resumeSecondMax;
                    }
                }
        }

        //@@--------this part for control bar, option bar, other widget, sub widget and info widget showing or not--------------------------------
        private Timer timer = new Timer();
        private static final int MSG_OSD_TIME_OUT = 0xd1;
        private static final int OSD_CTL_BAR = 0;
        private static final int OSD_OPT_BAR = 1;
        private int curOsdViewFlag = -1;
        private final int OSD_FADE_TIME = 5000; // osd showing timeout

        private final int RESUME_MODE = 0;
        private final int REPEAT_MODE = 1;
        private final int AUDIO_OPTION = 2;
        private final int AUDIO_TRACK = 3;
        private final int SOUND_TRACK = 4;
        private final int AUDIO_DTS_ASSET = 5;
        private final int DISPLAY_MODE = 6;
        private final int BRIGHTNESS = 7;
        private final int PLAY3D = 8;
        private int otherwidgetStatus = 0;

        protected void startOsdTimeout() {
            final Handler handler = new Handler() {
                public void handleMessage (Message msg) {
                    switch (msg.what) {
                        case MSG_OSD_TIME_OUT:
                            showNoOsdView();
                            break;
                    }
                    super.handleMessage (msg);
                }
            };
            TimerTask task = new TimerTask() {
                public void run() {
                    //@@if(!touchVolFlag) {
                    Message message = Message.obtain();
                    message.what = MSG_OSD_TIME_OUT;
                    handler.sendMessage (message);
                    //@@}
                }
            };
            stopOsdTimeout();
            if (timer == null) {
                timer = new Timer();
            }
            if (timer != null) {
                timer.schedule (task, OSD_FADE_TIME);
            }
        }

        private void stopOsdTimeout() {
            if (timer != null) {
                timer.cancel();
            }
            timer = null;
        }

        private int getCurOsdViewFlag() {
            LOGI (TAG, "[getCurOsdViewFlag]curOsdViewFlag:" + curOsdViewFlag);
            return curOsdViewFlag;
        }

        private void setCurOsdViewFlag (int osdView) {
            curOsdViewFlag = osdView;
        }

        private void showOtherWidget (int StrId) {
            if (null != otherwidget) {
                if (View.GONE == otherwidget.getVisibility()) {
                    otherwidget.setVisibility (View.VISIBLE);
                    if ( (null != optbar) && (View.VISIBLE == optbar.getVisibility())) {
                        optbar.setVisibility (View.GONE);
                    }
                }
                otherwidgetTitleTx.setText (StrId);
                otherwidget.requestFocus();
                otherwidgetStatus = StrId;
                stopOsdTimeout();
            }
        }

        private void showSubWidget (int StrId) {
            if ( (null != subwidget) && (View.GONE == subwidget.getVisibility())) {
                subwidget.setVisibility (View.VISIBLE);
                if ( (null != optbar) && (View.VISIBLE == optbar.getVisibility())) {
                    optbar.setVisibility (View.GONE);
                }
                subwidget.requestFocus();
                otherwidgetStatus = StrId;
                stopOsdTimeout();
            }
        }

        private void showInfoWidget (int StrId) {
            TextView title;
            if ( (null != infowidget) && (View.GONE == infowidget.getVisibility())) {
                infowidget.setVisibility (View.VISIBLE);
                if ( (null != optbar) && (View.VISIBLE == optbar.getVisibility())) {
                    optbar.setVisibility (View.GONE);
                }
                title = (TextView) findViewById (R.id.info_title);
                title.setText (R.string.str_file_information);
                otherwidgetStatus = StrId;
                infowidget.requestFocus();
                stopOsdTimeout();
            }
        }

        private void exitOtherWidget (ImageButton btn) {
            if ( (null != otherwidget) && (View.VISIBLE == otherwidget.getVisibility())) {
                otherwidget.setVisibility (View.GONE);
                if ( (null != optbar) && (View.GONE == optbar.getVisibility())) {
                    optbar.setVisibility (View.VISIBLE);
                }
                btn.requestFocus();
                btn.requestFocusFromTouch();
                startOsdTimeout();
            }
        }

        private void exitSubWidget (ImageButton btn) {
            if ( (null != subwidget) && (View.VISIBLE == subwidget.getVisibility())) {
                subwidget.setVisibility (View.GONE);
                if ( (null != optbar) && (View.GONE == optbar.getVisibility())) {
                    optbar.setVisibility (View.VISIBLE);
                }
                btn.requestFocus();
                btn.requestFocusFromTouch();
                startOsdTimeout();
            }
        }

        private void exitInfoWidget (ImageButton btn) {
            if ( (null != infowidget) && (View.VISIBLE == infowidget.getVisibility())) {
                infowidget.setVisibility (View.GONE);
                if ( (null != optbar) && (View.GONE == optbar.getVisibility())) {
                    optbar.setVisibility (View.VISIBLE);
                }
                btn.requestFocus();
                btn.requestFocusFromTouch();
                startOsdTimeout();
            }
        }

        private void showCtlBar() {
            LOGI (TAG, "[showCtlBar]ctlbar:" + ctlbar + ",ctlbar.getVisibility():" + ctlbar.getVisibility());
            LOGI (TAG, "[showCtlBar]optbar:" + optbar + ",optbar.getVisibility():" + optbar.getVisibility());
            if ( (null != ctlbar) && (View.GONE == ctlbar.getVisibility())) {
                //@@getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                if ( (null != optbar) && (View.VISIBLE == optbar.getVisibility())) {
                    optbar.setVisibility (View.GONE);
                }
                if ( (null != subwidget) && (View.VISIBLE == subwidget.getVisibility())) {
                    subwidget.setVisibility (View.GONE);
                }
                if ( (null != otherwidget) && (View.VISIBLE == otherwidget.getVisibility())) {
                    otherwidget.setVisibility (View.GONE);
                }
                if ( (null != infowidget) && (View.VISIBLE == infowidget.getVisibility())) {
                    infowidget.setVisibility (View.GONE);
                }
                ctlbar.setVisibility (View.VISIBLE);
                ctlbar.requestFocus();
                //ctlbar.requestFocusFromTouch();
                //optBtn.requestFocus();
                //optBtn.requestFocusFromTouch();
                setCurOsdViewFlag (OSD_CTL_BAR);
            }
            startOsdTimeout();
            updateProgressbar();
        }

        private void showOptBar() {
            if ( (null != optbar) && (View.GONE == optbar.getVisibility())) {
                //@@getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                if ( (null != ctlbar) && (View.VISIBLE == ctlbar.getVisibility())) {
                    ctlbar.setVisibility (View.GONE);
                }
                if ( (null != subwidget) && (View.VISIBLE == subwidget.getVisibility())) {
                    subwidget.setVisibility (View.GONE);
                }
                if ( (null != otherwidget) && (View.VISIBLE == otherwidget.getVisibility())) {
                    otherwidget.setVisibility (View.GONE);
                }
                if ( (null != infowidget) && (View.VISIBLE == infowidget.getVisibility())) {
                    infowidget.setVisibility (View.GONE);
                }
                optbar.setVisibility (View.VISIBLE);
                optbar.requestFocus();
                //optbar.requestFocusFromTouch();
                //ctlBtn.requestFocus();
                //ctlBtn.requestFocusFromTouch();
                setCurOsdViewFlag (OSD_OPT_BAR);
            }
            startOsdTimeout();
        }

        private void showNoOsdView() {
            stopOsdTimeout();
            if ( (null != ctlbar) && (View.VISIBLE == ctlbar.getVisibility())) {
                ctlbar.setVisibility (View.GONE);
            }
            if ( (null != optbar) && (View.VISIBLE == optbar.getVisibility())) {
                optbar.setVisibility (View.GONE);
            }
            if ( (null != subwidget) && (View.VISIBLE == subwidget.getVisibility())) {
                subwidget.setVisibility (View.GONE);
            }
            if ( (null != otherwidget) && (View.VISIBLE == otherwidget.getVisibility())) {
                otherwidget.setVisibility (View.GONE);
            }
            if ( (null != infowidget) && (View.VISIBLE == infowidget.getVisibility())) {
                infowidget.setVisibility (View.GONE);
            }
            showSystemUi (false);
            //@@getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            //@@WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        private void showOsdView() {
            LOGI (TAG, "[showOsdView]");
            if (null == ctlbar) {
                return;
            }
            if (null == optbar) {
                return;
            }
            int flag = getCurOsdViewFlag();
            LOGI (TAG, "[showOsdView]flag:" + flag);
            switch (flag) {
                case OSD_CTL_BAR:
                    showCtlBar();
                    break;
                case OSD_OPT_BAR:
                    showOptBar();
                    break;
                default:
                    LOGE (TAG, "[showOsdView]getCurOsdView error flag:" + flag + ",set CurOsdView default");
                    showCtlBar();
                    break;
            }
            showSystemUi (true);
        }

        private void switchOsdView() {
            if (null == ctlbar) {
                return;
            }
            if (null == optbar) {
                return;
            }
            if (isDemoVersion()) {
                Toast toast = Toast.makeText (VideoPlayer.this, "this function is not opened for demo version", Toast.LENGTH_SHORT);
                toast.setGravity (Gravity.BOTTOM, 0, 0);
                toast.setDuration (0x00000001);
                toast.show();
                return;
            }
            int flag = getCurOsdViewFlag();
            switch (flag) {
                case OSD_CTL_BAR:
                    showOptBar();
                    break;
                case OSD_OPT_BAR:
                    showCtlBar();
                    break;
                default:
                    LOGE (TAG, "[switchOsdView]getCurOsdView error flag:" + flag + ",set CurOsdView default");
                    showCtlBar();
                    break;
            }
        }

        @SuppressWarnings ("deprecation")
        @TargetApi (Build.VERSION_CODES.JELLY_BEAN)
        private void showSystemUi (boolean visible) {
            LOGI (TAG, "[showSystemUi]visible:" + visible + ",mVideoView:" + mVideoView);
            if (mVideoView == null) {
                return;
            }
            int flag = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                       | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                       | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            if (!visible) {
                // We used the deprecated "STATUS_BAR_HIDDEN" for unbundling
                flag |= View.STATUS_BAR_HIDDEN | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }
            mVideoView.setSystemUiVisibility (flag);
        }

        private int mLastSystemUiVis = 0;
        @TargetApi (Build.VERSION_CODES.JELLY_BEAN)
        private void setOnSystemUiVisibilityChangeListener() {
            //if (!ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_HIDE_NAVIGATION) return;
            mVideoView.setOnSystemUiVisibilityChangeListener (
            new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange (int visibility) {
                    LOGI (TAG, "[onSystemUiVisibilityChange]visibility:" + visibility + ",mLastSystemUiVis:" + mLastSystemUiVis);
                    int diff = mLastSystemUiVis ^ visibility;
                    mLastSystemUiVis = visibility;
                    LOGI (TAG, "[onSystemUiVisibilityChange]diff:" + diff);
                    if ( (diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                    && (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                        showOsdView();
                    }
                }
            });
        }

        //@@--------this part for showing certification of Dolby and DTS----------------------------------------------------
        private void showCertification() {
            if (certificationDoblyView == null && certificationDoblyPlusView == null && certificationDTSView == null && certificationDTSExpressView == null && certificationDTSHDMasterAudioView == null) {
                return;
            }
            if (mMediaInfo == null) {
                return;
            }
            if (mOption == null) {
                return;
            }
            closeCertification();
            if (mMediaInfo.getAudioTotalNum() <= 0) {
                return;
            }
            int track = mOption.getAudioTrack();
            if (track < 0) {
                track = 0;
            }
            else if (track >= mMediaInfo.getAudioTotalNum()) {
                track = mMediaInfo.getAudioTotalNum() - 1;
            }
            int ret = mMediaInfo.checkAudioCertification (mMediaInfo.getAudioFormat (track/*mMediaInfo.getCurAudioIdx()*/));
            LOGI (TAG, "[showCertification]ret:" + ret);
            if (ret == mMediaInfo.CERTIFI_Dolby) {
                if (mSystemControl.getPropertyBoolean("ro.platform.support.dolby", false)) {
                    certificationDoblyView.setVisibility (View.VISIBLE);
                    certificationDoblyPlusView.setVisibility (View.GONE);
                    certificationDTSView.setVisibility (View.GONE);
                    certificationDTSExpressView.setVisibility (View.GONE);
                    certificationDTSHDMasterAudioView.setVisibility (View.GONE);
                }
            }
            else if (ret == mMediaInfo.CERTIFI_Dolby_Plus) {
                if (mSystemControl.getPropertyBoolean("ro.platform.support.dolby", false)) {
                    certificationDoblyView.setVisibility (View.GONE);
                    certificationDoblyPlusView.setVisibility (View.VISIBLE);
                    certificationDTSView.setVisibility (View.GONE);
                    certificationDTSExpressView.setVisibility (View.GONE);
                    certificationDTSHDMasterAudioView.setVisibility (View.GONE);
                }
            }
            else if (ret == mMediaInfo.CERTIFI_DTS) {
                if (mSystemControl.getPropertyBoolean("ro.platform.support.dts", false)) {
                    certificationDoblyView.setVisibility (View.GONE);
                    certificationDoblyPlusView.setVisibility (View.GONE);
                    if (mDtsType == DTS_NOR) {
                        certificationDTSView.setVisibility (View.VISIBLE);
                        certificationDTSExpressView.setVisibility (View.GONE);
                        certificationDTSHDMasterAudioView.setVisibility (View.GONE);
                    }
                    else if (mDtsType == DTS_EXPRESS) {
                        certificationDTSView.setVisibility (View.GONE);
                        certificationDTSExpressView.setVisibility (View.VISIBLE);
                        certificationDTSHDMasterAudioView.setVisibility (View.GONE);
                    }
                    else if (mDtsType == DTS_HD_MASTER_AUDIO) {
                        certificationDTSView.setVisibility (View.GONE);
                        certificationDTSExpressView.setVisibility (View.GONE);
                        certificationDTSHDMasterAudioView.setVisibility (View.VISIBLE);
                    }
                }
            }
            else {
                certificationDoblyView.setVisibility (View.GONE);
                certificationDoblyPlusView.setVisibility (View.GONE);
                certificationDTSView.setVisibility (View.GONE);
                certificationDTSExpressView.setVisibility (View.GONE);
                certificationDTSHDMasterAudioView.setVisibility (View.GONE);
            }
        }

        private void closeCertification() {
            if (certificationDoblyView != null && certificationDoblyPlusView != null && certificationDTSView != null && certificationDTSExpressView != null && certificationDTSHDMasterAudioView != null) {
                if (certificationDoblyView.getVisibility() == View.VISIBLE) {
                    certificationDoblyView.setVisibility (View.GONE);
                }
                if (certificationDoblyPlusView.getVisibility() == View.VISIBLE) {
                    certificationDoblyPlusView.setVisibility (View.GONE);
                }
                if (certificationDTSView.getVisibility() == View.VISIBLE) {
                    certificationDTSView.setVisibility (View.GONE);
                }
                if (certificationDTSExpressView.getVisibility() == View.VISIBLE) {
                    certificationDTSExpressView.setVisibility (View.GONE);
                }
                if (certificationDTSHDMasterAudioView.getVisibility() == View.VISIBLE) {
                    certificationDTSHDMasterAudioView.setVisibility (View.GONE);
                }
            }
        }

        //@@--------this part for touch and key event-------------------------------------------------------------------
        public boolean onTouchEvent (MotionEvent event) {
            LOGI (TAG, "[onTouchEvent]ctlbar.getVisibility():" + ctlbar.getVisibility() + ",event.getAction():" + event.getAction());
            super.onTouchEvent (event);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if ( (ctlbar.getVisibility() == View.VISIBLE) || (optbar.getVisibility() == View.VISIBLE)) {
                    showNoOsdView();
                }
                else if ( (View.VISIBLE == otherwidget.getVisibility())
                          || (View.VISIBLE == infowidget.getVisibility())
                          || (View.VISIBLE == subwidget.getVisibility())) {
                    showNoOsdView();
                }
                else {
                    showOsdView();
                }
                int flag = getCurOsdViewFlag();
                if ( (OSD_CTL_BAR == flag) && ( (mState == STATE_PLAYING) || (mState == STATE_SEARCHING))) {
                    updateProgressbar();
                }
                intouch_flag = true;
            }
            return true;
        }

        public boolean onKeyUp (int keyCode, KeyEvent msg) {
            LOGI (TAG, "[onKeyUp]keyCode:" + keyCode + ",ctlbar.getVisibility():" + ctlbar.getVisibility());
            /*if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
                startOsdTimeout();
            }*/
            if (!browserBackDoing && ( (ctlbar.getVisibility() == View.VISIBLE) || (optbar.getVisibility() == View.VISIBLE))) {
                startOsdTimeout();
            }
            return false;
        }

        public boolean onKeyDown (int keyCode, KeyEvent msg) {
            LOGI (TAG, "[onKeyDown]keyCode:" + keyCode + ",ctlbar.getVisibility():" + ctlbar.getVisibility() + ",intouch_flag:" + intouch_flag);
            if ( (ctlbar.getVisibility() == View.VISIBLE) || (optbar.getVisibility() == View.VISIBLE)) {
                if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
                    startOsdTimeout();
                }
                else {
                    stopOsdTimeout();
                }
            }
            if (intouch_flag) {
                if ( (ctlbar.getVisibility() == View.VISIBLE) || (optbar.getVisibility() == View.VISIBLE)) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP
                            || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                            || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                            || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        int flag = getCurOsdViewFlag();
                        if (OSD_CTL_BAR == flag) {
                            ctlbar.requestFocusFromTouch();
                        }
                        else if (OSD_OPT_BAR == flag) {
                            optbar.requestFocusFromTouch();
                        }
                        intouch_flag = false;
                    }
                }
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) { // add for progressBar request focus fix bug 87713
                if ( (getCurOsdViewFlag() == OSD_CTL_BAR) && (ctlbar.getVisibility() == View.VISIBLE)) {
                    if (progressBar != null) {
                        progressBar.requestFocus();
                    }
                }
                else {
                    return false;
                }
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if ( (ctlbar.getVisibility() == View.GONE) && (optbar.getVisibility() == View.GONE)) {
                    showOsdView();
                    int flag = getCurOsdViewFlag();
                    if (OSD_CTL_BAR == flag) {
                        playBtn.requestFocusFromTouch();
                        playBtn.requestFocus();
                    }
                }
                else {
                    showNoOsdView();
                }
            }
            else if (keyCode == KeyEvent.KEYCODE_POWER) {
                if (mState == STATE_PLAYING
                        || mState == STATE_PAUSED
                        || mState == STATE_SEARCHING) {
                    pause();
                    //stop();
                }
            }
            else if (keyCode == KeyEvent.KEYCODE_BACK) {
                int flag = getCurOsdViewFlag();
                if (OSD_OPT_BAR == flag) {
                    if ( (otherwidget.getVisibility() == View.VISIBLE)
                            || (infowidget.getVisibility() == View.VISIBLE)
                            || (subwidget.getVisibility() == View.VISIBLE)) {
                        showOsdView();
                        switch (otherwidgetStatus) {
                            case R.string.setting_resume:
                                resumeModeBtn.requestFocusFromTouch();
                                resumeModeBtn.requestFocus();
                                break;
                            case R.string.setting_playmode:
                                repeatModeBtn.requestFocusFromTouch();
                                repeatModeBtn.requestFocus();
                                break;
                            case R.string.setting_3d_mode:
                                play3dBtn.requestFocusFromTouch();
                                play3dBtn.requestFocus();
                                break;
                            case R.string.setting_audiooption:
                                audiooptionBtn.requestFocusFromTouch();
                                audiooptionBtn.requestFocus();
                                break;
                            case R.string.setting_audiotrack:
                            case R.string.setting_soundtrack:
                                audioOption();
                                break;
                            case R.string.setting_audiodtsasset:
                                if (!showDtsAseetFromInfoLis) {
                                    audiotrackSelect();
                                }
                                break;
                            case R.string.setting_subtitle:
                                subtitleSwitchBtn.requestFocusFromTouch();
                                subtitleSwitchBtn.requestFocus();
                                break;
                            case R.string.setting_displaymode:
                                displayModeBtn.requestFocusFromTouch();
                                displayModeBtn.requestFocus();
                                break;
                            case R.string.setting_brightness:
                                brigtnessBtn.requestFocusFromTouch();
                                brigtnessBtn.requestFocus();
                                break;
                            case R.string.str_file_name:
                                fileinfoBtn.requestFocusFromTouch();
                                fileinfoBtn.requestFocus();
                                break;
                            default:
                                optbar.requestFocus();
                                break;
                        }
                    }
                    else {
                        switchOsdView();
                    }
                }
                else if (OSD_CTL_BAR == flag) {
                    browserBack();
                }
            }
            else if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_9) {
                if ( (ctlbar.getVisibility() == View.VISIBLE) || (optbar.getVisibility() == View.VISIBLE)) {
                    showNoOsdView();
                }
                else {
                    showOsdView();
                    int flag = getCurOsdViewFlag();
                    if (OSD_CTL_BAR == flag) {
                        playBtn.requestFocusFromTouch();
                        playBtn.requestFocus();
                    }
                }
            }
            else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                int flag = getCurOsdViewFlag();
                if (OSD_CTL_BAR == flag) {
                    showOsdView();
                }
                else if (OSD_OPT_BAR == flag) {
                    switchOsdView();
                }
                playPause();
                playBtn.requestFocusFromTouch();
                playBtn.requestFocus();
            }
            else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                playPrev();
            }
            else if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                playNext();
            }
            else if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                if (mCanSeek) {
                    fastForward();
                    fastforwordBtn.requestFocusFromTouch();
                    fastforwordBtn.requestFocus();
                }
            }
            else if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
                if (mCanSeek) {
                    fastBackward();
                    fastreverseBtn.requestFocusFromTouch();
                    fastreverseBtn.requestFocus();
                }
            }
            else if (keyCode == KeyEvent.KEYCODE_MUTE) {
                int flag = getCurOsdViewFlag();
                if (OSD_CTL_BAR == flag) {
                    showOsdView();
                    playBtn.requestFocusFromTouch();
                    playBtn.requestFocus();
                }
                else if (OSD_OPT_BAR == flag) {
                    if (! (otherwidget.getVisibility() == View.VISIBLE)
                            && ! (infowidget.getVisibility() == View.VISIBLE)
                            && ! (subwidget.getVisibility() == View.VISIBLE)) {
                        showOsdView();
                    }
                }
            }
            else if (keyCode == KeyEvent.KEYCODE_F10) {//3D switch
                // TODO: 3D switch
            }
            else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                int flag = getCurOsdViewFlag();
                if (OSD_CTL_BAR == flag) {
                    showOsdView();
                }
                else if (OSD_OPT_BAR == flag) {
                    switchOsdView();
                }
                if (mState == STATE_PAUSED) {
                    start();
                }
                else if (mState == STATE_SEARCHING) {
                    stopFWFB();
                    start();
                }
                playBtn.requestFocusFromTouch();
                playBtn.requestFocus();
            }
            else if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                int flag = getCurOsdViewFlag();
                if (OSD_CTL_BAR == flag) {
                    showOsdView();
                }
                else if (OSD_OPT_BAR == flag) {
                    switchOsdView();
                }
                if (mState == STATE_PLAYING) {
                    pause();
                }
                playBtn.requestFocusFromTouch();
                playBtn.requestFocus();
            }
            else {
                return super.onKeyDown (keyCode, msg);
            }
            return true;
        }

        //@@--------this part for subtitle switch wedgit------------------------------------------------------------------
        private subview_set sub_para = null;
        public static final String subSettingStr = "subtitlesetting";
        private int sub_switch_state = 0;
        private int sub_font_state = 0;
        private int sub_color_state = 0;
        private int sub_position_v_state = 0;
        private TextView t_subswitch = null ;
        private TextView t_subsfont = null ;
        private TextView t_subscolor = null ;
        private TextView t_subsposition_v = null;

        private void initSubtitle() {
            LOGI (TAG, "[initSubtitle]");
            SharedPreferences subSp = getSharedPreferences (subSettingStr, 0);
            sub_para = new subview_set();
            sub_para.totalnum = 0;
            sub_para.curid = 0;
            sub_para.curidbac = 0;
            sub_para.color = android.graphics.Color.WHITE;
            sub_para.font = 20;
            sub_para.position_v = 0;
            //sub_para.color = subSp.getInt("color", android.graphics.Color.WHITE);
            //sub_para.font=subSp.getInt("font", 20);
            //sub_para.position_v=subSp.getInt("position_v", 0);
            setSubtitleView();
        }

        private void subtitle_prepare() {
            if (mMediaPlayer != null) {
                sub_para.totalnum = mSubtitleManager.total();
            }
        }

        private void setSubtitleView() {
            LOGI (TAG, "[setSubtitleView]mMediaPlayer:" + mMediaPlayer);
            if (mMediaPlayer != null) {
                //mMediaPlayer.subtitleClear();
                mSubtitleManager.setGravity (Gravity.CENTER);
                mSubtitleManager.setTextColor (sub_para.color);
                mSubtitleManager.setTextSize (sub_para.font);
                mSubtitleManager.setTextStyle (Typeface.BOLD);
                mSubtitleManager.setPosHeight (getWindowManager().getDefaultDisplay().getHeight() *sub_para.position_v / 20 + 10);
            }
        }

        private void subtitle_control() {
            LOGI (TAG, "[subtitle_control]");
            final String color_text[] = {
                VideoPlayer.this.getResources().getString (R.string.color_white),
                VideoPlayer.this.getResources().getString (R.string.color_yellow),
                VideoPlayer.this.getResources().getString (R.string.color_blue)
            };
            Button ok = (Button) findViewById (R.id.button_ok);
            Button cancel = (Button) findViewById (R.id.button_canncel);
            ImageButton Bswitch_l = (ImageButton) findViewById (R.id.switch_l);
            ImageButton Bswitch_r = (ImageButton) findViewById (R.id.switch_r);
            ImageButton Bfont_l = (ImageButton) findViewById (R.id.font_l);
            ImageButton Bfont_r = (ImageButton) findViewById (R.id.font_r);
            ImageButton Bcolor_l = (ImageButton) findViewById (R.id.color_l);
            ImageButton Bcolor_r = (ImageButton) findViewById (R.id.color_r);
            ImageButton Bposition_v_l = (ImageButton) findViewById (R.id.position_v_l);
            ImageButton Bposition_v_r = (ImageButton) findViewById (R.id.position_v_r);
            TextView font = (TextView) findViewById (R.id.font_title);
            TextView color = (TextView) findViewById (R.id.color_title);
            TextView position_v = (TextView) findViewById (R.id.position_v_title);
            initSubSetOptions (color_text);
            ok.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    sub_para.curid = sub_switch_state;
                    sub_para.font = sub_font_state;
                    sub_para.position_v = sub_position_v_state;
                    LOGI (TAG, "[subtitle_control]sub_para.curid:" + sub_para.curid + ",sub_para.curidbac:" + sub_para.curidbac);
                    if (mMediaPlayer != null) {
                        if (sub_para.curid == sub_para.totalnum) {
                            mSubtitleManager.hide();
                        }
                        else {
                            if (sub_para.curidbac != sub_para.curid) {
                                mSubtitleManager.openIdx (sub_para.curid);
                                sub_para.curidbac = sub_para.curid;
                            }
                            else {
                                mSubtitleManager.display();
                            }
                        }
                    }
                    if (sub_color_state == 0) {
                        sub_para.color = android.graphics.Color.WHITE;
                    }
                    else if (sub_color_state == 1) {
                        sub_para.color = android.graphics.Color.YELLOW;
                    }
                    else {
                        sub_para.color = android.graphics.Color.BLUE;
                    }
                    SharedPreferences settings = getSharedPreferences (subSettingStr, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt ("color", sub_para.color);
                    editor.putInt ("font", sub_para.font);
                    editor.putInt ("position_v", sub_para.position_v);
                    editor.commit();
                    setSubtitleView();
                    if (mMediaPlayer != null) {
                        //still have error with new method
                        /*if(mMediaPlayer.subtitleGetSubType() == 1) { //bitmap
                            disableSubSetOptions();
                        }
                        else {
                            initSubSetOptions(color_text);
                        }*/
                        String subNameStr = mSubtitleManager.getCurName();
                        if (subNameStr != null) {
                            if (subNameStr.equals ("INSUB") || subNameStr.endsWith (".idx")) {
                                disableSubSetOptions();
                            }
                            else {
                                initSubSetOptions (color_text);
                            }
                        }
                        else {
                            initSubSetOptions (color_text);
                        }
                    }
                    else {
                        initSubSetOptions (color_text);
                    }
                    exitSubWidget (subtitleSwitchBtn);
                }
            });
            cancel.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    // do nothing
                    exitSubWidget (subtitleSwitchBtn);
                }
            });
            Bswitch_l.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    if (sub_switch_state <= 0) {
                        sub_switch_state = sub_para.totalnum;
                    }
                    else {
                        sub_switch_state --;
                    }
                    if (sub_switch_state == sub_para.totalnum) {
                        t_subswitch.setText (R.string.str_off);
                    }
                    else {
                        t_subswitch.setText (String.valueOf (sub_switch_state + 1) + "/" + String.valueOf (sub_para.totalnum));
                    }
                }
            });
            Bswitch_r.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    if (sub_switch_state >= sub_para.totalnum) {
                        sub_switch_state = 0;
                    }
                    else {
                        sub_switch_state ++;
                    }
                    if (sub_switch_state == sub_para.totalnum) {
                        t_subswitch.setText (R.string.str_off);
                    }
                    else {
                        t_subswitch.setText (String.valueOf (sub_switch_state + 1) + "/" + String.valueOf (sub_para.totalnum));
                    };
                }
            });
            Bfont_l.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    if (sub_font_state > 12) {
                        sub_font_state = sub_font_state - 2;
                    }
                    else {
                        sub_font_state = 30;
                    }
                    t_subsfont.setText (String.valueOf (sub_font_state));
                }
            });
            Bfont_r.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    if (sub_font_state < 30) {
                        sub_font_state = sub_font_state + 2;
                    }
                    else {
                        sub_font_state = 12;
                    }
                    t_subsfont.setText (String.valueOf (sub_font_state));
                }
            });
            Bcolor_l.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    if (sub_color_state <= 0) {
                        sub_color_state = 2;
                    }
                    else {
                        sub_color_state-- ;
                    }
                    t_subscolor.setText (color_text[sub_color_state]);
                }
            });
            Bcolor_r.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    if (sub_color_state >= 2) {
                        sub_color_state = 0;
                    }
                    else {
                        sub_color_state++ ;
                    }
                    t_subscolor.setText (color_text[sub_color_state]);
                }
            });
            Bposition_v_l.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    if (sub_position_v_state <= 0) {
                        sub_position_v_state = 15;
                    }
                    else {
                        sub_position_v_state-- ;
                    }
                    t_subsposition_v.setText (String.valueOf (sub_position_v_state));
                }
            });
            Bposition_v_r.setOnClickListener (new View.OnClickListener() {
                public void onClick (View v) {
                    if (sub_position_v_state >= 15) {
                        sub_position_v_state = 0;
                    }
                    else {
                        sub_position_v_state++ ;
                    }
                    t_subsposition_v.setText (String.valueOf (sub_position_v_state));
                }
            });
            if (mMediaPlayer != null) {
                //still have error with new method
                /*if(mMediaPlayer.subtitleGetSubType() == 1) { //bitmap
                    disableSubSetOptions();
                }
                else {
                    initSubSetOptions(color_text);
                }*/
                String subNameStr = mSubtitleManager.getCurName();
                if (subNameStr != null) {
                    if (subNameStr.equals ("INSUB") || subNameStr.endsWith (".idx")) {
                        disableSubSetOptions();
                    }
                    else {
                        initSubSetOptions (color_text);
                    }
                }
                else {
                    initSubSetOptions (color_text);
                }
            }
            else {
                initSubSetOptions (color_text);
            }
        }

        private void initSubSetOptions (String color_text[]) {
            t_subswitch = (TextView) findViewById (R.id.sub_swith111);
            t_subsfont = (TextView) findViewById (R.id.sub_font111);
            t_subscolor = (TextView) findViewById (R.id.sub_color111);
            t_subsposition_v = (TextView) findViewById (R.id.sub_position_v111);
            sub_switch_state = sub_para.curid;
            sub_font_state = sub_para.font;
            sub_position_v_state = sub_para.position_v;
            if (sub_para.color == android.graphics.Color.WHITE) {
                sub_color_state = 0;
            }
            else if (sub_para.color == android.graphics.Color.YELLOW) {
                sub_color_state = 1;
            }
            else {
                sub_color_state = 2;
            }
            if (sub_para.curid == sub_para.totalnum) {
                sub_para.curid = sub_para.totalnum;
                t_subswitch.setText (R.string.str_off);
            }
            else {
                t_subswitch.setText (String.valueOf (sub_para.curid + 1) + "/" + String.valueOf (sub_para.totalnum));
            }
            t_subsfont.setText (String.valueOf (sub_font_state));
            t_subscolor.setText (color_text[sub_color_state]);
            t_subsposition_v.setText (String.valueOf (sub_position_v_state));
            Button ok = (Button) findViewById (R.id.button_ok);
            Button cancel = (Button) findViewById (R.id.button_canncel);
            ImageButton Bswitch_l = (ImageButton) findViewById (R.id.switch_l);
            ImageButton Bswitch_r = (ImageButton) findViewById (R.id.switch_r);
            ImageButton Bfont_l = (ImageButton) findViewById (R.id.font_l);
            ImageButton Bfont_r = (ImageButton) findViewById (R.id.font_r);
            ImageButton Bcolor_l = (ImageButton) findViewById (R.id.color_l);
            ImageButton Bcolor_r = (ImageButton) findViewById (R.id.color_r);
            ImageButton Bposition_v_l = (ImageButton) findViewById (R.id.position_v_l);
            ImageButton Bposition_v_r = (ImageButton) findViewById (R.id.position_v_r);
            TextView font = (TextView) findViewById (R.id.font_title);
            TextView color = (TextView) findViewById (R.id.color_title);
            TextView position_v = (TextView) findViewById (R.id.position_v_title);
            font.setTextColor (android.graphics.Color.BLACK);
            color.setTextColor (android.graphics.Color.BLACK);
            position_v.setTextColor (android.graphics.Color.BLACK);
            t_subsfont.setTextColor (android.graphics.Color.BLACK);
            t_subscolor.setTextColor (android.graphics.Color.BLACK);
            t_subsposition_v.setTextColor (android.graphics.Color.BLACK);
            Bfont_l.setEnabled (true);
            Bfont_r.setEnabled (true);
            Bcolor_l.setEnabled (true);
            Bcolor_r.setEnabled (true);
            Bposition_v_l.setEnabled (true);
            Bposition_v_r.setEnabled (true);
            Bfont_l.setImageResource (R.drawable.fondsetup_larrow_unfocus);
            Bfont_r.setImageResource (R.drawable.fondsetup_rarrow_unfocus);
            Bcolor_l.setImageResource (R.drawable.fondsetup_larrow_unfocus);
            Bcolor_r.setImageResource (R.drawable.fondsetup_rarrow_unfocus);
            Bposition_v_l.setImageResource (R.drawable.fondsetup_larrow_unfocus);
            Bposition_v_r.setImageResource (R.drawable.fondsetup_rarrow_unfocus);
            Bswitch_l.setNextFocusUpId (R.id.switch_l);
            Bswitch_l.setNextFocusDownId (R.id.font_l);
            Bswitch_l.setNextFocusLeftId (R.id.switch_l);
            Bswitch_l.setNextFocusRightId (R.id.switch_r);
            Bswitch_r.setNextFocusUpId (R.id.switch_r);
            Bswitch_r.setNextFocusDownId (R.id.font_r);
            Bswitch_r.setNextFocusLeftId (R.id.switch_l);
            Bswitch_r.setNextFocusRightId (R.id.switch_r);
            Bfont_l.setNextFocusUpId (R.id.switch_l);
            Bfont_l.setNextFocusDownId (R.id.color_l);
            Bfont_l.setNextFocusLeftId (R.id.font_l);
            Bfont_l.setNextFocusRightId (R.id.font_r);
            Bfont_r.setNextFocusUpId (R.id.switch_r);
            Bfont_r.setNextFocusDownId (R.id.color_r);
            Bfont_r.setNextFocusLeftId (R.id.font_l);
            Bfont_r.setNextFocusRightId (R.id.font_r);
            Bcolor_l.setNextFocusUpId (R.id.font_l);
            Bcolor_l.setNextFocusDownId (R.id.position_v_l);
            Bcolor_l.setNextFocusLeftId (R.id.color_l);
            Bcolor_l.setNextFocusRightId (R.id.color_r);
            Bcolor_r.setNextFocusUpId (R.id.font_r);
            Bcolor_r.setNextFocusDownId (R.id.position_v_r);
            Bcolor_r.setNextFocusLeftId (R.id.color_l);
            Bcolor_r.setNextFocusRightId (R.id.color_r);
            Bposition_v_l.setNextFocusUpId (R.id.color_l);
            Bposition_v_l.setNextFocusDownId (R.id.button_ok);
            Bposition_v_l.setNextFocusLeftId (R.id.position_v_l);
            Bposition_v_l.setNextFocusRightId (R.id.position_v_r);
            Bposition_v_r.setNextFocusUpId (R.id.color_r);
            Bposition_v_r.setNextFocusDownId (R.id.button_canncel);
            Bposition_v_r.setNextFocusLeftId (R.id.position_v_l);
            Bposition_v_r.setNextFocusRightId (R.id.position_v_r);
            cancel.setNextFocusUpId (R.id.position_v_r);
            cancel.setNextFocusDownId (R.id.button_canncel);
            cancel.setNextFocusLeftId (R.id.button_ok);
            cancel.setNextFocusRightId (R.id.button_canncel);
            ok.setNextFocusUpId (R.id.position_v_l);
            ok.setNextFocusDownId (R.id.button_ok);
            ok.setNextFocusLeftId (R.id.button_ok);
            ok.setNextFocusRightId (R.id.button_canncel);
        }

        private void disableSubSetOptions() {
            Button ok = (Button) findViewById (R.id.button_ok);
            Button cancel = (Button) findViewById (R.id.button_canncel);
            ImageButton Bswitch_l = (ImageButton) findViewById (R.id.switch_l);
            ImageButton Bswitch_r = (ImageButton) findViewById (R.id.switch_r);
            ImageButton Bfont_l = (ImageButton) findViewById (R.id.font_l);
            ImageButton Bfont_r = (ImageButton) findViewById (R.id.font_r);
            ImageButton Bcolor_l = (ImageButton) findViewById (R.id.color_l);
            ImageButton Bcolor_r = (ImageButton) findViewById (R.id.color_r);
            ImageButton Bposition_v_l = (ImageButton) findViewById (R.id.position_v_l);
            ImageButton Bposition_v_r = (ImageButton) findViewById (R.id.position_v_r);
            TextView font = (TextView) findViewById (R.id.font_title);
            TextView color = (TextView) findViewById (R.id.color_title);
            TextView position_v = (TextView) findViewById (R.id.position_v_title);
            font.setTextColor (android.graphics.Color.LTGRAY);
            color.setTextColor (android.graphics.Color.LTGRAY);
            position_v.setTextColor (android.graphics.Color.LTGRAY);
            t_subsfont.setTextColor (android.graphics.Color.LTGRAY);
            t_subscolor.setTextColor (android.graphics.Color.LTGRAY);
            t_subsposition_v.setTextColor (android.graphics.Color.LTGRAY);
            Bfont_l.setEnabled (false);
            Bfont_r.setEnabled (false);
            Bcolor_l.setEnabled (false);
            Bcolor_r.setEnabled (false);
            Bposition_v_l.setEnabled (false);
            Bposition_v_r.setEnabled (false);
            Bfont_l.setImageResource (R.drawable.fondsetup_larrow_disable);
            Bfont_r.setImageResource (R.drawable.fondsetup_rarrow_disable);
            Bcolor_l.setImageResource (R.drawable.fondsetup_larrow_disable);
            Bcolor_r.setImageResource (R.drawable.fondsetup_rarrow_disable);
            Bposition_v_l.setImageResource (R.drawable.fondsetup_larrow_disable);
            Bposition_v_r.setImageResource (R.drawable.fondsetup_rarrow_disable);
            Bswitch_l.setNextFocusUpId (R.id.switch_l);
            Bswitch_l.setNextFocusDownId (R.id.button_ok);
            Bswitch_l.setNextFocusLeftId (R.id.switch_l);
            Bswitch_l.setNextFocusRightId (R.id.switch_r);
            Bswitch_r.setNextFocusUpId (R.id.switch_r);
            Bswitch_r.setNextFocusDownId (R.id.button_canncel);
            Bswitch_r.setNextFocusLeftId (R.id.switch_l);
            Bswitch_r.setNextFocusRightId (R.id.switch_r);
            ok.setNextFocusUpId (R.id.switch_l);
            ok.setNextFocusDownId (R.id.button_ok);
            ok.setNextFocusLeftId (R.id.button_ok);
            ok.setNextFocusRightId (R.id.button_canncel);
            cancel.setNextFocusUpId (R.id.switch_r);
            cancel.setNextFocusDownId (R.id.button_canncel);
            cancel.setNextFocusLeftId (R.id.button_ok);
            cancel.setNextFocusRightId (R.id.button_canncel);
        }

        //@@--------this part for other widget list view--------------------------------------------------------------------------------
        private String[] m_brightness = {"1", "2", "3", "4", "5", "6"}; // for brightness
        private int[] string_3d_id = {
            R.string.setting_3d_diable,
            R.string.setting_3d_lr,
            R.string.setting_3d_bt,
            /*R.string.setting_3d_auto,
            R.string.setting_3d_2d_l,
            R.string.setting_3d_2d_r,
            R.string.setting_3d_2d_t,
            R.string.setting_3d_2d_b,
            R.string.setting_3d_2d_auto1,
            R.string.setting_3d_2d_auto2,
            R.string.setting_2d_3d,
            R.string.setting_3d_field_depth,
            R.string.setting_3d_auto_switch,
            R.string.setting_3d_lr_switch,
            R.string.setting_3d_tb_switch,
            R.string.setting_3d_full_off,
            R.string.setting_3d_lr_full,
            R.string.setting_3d_tb_full,
            R.string.setting_3d_grating_open,
            R.string.setting_3d_grating_close,*/
        };

        private SimpleAdapter getMorebarListAdapter (int id, int pos) {
            return new SimpleAdapter (this, getMorebarListData (id, pos),
                                      R.layout.list_row,
                                      new String[] {"item_name", "item_sel"},
                                      new int[] {R.id.Text01, R.id.imageview}
                                     );
        }

        private List <? extends Map < String, ? >> getMorebarListData (int id, int pos) {
            // TODO Auto-generated method stub
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            Map<String, Object> map;
            switch (id) {
                case RESUME_MODE:
                    map = new HashMap<String, Object>();
                    map.put ("item_name", getResources().getString (R.string.str_on));
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    map = new HashMap<String, Object>();
                    map.put ("item_name", getResources().getString (R.string.str_off));
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    list.get (pos).put ("item_sel", R.drawable.item_img_sel);
                    break;
                case REPEAT_MODE:
                    map = new HashMap<String, Object>();
                    map.put ("item_name", getResources().getString (R.string.setting_playmode_repeatall));
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    map = new HashMap<String, Object>();
                    map.put ("item_name", getResources().getString (R.string.setting_playmode_repeatone));
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    list.get (pos).put ("item_sel", R.drawable.item_img_sel);
                    break;
                case AUDIO_OPTION:
                    map = new HashMap<String, Object>();
                    map.put ("item_name", getResources().getString (R.string.setting_audiotrack));
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    map = new HashMap<String, Object>();
                    map.put ("item_name", getResources().getString (R.string.setting_soundtrack));
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    break;
                case SOUND_TRACK:
                    map = new HashMap<String, Object>();
                    map.put ("item_name", getResources().getString (R.string.setting_soundtrack_stereo));
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    map = new HashMap<String, Object>();
                    map.put ("item_name", getResources().getString (R.string.setting_soundtrack_lmono));
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    map = new HashMap<String, Object>();
                    map.put ("item_name", getResources().getString (R.string.setting_soundtrack_rmono));
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    map = new HashMap<String, Object>();
                    map.put ("item_name", getResources().getString (R.string.setting_soundtrack_lrmix));
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    list.get (pos).put ("item_sel", R.drawable.item_img_sel);
                    break;
                case AUDIO_TRACK:
                    if (mMediaInfo != null) {
                        int audio_total_num = mMediaInfo.getAudioTotalNum();
                        for (int i = 0; i < audio_total_num; i++) {
                            map = new HashMap<String, Object>();
                            map.put ("item_name", mMediaInfo.getAudioFormatStr (mMediaInfo.getAudioFormat (i)));
                            map.put ("item_sel", R.drawable.item_img_unsel);
                            list.add (map);
                        }
                        LOGI (TAG, "list.size():" + list.size() + ",pos:" + pos + ",audio_total_num:" + audio_total_num);
                        if (pos < 0) {
                            pos = 0;
                        }
                        list.get (pos).put ("item_sel", R.drawable.item_img_sel);
                    }
                    break;
                case AUDIO_DTS_ASSET:
                    if (mMediaInfo != null) {
                        int dts_asset_total_num = getDtsAssetTotalNum(); //dts test
                        for (int i = 0; i < dts_asset_total_num; i++) {
                            map = new HashMap<String, Object>();
                            map.put ("item_name", "Apresentation" + Integer.toString (i));
                            map.put ("item_sel", R.drawable.item_img_unsel);
                            list.add (map);
                        }
                        LOGI (TAG, "list.size():" + list.size() + ",pos:" + pos + ",dts_asset_total_num:" + dts_asset_total_num);
                        if (pos < 0) {
                            pos = 0;
                        }
                        //list.get(pos).put("item_sel", R.drawable.item_img_sel);
                    }
                    break;
                case DISPLAY_MODE:
                    map = new HashMap<String, Object>();
                    map.put ("item_name", getResources().getString (R.string.setting_displaymode_normal));
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    map = new HashMap<String, Object>();
                    map.put ("item_name", getString (R.string.setting_displaymode_fullscreen));
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    map = new HashMap<String, Object>();
                    map.put ("item_name", "4:3");
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    map = new HashMap<String, Object>();
                    map.put ("item_name", "16:9");
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                    // TODO: 3D
                    /*
                    if (mSystemControl.getPropertyBoolean("3D_setting.enable", false)) {
                        if (is3DVideoDisplayFlag) {//judge is 3D
                            map = new HashMap<String, Object>();
                            map.put("item_name", getResources().getString(R.string.setting_displaymode_normal_noscaleup));
                            map.put("item_sel", R.drawable.item_img_unsel);
                            list.add(map);
                        }
                    }*/
                    list.get (pos).put ("item_sel", R.drawable.item_img_sel);
                    break;
                case BRIGHTNESS:
                    int size_bgh = m_brightness.length;
                    for (int i = 0; i < size_bgh; i++) {
                        map = new HashMap<String, Object>();
                        map.put ("item_name", m_brightness[i].toString());
                        map.put ("item_sel", R.drawable.item_img_unsel);
                        list.add (map);
                    }
                    list.get (pos).put ("item_sel", R.drawable.item_img_sel);
                    break;
                case PLAY3D:
                    int size_3d = 3;
                    for (int i = 0; i < size_3d; i++) {
                        map = new HashMap<String, Object>();
                        map.put ("item_name", getResources().getString (string_3d_id[i]));
                        map.put ("item_sel", R.drawable.item_img_unsel);
                        list.add (map);
                    }
                    if (mOption != null) {
                        list.get (mOption.get3DMode()).put ("item_sel", R.drawable.item_img_sel);
                    }
                    break;
                default:
                    break;
            }
            return list;
        }

}


//@@-------- this part for option item value read and write-----------------------------------------------------------
class Option {
        private static String TAG = "Option";
        private Activity mAct;
        private static SharedPreferences sp = null;

        private boolean resume = false;
        private int repeat = 0;
        private int audiotrack = -1;
        private int soundtrack = -1;
        private int audiodtsasset = -1;
        private int display = 0;
        private int _3dmode = 0;

        public static final int REPEATLIST = 0;
        public static final int REPEATONE = 1;
        public static final int DISP_MODE_NORMAL = 0;
        public static final int DISP_MODE_FULLSTRETCH = 1;
        public static final int DISP_MODE_RATIO4_3 = 2;
        public static final int DISP_MODE_RATIO16_9 = 3;
        private String RESUME_MODE = "ResumeMode";
        private String REPEAT_MODE = "RepeatMode";
        private String AUDIO_TRACK = "AudioTrack";
        private String SOUND_TRACK = "SoundTrack";
        private String AUDIO_DTS_ASSET = "AudioDtsAsset";
        private String DISPLAY_MODE = "DisplayMode";

        public Option (Activity act) {
            mAct = act;
            sp = mAct.getSharedPreferences ("optionSp", Activity.MODE_PRIVATE);
        }

        public boolean getResumeMode() { //book mark
            if (sp != null) {
                resume = sp.getBoolean (RESUME_MODE, true);
            }
            return resume;
        }

        public int getRepeatMode() {
            if (sp != null) {
                repeat = sp.getInt (REPEAT_MODE, 0);
            }
            return repeat;
        }

        public int getAudioTrack() {
            if (sp != null) {
                audiotrack = sp.getInt (AUDIO_TRACK, 0);
            }
            return audiotrack;
        }

        public int getSoundTrack() {
            if (sp != null) {
                soundtrack = sp.getInt (SOUND_TRACK, 0);
            }
            return soundtrack;
        }

        public int getAudioDtsAsset() {
            if (sp != null) {
                audiodtsasset = sp.getInt (AUDIO_DTS_ASSET, 0);
            }
            return audiodtsasset;
        }

        public int getDisplayMode() {
            if (sp != null) {
                display = sp.getInt (DISPLAY_MODE, 0);
            }
            return display;
        }

        public int get3DMode() {
            return _3dmode;
        }

        public void setResumeMode (boolean para) {
            if (sp != null) {
                sp.edit()
                .putBoolean (RESUME_MODE, para)
                .commit();
            }
        }

        public void setRepeatMode (int para) {
            if (sp != null) {
                sp.edit()
                .putInt (REPEAT_MODE, para)
                .commit();
            }
        }

        public void setAudioTrack (int para) {
            if (sp != null) {
                sp.edit()
                .putInt (AUDIO_TRACK, para)
                .commit();
            }
        }

        public void setSoundTrack (int para) {
            if (sp != null) {
                sp.edit()
                .putInt (SOUND_TRACK, para)
                .commit();
            }
        }

        public void setAudioDtsAsset (int para) {
            if (sp != null) {
                sp.edit()
                .putInt (AUDIO_DTS_ASSET, para)
                .commit();
            }
        }

        public void setDisplayMode (int para) {
            if (sp != null) {
                sp.edit()
                .putInt (DISPLAY_MODE, para)
                .commit();
            }
        }

        public void set3DMode (int para) {
            _3dmode = para;
        }
}

//@@--------this part for book mark---------------------------------------------------------------------------------
class Bookmark {
        private static String TAG = "Bookmark";
        private Activity mAct;
        private static SharedPreferences sp = null;
        private static final int BOOKMARK_NUM_MAX = 10;

        public Bookmark (Activity act) {
            mAct = act;
            sp = mAct.getSharedPreferences ("bookmarkSp", Activity.MODE_PRIVATE);
        }

        private String getSpStr (String name) {
            String str = null;
            if (sp != null) {
                str = sp.getString (name, "");
            }
            return str;
        }

        private int getSpInt (String name) {
            int ret = -1;
            if (sp != null) {
                ret = sp.getInt (name, 0);
            }
            return ret;
        }

        private void putSpStr (String name, String para) {
            if (sp != null) {
                sp.edit()
                .putString (name, para)
                .commit();
            }
        }

        private void putSpInt (String name, int para) {
            if (sp != null) {
                sp.edit()
                .putInt (name, para)
                .commit();
            }
        }

        public int get (String filename) {
            for (int i = 0; i < BOOKMARK_NUM_MAX; i++) {
                if (filename.equals (getSpStr ("filename" + i))) {
                    int position = getSpInt ("filetime" + i);
                    return position;
                }
            }
            return 0;
        }

        public int set (String filename, int time) {
            String isNull = null;
            int i = -1;
            for (i = 0; i < BOOKMARK_NUM_MAX;) {
                isNull = getSpStr ("filename" + i);
                if (isNull == null
                        || isNull.length() == 0
                        || isNull.equals (filename)) {
                    break;
                }
                i++;
            }
            if (i < BOOKMARK_NUM_MAX) {
                putSpStr ("filename" + i, filename);
                putSpInt ("filetime" + i, time);
            }
            else {
                for (int j = 0; j < BOOKMARK_NUM_MAX - 1; j++) {
                    putSpStr ("filename" + j,
                              getSpStr ("filename" + (j + 1)));
                    putSpInt ("filetime" + j,
                              getSpInt ("filetime" + (j + 1)));
                }
                putSpStr ("filename" + (BOOKMARK_NUM_MAX - 1), filename);
                putSpInt ("filetime" + (BOOKMARK_NUM_MAX - 1), time);
            }
            return 0;
        }
}

class ResumePlay {
        private Activity mAct;
        private static SharedPreferences sp = null;
        boolean enable = false; // the flag will reset to false if invoke onDestroy, in this case to distinguish resume play and bookmark play

        public ResumePlay (Activity act) {
            mAct = act;
            sp = mAct.getSharedPreferences ("ResumePlaySp", Activity.MODE_PRIVATE);
        }

        private String getSpStr (String name) {
            String str = null;
            if (sp != null) {
                str = sp.getString (name, "");
            }
            return str;
        }

        private int getSpInt (String name) {
            int ret = -1;
            if (sp != null) {
                ret = sp.getInt (name, 0);
            }
            return ret;
        }

        private void putSpStr (String name, String para) {
            if (sp != null) {
                sp.edit()
                .putString (name, para)
                .commit();
            }
        }

        private void putSpInt (String name, int para) {
            if (sp != null) {
                sp.edit()
                .putInt (name, para)
                .commit();
            }
        }

        public void setEnable (boolean en) {
            enable = en;
        }

        public boolean getEnable() {
            return enable;
        }

        public void set (String filename, int time) {
            putSpStr ("filename", filename);
            putSpInt ("filetime", time);
        }

        public String getFilepath() {
            String path = getSpStr ("filename");
            return path;
        }

        public int getTime() {
            int time = getSpInt ("filetime");
            return time;
        }

}

class subview_set {
        public int totalnum;
        public int curid;
        public int curidbac;
        public int color;
        public int font;
        public int position_v;
}

