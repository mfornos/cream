package play.modules.cream.observation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.modules.cream.annotations.JcrOnEvent;

/**
 * Plugin to register session-scoped observers in a Jcr workspace
 * 
 */
public class JcrObservationPlugin extends PlayPlugin {
    public static List<EventListenerHolder> observers;

    @Override
    public String getStatus() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        out.println("Jcr Observers:");
        out.println("~~~~~~~~~~~~~");

        if (observers == null) {
            out.println("(not yet initialized)");
            return sw.toString();
        }

        out.println("Count: " + observers.size());
        for (EventListenerHolder h : observers) {
            out.println(String.format("%s on %s", h.getListener().getClass().getName(), h.getOnEvent().absPath()));
        }
        return sw.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onApplicationStart() {
        observers = new ArrayList<EventListenerHolder>();
        for (Class clazz : Play.classloader.getAllClasses()) {
            if (EventListener.class.isAssignableFrom(clazz)) {
                if (clazz.isAnnotationPresent(JcrOnEvent.class)) {
                    try {
                        EventListenerHolder holder = new EventListenerHolder(clazz);
                        observers.add(holder);
                    } catch (InstantiationException e) {
                        Logger.error(e, e.getMessage());
                    } catch (IllegalAccessException e) {
                        Logger.error(e, e.getMessage());
                    }
                } else {
                    Logger.warn("%s lacks @OnJcrEvent annotation", clazz.getName());
                }
            }
        }
    }

    @Override
    public void onEvent(String message, Object context) {
        if (!observers.isEmpty() && message.equals("JcrPlugin.JcrSessionCreated")) {
            Session session = (Session) context;
            try {
                ObservationManager observationMan = session.getWorkspace().getObservationManager();
                for (EventListenerHolder h : observers) {
                    JcrOnEvent onEvent = h.getOnEvent();
                    String[] nodeTypeName = onEvent.nodeTypeName();
                    String[] uuid = onEvent.uuid();
                    observationMan.addEventListener(h.getListener(), onEvent.eventTypes(), onEvent.absPath(), onEvent
                            .isDeep(), (uuid.length > 0) ? uuid : null,
                            (nodeTypeName.length) > 0 ? nodeTypeName : null, onEvent.noLocal());
                }
            } catch (UnsupportedRepositoryOperationException e) {
                Logger.warn(e, e.getMessage());
            } catch (RepositoryException e) {
                Logger.error(e, e.getMessage());
            }
        }
    }
}
