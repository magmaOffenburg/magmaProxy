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
package magma.tools.SAProxy.impl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * This class represents a Simspark agent proxy server. The proxy server itself
 * is a {@link Thread} implementation. So to start the proxy, simply call its
 * start method. The proxy thread then starts listening to the proxy port. Each
 * incoming connection is forwarded to an own {@link AgentProxy} instance, which
 * handles the synchronization of one specific agent with the server. The proxy
 * server and all its connections can be shut down using the shutdown method.
 *
 * @author Stefan Glaser
 */
public class SimsparkAgentProxyServer extends Thread
{
	/** The proxy server socket */
	private ServerSocket proxySocket;

	/** The proxy server port */
	private int proxyPort;

	/** Simspark server IP */
	protected String ssHost;

	/** Simspark server port */
	protected int ssPort;

	/** List of agent proxies */
	protected ArrayList<AgentProxy> agentProxies;

	/** true if messages should be printed from start */
	protected boolean showMessages;

	public SimsparkAgentProxyServer(SimsparkAgentProxyServerParameter parameterObject)
	{
		this.proxyPort = parameterObject.getProxyPort();
		this.ssHost = parameterObject.getSsHost();
		this.ssPort = parameterObject.getSsPort();
		this.showMessages = parameterObject.isShowMessages();

		agentProxies = new ArrayList<AgentProxy>();
	}

	@Override
	public void run()
	{
		try {
			proxySocket = new ServerSocket(proxyPort);

			System.out.println("Proxy server listening on port: " + proxyPort);

			while (true) {
				// wait for new clients (agents)
				Socket clientSocket = proxySocket.accept();

				// remove obsolete agent proxies
				for (int i = agentProxies.size() - 1; i >= 0; i--) {
					if (!agentProxies.get(i).isActive()) {
						agentProxies.remove(i);
					}
				}

				// create new agent proxy
				AgentProxy agentProxy = createAgentProxy(clientSocket);
				if (agentProxy != null) {
					agentProxies.add(agentProxy);
				}
			}
		} catch (IOException e) {
			System.out.println("Proxy server socket closed!");
		}

		proxySocket = null;

		// shutdown and remove all agent proxies
		for (AgentProxy proxy : agentProxies) {
			proxy.stopProxy();
		}
		agentProxies.clear();
	}

	/**
	 * Factory method to create agent proxy
	 * @param clientSocket the socket the agent proxy works on
	 * @return a new instance of agent proxy
	 */
	protected AgentProxy createAgentProxy(Socket clientSocket)
	{
		AgentProxy agentProxy = new AgentProxy(clientSocket, ssHost, ssPort, showMessages);
		agentProxy.start(clientSocket, ssHost, ssPort, showMessages);
		return agentProxy;
	}

	/**
	 * Shutdown proxy server and all active agent-proxy instances.
	 */
	public void shutdown()
	{
		if (proxySocket != null && !proxySocket.isClosed()) {
			try {
				proxySocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Retrieve the current list of agent proxies.
	 *
	 * @return current list of agent proxies
	 */
	public ArrayList<AgentProxy> getAgentProxies()
	{
		return agentProxies;
	}

	public static class SimsparkAgentProxyServerParameter
	{
		private int proxyPort;

		private String ssHost;

		private int ssPort;

		private boolean showMessages;

		/**
		 *
		 */
		public SimsparkAgentProxyServerParameter(int proxyPort, String ssHost, int ssPort, boolean showMessages)
		{
			this.proxyPort = proxyPort;
			this.ssHost = ssHost;
			this.ssPort = ssPort;
			this.showMessages = showMessages;
		}

		/**
		 * @return the proxyPort
		 */
		public int getProxyPort()
		{
			return proxyPort;
		}

		/**
		 * @return the ssHost
		 */
		public String getSsHost()
		{
			return ssHost;
		}

		/**
		 * @return the ssPort
		 */
		public int getSsPort()
		{
			return ssPort;
		}

		/**
		 * @return the showMessages
		 */
		public boolean isShowMessages()
		{
			return showMessages;
		}
	}
}
