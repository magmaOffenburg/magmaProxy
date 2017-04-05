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

import static org.junit.Assert.assertEquals;
import magma.tools.SAProxy.impl.AgentProxy.ClientActionsForwarder;

import org.junit.Before;
import org.junit.Test;

public class AgentProxyTest
{
	private AgentProxy testee;

	@Before
	public void setUp() throws Exception
	{
		testee = new AgentProxy(null, "localhost", 3100, false);
	}

	@Test
	public void testCheckSay()
	{
		ClientActionsForwarder forwarder = testee.new ClientActionsForwarder();
		String msg = "(say b056200199200000000h)";
		byte[] action = msg.getBytes();
		forwarder.checkSay(action);
		assertEquals(0, testee.getInvalidSayMessageCount());

		// test invalid messages
		msg = "(say b056200199200000000h)(syn)";
		action = msg.getBytes();
		forwarder.checkSay(action);
		assertEquals(0, testee.getInvalidSayMessageCount());

		msg = "(say b056200199200000000h))";
		action = msg.getBytes();
		forwarder.checkSay(action);
		assertEquals(1, testee.getInvalidSayMessageCount());

		msg = "(say b0562001992000000 00h)";
		action = msg.getBytes();
		forwarder.checkSay(action);
		assertEquals(2, testee.getInvalidSayMessageCount());

		msg = "(say b0562001992000000(00h)";
		action = msg.getBytes();
		forwarder.checkSay(action);
		assertEquals(3, testee.getInvalidSayMessageCount());
	}

	@Test
	public void testRegressionBug2()
	{
		ClientActionsForwarder forwarder = testee.new ClientActionsForwarder();
		String msg = "(lae1 0)(rae1 0)(lae2 0)(rae2 0)(lae3 0)(rae3 0)(lae4 0)"
					 + "(rae4 0)(lle1 0)(rle1 0)(lle2 0)(rle2 0)(lle3 0)(rle3 -0.347824)"
					 + "(lle4 0)(rle4 0.322004)(lle5 0)(rle5 0)(lle6 0)(rle6 0)(lle7 0)"
					 + "(rle7 0)(he1 -6)(he2 0)(say u11rc03.92947)";
		byte[] action = msg.getBytes();
		forwarder.checkSay(action);
		assertEquals(0, testee.getInvalidSayMessageCount());
	}

	@Test
	public void testRegressionBug3()
	{
		ClientActionsForwarder forwarder = testee.new ClientActionsForwarder();
		String msg = "(say \"b056200199200000000h\")";
		byte[] action = msg.getBytes();
		forwarder.checkSay(action);
		assertEquals(0, testee.getInvalidSayMessageCount());
	}

	@Test
	public void testPrependSyn()
	{
		ClientActionsForwarder forwarder = testee.new ClientActionsForwarder();
		String msg = "testMessage";
		byte[] action = msg.getBytes();
		byte[] result = forwarder.prependSyn(action);
		assertEquals("(syn)testMessage", new String(result));
	}
}
