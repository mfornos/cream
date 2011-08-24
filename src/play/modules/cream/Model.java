package play.modules.cream;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jcrom.annotations.JcrBaseVersionCreated;
import org.jcrom.annotations.JcrCheckedout;
import org.jcrom.annotations.JcrCreated;
import org.jcrom.annotations.JcrPath;
import org.jcrom.annotations.JcrReference;
import org.jcrom.annotations.JcrUUID;
import org.jcrom.annotations.JcrVersionCreated;
import org.jcrom.annotations.JcrVersionName;

import play.Play;
import play.data.binding.BeanWrapper;
import play.data.validation.Validation;
import play.exceptions.UnexpectedException;
import play.utils.Utils;

public abstract class Model implements play.db.Model {

    /**
     * Prepare a query to find *all* entities.
     * 
     * @param Root
     *            path to search
     * 
     * @return JcrQuery
     */
    public static JcrQuery all(String rootPath) {
        throw new UnsupportedOperationException("Class not enhanced.");
    }

    public static <T extends Model> T create(Class<?> type, String name, Map<String, String[]> params,
            Annotation[] annotations) {
        try {
            Constructor c = type.getDeclaredConstructor();
            c.setAccessible(true);
            Object model = c.newInstance();
            return (T) edit(model, name, params, annotations);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends Model> T edit(Object o, String name, Map<String, String[]> params, Annotation[] annotations) {
        try {
            BeanWrapper bw = new BeanWrapper(o.getClass());
            // Start with relations
            Set<Field> fields = new HashSet<Field>();
            Class clazz = o.getClass();
            while (!clazz.equals(Object.class)) {
                Collections.addAll(fields, clazz.getDeclaredFields());
                clazz = clazz.getSuperclass();
            }
            for (Field field : fields) {
                boolean isEntity = false;
                String relation = null;
                boolean multiple = false;
                //
                if (Collection.class.isAssignableFrom(field.getType()) && field.isAnnotationPresent(JcrReference.class)) {
                    Class fieldType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    isEntity = true;
                    relation = fieldType.getName();
                    multiple = true;
                } else if (field.isAnnotationPresent(JcrReference.class)) {
                    isEntity = true;
                    relation = field.getType().getName();
                }

                if (isEntity) {
                    Class<Model> c = (Class<Model>) Play.classloader.loadClass(relation);
                    if (Model.class.isAssignableFrom(c)) {
                        String keyName = Model.Manager.factoryFor(c).keyName();
                        if (multiple && Collection.class.isAssignableFrom(field.getType())) {
                            Collection l = new ArrayList();
                            if (SortedSet.class.isAssignableFrom(field.getType())) {
                                l = new TreeSet();
                            } else if (Set.class.isAssignableFrom(field.getType())) {
                                l = new HashSet();
                            }
                            String[] ids = params.get(name + "." + field.getName() + "." + keyName);
                            if (ids != null) {
                                params.remove(name + "." + field.getName() + "." + keyName);
                                for (String _id : ids) {
                                    if (_id.equals("")) {
                                        continue;
                                    }
                                    // We only need uuid
                                    Constructor<Model> ctor = c.getDeclaredConstructor();
                                    try {
                                        Model to = ctor.newInstance();
                                        to.uuid = _id;
                                        l.add(to);
                                    } catch (Exception e) {
                                        Validation.addError(name + "." + field.getName(), "validation.notFound", _id);
                                    }
                                }
                                bw.set(field.getName(), o, l);
                            }
                        } else {
                            String[] ids = params.get(name + "." + field.getName() + "." + keyName);
                            if (ids != null && ids.length > 0 && !ids[0].equals("")) {
                                params.remove(name + "." + field.getName() + "." + keyName);
                                // We only need uuid
                                Constructor<Model> ctor = c.getDeclaredConstructor();
                                try {
                                    String localName = name + "." + field.getName();
                                    Model to = ctor.newInstance();
                                    to.uuid = ids[0];
                                    edit(to, localName, params, field.getAnnotations());
                                    params = Utils.filterMap(params, localName);
                                    bw.set(field.getName(), o, to);
                                } catch (Exception e) {
                                    Validation.addError(name + "." + field.getName(), "validation.notFound", ids[0]);
                                }
                            } else if (ids != null && ids.length > 0 && ids[0].equals("")) {
                                bw.set(field.getName(), o, null);
                                params.remove(name + "." + field.getName() + "." + keyName);
                            }
                        }
                    }
                }
                if (field.getType().isEnum()) {
                    String fieldKey = name + "." + field.getName();
                    String[] enumValues = params.get(fieldKey);
                    if (enumValues != null) {
                        Enum<?> enumValue = Enum.valueOf((Class<? extends Enum>) field.getType(), enumValues[0]);
                        bw.set(field.getName(), o, enumValue);
                        params.remove(fieldKey);
                    }
                }
            }
            bw.bind(name, o.getClass(), params, "", o, annotations);
            return (T) o;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Prepare a query to find entities.
     * 
     * @param query
     *            SQL2 query
     * @param params
     *            Params to bind to the query
     * @return JcrQuery
     */
    public static JcrQuery find(String query, Object... params) {
        throw new UnsupportedOperationException("Class not enhanced.");
    }

    /**
     * 
     * @param rootPath
     * @return
     */
    public static <T extends Model> List<T> findAll(String rootPath) {
        throw new UnsupportedOperationException("Class not enhanced.");
    }

    /**
     * Find the entity with the corresponding id.
     * 
     * @param id
     *            The entity id
     * @return The entity
     */
    public static <T extends Model> T findById(Object id) {
        throw new UnsupportedOperationException("Class not enhanced.");
    }

    public static <T extends Model> T get(String path) {
        throw new UnsupportedOperationException("Class not enhanced.");
    }

    @JcrUUID
    public String uuid;

    @JcrPath
    public String path;

    @JcrCreated
    public Date created;

    @JcrBaseVersionCreated
    public Date baseVersionCreated;

    @JcrVersionCreated
    public Date versionCreated;

    @JcrVersionName
    public String versionName;

    @JcrCheckedout
    public boolean checkedout;

    public void _delete() {
        JcrPersistence.remove(this);
    }

    @Override
    public Object _key() {
        return getId();
    }

    public void _save() {
        if (path == null) {
            path = JcrPersistence.getDefaultPath(this.getClass());
            JcrPersistence.create(this);
        } else {
            // TODO support weak references by path?
            if (uuid == null) {
                JcrPersistence.create(this);
            } else {
                JcrPersistence.update(this);
            }
        }
    }

    /**
     * store (ie insert) the entity.
     */
    public boolean create() {
        JcrPersistence.create(this);
        return true;
    }

    /**
     * Delete the entity.
     * 
     * @return The deleted entity.
     */
    public <T extends Model> T delete() {
        _delete();
        return (T) this;
    }

    public Date getCreated() {
        return created;
    }

    public String getId() {
        return uuid;
    }

    public String getPath() {
        return path;
    }

    public Date getUpdated() {
        return versionCreated;
    }

    public String getUuid() {
        return uuid;
    }

    public String getVersion() {
        return versionName;
    }

    public boolean isCheckedout() {
        return checkedout;
    }

    public <T> T merge() {
        return (T) JcrPersistence.merge(this);
    }

    public <T> T merge(String childNodeFilter, int maxDepth) {
        return (T) JcrPersistence.merge(this, childNodeFilter, maxDepth);
    }

    /**
     * store (ie insert) the entity.
     */
    public <T extends Model> T save() {
        _save();
        return (T) this;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public <T> T update() {
        return (T) JcrPersistence.update(this);
    }

    public <T> T update(String childNodeFilter, int maxDepth) {
        return (T) JcrPersistence.update(this, childNodeFilter, maxDepth);
    }

    public boolean validateAndCreate() {
        if (Validation.current().valid(this).ok) {
            return create();
        }
        return false;
    }

    public boolean validateAndSave() {
        if (Validation.current().valid(this).ok) {
            save();
            return true;
        }
        return false;
    }

}
