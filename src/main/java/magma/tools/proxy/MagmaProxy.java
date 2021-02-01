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
package magma.tools.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import magma.tools.proxy.impl.AgentProxy;
import magma.tools.proxy.impl.SimsparkAgentProxyServer;
import magma.tools.proxy.impl.SimsparkAgentProxyServer.SimsparkAgentProxyServerParameter;

/**
 * Proxy for RoboCup games.
 * @author Stefan Glaser
 */
public class MagmaProxy
{
	private static final String PROXY_VERSION = "2.1.4";

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
		SimsparkAgentProxyServer proxy = new SimsparkAgentProxyServer(parameterObject);
		new MagmaProxy(proxy).run(parameterObject.isDaemon());
	}

	public static SimsparkAgentProxyServerParameter parseParameters(String[] args)
	{
		int proxyPort = 3110;
		String ssHost = "127.0.0.1";
		int ssPort = 3100;
		boolean showMessages = false;
		boolean daemon = false;

		for (String arg : args) {
			if (arg.startsWith("--proxyport=")) {
				proxyPort = Integer.parseInt(arg.replaceFirst("--proxyport=", ""));
			} else if (arg.startsWith("--server=")) {
				ssHost = arg.replaceFirst("--server=", "");
			} else if (arg.startsWith("--serverport=")) {
				ssPort = Integer.parseInt(arg.replaceFirst("--serverport=", ""));
			} else if (arg.startsWith("--verbose")) {
				showMessages = true;
			} else if (arg.startsWith("--daemon")) {
				daemon = true;
			} else {
				System.out.println("Unknown Parameter: " + arg);
				System.out.println("Usage example: --proxyport=3110 --server=127.0.0.1 --serverport=3100");
				System.out.println("Use --verbose to display all messages");
			}
		}

		return new SimsparkAgentProxyServerParameter(proxyPort, ssHost, ssPort, showMessages, daemon);
	}

	public MagmaProxy(SimsparkAgentProxyServer proxy)
	{
		this.proxy = proxy;
	}

	public void run(boolean daemon)
	{
		System.out.println("Starting magmaProxy version " + PROXY_VERSION);
		proxy.start();

		if (daemon) {
			// Don't try to read from stdin while running in the background
			// Doing so will result in receiving SIGTTIN
			// (which will stop the whole process)
			return;
		}

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

			ArrayList<AgentProxy> agentProxies = proxy.getAgentProxies();

			switch (input) {
			case "q":
			case "quit":
				// shutdown
				shutdown = true;
				break;

			case "l":
			case "list":
				// list active agent proxies
				System.out.println("Active agents:");
				for (int i = 0; i < agentProxies.size(); i++) {
					AgentProxy agentProxy = agentProxies.get(i);
					if (agentProxy.isActive()) {
						System.out.println((i + 1) + ". " + agentProxy);
					}
				}
				break;

			case "v":
			case "verbose":
				// verbosely list active agent proxies
				System.out.println("Active agents:");
				for (int i = 0; i < agentProxies.size(); i++) {
					AgentProxy agentProxy = agentProxies.get(i);
					if (agentProxy.isActive()) {
						System.out.println((i + 1) + ". " + agentProxy.toStringVerbose());
					}
				}
				break;

			case "s":
			case "status":
				// print proxy status
				System.out.println("Proxy thread alive: " + proxy.isAlive());
				System.out.println("All agents (" + agentProxies.size() + "):");
				for (int i = 0; i < agentProxies.size(); i++) {
					System.out.println((i + 1) + ". " + agentProxies.get(i));
				}
				break;

			case "m":
				// switch messages on/off
				System.out.println("Proxy thread alive: " + proxy.isAlive());
				for (AgentProxy agentProxy : agentProxies) {
					agentProxy.toggleClientMessageDisplay();
				}
				break;

			case "n":
				// switch messages on/off
				System.out.println("Proxy thread alive: " + proxy.isAlive());
				for (AgentProxy agentProxy : agentProxies) {
					agentProxy.toggleServerMessageDisplay();
				}
				break;

			default:
				System.out.println("Command \"" + input + "\" unknown!");
				System.out.println("Known commands:");
				System.out.println("q; quit\t\t--> exit proxy server application");
				System.out.println("l; list\t\t--> list active agent proxy instances");
				System.out.println("v; verbose\t--> list active agent proxy instances verbosely");
				System.out.println("s; status\t--> print proxy status");
				System.out.println("m; \t--> print start of all client messages");
				System.out.println("n; \t--> print start of all server messages");
				break;
			}
		}

		proxy.shutdown();
	}
}
