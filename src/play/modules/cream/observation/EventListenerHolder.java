package play.modules.cream.observation;

import javax.jcr.observation.EventListener;

import play.modules.cream.annotations.OnJcrEvent;

public class EventListenerHolder {
    private final EventListener listener;
    private final OnJcrEvent onEvent;

    public EventListenerHolder(Class<? extends EventListener> clazz) throws InstantiationException,
            IllegalAccessException {
        this.listener = clazz.newInstance();
        this.onEvent = clazz.getAnnotation(OnJcrEvent.class);
    }

    public EventListener getListener() {
        return listener;
    }

    public OnJcrEvent getOnEvent() {
        return onEvent;
    }
}
