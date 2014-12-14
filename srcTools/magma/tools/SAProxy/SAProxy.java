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
package magma.tools.SAProxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import magma.tools.SAProxy.impl.AgentProxy;
import magma.tools.SAProxy.impl.SimsparkAgentProxyServer;
import magma.tools.SAProxy.impl.SimsparkAgentProxyServer.SimsparkAgentProxyServerParameter;

/**
 * @author Stefan Glaser
 */
public class SAProxy
{
	private SimsparkAgentProxyServer proxy;

	/**
	 * Instantiates and starts the Simspark agent proxy.
	 * 
	 * @param args Command line arguments <br>
	 *        <table>
	 *        <tr>
	 *        <td>--proxyport=</td>
	 *        <td>Proxy server port</td>
	 *        </tr>
	 *        <tr>
	 *        <td>--server=</td>
	 *        <td>Simspark server IP</td>
	 *        </tr>
	 *        <tr>
	 *        <td>--serverport=</td>
	 *        <td>Simspark server Port</td>
	 *        </tr>
	 *        <tr>
	 *        <td>--verbose</td>
	 *        <td>Shows the messages</td>
	 *        </tr>
	 *        </table>
	 */
	public static void main(String[] args)
	{
		SimsparkAgentProxyServerParameter parameterObject = parseParameters(args);
		SimsparkAgentProxyServer proxy = new SimsparkAgentProxyServer(
				parameterObject);
		SAProxy saproxy = new SAProxy(proxy);
		saproxy.mainLoop();
	}

	/**
	 * @param args
	 * @return
	 */
	public static SimsparkAgentProxyServerParameter parseParameters(String[] args)
	{
		int proxyPort = 3110;
		String ssHost = "127.0.0.1";
		int ssPort = 3100;
		boolean showMessages = false;

		for (String arg : args) {
			if (arg.startsWith("--proxyport=")) {
				proxyPort = Integer.valueOf(arg.replaceFirst("--proxyport=", ""));
			} else if (arg.startsWith("--server=")) {
				ssHost = arg.replaceFirst("--server=", "");
			} else if (arg.startsWith("--serverport=")) {
				ssPort = Integer.valueOf(arg.replaceFirst("--serverport=", ""));
			} else if (arg.startsWith("--verbose")) {
				showMessages = true;
				;
			} else {
				System.out.println("Unknown Parameter: " + arg);
				System.out.println("Usage example: --proxyport=3110"
						+ " --server=127.0.0.1 --serverport=3100");
				System.out.println("Use --verbose to display all messages");
			}
		}

		SimsparkAgentProxyServerParameter parameterObject = new SimsparkAgentProxyServerParameter(
				proxyPort, ssHost, ssPort, showMessages);
		return parameterObject;
	}

	/**
	 * @param proxy
	 */
	public SAProxy(SimsparkAgentProxyServer proxy)
	{
		this.proxy = proxy;
	}

	public void mainLoop()
	{

		System.out.println("Starting proxy version 2.0 ...");
		proxy.start();

		// open up standard input
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String input;
		boolean shutdown = false;

		while (!shutdown && proxy.isAlive()) {
			// Read next input command
			try {
				input = br.readLine();
			} catch (IOException ioe) {
				System.out.println("IO error. Shutdown...");
				break;
			}

			if ("q".equals(input) || "quit".equals(input)) {
				// shutdown

				shutdown = true;
			} else if ("l".equals(input) || "list".equals(input)) {
				// list active agent proxies

				ArrayList<AgentProxy> agentProxies = proxy.getAgentProxies();
				AgentProxy agentProxy;

				System.out.println("Active agents:");
				for (int i = 0, index = 1; i < agentProxies.size(); i++) {
					agentProxy = agentProxies.get(i);
					if (agentProxy.isActive()) {
						System.out.println(index + ". " + agentProxy);
						index++;
					}
				}
			} else if ("v".equals(input) || "verbose".equals(input)) {
				// verbosly list active agent proxies

				ArrayList<AgentProxy> agentProxies = proxy.getAgentProxies();
				AgentProxy agentProxy;

				System.out.println("Active agents:");
				for (int i = 0, index = 1; i < agentProxies.size(); i++) {
					agentProxy = agentProxies.get(i);
					if (agentProxy.isActive()) {
						System.out.println(index + ". "
								+ agentProxy.toStringVerbose());
						index++;
					}
				}
			} else if ("s".equals(input) || "status".equals(input)) {
				// print proxy status

				ArrayList<AgentProxy> agentProxies = proxy.getAgentProxies();

				System.out.println("Proxy thread alive: " + proxy.isAlive());
				System.out.println("All agents (" + agentProxies.size() + "):");
				for (int i = 0; i < agentProxies.size(); i++) {
					System.out.println((i + 1) + ". " + agentProxies.get(i));
				}

			} else if ("m".equals(input)) {
				// switch messages on/off
				ArrayList<AgentProxy> agentProxies = proxy.getAgentProxies();
				System.out.println("Proxy thread alive: " + proxy.isAlive());
				for (AgentProxy agentProxy : agentProxies) {
					agentProxy.switchClientMessageDisplay();
				}
			} else if ("n".equals(input)) {
				// switch messages on/off
				ArrayList<AgentProxy> agentProxies = proxy.getAgentProxies();
				System.out.println("Proxy thread alive: " + proxy.isAlive());
				for (AgentProxy agentProxy : agentProxies) {
					agentProxy.switchServerMessageDisplay();
				}
			} else {
				System.out.println("Command \"" + input + "\" unknown!");
				System.out.println("Known commands:");
				System.out.println("q; quit\t\t--> exit proxy server application");
				System.out
						.println("l; list\t\t--> list active agent proxy instances");
				System.out
						.println("v; verbose\t--> list active agent proxy instances verbosely");
				System.out.println("s; status\t--> print proxy status");
				System.out.println("m; \t--> print start of all client messages");
				System.out.println("n; \t--> print start of all server messages");
			}
		}

		proxy.shutdown();
	}
}
