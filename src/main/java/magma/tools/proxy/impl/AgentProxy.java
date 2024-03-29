/*******************************************************************************
 * Copyright 2008, 2015 Hochschule Offenburg
 * Klaus Dorer, Stefan Glaser
 *
 * This file is part of magma Simspark Agent Proxy.
 *
 * Simspark Agent Proxy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Simspark Agent Proxy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with it. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package magma.tools.proxy.impl;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * This Class represents a proxy implementation for one client agent. One agent
 * proxy consists of two threads, one for forwarding agent actions and one for
 * forwarding server messages and maintaining sync time.
 * <p>
 * New since 2015:
 * introduced the checking of say messages. This one version doesn't let chars
 * out of the wiki specified range pass through the server. Quotation marks are
 * being censored too.
 *
 * @author Stefan Glaser, Klaus Dorer
 */
public class AgentProxy
{
	/** The sync-message string */
	public static final byte[] SYNC_BYTES = "(syn)".getBytes();

	/**
	 * The maximum time between receiving a perception and responding a
	 * sync-message
	 */
	public static final int MAX_WAIT_TIME = 20;

	/** The connection to the Simspark server */
	private Connection serverConnection;

	/** The connection to the client agent */
	private Connection clientConnection;

	/** Simspark server perception forwarding and server-sync managing thread */
	private ServerPerceptionsForwarder serverForwarder;

	/** Client agent action forwarding thread */
	private ClientActionsForwarder clientForwarder;

	/** statistics of messages sent to server */
	private final MessageInfo sentMessages;

	/** statistics of messages received from server */
	private final MessageInfo receivedMessages;

	/** number of sent messages when receiving a message */
	private int sentMessagesWhenReceiving;

	/** the number of cycles missed to send a message to the server */
	private int missedCycles;

	/** true if we have received a syn message from this agent in this cycle */
	private boolean haveSynMessage;

	/** if true prints out sent and received messages */
	protected boolean showMessages;

	/** counts how many invalid say messages have been sent by client */
	private int invalidSayMessageCount;

	public AgentProxy(Socket clientSocket, String ssHost, int ssPort, boolean showMessages)
	{
		this.showMessages = showMessages;
		sentMessages = new MessageInfo(false);
		receivedMessages = new MessageInfo(true);
		missedCycles = 0;
		haveSynMessage = false;
		invalidSayMessageCount = 0;
	}

	public void start(Socket clientSocket, String ssHost, int ssPort, boolean showMessages)
	{
		try {
			System.out.print("Starting agent proxy for " + clientSocket + "... ");
			clientConnection = new Connection(clientSocket);
			serverConnection = new Connection(ssHost, ssPort);
			if (showMessages) {
				clientConnection.toggleMessageDisplay();
				serverConnection.toggleMessageDisplay();
			}

			clientForwarder = new ClientActionsForwarder();

			clientForwarder.start();

			System.out.println("done.");
		} catch (IOException e) {
			System.out.println("FAILED! (" + e.getMessage() + ")");
			stopProxy();
		}
	}

	/**
	 * Stop proxy. Close connections to Simspark server and client agent.
	 */
	public void stopProxy()
	{
		boolean success = false;

		if (clientConnection != null && clientConnection.isConnected()) {
			clientConnection.disconnect();
			success = true;
		}

		if (serverConnection != null && serverConnection.isConnected()) {
			serverConnection.disconnect();
			success = true;
		}

		if (success) {
			System.out.println(this);
			System.out.println("Closed   agent proxy for " + clientConnection);
		}
	}

	/**
	 * Retrieve proxy status.
	 *
	 * @return true, if both proxy threads are active, false otherwise
	 */
	public boolean isActive()
	{
		return serverForwarder != null && clientForwarder != null && serverForwarder.isAlive() &&
				clientForwarder.isAlive();
	}

	/**
	 * Send a message to the Simspark server.
	 *
	 * @param msg - the message to send
	 */
	private synchronized void sendServerMsg(byte[] msg)
	{
		serverConnection.sendMessage(msg);
		// check if a cycle was missed by the agent
		if (sentMessages.count == sentMessagesWhenReceiving && msg == SYNC_BYTES) {
			missedCycles++;
		}
	}

	/**
	 * Send a message to the client agent.
	 *
	 * @param msg - the message to send
	 */
	private void sendClientMsg(byte[] msg)
	{
		clientConnection.sendMessage(msg);
	}

	/**
	 * Receive a message from the Simspark sevrer. Blocking call.
	 *
	 * @return the next, complete message received from the Simspark server
	 */
	private byte[] receiveServerMsg()
	{
		return serverConnection.receiveMessage();
	}

	/**
	 * Receive a message from the client agent. Blocking call.
	 *
	 * @return the next, complete message received from the client agent
	 */
	private byte[] receiveClientMsg()
	{
		return clientConnection.receiveMessage();
	}

	@Override
	public String toString()
	{
		String connectedString = isActive() ? "active" : "inactive";
		return "Agent (" + connectedString + "):"
				+ " missed: " + missedCycles + " invalid say: " + invalidSayMessageCount +
				" connection: " + clientConnection;
	}

	public String toStringVerbose()
	{
		return this + "\nsent: " + sentMessages + "\nreceived: " + receivedMessages;
	}

	/**
	 * Server message forwarding thread. This thread listens to the server
	 * connection for incoming perception messages. Once a perception message is
	 * perceived, it forwards the message to the client agent and waits for
	 * {@link AgentProxy}.WAIT_TIME ms before it sends a sync-message to the
	 * server.
	 */
	class ServerPerceptionsForwarder extends Thread
	{
		@Override
		public void run()
		{
			// some clients where hanging, when sending the scene
			// string and not getting a server message
			sendServerMsg(SYNC_BYTES);

			while (true) {
				byte[] perception = receiveServerMsg();
				if (perception == null) {
					// shutdown when receiving null-message
					break;
				}

				perception = onNewServerMessage(perception);
				if (perception != null) {
					receivedMessages.newMessage(perception.length, receivedMessages.lastMessageTime);

					// forward perception message to client agent
					sentMessagesWhenReceiving = sentMessages.count;
					haveSynMessage = false;
					sendClientMsg(perception);
				}

				// If there is already another message in the input channel, skip
				// waiting time and sending of sync-message, until we run
				// synchronous again.
				if (!serverConnection.inputAvailable()) {
					// wait for 20ms
					try {
						Thread.sleep(MAX_WAIT_TIME / 2);
						if (!haveSynMessage) {
							Thread.sleep(MAX_WAIT_TIME / 2);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					// send sync message to Simspark server
					if (!haveSynMessage) {
						sendServerMsg(SYNC_BYTES);
					}
				}
			}

			stopProxy();
		}
	}

	/**
	 * Client action forwarding thread. This thread simply forwards all incoming
	 * messages from the client agent to the Simspark server. If an action
	 * message already contains a sync-command, it is removed from the message.
	 */
	class ClientActionsForwarder extends Thread
	{
		@Override
		public void run()
		{
			while (true) {
				// receive next client action message
				byte[] action = receiveClientMsg();
				if (action == null) {
					// shutdown when receiving null-message
					break;
				}

				if (findBytes(action, SYNC_BYTES)) {
					haveSynMessage = true;
					// prefix (syn) to avoid server from hanging in case of bad
					// say messages
					action = prependSyn(action);
				}

				if (action.length > 0) {
					// forward action message to Simspark server
					action = onNewClientMessage(action);
					if (action != null) {
						action = checkSay(action);
						sendServerMsg(action);
						sentMessages.newMessage(action.length, receivedMessages.lastMessageTime);

						if (serverForwarder == null) {
							// with lazy connect we have to wait to listen for
							// server messages until here
							serverForwarder = new ServerPerceptionsForwarder();
							serverForwarder.start();
						}
					}
				}
			}

			stopProxy();
		}

		/**
		 * Prefixes the action string with a (syn) to avoid problems of hanging
		 * server in case of bad formed say messages.
		 * @param action the action string
		 * @return the action string prefixed with (syn)
		 */
		protected byte[] prependSyn(byte[] action)
		{
			byte[] result = new byte[SYNC_BYTES.length + action.length];
			System.arraycopy(SYNC_BYTES, 0, result, 0, SYNC_BYTES.length);
			System.arraycopy(action, 0, result, SYNC_BYTES.length, action.length);
			return result;
		}

		protected byte[] checkSay(byte[] action)
		{
			boolean wrongMsgComposition = false;
			String msg = new String(action, StandardCharsets.UTF_8);

			int initSay = msg.indexOf("(say");
			int endSay;

			if (initSay != -1) {
				char nextCharAfterSay;
				endSay = msg.indexOf(")", initSay);
				nextCharAfterSay = '(';
				if (endSay < msg.length() - 1) {
					nextCharAfterSay = msg.charAt(endSay + 1);
				}

				if (nextCharAfterSay != '(' && nextCharAfterSay != '\0') {
					endSay = msg.indexOf(")", endSay + 1);
					wrongMsgComposition = true;
				} else {
					for (int i = initSay + 5; i < endSay; i++) {
						int ascii = msg.charAt(i);
						// check for invalid character range
						// the \" is not checked since it is not explicitly
						// forbidden in the manual
						if (ascii > 126 || ascii < 32 || ascii == 40 || ascii == 41 ||
								ascii == 32 /* || ascii == 34 */) {
							wrongMsgComposition = true;
							break;
						}
					}
				}
				if (wrongMsgComposition) {
					// System.out.println("Invalid say: " + msg);
					// switched off changing message
					// StringBuffer s = new StringBuffer(msg);
					// s.delete(initSay, endSay + 1);
					// s.insert(0, "(syn)");
					// msg = s.toString();
					invalidSayMessageCount++;
				}
			}
			// switched off to change the message, but we want to count invalid
			// messages
			// return msg.getBytes();
			return action;
		}

		boolean findBytes(byte[] arrayToSearch, byte[] bytesToFind)
		{
			for (int i = 0; i <= arrayToSearch.length - bytesToFind.length; i++) {
				int j = 0;
				for (; j < bytesToFind.length; j++) {
					if (arrayToSearch[i + j] != bytesToFind[j]) {
						break;
					}
				}
				if (j == bytesToFind.length) {
					return true;
				}
			}
			return false;
		}
	}

	private static class MessageInfo
	{
		private int count;

		private float avgMessageSize;

		private int maxMessageSize;

		private long lastMessageTime;

		private float avgMessageDelta;

		private long maxMessageDelta;

		private final boolean maxTimeInfoOk;

		public MessageInfo(boolean maxTimeInfoOk)
		{
			this.maxTimeInfoOk = maxTimeInfoOk;
			count = 0;
			avgMessageSize = 0;
			maxMessageSize = 0;
			avgMessageDelta = 0;
			maxMessageDelta = 0;
		}

		public void newMessage(int size, long referenceTime)
		{
			avgMessageSize = (avgMessageSize * count + size) / (count + 1);
			if (size > maxMessageSize) {
				maxMessageSize = size;
			}

			lastMessageTime = System.nanoTime();
			if (count > 0) {
				long delta = lastMessageTime - referenceTime;
				avgMessageDelta = (avgMessageDelta * count + delta) / (count + 1);
				if (referenceTime > 0 && delta > maxMessageDelta) {
					maxMessageDelta = delta;
				}
			}

			count++;
		}

		@Override
		public String toString()
		{
			String result = String.format(" count: %d avgSize: %4.2f maxSize: %d avgTimeDelta: %4.2f(ms)", count,
					avgMessageSize, maxMessageSize, avgMessageDelta / 1000000);
			if (maxTimeInfoOk) {
				result += String.format(" maxTimeDelta: %d(ms)", maxMessageDelta / 1000000);
			}
			return result;
		}
	}

	/**
	 * Switches on/off messages sent to the client
	 */
	public void toggleClientMessageDisplay()
	{
		clientConnection.toggleMessageDisplay();
	}

	/**
	 * Switches on/off messages sent to the server
	 */
	public void toggleServerMessageDisplay()
	{
		serverConnection.toggleMessageDisplay();
	}

	/**
	 * Called before a message from the server was forwarded to the client
	 * @param message the message received from the server
	 * @return the message that should be forwarded to the client, null if no
	 *         forward
	 */
	protected byte[] onNewServerMessage(byte[] message)
	{
		return message;
	}

	/**
	 * Called before a client message has been forwarded to the server
	 * @param message the message received from the client
	 * @return the message that should be forwarded to the server, null if no
	 *         forward
	 */
	public byte[] onNewClientMessage(byte[] message)
	{
		return message;
	}

	/**
	 * Accessor for invalid say message count
	 * @return the number of invalid say messages detected
	 */
	public int getInvalidSayMessageCount()
	{
		return invalidSayMessageCount;
	}
}
