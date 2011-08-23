package play.modules.cream.helpers;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtilsBean;

import play.Logger;

public class NullAwareBeanUtilsBean extends BeanUtilsBean {

	@Override
	public void copyProperty(Object dest, String name, Object value) throws IllegalAccessException,
			InvocationTargetException {
		if (value != null) {
			super.copyProperty(dest, name, value);
		}
	}

}
