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

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * A simple connection class, wrapping the communication from and to one socket.
 *
 * @author Stefan Glaser
 */
public class Connection
{
	/** Network socket */
	private Socket socket;

	/** Outgoing data stream */
	private DataOutputStream out;

	/** Incoming data stream */
	private InputStream in;

	/** Indicator if a connection is present */
	private boolean connected;

	/** Indicator if we establish the connection when first message is sent */
	private boolean lazyConnect;

	/** the host to connect to when lazy connecting */
	private String host;

	/** the port to connect to when lazy connecting */
	private int port;

	/** true if message starts should be printed */
	private boolean messageDisplay;

	/**
	 * Constructor to create a connection that does lazy connect on first send.
	 * @param host host address to connect to
	 * @param port port to connect to
	 */
	public Connection(String host, int port)
	{
		this.host = host;
		this.port = port;
		this.socket = null;
		lazyConnect = true;
		messageDisplay = false;
	}

	/**
	 * Constructor to create a connection on an existing socket
	 * @param socket the socket to use for the connection
	 * @throws IOException
	 */
	public Connection(Socket socket) throws IOException
	{
		this.socket = socket;
		connect();
	}

	private void connect() throws IOException
	{
		socket.setTcpNoDelay(true);

		in = new BufferedInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());

		connected = true;
	}

	/**
	 * @return true if this connection is connected
	 */
	public boolean isConnected()
	{
		return connected;
	}

	/**
	 * Disconnect from connection.
	 */
	public void disconnect()
	{
		if (connected) {
			connected = false;

			// System.out.println("Closing connection: " + socket);

			try {
				in.close();
				out.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Send a message using the given stream
	 *
	 * @param msg Message in ASCII form
	 */
	public void sendMessage(byte[] msg)
	{
		try {
			if (!connected && lazyConnect) {
				// we do a lazy connect to work around a problem that teams
				// not sending scene immediately cause the server to get stuck
				this.socket = new Socket(host, port);
				connect();
			}

			if (!connected) {
				return;
			}

			// do not send empty messages
			if (msg.length == 0) {
				if (messageDisplay) {
					System.out.println("<empty message>");
				}
				return;
			}

			if (messageDisplay) {
				System.out.println(new String(msg).substring(0, Math.min(40, msg.length)));
			}

			int len = msg.length;
			byte[] byteMsg = new byte[len + 4];

			// creation of the messages header (4 bytes)
			byteMsg[0] = (byte) ((len >> 24) & 0xFF);
			byteMsg[1] = (byte) ((len >> 16) & 0xFF);
			byteMsg[2] = (byte) ((len >> 8) & 0xFF);
			byteMsg[3] = (byte) (len & 0xFF);
			System.arraycopy(msg, 0, byteMsg, 4, len);

			out.write(byteMsg);
			out.flush();
		} catch (IOException e) {
			disconnect();
		}
	}

	/**
	 * Receive a message from the given stream. Blocking call.
	 *
	 * @return the next, complete received message, or null if the connection was
	 *         closed
	 */
	public byte[] receiveMessage()
	{
		if (!connected) {
			return null;
		}

		byte[] result;
		int length;

		try {
			int byte0 = in.read();
			int byte1 = in.read();
			int byte2 = in.read();
			int byte3 = in.read();
			length = byte0 << 24 | byte1 << 16 | byte2 << 8 | byte3;
			int total = 0;

			if (length < 0) {
				// server was shutdown
				System.out.println("Invalid message length: " + length);
				disconnect();
				return null;
			}

			result = new byte[length];
			while (total < length) {
				total += in.read(result, total, length - total);
			}

		} catch (IOException e) {
			System.out.println("Exception when receiving message on socket: " + socket.toString() + " Message: " + e);
			disconnect();
			return null;
		}
		return result;
	}

	public boolean inputAvailable()
	{
		try {
			return in.available() > 0;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public String toString()
	{
		return socket.toString();
	}

	public void toggleMessageDisplay()
	{
		messageDisplay = !messageDisplay;
	}
}
