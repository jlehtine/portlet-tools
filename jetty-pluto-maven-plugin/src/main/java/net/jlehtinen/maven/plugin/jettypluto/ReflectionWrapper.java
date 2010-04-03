package net.jlehtinen.maven.plugin.jettypluto;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;

public class ReflectionWrapper {

	protected final Object wrappedObject;

	public ReflectionWrapper(Object wrappedObject) {
		this.wrappedObject = wrappedObject;
	}

	public Object getWrappedObject() {
		return wrappedObject;
	}

	public Object getFieldValue(final String field) {
		return AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				try {
					Field f = getField(field);
					f.setAccessible(true);
					return f.get(getWrappedObject());
				} catch (Throwable t) {
					throw new RuntimeException(
							MessageFormat.format(
									"Failed to read field {0} of class {1} using reflection",
									new Object[] { field, wrappedObject.getClass().getName() }
							),
							t
					);
				}
			}
		});
	}

	protected Field getField(String field) {
		NoSuchFieldException initialNSFE = null;
		Class clazz = wrappedObject.getClass();
		while (clazz != null) {
			try {
				return clazz.getDeclaredField(field);
			} catch (NoSuchFieldException e) {
				if (initialNSFE == null) {
					initialNSFE = e;
				}
				clazz = clazz.getSuperclass();
			}
		}
		throw new RuntimeException(
				MessageFormat.format(
						"No field {0} found in class {1} or its superclasses",
						new Object[] { field, getWrappedObject().getClass().getName() }
				),
				initialNSFE
		);
	}
}
