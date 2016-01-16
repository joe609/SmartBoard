package com.example.android.bluetoothlegatt;

/**
 * Created by Joe on 10/12/2015.
 */
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.ParcelUuid;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public class DrawingView extends View implements ServiceRelay, Runnable
{
    //drawing path
    private Path drawPath;
    //drawing and canvas paint
    private Paint drawPaint, canvasPaint;
    //initial color
    private int paintColor = 0xFF660000;
    //canvas
    private Canvas drawCanvas;
    //canvas bitmap
    private Bitmap canvasBitmap;
    // a reference to our BluetoothLeService
    private BluetoothLeService service;
    private Thread bluetoothThread;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();

        bluetoothThread = new Thread(this);
        bluetoothThread.start();
    }



    private void setupDrawing(){
        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(20);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        canvasPaint = new Paint(Paint.DITHER_FLAG);
//get drawing area setup for interaction
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
//view given size
    }
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);
//draw view
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        isRunning = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                break;
            default:
                return false;
        }

        // send the X & Y coordinates over bluetooth.
        sendPointToBluetooth(touchX, touchY);

        postInvalidate(); // Indicate view should be redrawn
        return true; // Indicate we've consumed the touch
//detect user touch
    }

    private void sendPointToBluetooth(float touchX, float touchY) {
        // convert the point into a string.
        pointAsString = touchX + "," + touchY + ",";
    }

    @Override
    public void setService(BluetoothLeService service) {
        this.service = service;
    }

    private String oldPoint;
    private String pointAsString;
    private boolean isRunning = false;

    @Override
    public void run() {
        isRunning = true;

        while(isRunning) {
            try {

                if (oldPoint != pointAsString) {
                    BluetoothGattCharacteristic characteristic =
                            service.getSupportedGattServices().get(2).getCharacteristics().get(0);
                    characteristic.setValue(pointAsString);
                    // try writing a characteristic (data) to the BLE device.
                    service.mBluetoothGatt.writeCharacteristic(characteristic);

                    Log.e("SENT", "Characteristic sent.");
                    Log.d("Value", pointAsString);

                }
            }
            finally {
                oldPoint = pointAsString;
            }
        }
    }
}
