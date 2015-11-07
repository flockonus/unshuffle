package xyz.ordering.unshuffle;

import android.app.Service;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * Created by flockonus on 15-11-07.
 */
public class HoverLayer {

    private final Service service;
    private ImageView chatHead;
    private WindowManager window;
    private WindowManager.LayoutParams params;

    public HoverLayer(Service service_) {
        service = service_;
        window = (WindowManager) service.getSystemService(Service.WINDOW_SERVICE);
        drawMyShit();
        handleTouch();
    }

    private void drawMyShit() {
        chatHead = new ImageView(service);
        chatHead.setImageResource(R.drawable.keno);
        chatHead.setScaleX((float) 0.5);
        chatHead.setScaleY((float) 0.5);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        window.addView(chatHead, params);
    }

    private void handleTouch(){
        chatHead.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        window.updateViewLayout(chatHead, params);
                        return true;
                }
                return false;
            }
        });
    }

    public void destroy(){
        if (chatHead != null) window.removeView(chatHead);
    }


}
