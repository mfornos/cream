package play.modules.cream.helpers;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.RepositoryImpl;

import play.exceptions.UnexpectedException;

/**
 * Jcr repository helper
 */
public class JcrRepositoryHelper {

	/**
	 * The loaded repository.
	 */
	public static Repository repository = null;

	public static Credentials defaultCredentials;

	public static String defaultWorkspace;

	public static Session openSession() {
		return openSession(defaultCredentials, defaultWorkspace);
	}

	/**
	 * Open a session for the current thread.
	 * 
	 * @return A valid JCR session
	 */
	public static Session openSession(Credentials credentials, String workspace) {
		Session session;
		try {
			session = repository.login(credentials, workspace);
		} catch (LoginException e) {
			throw new UnexpectedException(e);
		} catch (NoSuchWorkspaceException e) {
			throw new UnexpectedException(e);
		} catch (RepositoryException e) {
			throw new UnexpectedException(e);
		}
		return session;
	}

	public static void shutdown() {
		if (RepositoryImpl.class.isAssignableFrom(repository.getClass())) {
			((RepositoryImpl) repository).shutdown();
		}
	}

	public static Session openSession(String workspace) {
		return openSession(defaultCredentials, workspace);
	}

	public static Session openSession(SimpleCredentials credentials) {
		return openSession(credentials, defaultWorkspace);
	}
}
