package org.apache.cordova.speech;

import java.util.ArrayList;

import org.apache.cordova.PermissionHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import android.content.pm.PackageManager;
import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.Manifest;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Style and such borrowed from the TTS and PhoneListener plugins
 */
public class SpeechRecognition extends CordovaPlugin {

    private final int REQ_CODE_SPEECH_OUTPUT = 143;

    private static final String LOG_TAG = SpeechRecognition.class.getSimpleName();
    public static final String ACTION_INIT = "init";
    public static final String ACTION_SPEECH_RECOGNIZE_START = "start";
    public static final String ACTION_SPEECH_RECOGNIZE_STOP = "stop";
    public static final String ACTION_SPEECH_RECOGNIZE_ABORT = "abort";
    public static final String NOT_PRESENT_MESSAGE = "Speech recognition is not present or enabled";

    private CallbackContext speechRecognizerCallbackContext;
    private boolean recognizerPresent = false;
    private SpeechRecognizer recognizer;
    private boolean aborted = false;
    private boolean listening = false;
    private String lang;
    private String path;
    private String prompt = "Fale agora por favor...";

    private static String [] permissions = { Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE };
    private static int RECORD_AUDIO = 0;
    private static int WRITE_EXTERNAL_STORAGE = 1;

    protected void getMicPermission()
    {
        PermissionHelper.requestPermission(this, RECORD_AUDIO, permissions[RECORD_AUDIO]);
    }

    private void promptForMic()
    {
        if(PermissionHelper.hasPermission(this, permissions[RECORD_AUDIO])) {
            this.startRecognition();
        }
        else
        {
            getMicPermission();
        }

    }

    protected void getWritePermission()
    {
        PermissionHelper.requestPermission(this, WRITE_EXTERNAL_STORAGE, permissions[WRITE_EXTERNAL_STORAGE]);
    }

    private void promptForWrite()
    {
        if(PermissionHelper.hasPermission(this, permissions[WRITE_EXTERNAL_STORAGE])) {
            this.startRecognition();
        }
        else
        {
            getWritePermission();
        }

    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException
    {
        for(int r:grantResults)
        {
            if(r == PackageManager.PERMISSION_DENIED)
            {
                fireErrorEvent();
                fireEvent("end");
                return;
            }
        }
        if(requestCode==RECORD_AUDIO)
            promptForMic();
        if(requestCode==WRITE_EXTERNAL_STORAGE)
            promptForWrite();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        // Dispatcher
        if (ACTION_INIT.equals(action)) {
            // init
            if (DoInit()) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
                
                Handler loopHandler = new Handler(Looper.getMainLooper());
                loopHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        recognizer = SpeechRecognizer.createSpeechRecognizer(cordova.getActivity().getBaseContext());
                        recognizer.setRecognitionListener(new SpeechRecognitionListner());
                    }
                    
                });
            } else {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, NOT_PRESENT_MESSAGE));
            }
        }
        else if (ACTION_SPEECH_RECOGNIZE_START.equals(action)) {
            // recognize speech
            if (!recognizerPresent) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, NOT_PRESENT_MESSAGE));
            }
            this.lang = args.optString(0, "en");
            this.path = args.optString(1, "/");


            cordova.setActivityResultCallback (this);

            this.speechRecognizerCallbackContext = callbackContext;
            this.promptForMic();
        }
        else if (ACTION_SPEECH_RECOGNIZE_STOP.equals(action)) {
            stop(false);
        }
        else if (ACTION_SPEECH_RECOGNIZE_ABORT.equals(action)) {
            stop(true);
        }
        else {
            // Invalid action
            String res = "Unknown action: " + action;
            return false;
        }
        return true;
    }

    private Intent recognizerIntent;
    private void startRecognition() {

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,lang);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,lang);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        recognizerIntent.putExtra("android.speech.extra.GET_AUDIO_FORMAT","audio/AMR");
        recognizerIntent.putExtra("android.speech.extra.GET_AUDIO",true);

        Handler loopHandler = new Handler(Looper.getMainLooper());
        loopHandler.post(new Runnable() {

            @Override
            public void run() {
                //recognizer.startListening(recognizerIntent);
                cordova.getActivity().startActivityForResult(recognizerIntent, REQ_CODE_SPEECH_OUTPUT);
            }

        });

        PluginResult res = new PluginResult(PluginResult.Status.NO_RESULT);
        res.setKeepCallback(true);
        this.speechRecognizerCallbackContext.sendPluginResult(res);
    }
    
    private void stop(boolean abort) {
        this.aborted = abort;
        Handler loopHandler = new Handler(Looper.getMainLooper());
        loopHandler.post(new Runnable() {

            @Override
            public void run() {
                recognizer.stopListening();
            }
            
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQ_CODE_SPEECH_OUTPUT: {
                if (resultCode == RESULT_OK && null != data){
                    // Recognition Successfull
                    ArrayList<String> voiceInText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    for(int i=0;i<voiceInText.size();i++){
                        Log.e(LOG_TAG, i+":"+voiceInText.get(i));
                    }
                    String transcription = voiceInText.get(0);
                    String filepath = null;
                    if(data.getData()!=null) {
                        Uri audioUri = data.getData();
                        InputStream filestream = null;
                        try {
                            filestream = getContentResolver().openInputStream(audioUri);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        Log.d(LOG_TAG, "getPath(): " + audioUri.getPath());
                        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + this.path);
                        if(!dir.exists())dir.mkdir();
                        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + this.path + CreateRandomAudioFileName(5) + "v4r.amr");
                        copyInputStreamToFile(filestream, file);

                        Log.d(LOG_TAG, file.getAbsolutePath()+"|||"+file.getAbsoluteFile());
                        filepath = file.getAbsoluteFile();

                    }
                    fireRecognitionEvent(transcription, filepath);
                }
                else showVoiceText.setText("NOK: " + requestCode + " -> " + resultCode + "->"+ data.getData());
                break;
            }
        }
    }

    /**
     * Initialize the speech recognizer by checking if one exists.
     */
    private boolean DoInit() {
        this.recognizerPresent = SpeechRecognizer.isRecognitionAvailable(this.cordova.getActivity().getBaseContext());
        return this.recognizerPresent;
    }

    private void fireRecognitionEvent(String transcript, String audioFilePath) {
        JSONObject event = new JSONObject();
        JSONObject result = new JSONObject();
        try {
         
            result.put("transcript", transcript);
            result.put("audioFilePath", audioFilePath);
            event.put("type", "result");
            event.put("emma", null);
            event.put("interpretation", null);
            event.put("result", result);
        } catch (JSONException e) {
            // this will never happen
        }
        PluginResult pr = new PluginResult(PluginResult.Status.OK, event);
        pr.setKeepCallback(true);
        this.speechRecognizerCallbackContext.sendPluginResult(pr); 
    }
/*
    private void fireRecognitionEvent(ArrayList<String> transcripts, float[] confidences) {
        JSONObject event = new JSONObject();
        JSONArray results = new JSONArray();
        try {
            for(int i=0; i<transcripts.size(); i++) {
                JSONArray alternatives = new JSONArray();
                JSONObject result = new JSONObject();
                result.put("transcript", transcripts.get(i));
                result.put("final", true);
                if (confidences != null) {
                    result.put("confidence", confidences[i]);
                }
                alternatives.put(result);
                results.put(alternatives);
            }
            event.put("type", "result");
            event.put("emma", null);
            event.put("interpretation", null);
            event.put("results", results);
        } catch (JSONException e) {
            // this will never happen
        }
        PluginResult pr = new PluginResult(PluginResult.Status.OK, event);
        pr.setKeepCallback(true);
        this.speechRecognizerCallbackContext.sendPluginResult(pr); 
    }
*/

    private void fireEvent(String type) {
        JSONObject event = new JSONObject();
        try {
            event.put("type",type);
        } catch (JSONException e) {
            // this will never happen
        }
        PluginResult pr = new PluginResult(PluginResult.Status.OK, event);
        pr.setKeepCallback(true);
        this.speechRecognizerCallbackContext.sendPluginResult(pr); 
    }

    private void fireErrorEvent() {
        JSONObject event = new JSONObject();
        try {
            event.put("type","error");
        } catch (JSONException e) {
            // this will never happen
        }
        PluginResult pr = new PluginResult(PluginResult.Status.ERROR, event);
        pr.setKeepCallback(true);
        this.speechRecognizerCallbackContext.sendPluginResult(pr); 
    }

    class SpeechRecognitionListner implements RecognitionListener {

        @Override
        public void onBeginningOfSpeech() {
            Log.d(LOG_TAG, "begin speech");
            fireEvent("start");
            fireEvent("audiostart");
            fireEvent("soundstart");
            fireEvent("speechstart");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.d(LOG_TAG, "buffer received");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(LOG_TAG, "end speech");
            fireEvent("speechend");
            fireEvent("soundend");
            fireEvent("audioend");
            fireEvent("end");
        }

        @Override
        public void onError(int error) {
            Log.d(LOG_TAG, "error speech "+error);
            if (listening || error == 9) {
                fireErrorEvent();
                fireEvent("end");
            }
            listening = false;
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            Log.d(LOG_TAG, "event speech");
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            Log.d(LOG_TAG, "partial results");
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(LOG_TAG, "ready for speech");
            listening = true;
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(LOG_TAG, "results");
            String str = new String();
            Log.d(LOG_TAG, "onResults " + results);
            Log.d(LOG_TAG, "onResults.getData " + results.getData());
            ArrayList<String> transcript = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float[] confidence = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
            if (transcript.size() > 0) {
                Log.d(LOG_TAG, "fire recognition event");
                fireRecognitionEvent(transcript, confidence);
            } else {
                Log.d(LOG_TAG, "fire no match event");
                fireEvent("nomatch");
            }
            listening = false;
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            Log.d(LOG_TAG, "rms changed");
        }
        
    }


    private void copyInputStreamToFile(InputStream in, File file) {
        OutputStream out=null;
        try {
            out = new FileOutputStream(file);

            byte[] buf = new byte[1024];
            while (true) {
                int len = in.read(buf);
                if (len <= 0) {
                    break;
                }
                out.write(buf, 0, len);
            }

        } catch (Exception e) {

            Log.e("v4f","copyInputStreamToFile: "+e.getMessage());

        } finally {

            try{
                if(out!=null) out.close();
            } catch(Exception e){

            }
            try{
                in.close();
            } catch(Exception e){

            }


        }
    }
}