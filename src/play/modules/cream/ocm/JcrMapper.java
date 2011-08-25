package play.modules.cream.ocm;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionManager;

import org.jcrom.JcrMappingException;
import org.jcrom.Jcrom;

import play.Play;
import play.exceptions.UnexpectedException;
import play.modules.cream.JcrMetadata;
import play.modules.cream.JcrMetadata.MD;
import play.modules.cream.JcrQuery;
import play.modules.cream.Model;
import play.modules.cream.helpers.NullAwareBeanUtilsBean;

// TODO replace some strategic exceptions with return null
public class JcrMapper {

    // Jcrom is thread-safe
    public static Jcrom jcrom;

    public static NullAwareBeanUtilsBean beanUtils = new NullAwareBeanUtilsBean();

    public static Node addNode(Node parentNode, Object entity) throws JcrMappingException {
        return jcrom.addNode(parentNode, entity);
    }

    public static Node addNode(Node arg0, Object arg1, String[] arg2) throws JcrMappingException {
        return jcrom.addNode(arg0, arg1, arg2);
    }

    public static <T> T create(String parentNodePath, T entity) {
        try {
            MD md = getMetadata(entity.getClass());
            String entityName = jcrom.getName(entity);
            if (entityName == null || entityName.equals("")) {
                throw new JcrMappingException("The name of the entity being created is empty!");
            }
            if (parentNodePath == null || parentNodePath.equals("")) {
                throw new JcrMappingException("The parent path of the entity being created is empty!");
            }

            Node parentNode;
            Session session = getSession();
            Node rootNode = session.getRootNode();
            if (parentNodePath.equals("/")) {
                // special case, add directly to the root node
                parentNode = rootNode;
            } else {
                String relativePath = relativePath(parentNodePath);
                if (rootNode.hasNode(relativePath)) {
                    parentNode = rootNode.getNode(relativePath);
                } else {
                    // if not found create it
                    parentNode = rootNode.addNode(relativePath);
                }
            }
            Node newNode = jcrom.addNode(parentNode, entity, md.mixinTypes);
            session.save();
            if (md.isVersionable) {
                checkinRecursively(session.getWorkspace().getVersionManager(), newNode);
            }
            return entity;
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not create node", e);
        }
    }

    public static <T> T create(T entity) {
        return create(jcrom.getPath(entity), entity);
    }

    public static boolean exists(String path) {
        try {
            return getSession().getRootNode().hasNode(relativePath(path));
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not check if node exists", e);
        }
    }

    public static <T> JcrQuery<T> find(final String className, final String queryString, Object... params)
            throws RepositoryException {
        Class clazz = Play.classloader.getClassIgnoreCase(className);
        return executeQuery(clazz, queryString, params);
    }

    public static <T> JcrQuery<T> findAll(Class<T> clazz, String rootPath) {
        return findAll(clazz, rootPath, "*", -1);
    }

    public static <T> JcrQuery<T> findAll(Class<T> clazz, String rootPath, String childNameFilter, int maxDepth) {
        try {
            NodeIterator nodeIterator = getSession().getRootNode().getNode(relativePath(rootPath)).getNodes();
            return toJcrQuery(clazz, nodeIterator);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not find nodes", e);
        }
    }

    public static <T> JcrQuery<T> findAll(String className, String rootPath) {
        return findAll(Play.classloader.getClassIgnoreCase(className), rootPath);
    }

    public static <T> T fromNode(Class<T> entityClass, Node node) throws JcrMappingException {
        return jcrom.fromNode(entityClass, node);
    }

    public static <T> T fromNode(Class<T> arg0, Node arg1, String arg2, int arg3) throws JcrMappingException {
        return jcrom.fromNode(arg0, arg1, arg2, arg3);
    }

    public static <T> T get(Class<T> clazz, String path) {
        return get(clazz, path, "*", -1);
    }

    public static <T> T get(Class<T> clazz, String path, String childNodeFilter, int maxDepth) {
        if (exists(path)) {
            Node node;
            try {
                node = getSession().getRootNode().getNode(relativePath(path));
            } catch (RepositoryException e) {
                throw new JcrMappingException("Could not get node", e);
            }
            return (T) jcrom.fromNode(clazz, node, childNodeFilter, maxDepth);
        } else {
            return null;
        }
    }

    public static <T extends Model> T get(String className, String path) {
        Class clazz = Play.classloader.getClassIgnoreCase(className);
        return (T) get(clazz, path);
    }

    public static String getDefaultPath(Class<?> modelClass) {
        return "/" + modelClass.getSimpleName().toLowerCase();
    }

    public static String getName(Object arg0) throws JcrMappingException {
        return jcrom.getName(arg0);
    }

    public static String getPath(Object arg0) throws JcrMappingException {
        return jcrom.getPath(arg0);
    }

    public static QueryManager getQueryManager() throws RepositoryException {
        return getSession().getWorkspace().getQueryManager();
    }

    public static ObservationManager getObservationManager() throws RepositoryException {
        return getSession().getWorkspace().getObservationManager();
    }

    public static long getSize(String rootPath) {
        try {
            NodeIterator nodeIterator = getSession().getRootNode().getNode(relativePath(rootPath)).getNodes();
            return nodeIterator.getSize();
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get list size", e);
        }
    }

    public static boolean isMapped(Class entityClass) {
        return jcrom.isMapped(entityClass);
    }

    public static <T> T loadByUUID(Class<T> clazz, String uuid) {
        return loadByUUID(clazz, uuid, "*", -1);
    }

    public static <T> T loadByUUID(Class<T> clazz, String uuid, String childNodeFilter, int maxDepth) {
        Node node;
        try {
            node = getSession().getNodeByIdentifier(uuid);
        } catch (RepositoryException e) {
            // throw new JcrMappingException("Could not load node", e);
            return null;
        }
        return (T) jcrom.fromNode(clazz, node, childNodeFilter, maxDepth);
    }

    public static <T extends Model> T loadByUUID(String className, String uuid) {
        return (T) loadByUUID(Play.classloader.getClassIgnoreCase(className), uuid);
    }

    public static void map(Class classToMap) {
        jcrom.map(classToMap);
    }

    public static <T extends Model> T merge(T entity) {
        return merge(entity, "*", -1);
    }

    public static <T extends Model> T merge(T entity, String childNodeFilter, int maxDepth) {
        T original = (T) loadByUUID(entity.getClass(), entity.uuid);
        try {
            beanUtils.copyProperties(original, entity);
        } catch (IllegalAccessException e) {
            throw new UnexpectedException(e);
        } catch (InvocationTargetException e) {
            throw new UnexpectedException(e);
        }
        return update(original, childNodeFilter, maxDepth);
    }

    public static <T> T move(T entity, String newParentPath) {
        // if this is a versionable node, then we need to check out both
        // the old parent and the new parent before moving the node
        try {
            String sourcePath = jcrom.getPath(entity);
            String entityName = jcrom.getName(entity);
            Node oldParent = null;
            Node newParent = null;
            MD md = getMetadata(entity.getClass());
            Session session = getSession();
            VersionManager versionManager = null;

            if (md.isVersionable) {
                versionManager = session.getWorkspace().getVersionManager();
                oldParent = session.getRootNode().getNode(relativePath(sourcePath)).getParent();
                newParent = newParentPath.equals("/") ? session.getRootNode() : session.getRootNode().getNode(
                        relativePath(newParentPath));

                if (hasMixinType(oldParent, "mix:versionable")) {
                    versionManager.checkout(oldParent.getPath());
                }
                if (hasMixinType(newParent, "mix:versionable")) {
                    versionManager.checkout(newParent.getPath());
                }
            }

            if (newParentPath.equals("/")) {
                // special case, moving to root
                session.move(sourcePath, newParentPath + entityName);
            } else {
                session.move(sourcePath, newParentPath + "/" + entityName);
            }
            session.save();

            if (md.isVersionable) {
                if (hasMixinType(oldParent, "mix:versionable") && oldParent.isCheckedOut()) {
                    versionManager.checkin(oldParent.getPath());
                }
                if (hasMixinType(newParent, "mix:versionable") && newParent.isCheckedOut()) {
                    versionManager.checkin(newParent.getPath());
                }
            }

        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not move node", e);
        }

        return entity;
    }

    public static void remove(Model model) {
        remove(jcrom.getPath(model), model.getClass());
    }

    public static void remove(String path, Class<?> clazz) {
        try {
            Node parent = null;
            MD md = getMetadata(clazz);
            Session session = getSession();
            VersionManager versionManager = null;
            if (md.isVersionable) {
                versionManager = getSession().getWorkspace().getVersionManager();
                parent = session.getRootNode().getNode(relativePath(path)).getParent();
                if (hasMixinType(parent, "mix:versionable")) {
                    versionManager.checkout(parent.getPath());
                }
            }

            session.getRootNode().getNode(relativePath(path)).remove();
            session.save();

            if (md.isVersionable) {
                if (hasMixinType(parent, "mix:versionable") && parent.isCheckedOut()) {
                    versionManager.checkin(parent.getPath());
                }
            }

        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not remove node", e);
        }
    }

    public static void removeByUUID(String uuid, Class<?> clazz) {
        try {
            Session session = getSession();
            Node node = session.getNodeByIdentifier(uuid);
            Node parent = null;
            MD md = getMetadata(clazz);
            VersionManager versionManager = null;
            if (md.isVersionable) {
                versionManager = session.getWorkspace().getVersionManager();
                parent = node.getParent();
                if (hasMixinType(parent, "mix:versionable")) {
                    versionManager.checkout(parent.getPath());
                }
            }

            node.remove();
            session.save();

            if (md.isVersionable) {
                if (hasMixinType(parent, "mix:versionable") && parent.isCheckedOut()) {
                    versionManager.checkin(parent.getPath());
                }
            }

        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not remove node", e);
        }
    }

    public static void setBaseVersionInfo(Object object, String name, Calendar created) {
        jcrom.setBaseVersionInfo(object, name, created);
    }

    /**
     * Maps JCR nodes to a List of JcrEntity implementations.
     * 
     * @param nodeIterator
     *            the iterator pointing to the nodes
     * @param childNameFilter
     *            comma separated list of names of child nodes to load ("*"
     *            loads all, "none" loads no children, and "-" at the beginning
     *            makes it an exclusion filter)
     * @param maxDepth
     *            the maximum depth of loaded child nodes (0 means no child
     *            nodes are loaded, while a negative value means that no
     *            restrictions are set on the depth).
     * @return a list of objects mapped from the nodes
     */
    public static <T> List<T> toList(Class<?> clazz, NodeIterator nodeIterator, String childNameFilter, int maxDepth) {
        List<T> objects = new ArrayList<T>();
        while (nodeIterator.hasNext()) {
            objects.add((T) jcrom.fromNode(clazz, nodeIterator.nextNode(), childNameFilter, maxDepth));
        }
        return objects;
    }

    /**
     * Maps JCR nodes to a List of JcrEntity implementations.
     * 
     * @param nodeIterator
     *            the iterator pointing to the nodes
     * @param childNameFilter
     *            comma separated list of names of child nodes to load ("*"
     *            loads all, "none" loads no children, and "-" at the beginning
     *            makes it an exclusion filter)
     * @param maxDepth
     *            the maximum depth of loaded child nodes (0 means no child
     *            nodes are loaded, while a negative value means that no
     *            restrictions are set on the depth).
     * @param resultSize
     *            the number of items to retrieve from the iterator
     * @return a list of objects mapped from the nodes
     */
    public static <T> List<T> toList(Class<?> clazz, NodeIterator nodeIterator, String childNameFilter, int maxDepth,
            long resultSize) {
        List<T> objects = new ArrayList<T>();
        long counter = 0;
        while (nodeIterator.hasNext()) {
            if (counter == resultSize) {
                break;
            }
            objects.add((T) jcrom.fromNode(clazz, nodeIterator.nextNode(), childNameFilter, maxDepth));
            counter++;
        }
        return objects;
    }

    public static <T> T update(T entity) {
        return update(entity, "*", -1);
    }

    public static <T> T update(T entity, String childNodeFilter, int maxDepth) {
        Node node;
        try {
            node = getSession().getRootNode().getNode(relativePath(jcrom.getPath(entity)));
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not update node", e);
        }
        return update(node, entity, childNodeFilter, maxDepth);
    }

    public static <T> T updateByUUID(T entity, String uuid) {
        return updateByUUID(entity, uuid, "*", -1);
    }

    public static <T> T updateByUUID(T entity, String uuid, String childNodeFilter, int maxDepth) {
        Node node;
        try {
            node = getSession().getNodeByIdentifier(uuid);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not update node", e);
        }
        return update(node, entity, childNodeFilter, maxDepth);
    }

    public static String updateNode(Node node, Object entity) throws JcrMappingException {
        return jcrom.updateNode(node, entity);
    }

    public static String updateNode(Node node, Object entity, String arg2, int arg3) throws JcrMappingException {
        return jcrom.updateNode(node, entity, arg2, arg3);
    }

    // TODO Sanitize input or implement some sort of PreparedStatement
    protected static <T> JcrQuery<T> executeQuery(Class<T> clazz, String queryString, Object... params)
            throws RepositoryException {
        QueryManager queryMan = getQueryManager();
        Query query = queryMan.createQuery(String.format(queryString, params), Query.JCR_SQL2);
        QueryResult queryResult = query.execute();
        NodeIterator nodeItor = queryResult.getNodes();
        return toJcrQuery(clazz, nodeItor);
    }

    protected static MD getMetadata(Class<?> clazz) {
        return JcrMetadata.getInstance().lookup(clazz);
    }

    protected static Session getSession() {
        return play.modules.cream.JcrPlugin.getCurrentSession();
    }

    protected static boolean hasMixinType(Node node, String mixinType) throws RepositoryException {
        for (NodeType nodeType : node.getMixinNodeTypes()) {
            if (nodeType.getName().equals(mixinType)) {
                return true;
            }
        }
        return false;
    }

    protected static String relativePath(String absolutePath) {
        if (absolutePath.startsWith("/")) {
            return absolutePath.substring(1);
        } else {
            return absolutePath;
        }
    }

    protected static <T> JcrQuery<T> toJcrQuery(final Class<T> clazz, NodeIterator nodeIterator) {
        return new JcrQuery<T>(clazz, nodeIterator);
    }

    protected static <T> T update(Node node, T entity, String childNodeFilter, int maxDepth) {
        try {
            Session session = getSession();
            VersionManager versionManager = null;
            MD md = getMetadata(entity.getClass());
            if (md.isVersionable) {
                versionManager = session.getWorkspace().getVersionManager();
                checkoutRecursively(versionManager, node);
            }
            jcrom.updateNode(node, entity, childNodeFilter, maxDepth);
            session.save();
            if (md.isVersionable) {
                checkinRecursively(versionManager, node);
            }
            return entity;
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not update node", e);
        }
    }

    private static void checkinRecursively(VersionManager versionManager, Node node) {
        try {
            NodeIterator it = node.getNodes();
            while (it.hasNext()) {
                checkinRecursively(versionManager, it.nextNode());
            }
            if (node.isCheckedOut() && node.isNodeType("mix:versionable")) {
                versionManager.checkin(node.getPath());
            }

        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not perform check-in", e);
        }
    }

    private static void checkoutRecursively(VersionManager versionManager, Node node) {
        try {
            NodeIterator it = node.getNodes();
            while (it.hasNext()) {
                checkoutRecursively(versionManager, it.nextNode());
            }
            if (!node.isCheckedOut() && node.isNodeType("mix:versionable")) {
                versionManager.checkout(node.getPath());
            }

        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not perform check-in", e);
        }
    }

    private JcrMapper() {

    }

}
