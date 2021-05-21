package com.iamrohan.musicplayer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.palette.graphics.Palette;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import static com.iamrohan.musicplayer.AlbumDetailsAdapter.albumFiles;
import static com.iamrohan.musicplayer.ApplicationClass.ACTION_NEXT;
import static com.iamrohan.musicplayer.ApplicationClass.ACTION_PLAY;
import static com.iamrohan.musicplayer.ApplicationClass.ACTION_PREVIOUS;
import static com.iamrohan.musicplayer.ApplicationClass.CHANNEL_ID_2;
import static com.iamrohan.musicplayer.MainActivity.musicFiles;

public class PlayerActivity extends AppCompatActivity implements ActionPlaying , ServiceConnection {

    TextView song_name , artist_name ,duration_played , duration_total;
    ImageView cover_art , nextBtn , prevBtn , backBtn , shuffleBtn , repeatBtn;
    FloatingActionButton playPauseBtn;
    SeekBar seekBar;

    int position = -1;
    static ArrayList<MusicFiles> listSongs = new ArrayList<>();

    static Uri uri;
    //static MediaPlayer mediaPlayer;

    private Handler handler = new Handler();

    private Thread playThread , prevThread , nextThread;

    MusicService musicService;

    MediaSessionCompat mediaSessionCompat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        mediaSessionCompat = new MediaSessionCompat(getBaseContext() , "My Audio");
        initViews();
        getIntentMethod();




        //mediaPlayer.setOnCompletionListener(this);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(musicService != null && fromUser){
                    musicService.seekTo(progress *1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(musicService != null){
                    int mCurrentPosition = musicService.getCurrentPosition()/1000;
                    seekBar.setProgress(mCurrentPosition);
                    duration_played.setText(formattedTime(mCurrentPosition));
                }
                handler.postDelayed(this , 1000);
            }
        });

    }

    @Override
    protected void onResume() {
        Intent i = new Intent(this, MusicService.class);
        bindService(i , this,BIND_AUTO_CREATE);


        playThreadBtn();
        nextThreadBtn();
        prevThreadBtn();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    private void playThreadBtn() {

        playThread = new Thread(){
            @Override
            public void run() {
                super.run();
                playPauseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPauseBtnClicked();
                    }
                });
            }
        };
        playThread.start();

    }

    public void playPauseBtnClicked(){

        if(musicService.isPlaying()){
            playPauseBtn.setImageResource(R.drawable.ic_play);
            showNotification(R.drawable.ic_play);
            musicService.pause();
            seekBar.setMax(musicService.getDuration() / 1000);

            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this , 1000);
                }
            });


        }else{
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            showNotification(R.drawable.ic_pause);
            musicService.start();
            seekBar.setMax(musicService.getDuration() / 1000);

            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this , 1000);
                }
            });

        }
    }

    private void nextThreadBtn(){

        nextThread = new Thread(){
            @Override
            public void run() {
                super.run();
                nextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nextBtnClicked();
                    }
                });
            }
        };
        nextThread.start();

    }

    public void nextBtnClicked(){

        if(musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            position = ((position + 1) % listSongs.size());
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
           // mediaPlayer = MediaPlayer.create(getApplicationContext() , uri);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());

            seekBar.setMax(musicService.getDuration() / 1000);

            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                        duration_played.setText(formattedTime(mCurrentPosition));
                    }
                    handler.postDelayed(this , 1000);
                }
            });
            musicService.OnCompleted();
            showNotification(R.drawable.ic_pause);
            //mediaPlayer.setOnCompletionListener(this);
            playPauseBtn.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();
        }else{
            musicService.stop();
            musicService.release();
            position = ((position + 1) % listSongs.size());
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);

           // mediaPlayer = MediaPlayer.create(getApplicationContext() , uri);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());

            seekBar.setMax(musicService.getDuration() / 1000);

            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                        duration_played.setText(formattedTime(mCurrentPosition));
                    }
                    handler.postDelayed(this , 1000);
                }
            });
            musicService.OnCompleted();
            showNotification(R.drawable.ic_play);
            //mediaPlayer.setOnCompletionListener(this);
            playPauseBtnClicked();
            playPauseBtn.setBackgroundResource(R.drawable.ic_play);
        }

    }

    private void prevThreadBtn(){

        prevThread = new Thread(){
            @Override
            public void run() {
                super.run();
                prevBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prevBtnClicked();
                    }
                });
            }
        };
        prevThread.start();

    }

    public void prevBtnClicked(){

        if(musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            position = ((position - 1) < 0 ? (listSongs.size()-1): (position-1));
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            //mediaPlayer = MediaPlayer.create(getApplicationContext() , uri);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());

            seekBar.setMax(musicService.getDuration() / 1000);

            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                        duration_played.setText(formattedTime(mCurrentPosition));
                    }
                    handler.postDelayed(this , 1000);
                }
            });
            musicService.OnCompleted();
            showNotification(R.drawable.ic_pause);
            //mediaPlayer.setOnCompletionListener(this);
            playPauseBtn.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();
        }else{
            musicService.stop();
            musicService.release();
            position = ((position - 1) < 0 ? (listSongs.size()-1): (position-1));
            uri = Uri.parse(listSongs.get(position).getPath());
           musicService.createMediaPlayer(position);

            //mediaPlayer = MediaPlayer.create(getApplicationContext() , uri);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());

            seekBar.setMax(musicService.getDuration() / 1000);

            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                        duration_played.setText(formattedTime(mCurrentPosition));
                    }
                    handler.postDelayed(this , 1000);
                }
            });
            musicService.OnCompleted();
            showNotification(R.drawable.ic_play);
            //mediaPlayer.setOnCompletionListener(this);\
            playPauseBtnClicked();
            playPauseBtn.setBackgroundResource(R.drawable.ic_play);
        }


    }

    private String formattedTime(int mCurrentPosition){

        String totalOut = "";
        String totalNew = "";
        String seconds = String.valueOf(mCurrentPosition % 60);
        String min = String.valueOf(mCurrentPosition / 60);
        totalOut = min + ":" +seconds;
        totalNew = min + ":" + "0" +seconds;
        if(seconds.length() == 1){
            return totalNew;
        }else{
            return totalOut;
        }

    }

    private void getIntentMethod(){
        position = getIntent().getIntExtra("position" , -1);
        String sender = getIntent().getStringExtra("sender");

        if(sender != null && sender.equals("albumDetails")){
            listSongs = albumFiles;
        }
        else{
            listSongs = musicFiles;
        }

        if(listSongs != null){
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            uri = Uri.parse(listSongs.get(position).getPath());
        }
        showNotification(R.drawable.ic_pause);
        Intent i = new Intent(this , MusicService.class);
        i.putExtra("servicePosition",position);
        startService(i);


    }

    private void initViews(){

        song_name = findViewById(R.id.song_name);
        artist_name = findViewById(R.id.song_artist);
        duration_played = findViewById(R.id.durationPlayed);
        duration_total = findViewById(R.id.durationTotal);
        cover_art = findViewById(R.id.cover_art);
        nextBtn = findViewById(R.id.id_next);
        prevBtn = findViewById(R.id.id_prev);
        backBtn = findViewById(R.id.back_btn);
        shuffleBtn = findViewById(R.id.id_shuffle);
        repeatBtn = findViewById(R.id.id_repeat);
        playPauseBtn = findViewById(R.id.play_pause);
        seekBar = findViewById(R.id.seekbar);


    }

    private void metaData(Uri uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        int durationTotal = Integer.parseInt(listSongs.get(position).getDuration()) / 1000;
        duration_total.setText(formattedTime(durationTotal));
        byte[] art = retriever.getEmbeddedPicture();

        Bitmap bitmap = null;

        if(art != null){

            bitmap = BitmapFactory.decodeByteArray(art , 0 , art.length);
            ImageAnimation(this , cover_art , bitmap);


//            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
//                @Override
//                public void onGenerated(@Nullable Palette palette) {
//                    Palette.Swatch swatch = palette.getDominantSwatch();
//                    if(swatch != null){
//                        ImageView gradient = findViewById(R.id.imageViewGradient);
//                        RelativeLayout mContainer = findViewById(R.id.mContainer);
//                        gradient.setBackgroundResource(R.drawable.gradient_bg);
//                        mContainer.setBackgroundResource(R.drawable.main_bg);
//
//                        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP , new int[]{swatch.getRgb() , 0x00000000});
//                        gradient.setBackground(gradientDrawable);
//                        GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP , new int[]{swatch.getRgb() , swatch.getRgb()});
//                        mContainer.setBackground(gradientDrawableBg);
//                        song_name.setTextColor(swatch.getTitleTextColor());
//                        artist_name.setTextColor(swatch.getBodyTextColor());
//                    }
//                    else{
//                        ImageView gradient = findViewById(R.id.imageViewGradient);
//                        RelativeLayout mContainer = findViewById(R.id.mContainer);
//                        gradient.setBackgroundResource(R.drawable.gradient_bg);
//                        mContainer.setBackgroundResource(R.drawable.main_bg);
//
//                        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP , new int[]{ 0xff000000 , 0x00000000});
//                        gradient.setBackground(gradientDrawable);
//                        GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP , new int[]{ 0xff000000,  0x00000000});
//                        mContainer.setBackground(gradientDrawableBg);
//                        song_name.setTextColor(Color.WHITE);
//                        artist_name.setTextColor(Color.DKGRAY);
//                    }
//                }
//            });
        }
    }

    public void ImageAnimation(Context context , ImageView imageView , Bitmap bitmap){

        Animation animOut = AnimationUtils.loadAnimation(context , android.R.anim.fade_out);
        Animation animIn = AnimationUtils.loadAnimation(context , android.R.anim.fade_in);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(context).load(bitmap).into(imageView);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(animIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageView.startAnimation(animOut);

    }



    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder myBinder = (MusicService.MyBinder)service;
        musicService = myBinder.getService();
        musicService.setCallBack(this);
        seekBar.setMax(musicService.getDuration()/1000);
        metaData(uri);

        song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText(listSongs.get(position).getArtist());

        musicService.OnCompleted();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

        musicService = null;
    }

    void showNotification(int playPauseBtn){

        Intent intent = new Intent(this, PlayerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this,0 , intent , 0);

        Intent prevIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PREVIOUS);
        PendingIntent prevPending = PendingIntent.getActivity(this,0 , prevIntent , PendingIntent.FLAG_UPDATE_CURRENT);

        Intent pauseIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PLAY);
        PendingIntent pausePending = PendingIntent.getBroadcast(this,0 , pauseIntent , PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getBroadcast(this,0 , nextIntent , PendingIntent.FLAG_UPDATE_CURRENT);



        byte[] picture = null;
        picture = getAlbumArt(listSongs.get(position).getPath());
        Bitmap thumb = null;

        if(picture != null){
            thumb = BitmapFactory.decodeByteArray(picture , 0 , picture.length);
        }else {
            thumb = BitmapFactory.decodeResource(getResources() , R.drawable.ic_launcher_background);
        }

        Notification notification = new NotificationCompat.Builder(this , CHANNEL_ID_2)
                .setSmallIcon(playPauseBtn)
                .setLargeIcon(thumb)
                .setContentTitle(listSongs.get(position).getTitle())
                .setContentText(listSongs.get(position).getArtist())
                .addAction(R.drawable.ic_skip_previous , "Previous" , prevPending)
                .addAction(playPauseBtn, "Pause" , pausePending)
                .addAction(R.drawable.ic_skip_next , "Next" , nextPending)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSessionCompat.getSessionToken()))
                .setNotificationSilent()
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0 , notification);
    }

    private byte[] getAlbumArt(String uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }
}