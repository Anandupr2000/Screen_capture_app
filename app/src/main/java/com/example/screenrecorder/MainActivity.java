package com.example.screenrecorder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.recordscreen.R;
import com.example.recordscreen.databinding.ActivitymainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    ActivitymainBinding binding;

    private MediaRecorder mediaRecorder;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjectionCallback mediaProjectionCallback;
    private VirtualDisplay virtualDisplay;
    private DisplayMetrics metrics;

    private int screenDensity;
    private static final int REQUEST_PERMISSION = 10;
    private static final int REQUEST_CODE = 1000;

    private String videoUrl="";
    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    static {
        ORIENTATION.append(Surface.ROTATION_0,90);
        ORIENTATION.append(Surface.ROTATION_90,0);
        ORIENTATION.append(Surface.ROTATION_180,270);
        ORIENTATION.append(Surface.ROTATION_270,180);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitymainBinding.inflate(getLayoutInflater());
//        setContentView(R.layout.activity_main2);
        setContentView(binding.getRoot());

        metrics = Resources.getSystem().getDisplayMetrics();
        screenDensity = metrics.densityDpi;
        mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        binding.tBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)+
                        ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)!=
                        PackageManager.PERMISSION_GRANTED){
                    if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)||
                            ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                    Manifest.permission.RECORD_AUDIO))
                    {
                        binding.tBtn.setChecked(false);
                        Snackbar.make(findViewById(android.R.id.content),R.string.permission_text, Snackbar.LENGTH_INDEFINITE)
                                .setAction("Enable", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.RECORD_AUDIO},REQUEST_PERMISSION);
                                    }
                                }).show();
                    }
                    else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.RECORD_AUDIO},REQUEST_PERMISSION);
                    }
                }
                else {
                    onScreenShare(v);
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode!=REQUEST_CODE){
            return;
        }
        if(resultCode!=RESULT_OK){
            Toast.makeText(MainActivity.this,"Permission Denied",Toast.LENGTH_SHORT).show();
            binding.tBtn.setChecked(false);
            return;
        }
        mediaProjectionCallback = new MediaProjectionCallback();
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode,data);
        mediaProjection.registerCallback(mediaProjectionCallback,null);
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private VirtualDisplay createVirtualDisplay() {
        return mediaProjection.createVirtualDisplay("virtual display",metrics.widthPixels,metrics.heightPixels,
                screenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,mediaRecorder.getSurface(),null,null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void onScreenShare(View v) {
        if(((ToggleButton)v).isChecked()){
            binding.recordText.setVisibility(View.VISIBLE);
            binding.cmTimer.start();

            initiateRecorder();
            shareScreen();
        }
        else {
            binding.recordText.setVisibility(View.INVISIBLE);
            try {
                mediaRecorder.stop();
            }catch (Exception e){
                System.out.println("\n************************ exception occurred **********************\n");
                e.printStackTrace();
            }
            mediaRecorder.reset();

            stopScreenSharing();
            binding.cmTimer.stop();
            binding.cmTimer.setBase(SystemClock.elapsedRealtime());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE:{
                if ((grantResults.length>0)&&(grantResults[0]+grantResults[1])== PackageManager.PERMISSION_GRANTED){
                    onScreenShare(binding.tBtn);
                }
                else {
                    binding.tBtn.setChecked(false);
                    Snackbar.make(findViewById(android.R.id.content),R.string.permission_text,Snackbar.LENGTH_INDEFINITE)
                            .setAction("ENABLE", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent();
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.setData(Uri.parse("package : "+getPackageName()));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    startActivity(intent);
                                }
                            }).show();
                }
                break;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopScreenSharing() {
        if(virtualDisplay!=null){
            virtualDisplay.release();
            destroyMediaProjection();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void destroyMediaProjection() {
        if(mediaProjection!=null){
            mediaProjection.unregisterCallback(mediaProjectionCallback);
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void shareScreen() {
        if(mediaProjection==null){
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),REQUEST_CODE);
            return;
        }
        virtualDisplay = createVirtualDisplay();
//        mediaRecorder.start();
    }

    @SuppressLint("SimpleDateFormat")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initiateRecorder() {
        try{
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            videoUrl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    + new StringBuilder("/ScreenRecorder ")
                    .append(new SimpleDateFormat("dd-MM-yyyy-hh_mm_ss").format(new Date()))
                    .append(".mp4").toString();
            mediaRecorder.setOutputFile(videoUrl);//Sets the path of the output file to be produced.
            mediaRecorder.setVideoSize(metrics.widthPixels,metrics.heightPixels);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(512*1000);
            mediaRecorder.setVideoFrameRate(2);
//            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int rotation = getApplicationContext().getDisplay().getRotation();
            int orientation = ORIENTATION.get(rotation+90);

            mediaRecorder.setOrientationHint(orientation);
            mediaRecorder.prepare();
            mediaRecorder.start();
        }
        catch (Exception e ){
            System.out.println("***************** Exception Occurred during initializing recorder *****************");
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            super.onStop();
            if(binding.tBtn.isChecked()){
                binding.tBtn.setChecked(false);
                mediaRecorder.stop();
                mediaRecorder.reset();
            }
            mediaProjection = null;
            stopScreenSharing();
        }
    }
}