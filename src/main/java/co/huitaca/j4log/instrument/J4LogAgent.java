package co.huitaca.j4log.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.security.ProtectionDomain;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import co.huitaca.j4log.jmx.J4Log;

public class J4LogAgent {

	public static void premain(final String agentArgument, final Instrumentation instrumentation)
			throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException,
			NotCompliantMBeanException {

		// instrumentation.addTransformer(new Transformer(), true);
		ManagementFactory.getPlatformMBeanServer().registerMBean(J4Log.getInstance(),
				new ObjectName(J4Log.OBJECT_NAME));

	}

	static class Transformer implements ClassFileTransformer {

		@Override
		public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
				ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

			return null;
		}

	}
}
