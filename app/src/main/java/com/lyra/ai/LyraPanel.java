package com.lyra.ai;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.util.Locale;

final class LyraPanel extends FrameLayout {
    interface Ask { void run(String question); }
    Ask onAsk;
    Runnable onMic, onSettings, onSelect;
    final Orb orb;
    private final EditText input;
    private final ScrollView answerScroll;
    private final TextView answer;
    private final ActionIcon sendButton;
    private final ActionIcon selectButton;
    private final boolean compact;
    private final int margin;

    LyraPanel(Context ctx, boolean compact) {
        super(ctx);
        this.compact = compact;
        margin = compact ? 12 : 21;
        setBackgroundColor(Color.BLACK);

        orb = new Orb(ctx);
        int diameter = compact ? 190 : 300;
        FrameLayout.LayoutParams sphere = new FrameLayout.LayoutParams(dp(diameter), dp(diameter), Gravity.CENTER);
        orb.setTranslationY(-dp(compact ? 25 : 75));
        addView(orb, sphere);
        orb.setContentDescription("Lyra asistanı");
        orb.setOnLongClickListener(v -> { if (onSettings != null) onSettings.run(); return true; });

        answerScroll = new ScrollView(ctx);
        answerScroll.setVisibility(GONE);
        answerScroll.setFillViewport(false);
        answerScroll.setVerticalScrollBarEnabled(false);
        answerScroll.setBackgroundColor(0xE9000000);
        answerScroll.setPadding(dp(14), dp(12), dp(14), dp(4));
        answer = new TextView(ctx);
        answer.setTextColor(0xFFE4EEFF);
        answer.setTextSize(compact ? 15 : 16);
        answer.setLineSpacing(dp(4), 1.03f);
        answer.setTextIsSelectable(true);
        answerScroll.addView(answer);
        FrameLayout.LayoutParams answerParams = new FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, dp(compact ? 158 : 245), Gravity.BOTTOM);
        answerParams.leftMargin = dp(margin);
        answerParams.rightMargin = dp(margin);
        answerParams.bottomMargin = dp(compact ? 86 : 116);
        addView(answerScroll, answerParams);

        LinearLayout bar = new LinearLayout(ctx);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF203E6D, 0xFF0D203C});
        background.setCornerRadius(dp(34));
        background.setStroke(dp(1), 0xFF476A99);
        bar.setBackground(background);
        bar.setElevation(dp(8));

        input = new EditText(ctx);
        input.setTextSize(15f);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0xFFA6BBDA);
        input.setHint("Bir şey sor...");
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setSingleLine(false);
        input.setMaxLines(3);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setPadding(dp(20), dp(12), dp(4), dp(12));
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) { submit(); return true; }
            return false;
        });
        bar.addView(input, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));

        selectButton = new ActionIcon(ctx, true);
        selectButton.setVisibility(GONE);
        selectButton.setContentDescription("Ekrandan soruyu seç");
        selectButton.setOnClickListener(v -> { if (onSelect != null) onSelect.run(); });
        bar.addView(selectButton, new LinearLayout.LayoutParams(dp(44), dp(54)));

        sendButton = new ActionIcon(ctx, false);
        sendButton.setContentDescription("Sesle sor");
        sendButton.setOnClickListener(v -> {
            if (input.getText().length() > 0) submit();
            else if (onMic != null) onMic.run();
        });
        bar.addView(sendButton, new LinearLayout.LayoutParams(dp(56), dp(54)));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.send = s.length() > 0;
                sendButton.setContentDescription(s.length() > 0 ? "Gönder" : "Sesle sor");
                sendButton.invalidate();
            }
            @Override public void afterTextChanged(Editable editable) {}
        });
        FrameLayout.LayoutParams footer = new FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        footer.leftMargin = dp(margin); footer.rightMargin = dp(margin);
        footer.bottomMargin = dp(compact ? 14 : 27);
        addView(bar, footer);
    }

    void showSelectionButton(boolean enabled) {
        selectButton.setVisibility(enabled ? VISIBLE : GONE);
    }
    void setQuestion(String str) { input.setText(str); input.setSelection(input.length()); }
    void showThinking() {
        orb.mode = Orb.Mode.THINKING;
        showResult("Çözüm hazırlanıyor…");
    }
    void showAnswer(String str) {
        orb.mode = Orb.Mode.IDLE;
        showResult(str);
    }
    private void showResult(String str) {
        answer.setText(str);
        answerScroll.setVisibility(VISIBLE);
        answerScroll.scrollTo(0,0);
        orb.animate().translationY(-dp(compact ? 105 : 180)).scaleX(.68f).scaleY(.68f).setDuration(300).start();
    }
    private void submit() {
        String q = input.getText().toString().trim();
        if (q.isEmpty()) return;
        input.setText("");
        ((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(input.getWindowToken(), 0);
        input.clearFocus();
        if (onAsk != null) onAsk.run(q);
    }
    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + .5f); }

    private static class ActionIcon extends View {
        private final boolean crop;
        boolean send = false;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        ActionIcon(Context context, boolean crop) { super(context); this.crop = crop; setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float d = getResources().getDisplayMetrics().density;
            float x = getWidth()/2f, y = getHeight()/2f;
            p.setColor(0xFFE8F6FF);
            p.setStrokeWidth(1.8f*d); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
            p.setStyle(Paint.Style.STROKE);
            if (crop) {
                float r = 8*d;
                canvas.drawLine(x-r,y-3*d,x-r,y-r,p);
                canvas.drawLine(x-r,y-r,x-3*d,y-r,p);
                canvas.drawLine(x+r,y+3*d,x+r,y+r,p);
                canvas.drawLine(x+r,y+r,x+3*d,y+r,p);
                canvas.drawRoundRect(x-4*d,y-4*d,x+4*d,y+4*d,2*d,2*d,p);
            } else if (send) {
                Path arrow = new Path();
                arrow.moveTo(x-8*d,y+5*d); arrow.lineTo(x+8*d,y); arrow.lineTo(x-8*d,y-6*d);
                arrow.moveTo(x+8*d,y); arrow.lineTo(x-4*d,y);
                canvas.drawPath(arrow,p);
            } else {
                p.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(x-3.5f*d,y-9*d,x+3.5f*d,y+4*d,3.5f*d,3.5f*d,p);
                p.setStyle(Paint.Style.STROKE);
                canvas.drawArc(x-7*d,y-4*d,x+7*d,y+9*d,0,180,false,p);
                canvas.drawLine(x,y+9*d,x,y+13*d,p);
                canvas.drawLine(x-4*d,y+13*d,x+4*d,y+13*d,p);
            }
        }
    }

    static final class Orb extends View {
        enum Mode { IDLE, LISTENING, THINKING, SPEAKING }
        Mode mode = Mode.IDLE;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private ValueAnimator animator;
        private float phase = 0f;

        Orb(Context ctx) {
            super(ctx);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
        }
        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (animator == null) {
                animator = ValueAnimator.ofFloat(0f, (float)(2*Math.PI));
                animator.setDuration(5800);
                animator.setInterpolator(new android.view.animation.LinearInterpolator());
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.addUpdateListener(v -> { phase = (float)v.getAnimatedValue(); invalidate(); });
                animator.start();
            }
        }
        @Override protected void onDetachedFromWindow() {
            if (animator != null) animator.cancel();
            animator = null;
            super.onDetachedFromWindow();
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float size = Math.min(getWidth(),getHeight());
            if (size <= 0) return;
            float cx = getWidth()/2f, cy=getHeight()/2f, r=size*.305f;
            float pulse = 1f + (mode == Mode.LISTENING ? .09f : mode == Mode.THINKING ? .05f : .025f)
                * (float)Math.sin(phase*1.8f);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(cx,cy,r*2.1f,
                new int[]{0x492B74D9,0x161B58A9,0x00000000}, new float[]{0f,.52f,1f},Shader.TileMode.CLAMP));
            canvas.drawCircle(cx,cy,r*2.1f,paint);
            paint.setShader(new RadialGradient(cx-r*.17f,cy-r*.20f,r*1.12f,
                new int[]{0xD60C276C,0xFF061631,0xFF01040B},null,Shader.TileMode.CLAMP));
            canvas.drawCircle(cx,cy,r*pulse,paint); paint.setShader(null);
            for (int layer=0;layer<5;layer++) {
                Path path = new Path();
                for (int i=0;i<=124;i++) {
                    float t=i/124f;
                    double theta=phase*(layer%2==0?1:-.6) + t*Math.PI*2 + layer*.88;
                    float rad=r*pulse*(.74f+.20f*(float)Math.sin(t*10.5+phase+layer*1.4));
                    float x=cx+(float)Math.cos(theta)*rad;
                    float y=cy+(float)Math.sin(theta)*rad*(.84f+.12f*(float)Math.sin(theta*2+layer));
                    if (i==0) path.moveTo(x,y); else path.lineTo(x,y);
                }
                stroke.setColor(layer%2==0?0xFF4AD0FF:0xFF7879FF);
                stroke.setAlpha(26); stroke.setStrokeWidth(size*.061f); canvas.drawPath(path,stroke);
                stroke.setAlpha(70); stroke.setStrokeWidth(size*.025f); canvas.drawPath(path,stroke);
                stroke.setAlpha(225); stroke.setStrokeWidth(size*.006f); canvas.drawPath(path,stroke);
            }
            Path crescent=new Path();
            for(int i=0;i<=90;i++) {
                float t=i/90f;
                double a=-1.2+t*3.25 + Math.sin(phase*.46)*.28;
                float rr=r*pulse*(.96f+.03f*(float)Math.sin(t*8+phase));
                float x=cx+(float)Math.cos(a)*rr;
                float y=cy+(float)Math.sin(a)*rr;
                if (i==0)crescent.moveTo(x,y);else crescent.lineTo(x,y);
            }
            stroke.setShader(new LinearGradient(cx-r,cy-r,cx+r,cy+r,
                new int[]{0x0046BAFF,0xFF4CC7FF,Color.WHITE,0xFF5473FF},null,Shader.TileMode.CLAMP));
            stroke.setStrokeWidth(size*.010f); stroke.setAlpha(225); canvas.drawPath(crescent,stroke);
            stroke.setShader(null); stroke.setAlpha(255);
        }
    }
}