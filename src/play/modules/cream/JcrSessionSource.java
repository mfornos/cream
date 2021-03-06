package play.modules.cream;

import javax.jcr.Session;

import play.inject.BeanSource;

public class JcrSessionSource implements BeanSource {
    private final Session session;

    public JcrSessionSource(Session session) {
        this.session = session;
    }

    public void close() {
        session.logout();
    }

    @SuppressWarnings("unchecked")
    public <T> T getBeanOfType(Class<T> type) {
        return (T) ((Session.class.isAssignableFrom(type)) ? session : null);
    }
}
