package play.modules.cream;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.jcrom.Jcrom;
import org.jcrom.annotations.JcrNode;
import org.jcrom.util.ReflectionUtils;

public class JcrMetadata {
    public class MD {
        public String[] mixinTypes;
        public boolean isVersionable;

        public MD(Class<?> clazz) {
            this.mixinTypes = findMixins(clazz);
            this.isVersionable = checkIfVersionable(clazz);
        }

        private boolean checkIfVersionable(Class<?> clazz) {
            for (String mixinType : mixinTypes) {
                if (mixinType.equals("mix:versionable")) {
                    return true;
                }
            }
            return false;
        }

        private String[] findMixins(Class<?> clazz) {
            String[] mixins;
            JcrNode jcrNode = ReflectionUtils.getJcrNodeAnnotation(clazz);
            if (jcrNode != null && jcrNode.mixinTypes() != null) {
                mixins = jcrNode.mixinTypes();
            } else {
                mixins = new ArrayUtils().EMPTY_STRING_ARRAY;
            }
            return mixins;
        }
    }

    public static JcrMetadata getInstance() {
        if (INSTANCE == null)
            createInstance();
        return INSTANCE;
    }

    private synchronized static void createInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JcrMetadata();
        }
    }

    public Map<Class<?>, MD> metadata;

    private static JcrMetadata INSTANCE = null;

    private JcrMetadata() {
        metadata = new HashMap<Class<?>, JcrMetadata.MD>();
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public void intialize(Jcrom jcrom) {
        for (Class<?> clazz : jcrom.getMappedClasses()) {
            metadata.put(clazz, new MD(clazz));
        }
    }

    public MD lookup(Class<?> clazz) {
        return metadata.get(clazz);
    }
}
