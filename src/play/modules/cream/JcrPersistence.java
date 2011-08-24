package play.modules.cream;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.jcrom.JcrMappingException;
import org.jcrom.Jcrom;

import play.Play;
import play.exceptions.UnexpectedException;
import play.modules.cream.JcrMetadata.MD;
import play.modules.cream.helpers.NullAwareBeanUtilsBean;

// TODO Refactor: extract JcrVersionManager, JcrQueryManager or something similar
// TODO replace some strategic exceptions with return null
public class JcrPersistence {

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
        Workspace workspace = JcrPlugin.getCurrentSession().getWorkspace();
        QueryManager queryMan = workspace.getQueryManager();
        return queryMan;
    }

    public static long getSize(String rootPath) {
        try {
            NodeIterator nodeIterator = getSession().getRootNode().getNode(relativePath(rootPath)).getNodes();
            return nodeIterator.getSize();
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get list size", e);
        }
    }

    public static <T> T getVersion(Class<T> clazz, String path, String versionName) {
        return getVersion(clazz, path, versionName, "*", -1);
    }

    public static <T> T getVersion(Class<T> clazz, String path, String versionName, String childNodeFilter, int maxDepth) {
        try {
            return getVersion(clazz, getSession().getRootNode().getNode(relativePath(path)), versionName,
                    childNodeFilter, maxDepth);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version", e);
        }
    }

    public static <T> T getVersionByUUID(Class<T> clazz, String uuid, String versionName) {
        return getVersionByUUID(clazz, uuid, versionName, "*", -1);
    }

    public static <T> T getVersionByUUID(Class<T> clazz, String uuid, String versionName, String childNodeFilter,
            int maxDepth) {
        try {
            return getVersion(clazz, getSession().getNodeByIdentifier(uuid), versionName, childNodeFilter, maxDepth);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version", e);
        }
    }

    public static <T> List<T> getVersionList(Class<?> clazz, String path) {
        try {
            return getVersionList(clazz, getSession().getRootNode().getNode(relativePath(path)), "*", -1);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version list", e);
        }
    }

    public static <T> List<T> getVersionList(Class<?> clazz, String path, String childNameFilter, int maxDepth) {
        try {
            return getVersionList(clazz, getSession().getRootNode().getNode(relativePath(path)), childNameFilter,
                    maxDepth);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version list", e);
        }
    }

    public static <T> List<T> getVersionList(Class<?> clazz, String path, String childNameFilter, int maxDepth,
            long startIndex, long resultSize) {
        try {
            return getVersionList(clazz, getSession().getRootNode().getNode(relativePath(path)), childNameFilter,
                    maxDepth, startIndex, resultSize);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version list", e);
        }
    }

    public static <T> List<T> getVersionListByUUID(Class<?> clazz, String uuid) {
        try {
            return getVersionList(clazz, getSession().getNodeByIdentifier(uuid), "*", -1);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version list", e);
        }
    }

    public static <T> List<T> getVersionListByUUID(Class<?> clazz, String uuid, String childNameFilter, int maxDepth) {
        try {
            return getVersionList(clazz, getSession().getNodeByIdentifier(uuid), childNameFilter, maxDepth);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version list", e);
        }
    }

    public static <T> List<T> getVersionListByUUID(Class<?> clazz, String uuid, String childNameFilter, int maxDepth,
            long startIndex, long resultSize) {
        try {
            return getVersionList(clazz, getSession().getNodeByIdentifier(uuid), childNameFilter, maxDepth, startIndex,
                    resultSize);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version list", e);
        }
    }

    public static long getVersionSize(String path) {
        try {
            return getVersionSize(getSession().getRootNode().getNode(relativePath(path)));
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version history size", e);
        }
    }

    public static long getVersionSizeByUUID(String uuid) {
        try {
            return getVersionSize(getSession().getNodeByIdentifier(uuid));
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version history size", e);
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

    public static void removeVersion(String path, String versionName) {
        try {
            removeVersion(getSession().getRootNode().getNode(relativePath(path)), versionName);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not remove version", e);
        }
    }

    public static void removeVersionByUUID(String uuid, String versionName) {
        try {
            removeVersion(getSession().getNodeByIdentifier(uuid), versionName);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not remove version", e);
        }
    }

    public static void restoreVersion(String path, String versionName) {
        restoreVersion(path, versionName, true);
    }

    public static void restoreVersion(String path, String versionName, boolean removeExisting) {
        try {
            restoreVersion(getSession().getRootNode().getNode(relativePath(path)), versionName, removeExisting);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not restore version", e);
        }
    }

    public static void restoreVersionByUUID(String uuid, String versionName) {
        restoreVersionByUUID(uuid, versionName, true);
    }

    public static void restoreVersionByUUID(String uuid, String versionName, boolean removeExisting) {
        try {
            restoreVersion(getSession().getNodeByIdentifier(uuid), versionName, removeExisting);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not restore version", e);
        }
    }

    public static void setBaseVersionInfo(Object arg0, String arg1, Calendar arg2) throws JcrMappingException {
        jcrom.setBaseVersionInfo(arg0, arg1, arg2);
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

    protected static <T> T getVersion(Class<T> clazz, Node node, String versionName, String childNodeFilter,
            int maxDepth) {
        try {
            VersionManager versionManager = getSession().getWorkspace().getVersionManager();
            VersionHistory versionHistory = versionManager.getVersionHistory(node.getPath());
            Version version = versionHistory.getVersion(versionName);
            return (T) jcrom.fromNode(clazz, version.getNodes().nextNode(), childNodeFilter, maxDepth);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version", e);
        }
    }

    protected static <T> List<T> getVersionList(Class<?> clazz, Node node, String childNameFilter, int maxDepth) {
        try {
            List<T> versionList = new ArrayList<T>();
            VersionManager versionManager = getSession().getWorkspace().getVersionManager();
            VersionHistory versionHistory = versionManager.getVersionHistory(node.getPath());
            VersionIterator versionIterator = versionHistory.getAllVersions();
            versionIterator.skip(1);
            while (versionIterator.hasNext()) {
                Version version = versionIterator.nextVersion();
                NodeIterator nodeIterator = version.getNodes();
                while (nodeIterator.hasNext()) {
                    T entityVersion = (T) jcrom.fromNode(clazz, nodeIterator.nextNode(), childNameFilter, maxDepth);
                    Version baseVersion = versionManager.getBaseVersion(node.getPath());
                    jcrom.setBaseVersionInfo(entityVersion, baseVersion.getName(), baseVersion.getCreated());
                    versionList.add(entityVersion);
                }
            }
            return versionList;
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version list", e);
        }
    }

    protected static <T> List<T> getVersionList(Class<?> clazz, Node node, String childNameFilter, int maxDepth,
            long startIndex, long resultSize) {
        try {
            List<T> versionList = new ArrayList<T>();
            VersionManager versionManager = getSession().getWorkspace().getVersionManager();
            VersionHistory versionHistory = versionManager.getVersionHistory(node.getPath());
            VersionIterator versionIterator = versionHistory.getAllVersions();
            versionIterator.skip(1 + startIndex);

            long counter = 0;
            while (versionIterator.hasNext()) {
                if (counter == resultSize) {
                    break;
                }
                Version version = versionIterator.nextVersion();
                NodeIterator nodeIterator = version.getNodes();
                while (nodeIterator.hasNext()) {
                    versionList.add((T) jcrom.fromNode(clazz, nodeIterator.nextNode(), childNameFilter, maxDepth));
                }
                counter++;
            }
            return versionList;
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version list", e);
        }
    }

    protected static long getVersionSize(Node node) {
        try {
            VersionManager versionManager = getSession().getWorkspace().getVersionManager();
            VersionHistory versionHistory = versionManager.getVersionHistory(node.getPath());
            return versionHistory.getAllVersions().getSize() - 1;
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version history size", e);
        }
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

    protected static void removeVersion(Node node, String versionName) {
        try {
            VersionManager versionManager = getSession().getWorkspace().getVersionManager();
            versionManager.getVersionHistory(node.getPath()).removeVersion(versionName);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not remove version", e);
        }
    }

    protected static void restoreVersion(Node node, String versionName, boolean removeExisting) {
        try {
            VersionManager versionManager = getSession().getWorkspace().getVersionManager();
            versionManager.checkout(node.getPath());
            versionManager.restore(node.getPath(), versionName, removeExisting);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not restore version", e);
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

}
