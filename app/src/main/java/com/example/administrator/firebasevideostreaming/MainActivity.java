package com.example.administrator.firebasevideostreaming;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity {


    public static boolean isFirstOpen = false;
    private boolean isPlaying;
    private String str,
            videoUriString3 = "放入上傳到Firebase的Storage的片源下載網址, 比如說https://firebasestorage.googleapis.com...";
    private int current = 0, duration = 0, currentPercent, hh, mm, ss;

    private Uri videoUri;
    private VideoView mainVideoView;
    private ImageView playImageView;
    private TextView cuttentTimerTextView, durationTimerTextView;
    private ProgressBar videoPregressProgressBar, bufferProgressProgressBar;
    private VideoProgress videoProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeView();

        /** 設定播放來源 */
        videoUri = Uri.parse(videoUriString3);                                                       // 将String类型的路径转化成Uri类型

        mainVideoView.setVideoURI(videoUri);                                                        // 设置URI播放源
        mainVideoView.requestFocus();                                                               // 获取焦点
        playImageView.setImageResource(R.drawable.pause);
        playImageView.setClickable(false);

        mainVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {

                if (isFirstOpen == false) {
                    bufferProgressProgressBar.setVisibility(View.INVISIBLE);
                    isFirstOpen = true;
                }

                /** 播放影片的前置作業 */
                playImageView.setClickable(true);
                duration = mp.getDuration() / 1000;
                updateTime(durationTimerTextView, duration);
                isPlaying = true;
                playImageView.setImageResource(R.drawable.pause);

                /** 監聽seekTo是否完成, 再進行後續動作,  */
                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mediaPlayer) {
                        bufferProgressProgressBar.setVisibility(View.INVISIBLE);
                        mainVideoView.start();
                        isPlaying = true;
                    }
                });
            }
        });

        mainVideoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {

                /** 當影片發生緩衝時, 設定對應的操作 */
                if (i == mediaPlayer.MEDIA_INFO_BUFFERING_START) {                               // 开始缓冲
                    bufferProgressProgressBar.setVisibility(View.VISIBLE);
                    isPlaying = false;
                } else if (i == mediaPlayer.MEDIA_INFO_BUFFERING_END) {                          // 缓冲结束
                    bufferProgressProgressBar.setVisibility(View.INVISIBLE);
                    isPlaying = true;
                }
                return false;
            }
        });

        /** 開始播放影片, 並且透過AsyncTask 來動態調整ui介面 */
        mainVideoView.start();                                                                      // 开始播放
        isPlaying = true;
        videoProgress = new VideoProgress();
        videoProgress.execute();

        playImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /** 根據影片是否正在播放中 來進行相對的操作, 如果判斷已經播放完 則會重新播放 */
                if (isPlaying) {
                    isPlaying = false;
                    playImageView.setImageResource(R.drawable.play);
                    mainVideoView.pause();
                } else {
                    if (videoProgress == null) {
                        isPlaying = true;
                        mainVideoView.start();
                        playImageView.setImageResource(R.drawable.pause);
                        videoPregressProgressBar.setProgress(0);
                        videoProgress = new VideoProgress();
                        videoProgress.execute();
                    } else {
                        isPlaying = true;
                        mainVideoView.start();
                        playImageView.setImageResource(R.drawable.pause);
                    }
                }
            }
        });
    }

    /**
     * 初始化View
     */
    private void initializeView() {
        mainVideoView = findViewById(R.id.mainVideoView);
        playImageView = findViewById(R.id.playImageView);
        cuttentTimerTextView = findViewById(R.id.cuttentTimerTextView);
        durationTimerTextView = findViewById(R.id.durationTimerTextView);
        videoPregressProgressBar = findViewById(R.id.videoPregressProgressBar);
        bufferProgressProgressBar = findViewById(R.id.bufferProgressProgressBar);
    }

    @Override
    protected void onPause() {
        super.onPause();

        /** 在app不被關注時, 降低AsyncTask持續運轉帶來的消耗 */
        isPlaying = false;
        playImageView.setImageResource(R.drawable.play);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /** 在app重新取得焦點後, 恢復AsyncTask的正常工作, 並且將VideoView定位到上次播放時的進度 開始播放 */
        isPlaying = true;
        bufferProgressProgressBar.setVisibility(View.VISIBLE);
        playImageView.setImageResource(R.drawable.pause);
        mainVideoView.seekTo(current * 1000);
    }

    @Override
    protected void onStop() {
        super.onStop();

        /** 在app不被關注時, 降低AsyncTask持續運轉帶來的消耗 */
        isPlaying = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /** 結束AsyncTask */
        isPlaying = false;
        if (videoProgress != null) {
            videoProgress.cancel(true);
            videoProgress = null;
        }
    }

    private class VideoProgress extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            do {

                /** 讓這個循環 每隔0.4秒執行一次, 如果不設定間隔的話, 會因為過於耗能 導致手機過熱 */
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                /** 可以搭配判斷當前app是否屬於焦點中, 否則的話 可以透過設定false 來降低耗能 */
                if (isPlaying) {
                    try {
                        current = mainVideoView.getCurrentPosition() / 1000;
                        currentPercent = current * 100 / duration;
                        publishProgress(currentPercent, current);
                    } catch (Exception e) {
                    }
                }

                /** 如果影片播放完畢, 就跳出這個循環 */
                if (videoPregressProgressBar.getProgress() == 100) {
                    break;
                }

                /** 如果接收到cancel()的指定, 也跳出 */
                if (isCancelled()) {
                    break;
                }
            } while (videoPregressProgressBar.getProgress() <= 100);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            /** 透過不斷接收到publishProgress() 來持續更新ui */
            videoPregressProgressBar.setProgress(values[0]);
            updateTime(cuttentTimerTextView, values[1]);

            /** 如果影片播放完畢, 則執行對應的ui操作 */
            if (values[0] == 100) {
                isPlaying = false;
                playImageView.setImageResource(R.drawable.play);
                videoProgress = null;
            }
        }
    }

    /**
     * 時間的格式化
     *
     * @param textView, 想要對哪個TextView 進行設置
     * @param second,   總共的秒數
     */
    public void updateTime(TextView textView, int second) {
        hh = second / 3600;                                                                         // 小時
        mm = second % 3600 / 60;                                                                    // 分鐘
        ss = second % 60;                                                                           // 時分秒中的秒的得數
        str = null;
        if (hh != 0) {                                                                              // 如果是個位數的話, 前面可以會補個0, 時分秒
            str = String.format("%02d:%02d:%02d", hh, mm, ss);
        } else {
            str = String.format("%02d:%02d", mm, ss);
        }
        textView.setText(str);
    }
}
