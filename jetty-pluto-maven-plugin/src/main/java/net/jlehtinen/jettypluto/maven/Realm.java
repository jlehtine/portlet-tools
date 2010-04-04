package net.jlehtinen.jettypluto.maven;

import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.security.UserRealm;

/**
 * Hash map based user realm initialized from a set of User records.
 */
public class Realm implements UserRealm {

	/**
	 * Wrapper class for {@link User} promoted to a specified role.
	 */
	protected static class PromotedUser extends User {
		
		/** Wrapped user */
		protected final User user;
		
		/** Added role */
		protected final String role;
		
		/**
		 * Constructs a new instance.
		 * 
		 * @param user promoted user
		 * @param role added role
		 */
		public PromotedUser(User user, String role) {
			this.user = user;
			this.role = role;
		}

		public boolean hasRole(String role) {
			return role.equals(this.role) || user.hasRole(role);
		}

		/**
		 * Returns the original unpromoted user.
		 * 
		 * @return original unpromoted user
		 */
		public User getUser() {
			return user;
		}
	}
	
	/** Realm name */
	protected final String name;
	
	/** Map of users by name */
	protected final Map usersByName;

	/**
	 * Constructs and initializes a new instance using the specified users.
	 * 
	 * @param name realm name
	 * @param users collection of users
	 */
	public Realm(String name, Collection users) {
		this.name = name;
		usersByName = new HashMap();
		Iterator iter = users.iterator();
		while (iter.hasNext()) {
			User u = (User) iter.next();
			usersByName.put(u.getUsername(), u);
		}
	}
	
	public String getName() {
		return name;
	}

	public Principal authenticate(String username, Object credentials, Request request) {
		Object o = usersByName.get(username);
		if (o != null) {
			User u = (User) o;
			if (u.authenticate(credentials)) {
				return u;
			}
		}
		return null;
	}

	public boolean reauthenticate(Principal user) {
		return true;
	}

	public void disassociate(Principal user) {
	}

	public Principal getPrincipal(String username) {
		Object o = usersByName.get(username);
		if (o != null) {
			return (Principal) o;
		} else {
			return null;
		}
	}

	public boolean isUserInRole(Principal user, String role) {
		User u = (User) user;
		return u.hasRole(role);
	}

	public void logout(Principal user) {
	}

	public Principal pushRole(Principal user, String role) {
		return new PromotedUser((User) user, role);
	}

	public Principal popRole(Principal user) {
		return ((PromotedUser) user).getUser();
	}

}
