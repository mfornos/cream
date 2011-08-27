package play.modules.cream;

import javax.jcr.Session;

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

    public static Session getSession() {
        return sessionSource.get().getBeanOfType(Session.class);
    }

    static void addSession(Session currentSession) {
        sessionSource.set(new JcrSessionSource(currentSession));
        Injector.inject(sessionSource.get());
    }

    private JCR() {

    }
}
