/*
   Copyright 2013, 2018-2020 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.receivers;

import java.io.IOException;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
/**
 * Singleton class that knows about the ServiceListeners that are active.
 * <br/>
 * This class is to be used as a facade for different services that implement
 * the <code>ServiceClient</code> interface.<br/>
 * This class is exposed as a webservice, to be able to provide a single point
 * of entry to all adapters that have a ServiceListener as a IReceiver.
 *
 * @author Johan Verrips
 * @author Niels Meijer
 *
 * @see ServiceClient
 */
public class ServiceDispatcher  {
	protected Logger log = LogUtil.getLogger(this);

	private ConcurrentSkipListMap<String, ServiceClient> registeredListeners = new ConcurrentSkipListMap<String, ServiceClient>();
	private static ServiceDispatcher self = null;

	/**
	 * Use this method to get hold of the <code>ServiceDispatcher</code>
	 * @return an instance of this class
	 */
	public static synchronized ServiceDispatcher getInstance() {
		if (self == null) {
			self = new ServiceDispatcher();
		}
		return (self);
	}

	/**
	 * Dispatch a request.
	 *
	 * @since 4.3
	 */
	public String dispatchRequest(String serviceName, String request, PipeLineSession session) throws ListenerException {
		log.debug("dispatchRequest for service [{}] correlationId [{}] message [{}]", serviceName, session.getCorrelationId(), request);

		ServiceClient client = registeredListeners.get(serviceName);
		if (client == null) {
			throw new ListenerException("service ["+serviceName+"] is not registered");
		}

		String result;
		try {
			result = client.processRequest(new Message(request), session).asString();
		} catch (IOException e) {
			throw new ListenerException(e);
		}
		if (result == null) {
			log.warn("result is null!");
		}

		return result;
	}

	/**
	 * Retrieve the names of the registered listeners in alphabetical order.
	 * @return Iterator with the names.
	 */
	public Iterator<String> getRegisteredListenerNames() {
		SortedSet<String> sortedKeys = new TreeSet<String>(registeredListeners.keySet());
		return sortedKeys.iterator();
	}

	/**
	 * Check whether a serviceName is registered at the <code>ServiceDispatcher</code>.
	 * @return true if the service is registered at this dispatcher, otherwise false
	 */
	public boolean isRegisteredServiceListener(String name) {
		return (registeredListeners.get(name)!=null);
	}

	public void registerServiceClient(String name, ServiceClient listener) throws ListenerException{
		if (isRegisteredServiceListener(name)) {
			//TODO throw ListenerException if already registered!
			log.warn("listener ["+name+"] already registered with ServiceDispatcher");
		}

		registeredListeners.put(name, listener);
		log.info("Listener ["+name+"] registered at ServiceDispatcher");
	}

	public void unregisterServiceClient(String name) {
		if (!isRegisteredServiceListener(name)) {
			log.warn("listener ["+name+"] not registered with ServiceDispatcher");
		} else {
			registeredListeners.remove(name);
			log.info("Listener ["+name+"] unregistered from ServiceDispatcher");
		}
	}

	public ServiceClient getListener(String name) {
		return registeredListeners.get(name);
	}
}
