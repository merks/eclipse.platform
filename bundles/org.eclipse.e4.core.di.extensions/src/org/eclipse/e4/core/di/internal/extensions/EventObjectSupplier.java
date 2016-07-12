/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     ARTAL Technologies <simon.chemouil@artal.fr> - Allow wildcards in topic names
 *     Lars.Vogel <Lars.Vogel@vogella.com> - Bug 472654
 *     Alex Blewitt <alex.blewitt@gmail.com> - Bug 476364
 *******************************************************************************/
package org.eclipse.e4.core.di.internal.extensions;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.PreDestroy;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.di.extensions.EventUtils;
import org.eclipse.e4.core.di.suppliers.ExtendedObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * This class is instantiated and wired by declarative services.
 */
@Component(service = ExtendedObjectSupplier.class, immediate = true, name = "org.eclipse.e4.core.services.events", property = "dependency.injection.annotation=org.eclipse.e4.core.di.extensions.EventTopic")
public class EventObjectSupplier extends ExtendedObjectSupplier {

	private EventAdmin eventAdmin;

	public EventAdmin getEventAdmin() {
		return eventAdmin;
	}

	@Reference
	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	// can be removed after Bug 492726 is fixed
	protected void unsetEventAdmin(EventAdmin ea) {
		this.eventAdmin = null;
	}

	protected Map<String, Event> currentEvents = new HashMap<>();

	class DIEventHandler implements EventHandler {

		final private IRequestor requestor;
		final private String topic;

		public DIEventHandler(String topic, IRequestor requestor) {
			this.topic = topic;
			this.requestor = requestor;
		}

		@Override
		public void handleEvent(Event event) {
			if (!requestor.isValid()) {
				unsubscribe(requestor);
				return;
			}

			addCurrentEvent(topic, event);
			requestor.resolveArguments(false);
			removeCurrentEvent(topic);

			requestor.execute();
		}
	}

	// A combo of { IRequestor + topic } used in Map lookups
	static private class Subscriber {
		private IRequestor requestor;
		private String topic;

		public Subscriber(IRequestor requestor, String topic) {
			super();
			this.requestor = requestor;
			this.topic = topic;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((requestor == null) ? 0 : requestor.hashCode());
			result = prime * result + ((topic == null) ? 0 : topic.hashCode());
			return result;
		}

		public IRequestor getRequestor() {
			return requestor;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Subscriber other = (Subscriber) obj;
			if (requestor == null) {
				if (other.requestor != null)
					return false;
			} else if (!requestor.equals(other.requestor))
				return false;
			if (topic == null) {
				if (other.topic != null)
					return false;
			} else if (!topic.equals(other.topic))
				return false;
			return true;
		}

	}

	private Map<Subscriber, ServiceRegistration<EventHandler>> registrations = new HashMap<>();

	protected void addCurrentEvent(String topic, Event event) {
		synchronized (currentEvents) {
			currentEvents.put(topic, event);
		}
	}

	protected void removeCurrentEvent(String topic) {
		synchronized (currentEvents) {
			currentEvents.remove(topic);
		}
	}

	@Override
	public Object get(IObjectDescriptor descriptor, IRequestor requestor, boolean track, boolean group) {
		if (descriptor == null)
			return null;
		String topic = getTopic(descriptor);
		if (topic == null || eventAdmin == null || topic.length() == 0)
			return IInjector.NOT_A_VALUE;

		if (track)
			subscribe(topic, requestor);
		else
			unsubscribe(requestor);

		if (!currentEvents.containsKey(topic))
			return IInjector.NOT_A_VALUE;

		// convert to fit destination
		Class<?> descriptorsClass = getDesiredClass(descriptor.getDesiredType());
		if (descriptorsClass.equals(Event.class))
			return currentEvents.get(topic);
		return currentEvents.get(topic).getProperty(EventUtils.DATA);
	}

	private void subscribe(String topic, IRequestor requestor) {
		Subscriber subscriber = new Subscriber(requestor, topic);
		synchronized (registrations) {
			if (registrations.containsKey(subscriber))
				return;
		}
		BundleContext bundleContext = FrameworkUtil.getBundle(EventObjectSupplier.class).getBundleContext();
		if (bundleContext == null)
			throw new InjectionException(
					"Unable to subscribe to events: org.eclipse.e4.core.di.extensions bundle is not activated"); //$NON-NLS-1$

		String[] topics = new String[] { topic };
		Dictionary<String, Object> d = new Hashtable<>();
		d.put(EventConstants.EVENT_TOPIC, topics);
		EventHandler wrappedHandler = makeHandler(topic, requestor);
		ServiceRegistration<EventHandler> registration = bundleContext.registerService(EventHandler.class,
				wrappedHandler, d);
		// due to the way requestors are constructed this limited synch should
		// be OK
		synchronized (registrations) {
			registrations.put(subscriber, registration);
		}
	}

	protected EventHandler makeHandler(String topic, IRequestor requestor) {
		return new DIEventHandler(topic, requestor);
	}

	protected String getTopic(IObjectDescriptor descriptor) {
		if (descriptor == null)
			return null;
		EventTopic qualifier = descriptor.getQualifier(EventTopic.class);
		return qualifier.value();
	}

	protected void unsubscribe(IRequestor requestor) {
		if (requestor == null)
			return;
		synchronized (registrations) {
			Iterator<Entry<Subscriber, ServiceRegistration<EventHandler>>> i = registrations.entrySet().iterator();
			while (i.hasNext()) {
				Entry<Subscriber, ServiceRegistration<EventHandler>> entry = i.next();
				Subscriber key = entry.getKey();
				if (!requestor.equals(key.getRequestor()))
					continue;
				ServiceRegistration<EventHandler> registration = entry.getValue();
				registration.unregister();
				i.remove();
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@PreDestroy
	public void dispose() {
		ServiceRegistration[] array;
		synchronized (registrations) {
			Collection<ServiceRegistration<EventHandler>> values = registrations.values();
			array = values.toArray(new ServiceRegistration[values.size()]);
			registrations.clear();
		}
		for (int i = 0; i < array.length; i++) {
			array[i].unregister();
		}
	}

	private Class<?> getDesiredClass(Type desiredType) {
		if (desiredType instanceof Class<?>)
			return (Class<?>) desiredType;
		if (desiredType instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType) desiredType).getRawType();
			if (rawType instanceof Class<?>)
				return (Class<?>) rawType;
		}
		return null;
	}

}
