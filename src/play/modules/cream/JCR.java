package play.modules.cream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.VersionManager;

import play.inject.Injector;

public class JCR {
    private static ThreadLocal<JcrSessionSource> sessionSource = new ThreadLocal<JcrSessionSource>();

    public static void closeSession() {
        JcrSessionSource currentSessionSource = sessionSource.get();
        if (currentSessionSource != null) {
            currentSessionSource.close();
            sessionSource.set(null);
        }
    }

    public static ObservationManager getObservationManager() throws RepositoryException {
        return getSession().getWorkspace().getObservationManager();
    }

    public static QueryManager getQueryManager() throws RepositoryException {
        return getSession().getWorkspace().getQueryManager();
    }

    public static Session getSession() {
        return sessionSource.get().getBeanOfType(Session.class);
    }

    public static VersionManager getVersionManager() throws UnsupportedRepositoryOperationException,
            RepositoryException {
        return getSession().getWorkspace().getVersionManager();
    }

    static void addSession(Session currentSession) {
        sessionSource.set(new JcrSessionSource(currentSession));
        Injector.inject(sessionSource.get());
    }

    private JCR() {

    }
}
