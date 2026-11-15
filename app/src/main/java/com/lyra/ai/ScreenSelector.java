package com.lyra.ai;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;

final class ScreenSelector extends FrameLayout {
    interface Selected {void onSelected(Bitmap bmp);}
    final private Bitmap screenshot;
    final private Sketch sketch;
    ScreenSelector(Context ctx,Bitmap bmp,Selected chosen,Runnable cancel) {
        super(ctx);
        screenshot=bmp;
        setBackgroundColor(Color.BLACK);
        ImageView preview=new ImageView(ctx);
        preview.setImageBitmap(bmp);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(preview,new LayoutParams(-1,-1));
        sketch=new Sketch(ctx);
        addView(sketch,new LayoutParams(-1,-1));
        LinearLayout actions=new LinearLayout(ctx);
        actions.setGravity(Gravity.CENTER);
        actions.setBackgroundColor(0xEA030916);
        TextView back=button(ctx,"Vazgeç");
        back.setOnClickListener(v->cancel.run());
        actions.addView(back);
        TextView solve=button(ctx,"Seçili soruyu çöz");
        solve.setTextColor(0xFF72D7FF);
        solve.setOnClickListener(v-> {
            RectF region=sketch.box();
            if(region==null)return;
            float scale=Math.min(getWidth()/(float)bmp.getWidth(),getHeight()/(float)bmp.getHeight());
            float x=(getWidth()-bmp.getWidth()*scale)/2f;
            float y=(getHeight()-bmp.getHeight()*scale)/2f;
            int left=Math.max(0,Math.min(bmp.getWidth()-1,(int)((region.left-x)/scale)));
            int top=Math.max(0,Math.min(bmp.getHeight()-1,(int)((region.top-y)/scale)));
            int right=Math.max(left+1,Math.min(bmp.getWidth(),(int)((region.right-x)/scale)));
            int bottom=Math.max(top+1,Math.min(bmp.getHeight(),(int)((region.bottom-y)/scale)));
            chosen.onSelected(Bitmap.createBitmap(bmp,left,top,right-left,bottom-top));
        });
        actions.addView(solve);
        addView(actions,new LayoutParams(-1,-2,Gravity.BOTTOM));
    }
    private TextView button(Context ctx,String title) {
        TextView text=new TextView(ctx);
        text.setText(title);
        text.setTextColor(Color.WHITE);
        text.setTextSize(15);
        int d=(int)(getResources().getDisplayMetrics().density*16);
        text.setPadding(d,d,d,d);
        return text;
    }
    private static final class Sketch extends View {
        private float a,b,x,y;
        private boolean active;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Sketch(Context c){super(c);}
        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){
                a=x=e.getX();b=y=e.getY();active=true;invalidate();return true;
            }
            if(e.getActionMasked()==MotionEvent.ACTION_MOVE||e.getActionMasked()==MotionEvent.ACTION_UP) {
                x=e.getX();y=e.getY();invalidate();return true;
            }
            return true;
        }
        RectF box() {
            if(!active||Math.abs(a-x)<32||Math.abs(b-y)<32)return null;
            return new RectF(Math.min(a,x),Math.min(b,y),Math.max(a,x),Math.max(b,y));
        }
        @Override protected void onDraw(Canvas canvas) {
            RectF rect=box();
            if(rect==null)return;
            paint.setColor(0x223DD3FF);paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(rect,14,14,paint);
            paint.setColor(0xFF52D3FF);paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(getResources().getDisplayMetrics().density*3);
            canvas.drawRoundRect(rect,14,14,paint);
        }
    }
}