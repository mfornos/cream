package play.modules.cream.observation;

import javax.jcr.observation.EventListener;

import play.modules.cream.annotations.JcrOnEvent;

public class EventListenerHolder {
    private final EventListener listener;
    private final JcrOnEvent onEvent;

    public EventListenerHolder(Class<? extends EventListener> clazz) throws InstantiationException,
            IllegalAccessException {
        this.listener = clazz.newInstance();
        this.onEvent = clazz.getAnnotation(JcrOnEvent.class);
    }

    public EventListener getListener() {
        return listener;
    }

    public JcrOnEvent getOnEvent() {
        return onEvent;
    }
}
