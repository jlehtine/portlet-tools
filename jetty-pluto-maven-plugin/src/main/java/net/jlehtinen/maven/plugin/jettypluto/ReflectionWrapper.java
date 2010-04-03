package net.jlehtinen.maven.plugin.jettypluto;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;

/**
 * Wraps an object with protected or private fields and methods and provides
 * access to them using reflection.
 */
public class ReflectionWrapper {

	/** The wrapped object */
	protected final Object wrappedObject;

	/**
	 * Constructs a new instance.
	 * 
	 * @param wrappedObject wrapped object
	 */
	public ReflectionWrapper(Object wrappedObject) {
		this.wrappedObject = wrappedObject;
	}

	/**
	 * Returns the wrapped object.
	 * 
	 * @return wrapped object
	 */
	public Object getWrappedObject() {
		return wrappedObject;
	}

	/**
	 * Returns the value of the specified field in the wrapped object.
	 * 
	 * @param field name of the field
	 * @return value of the field
	 */
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
									new Object[] { field, getWrappedObject().getClass().getName() }
							),
							t
					);
				}
			}
		});
	}
	
	/**
	 * Invokes the specified method on the wrapped object.
	 * 
	 * @param method name of the method
	 * @param methodArgTypes method argument signature
	 * @param methodArgs method invocation arguments
	 * @return method return value, if any
	 */
	public Object invokeMethod(final String method, final Class[] methodArgTypes, final Object[] methodArgs) {
		return AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				try {
					Method m = getMethod(method, methodArgTypes);
					m.setAccessible(true);
					return m.invoke(wrappedObject, methodArgs);
				} catch (Throwable t) {
					throw new RuntimeException(
							MessageFormat.format(
									"Failed to invoke method {0} of class {1} using reflection",
									new Object[] { method, getWrappedObject().getClass().getName() }
							),
							t
					);
				}
			}
		});
	}

	/**
	 * Returns the specified field of the wrapped object.
	 * 
	 * @param field name of the field
	 * @return reflected field
	 */
	protected Field getField(String field) {
		NoSuchFieldException initialNSFE = null;
		Class clazz = getWrappedObject().getClass();
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
	
	/**
	 * Returns the specified method of the wrapped object.
	 * 
	 * @param method name of the method
	 * @param methodArgTypes method argument signature
	 * @return reflected method
	 */
	protected Method getMethod(String method, Class[] methodArgTypes) {
		NoSuchMethodException initialNSME = null;
		Class clazz = getWrappedObject().getClass();
		while (clazz != null) {
			try {
				return clazz.getDeclaredMethod(method, methodArgTypes);
			} catch (NoSuchMethodException e) {
				if (initialNSME == null) {
					initialNSME = e;
				}
				clazz = clazz.getSuperclass();
			}
		}
		throw new RuntimeException(
				MessageFormat.format(
						"No method {0} found in class {1} or its superclasses",
						new Object[] { method, getWrappedObject().getClass().getName() }
				),
				initialNSME
		);
	}
}
