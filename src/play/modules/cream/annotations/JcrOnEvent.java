package play.modules.cream.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)
public @interface JcrOnEvent {
    public String absPath();

    public int eventTypes();

    public boolean isDeep() default false;

    public String[] nodeTypeName() default {};

    public boolean noLocal() default false;

    public String[] uuid() default {};
}
