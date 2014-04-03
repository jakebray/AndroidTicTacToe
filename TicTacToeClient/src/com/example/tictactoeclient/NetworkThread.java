package com.example.tictactoeclient;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Random;

import android.util.Log;


public class NetworkThread extends Thread {
	
	private static final int MAX_PACKET_SIZE = 512;
	private static final InetSocketAddress serverSocketAddress = new InetSocketAddress(MainActivity.SERVER_ADDRESS, MainActivity.DEFAULT_PORT);
	
	private DatagramSocket socket;
	private int myID = 0;
	private String myGroup;
	private WeakReference<MainActivity> wr;
	
	protected int myMove;
	
	public NetworkThread(MainActivity ma) {
		wr = new WeakReference<MainActivity>(ma);
	}

	@Override
	public void run() {
		try {
			MainActivity ma = wr.get();
			// get weak reference to the MainActivity
			setup();
			waitAFew();
			
			determineXorO();
			
			while (true) {
				moveX();
				if (ma.isGameOver() != 0) {
					break;
				}
				moveO();
				if (ma.isGameOver() != 0) {
					break;
				}
			}
			
			if (ma.isGameOver() == 1) {
				sendTVMessage("** YOU WON! **");
				sendEndMessage("** YOU WON! **");
			} else if (ma.isGameOver() == -1){
				sendTVMessage("-- YOU LOST :( --");
				sendEndMessage("-- YOU LOST :( --");
			} else {
				sendTVMessage(">> TIE GAME <<");
				sendEndMessage(">> TIE GAME <<");
			}
			
			// send QUIT request to the server, to free up some memory
			sendAndReceive("QUIT " + myID + " " + myGroup);
			
			
		} catch (SocketException e) {
			Log.i("NetworkThread", e.toString());
		} catch (IOException e) {
			Log.i("NetworkThread", e.toString());
		}
	}

	private void moveX() throws IOException {
		if(MainActivity.myMark.equals("X")) {
			myMove();
		} else {
			opponentMove();
		}
	}

	private void moveO() throws IOException {
		if(MainActivity.myMark.equals("O")) {
			myMove();
		} else {
			opponentMove();
		}
	}
	
	private void myMove() throws IOException {
		// send message to enable buttons
		sendToggleMessage("E");
		// wait until user has clicked a button (made a move)
		try {
			synchronized(this) {
				wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// send message to disable buttons
		sendToggleMessage("D");
		
		sendAndReceive("MSG " + myID + " " + myGroup + " " + myMove);
	}

	private void opponentMove() throws IOException {
		String opponentMSG;
		do {
			waitAFew();
			opponentMSG = pollAndReceive("POLL " + myID);
		} while (opponentMSG.equals("EMPTY"));
		sendBMessage(opponentMSG);
		waitAFew();
	}

	// helper methods
	private void setup() throws IOException {
		socket = new DatagramSocket();
		sendAndReceive("NAME " + getId());
		myGroup = findAndReceive("FIND " + myID);
	}
	
	private void determineXorO() throws IOException {
		Random r = new Random();
		int myRandInt = r.nextInt();
		sendAndReceive("MSG " + myID + " " + myGroup + " " + myRandInt);
		
		sendTVMessage("Determining Order of Play...");
		
		// wait a few seconds for opponents to send their rand nums
		waitAFew();
		
		String opponentMSG = pollAndReceive("POLL " + myID);
		
		if (myRandInt > Integer.parseInt(opponentMSG)) {
			// this client goes first, so has X as mark
			MainActivity.myMark = "X";
			MainActivity.opponentMark = "O";
			sendTVMessage("You are X's (You go 1st)");
		} else {
			MainActivity.myMark = "O";
			MainActivity.opponentMark = "X";
			sendTVMessage("You are O's (You go 2nd)");
		}
	}

	void sendAndReceive (String message) throws IOException {
		byte[] buf = new byte[MAX_PACKET_SIZE];
		DatagramPacket in = new DatagramPacket(buf, buf.length);
		
		DatagramPacket out = new DatagramPacket(message.getBytes(), message.length(),
				serverSocketAddress);
		
		socket.send(out);
		socket.receive(in);
		
		if(myID == 0) {
			String str = new String(in.getData(), 0, in.getLength())
			.trim();
			String[] strings = str.split(" ");
			myID = Integer.parseInt(strings[strings.length - 1]);
		}
		
		
		message = "ACK " + myID;
		out = new DatagramPacket(message.getBytes(), message.length(),
				serverSocketAddress);
		socket.send(out);
	}

	String findAndReceive(String message) throws IOException {
		String groupName;
		
		byte[] buf = new byte[MAX_PACKET_SIZE];
		DatagramPacket in = new DatagramPacket(buf, buf.length);
		
		DatagramPacket out = new DatagramPacket(message.getBytes(), message.length(),
				serverSocketAddress);
		
		socket.send(out);
		socket.receive(in);
		
		String str = new String(in.getData(), 0, in.getLength())
		.trim();
		
		if (str.startsWith("WAITING")) {
			sendTVMessage("Waiting...");
			message = "ACK " + myID;
			out = new DatagramPacket(message.getBytes(), message.length(),
					serverSocketAddress);
			socket.send(out);
			// wait for FOUND response
			socket.receive(in);
		}
		// after FOUND is received, ACK and get the groupName
		message = "ACK " + myID;
		out = new DatagramPacket(message.getBytes(), message.length(),
				serverSocketAddress);
		socket.send(out);
		
		sendTVMessage("FOUND!");
		
		str = new String(in.getData(), 0, in.getLength())
		.trim();
		// place the token after "FOUND" into groupName
		groupName = str.split(" ")[1];
		
		return groupName;
	}
	
	String pollAndReceive (String message) throws IOException {
		byte[] buf = new byte[MAX_PACKET_SIZE];
		DatagramPacket in = new DatagramPacket(buf, buf.length);
		
		DatagramPacket out = new DatagramPacket(message.getBytes(), message.length(),
				serverSocketAddress);
		
		socket.send(out);
		socket.receive(in);
		
		String str = parsePOLL(new String(in.getData(), 0, in.getLength())
		.trim());
		
		message = "ACK " + myID;
		out = new DatagramPacket(message.getBytes(), message.length(),
				serverSocketAddress);
		socket.send(out);
		
		if (str.equals("EMPTY")) {
			return str;
		}
		
		// receive EMPTY notification and ACK it
		socket.receive(in);
		out = new DatagramPacket(message.getBytes(), message.length(),
				serverSocketAddress);
		socket.send(out);
		
		
		return str;
	}
	
	// helpers
	String parsePOLL(String message) {
		String[] tokens = message.split(" ");
		
		if (tokens.length == 1) {
			return "EMPTY";
		}
		
		StringBuilder builder = new StringBuilder();
		// the rest of the tokens (after "MSG", "ID", and "GroupName") are the message
		builder.append(tokens[4]);
		for (int i = 5; i < tokens.length; i++) {
			builder.append(" ");
			builder.append(tokens[i]);
		}
		return builder.toString();
	}
	
	void sendTVMessage (String message) {
		MainActivity.handler.obtainMessage(MainActivity.MSG_TV, message).sendToTarget();
	}
	
	void sendBMessage (String message) {
		MainActivity.handler.obtainMessage(MainActivity.MSG_B, message).sendToTarget();
	}
	
	void sendToggleMessage(String message) {
		MainActivity.handler.obtainMessage(MainActivity.MSG_TOGGLE, message).sendToTarget();
	}
	
	void sendEndMessage(String message) {
		MainActivity.handler.obtainMessage(MainActivity.MSG_END, message).sendToTarget();
	}
	
	// waits 3 seconds
	void waitAFew() {
		try {
			sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
