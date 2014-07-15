package com.yakindu.com4j.plugin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com4j.COM4J;
import com4j.util.ComObjectCollector;

/**
 * The activator registers a listener on the Com4j component and keeps a list of all com objects which will be disposed
 * when the bundle is stopped.
 */
public class Com4JActivator implements BundleActivator {

	final static ComObjectCollector COM_OBJECT_COLLECTOR = new ComObjectCollector();

	@Override
	public void start(BundleContext context) throws Exception {
		COM4J.addListener(COM_OBJECT_COLLECTOR);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		COM_OBJECT_COLLECTOR.disposeAll();
		COM4J.removeListener(COM_OBJECT_COLLECTOR);
	}

}
