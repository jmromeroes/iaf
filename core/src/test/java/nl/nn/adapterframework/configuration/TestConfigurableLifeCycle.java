package nl.nn.adapterframework.configuration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestConfigurableLifeCycle {

	@Test
	public void start() {
		AdapterManager manager = new AdapterManager();
		manager.configure();
		manager.start();
		assertTrue(manager.isRunning());

		manager.close();
	}

	@Test
	public void cantStartWithoutConfigure() {
		AdapterManager manager = new AdapterManager();
		manager.start();
		assertFalse(manager.isRunning());

		manager.close();
	}

	@Test
	public void canStop() {
		AdapterManager manager = new AdapterManager();
		manager.configure();
		manager.start();
		manager.stop();
		assertFalse(manager.isRunning());

		manager.close();
	}

	@Test
	public void canStartAfterStopped() {
		AdapterManager manager = new AdapterManager();
		manager.configure();
		manager.start();
		manager.stop();

		manager.configure();
		manager.start();
		assertTrue(manager.isRunning());

		manager.close();
	}

	@Test
	public void cantStartAfterStoppedButNotReconfigured() {
		AdapterManager manager = new AdapterManager();
		manager.configure();
		manager.start();
		manager.stop();

		manager.start();
		assertFalse(manager.isRunning());

		manager.close();
	}
}
