package observers;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import play.Logger;
import play.modules.cream.annotations.OnJcrEvent;

@OnJcrEvent(eventTypes = Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_CHANGED, absPath = "/", isDeep = true)
public class LoggingObserver implements EventListener {

    @Override
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            Event event = events.nextEvent();
            Logger.info("JcrEvent received: { %s }", event.toString());
        }
    }
}
