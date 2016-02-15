/*
 * Copyright 2016 Camilo Bermúdez
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.huitaca.j4log.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.bytecode.Descriptor;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import co.huitaca.j4log.J4LogPlugin;
import co.huitaca.j4log.LogLevel;
import co.huitaca.j4log.jmx.J4Log;
import co.huitaca.j4log.plugins.PluginManager;

public class J4LogAgent {

	// private static final Logger LOGGER =
	// Logger.getLogger(J4LogAgent.class.getName());

	public static void premain(final String agentArgument,
			final Instrumentation instrumentation)
			throws MalformedObjectNameException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			NotCompliantMBeanException {

		    System.out.println("Activating j4Log agent");
		    setInitialStates(buildInitialState(agentArgument));
		    
		    instrumentation.addTransformer(new Transformer(
		    		buildObservedClassesMap()), false);
		    ManagementFactory.getPlatformMBeanServer().registerMBean(
		    		J4Log.getInstance(), new ObjectName(J4Log.OBJECT_NAME));

	}

	private static void setInitialStates(Map<String, LogLevel> initialState) {
		System.out.println("Initial state: " + initialState);
		for (J4LogPlugin plugin : PluginManager.getPlugins()) {
			plugin.setInitialState(initialState);
		}
	}
	
	private static Map<String, LogLevel> buildInitialState(String agentArgument) {
	
		if(agentArgument == null) {
			return Collections.emptyMap();
		}
		Map<String, LogLevel> initialState = new HashMap<String, LogLevel>();
		String[] loggers = agentArgument.split(",");
		for (String logger : loggers) {
			String[] spl = logger.split("=");
			if(spl.length > 1) {
				String name = spl[0];
				String level = spl[1];
				try {
					initialState.put(name, LogLevel.valueOf(level.toUpperCase()));
				} catch (Exception e) {	
					// it could be anything, just ignore	
				}
			}
		}
		return initialState;
	}
	
	private static Map<String, List<J4LogPlugin>> buildObservedClassesMap() {

		Map<String, List<J4LogPlugin>> map = new HashMap<String, List<J4LogPlugin>>();
		for (J4LogPlugin plugin : PluginManager.getPlugins()) {

			System.out.println("Loading plugin " + plugin);
			for (String className : plugin.getObservedClasses()) {
				if (className == null || "".equals(className.trim())) {
					continue;
				}
				String jvmName = Descriptor.toJvmName(className);
				if (map.containsKey(jvmName)) {
					map.get(jvmName).add(plugin);
				} else {
					List<J4LogPlugin> plugins = new ArrayList<J4LogPlugin>();
					plugins.add(plugin);
					map.put(jvmName, plugins);
				}
			}
		}

		return map;
	}

	static class Transformer implements ClassFileTransformer {

		private Map<String, List<J4LogPlugin>> observedClassesMap;

		public Transformer(Map<String, List<J4LogPlugin>> observedClassesMap) {
			super();
			this.observedClassesMap = observedClassesMap;
		}

		@Override
		public byte[] transform(ClassLoader loader, String className,
				Class<?> classBeingRedefined,
				ProtectionDomain protectionDomain, byte[] classfileBuffer)
				throws IllegalClassFormatException {

			List<J4LogPlugin> plugins = observedClassesMap.get(className);
			if (plugins == null) {
				return null;
			}

			byte[] tempBuffer;
			boolean transformedAtLeastOnce = false;
			for (J4LogPlugin plugin : plugins) {
				tempBuffer = plugin.onClassLoaded(
						Descriptor.toJavaName(className), loader,
						protectionDomain, classfileBuffer);
				if (tempBuffer != null) {
					transformedAtLeastOnce = true;
					classfileBuffer = tempBuffer;
				}
			}

			return transformedAtLeastOnce ? classfileBuffer : null;
		}

	}
}
