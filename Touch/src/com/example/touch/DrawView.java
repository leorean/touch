package com.example.touch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class DrawView extends View{

	private Paint mPaint;
	private Bitmap mBitmap;
	private Canvas mCanvas;
	private Path mPath;
	private Paint mBitmapPaint;
	private Paint circlePaint;
	private Path circlePath;
	private Paint textPaint;
	//private float pressure;
	private int finger = 0;
	private float mX, mY;
	private static final float TOUCH_TOLERANCE = 4;
	
	private	int tx[] = {0,0,0,0,0,0,0,0,0,0}, ty[] = {0,0,0,0,0,0,0,0,0,0};
	private boolean isLogging = false;
	private BufferedWriter writer;
	private File file;

	public DrawView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		// line path
		mPath = new Path();

		// line properties
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(Color.GREEN);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(10);

		// bitmap paint
		mBitmapPaint = new Paint(Paint.DITHER_FLAG);

		// circle path and paint properties
		circlePaint = new Paint();
		circlePath = new Path();

		// text properties
		textPaint = new Paint();
		textPaint.setTextSize(30);
		textPaint.setColor(Color.BLACK);
	}
	
	
	/**
	 * listen to view changes
	 * initialize bitmap and canvas object
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap);

	}

	/**
	 * draw line according to finger touch movement with canvas object
	 * 
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// display pressure near touch
		canvas.drawText(String.format("%.4f", MainActivity.pressure) + " kOhm, " + String.format("%.2f", MainActivity.newton) + " N" , tx[0]-200, ty[0]-100, textPaint);

		// display x and y position at top-left corner
		// comment this line for not showing the position
		canvas.drawText("x: " + String.valueOf(tx[0]) + " y: " + String.valueOf(ty[0]), 5, 30, textPaint);
		
		// draw line and circle around finger touch
		canvas.drawBitmap(mBitmap,0,0,mBitmapPaint);
		canvas.drawPath(mPath, mPaint);
		canvas.drawPath(circlePath, circlePaint);
	}

	/**
	 * onTouchEvent listener performs actions when screen is touched
	 * ACTION_DOWN: {@link DrawView#touch_start(float, float)}
	 * ACTION_MOVE: {@link DrawView#touch_move(float, float)}
	 * ACTION_UP: {@link DrawView#touch_up()}
	 * 
	 * call invalidate() to update screen and to call onDraw()
	 */

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//pX = (int)event.getX();
		//pY = (int)event.getY();
		
		Arrays.fill(tx, 0);
		Arrays.fill(ty, 0);
		for(int i = 0; i < event.getPointerCount(); i++) {
			tx[i] = (int) event.getX(i);
			ty[i] = (int) event.getY(i);
			//pressure = event.getPressure(i);
			finger = i;
		}
		
		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			touch_start(tx[0], ty[0]);
			invalidate();
		}
		
		if(event.getAction() == MotionEvent.ACTION_MOVE) {
			touch_move(tx[0], ty[0]);
			if(writer != null) {
				try {
					writer.append("x: " + String.valueOf(tx[0]) + ";" + " y: " + String.valueOf(ty[0]) + "; " + String.format("%.4f", MainActivity.pressure) + " kOhm; " + String.format("%.2f", MainActivity.newton) + " N");
					writer.newLine();
				} catch (IOException e) {
					Log.v("Error", e.getMessage());
				}
			}
			invalidate();
		}
		
		if(event.getAction() == MotionEvent.ACTION_UP) {
			touch_up();
			invalidate();
		}
		
		if(event.getAction() == MotionEvent.ACTION_POINTER_UP && !isLogging) {
			
			if(finger+1 == 2) {
					startLogging();
			}
		}
		
		if(event.getAction() == MotionEvent.ACTION_POINTER_DOWN && isLogging) {
			
			stopLogging();		
		}
		
		invalidate();
		return true;
	}


	private void touch_start(float x, float y) {
		mPath.reset();
		mPath.moveTo(x, y);
		mX = x;
		mY = y;
	}

	private void touch_move(float x, float y) {
		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
			mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
			mX = x;
			mY = y;

			// set line stroke width according to pressure
			// put in here pressure from multimeter
			mPaint.setStrokeWidth(10);            

			circlePath.reset();
			circlePath.addCircle(mX, mY, 30, Path.Direction.CW);
		}
	}

	private void touch_up() {
		mPath.lineTo(mX, mY);
		circlePath.reset();

		// uncomment to "save" all drawn lines
		//mCanvas.drawPath(mPath,  mPaint);

		// uncomment to delete drawn line after touch release
		//mPath.reset();
	}
	
	/**
	 * start the logging process and set isLogging to true
	 */
	private void startLogging() {
		try
		{
			Toast.makeText(getContext(), "Enable Logging...", Toast.LENGTH_SHORT).show();
			//DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			file = new File(getContext().getExternalFilesDir(null), "log_"+cal.getTime().toString()+".txt");
			if (!file.exists())
				file.createNewFile();
			Log.v("file", file.getAbsolutePath().toString());
		} catch (IOException e) {Log.v("Error",e.getMessage());}

		try
		{
			writer = new BufferedWriter(new FileWriter(file, true));
			isLogging = true;

		} catch (IOException e) {Log.v("Error",e.getMessage());}
	}
	
	/**
	 * stop the logging process and set isLogging to false
	 */
	
	private void stopLogging() {
		try
		{
			Toast.makeText(getContext(),"Logging Stopped...\n Saved File as " + file.getAbsolutePath().toString(),Toast.LENGTH_LONG).show();
			writer.close();
			writer = null;

		} catch (IOException e) {Log.v("Error",e.getMessage());}
		isLogging = false;
	}
}