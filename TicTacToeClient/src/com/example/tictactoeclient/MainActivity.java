package com.example.tictactoeclient;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	// static constants
	protected static final String SERVER_ADDRESS = "54.186.194.129";
	protected static final int DEFAULT_PORT = 20000;
	// TextView msg
	protected static final int MSG_TV = 1;
	// Button msg
	protected static final int MSG_B = 2;
	// Enable/Disable Button msg
	protected static final int MSG_TOGGLE = 3;
	// End of Game msg
	protected static final int MSG_END = 4;
	
	final int[] buttonIDArray = {R.id.b0, R.id.b1, R.id.b2,
								 R.id.b3, R.id.b4, R.id.b5,
								 R.id.b6, R.id.b7, R.id.b8};
	
	protected HashMap<Button, Integer> board = new HashMap<Button, Integer>(9);
	
	protected TextView tvInfoText;
	protected Button[] buttons = new Button[9];
	
	protected static NetworkHandler handler;
	NetworkThread networkThread;
	
	// game variables
	protected static String myMark;
	protected static String opponentMark;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		tvInfoText = (TextView) findViewById(R.id.textView1);
		for (int i = 0; i < buttons.length; ++i) {
			buttons[i] = (Button) findViewById(buttonIDArray[i]);
			board.put(buttons[i], i);
		}
		
		disableButtons();
		
		handler = new NetworkHandler(this);
		// start a new thread to perform network I/O
		networkThread = new NetworkThread(this);
		networkThread.start();
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.exit) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	// helpers
	public void onBClick(View v) {
		Button clicked = (Button) v;
		if (isValidMove(clicked)) {
			int move = board.get(clicked);
			makeMove(move, myMark);
			networkThread.myMove = move;
			synchronized (networkThread) {
				networkThread.notify();
			}
		} else {
			Toast.makeText(getApplicationContext(), "Invalid Move!", Toast.LENGTH_SHORT).show();
		}
	}
	
	// Handler class
	static class NetworkHandler extends Handler {
		
		// prevents memory leaks
		WeakReference<MainActivity> wr;
		
		public NetworkHandler(MainActivity ma) {
			wr = new WeakReference<MainActivity>(ma);
		}
		
		@Override
		public void handleMessage(Message msg) {
			MainActivity ma = wr.get();
			if(msg.what == MSG_TV) {
				ma.tvInfoText.setText((String) msg.obj);
			}
			else if(msg.what == MSG_B) {
				ma.makeMove(Integer.parseInt((String) msg.obj), opponentMark);
			}
			else if(msg.what == MSG_TOGGLE) {
				if (msg.obj.equals("E")) {
					ma.enableButtons();
				} else {
					ma.disableButtons();
				}
			}
			else if(msg.what == MSG_END) {
				ma.displayMessage((String) msg.obj);
			}
		}
	}
	
	public void displayMessage(String msg) {
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}
	
	/*	returns 0 if not over
	 * 		    1 if won
	 * 		   -1 if lost
	 */
	public int isGameOver() {
		if((buttons[0].getText().equals(myMark) && buttons[1].getText().equals(myMark) && buttons[2].getText().equals(myMark)) ||
	       (buttons[3].getText().equals(myMark) && buttons[4].getText().equals(myMark) && buttons[5].getText().equals(myMark)) ||
	       (buttons[6].getText().equals(myMark) && buttons[7].getText().equals(myMark) && buttons[8].getText().equals(myMark)) ||
	       (buttons[0].getText().equals(myMark) && buttons[3].getText().equals(myMark) && buttons[6].getText().equals(myMark)) ||
	       (buttons[1].getText().equals(myMark) && buttons[4].getText().equals(myMark) && buttons[7].getText().equals(myMark)) ||
	       (buttons[2].getText().equals(myMark) && buttons[5].getText().equals(myMark) && buttons[8].getText().equals(myMark)) ||
	       (buttons[0].getText().equals(myMark) && buttons[4].getText().equals(myMark) && buttons[8].getText().equals(myMark)) ||
	       (buttons[2].getText().equals(myMark) && buttons[4].getText().equals(myMark) && buttons[6].getText().equals(myMark))) {
			return 1;
		}
		else if((buttons[0].getText().equals(opponentMark) && buttons[1].getText().equals(opponentMark) && buttons[2].getText().equals(opponentMark)) ||
		       (buttons[3].getText().equals(opponentMark) && buttons[4].getText().equals(opponentMark) && buttons[5].getText().equals(opponentMark)) ||
		       (buttons[6].getText().equals(opponentMark) && buttons[7].getText().equals(opponentMark) && buttons[8].getText().equals(opponentMark)) ||
		       (buttons[0].getText().equals(opponentMark) && buttons[3].getText().equals(opponentMark) && buttons[6].getText().equals(opponentMark)) ||
		       (buttons[1].getText().equals(opponentMark) && buttons[4].getText().equals(opponentMark) && buttons[7].getText().equals(opponentMark)) ||
		       (buttons[2].getText().equals(opponentMark) && buttons[5].getText().equals(opponentMark) && buttons[8].getText().equals(opponentMark)) ||
		       (buttons[0].getText().equals(opponentMark) && buttons[4].getText().equals(opponentMark) && buttons[8].getText().equals(opponentMark)) ||
		       (buttons[2].getText().equals(opponentMark) && buttons[4].getText().equals(opponentMark) && buttons[6].getText().equals(opponentMark))) {
    	   return -1;
		}
		else {
			// check if board is full
			boolean allFull = true;
			for (Button b : buttons) {
				if (b.getText().equals("")) {
					allFull = false;
					break;
				}
			}
			if(allFull) {
				return 3;
			}
		}
		
		return 0;
	}
	
	public boolean isValidMove(Button b) {
		return b.getText().equals("");
	}
	
	public void makeMove(int i, String mark) {
		buttons[i].setText(mark);
	}
	
	public void enableButtons() {
		for(Button b : buttons) {
			b.setEnabled(true);
		}
	}
	
	public void disableButtons() {
		for(Button b : buttons) {
			b.setEnabled(false);
		}
	}
}
