package com.snob.marquee.falling_pdm;

import android.os.*;
import android.view.*;
import android.content.*;
import android.hardware.*;
import android.graphics.*;
import android.support.v7.app.*;
import android.support.annotation.*;

import java.util.*;

public class GameActivity extends AppCompatActivity implements SensorEventListener {

    private String debugStr;

    private final float SPEED = 20;
    private final float PLAYER_SCALE = 0.065f;

    private final long SENSOR_RATE = 20;
    private final long TIMER_PERIOD = 60;

    private final float RECT_INC_RATE = 0.01f;
    private final float RECT_MAX_SCALE = 0.90f;

    private Point size;
    private Player player;
    private float realRadius;

    private GameView canvas;

    private Timer timer;
    private Handler handler;

    private Sensor accelerometer;
    private SensorManager sensorManager;

    private long lastUpdateTime;
    private float[] accelerations;

    private float rectScale;
    private float[] maxRectPoints;

    private float[] offSetX;
    private float[] offSetY;

    private class Player {
        public float scale;
        public float speed;
        public float[] pos = new float[2];

        public Player(@FloatRange(from = 0) float scale, float x, float y, float speed) {
            this.scale = scale;
            this.speed = speed;

            this.pos[0] = x;
            this.pos[1] = y;
        }

        public void movX(float movX) {
            float fX = this.pos[0] + movX;

            if (fX >= offSetX[0] && fX <= offSetX[1])
                this.pos[0] += movX;
            else if (fX < offSetX[0])
                this.pos[0] = offSetX[0];
            else
                this.pos[0] = offSetX[1];
        }

        public void movY(float movY) {
            float fY = this.pos[1] + movY;

            if (fY >= offSetY[0] && fY <= offSetY[1])
                this.pos[1] += movY;
            else if (fY < offSetY[0])
                this.pos[1] = offSetY[0];
            else
                this.pos[1] = offSetY[1];
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.rectScale = 0;
        this.size = new Point();
        this.offSetX = new float[2];
        this.offSetY = new float[2];
        this.accelerations = new float[2];
        this.lastUpdateTime = System.currentTimeMillis();

        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.sensorManager.registerListener(this, this.accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        getWindowManager().getDefaultDisplay().getSize(this.size);

        this.maxRectPoints = calcRectPoints(RECT_MAX_SCALE);

        this.realRadius = this.size.x * PLAYER_SCALE;

        this.offSetX[0] = this.maxRectPoints[0] + this.realRadius;
        this.offSetX[1] = this.maxRectPoints[2] - this.realRadius;

        this.offSetY[0] = this.maxRectPoints[1] + this.realRadius;
        this.offSetY[1] = this.maxRectPoints[3] - this.realRadius;

        float speed = this.size.x / 1920 * SPEED;
        float startX = this.size.x / 2 - this.realRadius;
        float startY = this.size.y / 2 - this.realRadius;

        this.player = new Player(PLAYER_SCALE, startX, startY, speed);

        this.canvas = new GameView(GameActivity.this);
        setContentView(this.canvas);
        getWindow().getDecorView().setSystemUiVisibility(StartActivity.UI_OPTIONS);

        this.handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                canvas.invalidate();
            }
        };

        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                float mov = player.speed * accelerations[1];
                player.movX(mov);

                mov = player.speed * accelerations[0];
                player.movY(mov);

                rectScale += RECT_INC_RATE;
                if (rectScale >= RECT_MAX_SCALE)
                    rectScale = 0;

                handler.sendEmptyMessage(0);
            }
        }, 0, TIMER_PERIOD);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor eventSensor = event.sensor;

        if (eventSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - this.lastUpdateTime >= SENSOR_RATE) {
                this.lastUpdateTime = currentTime;

                float aX = event.values[0];
                float aY = event.values[1];

                this.accelerations[0] = Math.min(Math.abs(aX), 10) * Math.signum(aX);
                this.accelerations[1] = Math.min(Math.abs(aY), 10) * Math.signum(aY);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public float[] calcRectPoints(float scale) {
        float[] ret = new float[4];
        ret[0] = (size.x - size.x * scale) / 2;
        ret[1] = (size.y - size.y * scale) / 2;

        ret[2] = (size.x + size.x * scale) / 2;
        ret[3] = (size.y + size.y * scale) / 2;

        return ret;
    }

    private class GameView extends View {

        private Paint pen;

        public GameView(Context context) {
            super(context);
            setFocusable(true);

            this.pen = new Paint();
            this.pen.setTextSize(50f);
            this.pen.setAntiAlias(true);
            this.pen.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas screen) {
            //screen.drawText(debugStr, size.x / 2, size.y / 2, this.pen);

            this.pen.setARGB(255, 0, 0, 0);
            float[] coords = calcRectPoints(rectScale);
            screen.drawRect(coords[0], coords[1], coords[2], coords[3], this.pen);

            this.pen.setARGB(255, 0, 0, 255);
            screen.drawCircle(player.pos[0], player.pos[1], realRadius, this.pen);
        }
    }
}