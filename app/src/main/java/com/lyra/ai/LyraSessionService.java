package com.lyra.ai;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.service.voice.*;
import android.speech.*;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;

public final class LyraSessionService extends VoiceInteractionSessionService {
    @Override public VoiceInteractionSession onNewSession(Bundle args) {
        return new Session(this);
    }
    private static final class Session extends VoiceInteractionSession {
        private final Context context;
        private FrameLayout container;
        private LyraPanel panel;
        private Bitmap screenshot;
        private SpeechRecognizer recognizer;
        private int counter=0;

        Session(Context context) {super(context);this.context=context;}

        @Override public View onCreateContentView() {
            container=new FrameLayout(context);
            container.setBackgroundColor(Color.BLACK);
            displayPanel();
            return container;
        }
        @Override public void onShow(Bundle args,int flags) {
            super.onShow(args,flags);
            if(container!=null) displayPanel();
            sizeWindow(false);
        }
        private void sizeWindow(boolean full) {
            if(getWindow()==null || getWindow().getWindow()==null) return;
            android.view.Window win=getWindow().getWindow();
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setGravity(Gravity.BOTTOM);
            win.setLayout(-1,full?-1:dip(360));
            win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params=win.getAttributes();
            params.dimAmount=full?0f:.16f;
            win.setAttributes(params);
        }
        private void displayPanel() {
            if(container==null)return;
            container.removeAllViews();
            panel=new LyraPanel(context,true);
            container.addView(panel,new FrameLayout.LayoutParams(-1,-1));
            panel.onAsk = text -> ask(text,null);
            panel.onMic = this::listen;
            panel.onSettings = this::configure;
            panel.onSelect = this::selectScreenshot;
            panel.showSelectionButton(screenshot!=null);
        }
        @Override public void onHandleScreenshot(Bitmap image) {
            super.onHandleScreenshot(image);
            screenshot=image;
            if(panel!=null)panel.showSelectionButton(image!=null);
        }
        private void ask(String question,Bitmap image) {
            String key=Secrets.load(context);
            if(key.isEmpty()) { panel.showAnswer("API anahtarı eksik. Küreye uzun basarak ayarlayabilirsin."); return; }
            panel.showThinking();
            int current=++counter;
            ApiClient.ask(key,question,image,reply -> new Handler(Looper.getMainLooper()).post(() -> {
                if(current==counter && panel!=null)panel.showAnswer(reply);
                if(image!=null && !image.isRecycled())image.recycle();
            }));
        }
        private void configure() {
            Intent intent=new Intent(context,MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("configure",true);
            context.startActivity(intent);
            finish();
        }
        private void listen() {
            if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
                panel.showAnswer("Mikrofon izni için Lyra uygulamasını açmalısın.");
                return;
            }
            if(!SpeechRecognizer.isRecognitionAvailable(context)){
                panel.showAnswer("Ses tanıma hizmeti bulunamadı.");return;
            }
            if(recognizer!=null)recognizer.destroy();
            recognizer=SpeechRecognizer.createSpeechRecognizer(context);
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params){ panel.orb.mode=LyraPanel.Orb.Mode.LISTENING; }
                @Override public void onBeginningOfSpeech(){}
                @Override public void onRmsChanged(float rms){}
                @Override public void onBufferReceived(byte[] data){}
                @Override public void onEndOfSpeech(){panel.orb.mode=LyraPanel.Orb.Mode.THINKING;}
                @Override public void onError(int error) { panel.orb.mode=LyraPanel.Orb.Mode.IDLE;panel.showAnswer("Ses algılanamadı."); }
                @Override public void onResults(Bundle b) {
                    ArrayList<String> texts=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if(texts!=null&&!texts.isEmpty())ask(texts.get(0),null);
                }
                @Override public void onPartialResults(Bundle b){}
                @Override public void onEvent(int type,Bundle b){}
            });
            Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"tr-TR");
            recognizer.startListening(intent);
        }
        private void selectScreenshot() {
            if(container==null||screenshot==null)return;
            container.removeAllViews();
            container.addView(new ScreenSelector(context,screenshot,
                cropped -> {
                    displayPanel();sizeWindow(false);
                    ask("Görseldeki YKS sorusunu çöz ve tüm seçenekleri kontrol et.",cropped);
                },() -> {displayPanel();sizeWindow(false);}),new FrameLayout.LayoutParams(-1,-1));
            sizeWindow(true);
        }
        private int dip(int val){return (int)(val*context.getResources().getDisplayMetrics().density+.5f);}
        @Override public void onHide() {
            ++counter;
            if(recognizer!=null){recognizer.destroy();recognizer=null;}
            screenshot=null;
            super.onHide();
        }
    }
}