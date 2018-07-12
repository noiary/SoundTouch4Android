package com.maodq.soundtouch;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity implements View.OnClickListener {
    TextView textViewConsole = null;
    //    EditText editSourceFile = null;
    EditText editOutputFile = null;
    EditText editTempo = null;
    EditText editPitch = null;
    CheckBox checkBoxPlay = null;

    StringBuilder consoleText = new StringBuilder();

    //    private static final String INPUT_FILE_NAME = "file:///android_asset/test.wav";
    private static final String INPUT_FILE_NAME = "test.wav";
    private static final float MAX_TEMPO = 300f;
    private static final float MAX_PITCH = 20f;


    /// Called when the activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);

        textViewConsole = findViewById(R.id.textViewResult);
        editOutputFile = findViewById(R.id.editTextOutFileName);

        editTempo = findViewById(R.id.editTextTempo);
        editPitch = findViewById(R.id.editTextPitch);

        Button buttonProcess = findViewById(R.id.buttonProcess);
        buttonProcess.setOnClickListener(this);

        checkBoxPlay = findViewById(R.id.checkBoxPlay);


        SeekBar sbTempo = findViewById(R.id.sb_tempo);
        sbTempo.setOnSeekBarChangeListener(new MyOnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateEditTextByProgress(editTempo, seekBar, progress);
            }
        });
        SeekBar sbPitch = findViewById(R.id.sb_pitch);
        sbPitch.setOnSeekBarChangeListener(new MyOnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateEditTextByProgress(editPitch, seekBar, progress);
            }
        });


        // Check soundtouch library presence & version
        checkLibVersion();
    }

    private void updateEditTextByProgress(EditText et, SeekBar seekBar, int progress) {
        int currentProgress;
        int max = seekBar.getMax();
        if (et.getId() == R.id.editTextTempo) {
            currentProgress = (int) (progress * (MAX_TEMPO / max));
        } else {
            currentProgress = (int) ((progress - max / 2) * (MAX_PITCH / max) * 2);
        }

        et.setText(String.valueOf(currentProgress));
    }


    /// Function to append status text onto "console box" on the Activity
    public void appendToConsole(final String text) {
        // run on UI thread to avoid conflicts
        runOnUiThread(new Runnable() {
            public void run() {
                consoleText.append(text);
                consoleText.append("\n");
                textViewConsole.setText(consoleText);
            }
        });
    }


    /// print SoundTouch native library version onto console
    protected void checkLibVersion() {
        String ver = SoundTouch.getVersionString();
        appendToConsole("SoundTouch native library version = " + ver);
    }


    /// Button click handler
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
//            case R.id.buttonSelectSrcFile:
//            case R.id.buttonSelectOutFile:
//                // one of the file select buttons clicked ... we've not just implemented them ;-)
//                Toast.makeText(this, "File selector not implemented, sorry! Enter the file path manually ;-)", Toast.LENGTH_LONG).show();
//                break;

            case R.id.buttonProcess:
                // button "process" pushed
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    process();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
                break;
        }

    }


    /// Play audio file
    protected void playWavFile(String fileName) {
        File file2play = new File(fileName);
        Uri uri;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(this, "com.maodq.soundtouch.fileprovider", file2play);
            intent.setData(uri);
            // 授予临时权限
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file2play);
            intent.setDataAndType(uri, "audio/wav");
        }


        startActivity(intent);
    }


    /// Helper class that will execute the SoundTouch processing. As the processing may take
    /// some time, run it in background thread to avoid hanging of the UI.
    protected class ProcessTask extends AsyncTask<ProcessTask.Parameters, Integer, Long> {
        /// Helper class to store the SoundTouch file processing parameters
        public final class Parameters {
            String inFileName;
            String outFileName;
            float tempo;
            float pitch;
        }


        /// Function that does the SoundTouch processing
        public final long doSoundTouchProcessing(ProcessTask.Parameters params) {

            // assets中的文件拷贝到download目录
            String inputPath = Util.copyAssets(MainActivity.this.getApplicationContext(), params.inFileName);
            if (inputPath == null) {
                return -1L;
            }

            SoundTouch st = new SoundTouch();
            st.setTempo(params.tempo);
            st.setPitchSemiTones(params.pitch);
            Log.i("SoundTouch", "process file " + inputPath);
            long startTime = System.currentTimeMillis();
            int res = st.processFile(inputPath, params.outFileName);
            long endTime = System.currentTimeMillis();
            float duration = (endTime - startTime) * 0.001f;

            Log.i("SoundTouch", "process file done, duration = " + duration);
            appendToConsole("Processing done, duration " + duration + " sec.");
            if (res != 0) {
                String err = SoundTouch.getErrorString();
                appendToConsole("Failure: " + err);
                return -1L;
            }

            // Play file if so is desirable
            if (checkBoxPlay.isChecked()) {
                playWavFile(params.outFileName);
            }
            return 0L;
        }


        /// Overloaded function that get called by the system to perform the background processing
        @Override
        protected Long doInBackground(ProcessTask.Parameters... aparams) {
            return doSoundTouchProcessing(aparams[0]);
        }
    }


    /// process a file with SoundTouch. Do the processing using a background processing
    /// task to avoid hanging of the UI
    protected void process() {
        try {
            ProcessTask task = new ProcessTask();
            ProcessTask.Parameters params = task.new Parameters();
            // parse processing parameters
//            params.inFileName = editSourceFile.getText().toString();
            params.inFileName = INPUT_FILE_NAME;
            params.outFileName = editOutputFile.getText().toString();
            params.tempo = 0.01f * Float.parseFloat(editTempo.getText().toString());
            params.pitch = Float.parseFloat(editPitch.getText().toString());

            // update UI about status
            appendToConsole("Process audio file :" + params.inFileName + " => " + params.outFileName);
            appendToConsole("Tempo = " + params.tempo);
            appendToConsole("Pitch adjust = " + params.pitch);

            Toast.makeText(this, "Starting to process file " + params.inFileName + "...", Toast.LENGTH_SHORT).show();

            // start SoundTouch processing in a background thread
            task.execute(params);
//			task.doSoundTouchProcessing(params);	// this would run processing in main thread

        } catch (Exception exp) {
            exp.printStackTrace();
        }

    }

    private static class MyOnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }
}
