/*******************************************************************************
 * Copyright 2008, 2013 Hochschule Offenburg
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
	private String ssHost;

	/** Simspark server port */
	private int ssPort;

	/** List of agent proxies */
	private ArrayList<AgentProxy> agentProxies;

	/** true if messages should be printed from start */
	private boolean showMessages;

	public SimsparkAgentProxyServer(int proxyPort, String ssHost, int ssPort,
			boolean showMessages)
	{
		this.proxyPort = proxyPort;
		this.ssHost = ssHost;
		this.ssPort = ssPort;
		this.showMessages = showMessages;

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
				AgentProxy agentProxy = new AgentProxy(clientSocket, ssHost,
						ssPort, showMessages);
				agentProxies.add(agentProxy);
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
}
