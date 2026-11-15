package com.lyra.ai;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int SPEECH_REQ = 811;
    private static final int AUDIO_REQ = 812;
    private LyraPanel panel;
    private int requestNumber = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(0);
        panel = new LyraPanel(this, false);
        setContentView(panel);
        panel.onAsk = this::ask;
        panel.onMic = this::microphone;
        panel.onSettings = this::configure;
        if (getIntent().getBooleanExtra("configure", false)) panel.post(this::configure);
    }

    private void ask(String question) {
        String key = Secrets.load(this);
        if (key == null || key.isEmpty()) {
            panel.setQuestion(question);
            configure();
            return;
        }
        final int request = ++requestNumber;
        panel.showThinking();
        ApiClient.ask(key, question, null, answer -> runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed() && requestNumber == request)
                panel.showAnswer(answer);
        }));
    }

    private void configure() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Google AI Studio API anahtarı");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setText(Secrets.load(this));
        input.setPadding(36, 24, 36, 24);
        new AlertDialog.Builder(this)
            .setTitle("Bağlantı")
            .setMessage("Ücretsiz API katmanında oluşturduğun kendi anahtarını gir. Ücretli kullanımı uygulama etkinleştirmez.")
            .setView(input)
            .setPositiveButton("Kaydet", (d,w) -> {
                try {
                    Secrets.save(this, input.getText().toString().trim());
                    Toast.makeText(this, "Anahtar kaydedildi", Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    Toast.makeText(this, "Anahtar kaydedilemedi", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Vazgeç", null)
            .show();
    }

    private void microphone() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, AUDIO_REQ);
        } else openSpeech();
    }
    private void openSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
        try { startActivityForResult(intent, SPEECH_REQ); }
        catch (Exception e) { panel.showAnswer("Bu cihazda ses tanıma hizmeti kullanılamıyor."); }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode == AUDIO_REQ && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) openSpeech();
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQ && resultCode == RESULT_OK && data != null) {
            java.util.ArrayList<String> texts = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (texts != null && !texts.isEmpty() && !texts.get(0).trim().isEmpty()) ask(texts.get(0).trim());
        }
    }
}