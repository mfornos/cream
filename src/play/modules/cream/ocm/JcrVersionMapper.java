package play.modules.cream.ocm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.jcrom.JcrMappingException;

import play.modules.cream.JCR;

public class JcrVersionMapper {
    public static <T> T getVersion(Class<T> clazz, String path, String versionName) {
        return getVersion(clazz, path, versionName, "*", -1);
    }

    public static <T> T getVersion(Class<T> clazz, String path, String versionName, String childNodeFilter, int maxDepth) {
        try {
            return getVersion(clazz, getSession().getRootNode().getNode(JcrMapper.relativePath(path)), versionName,
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
            return getVersionList(clazz, getSession().getRootNode().getNode(JcrMapper.relativePath(path)), "*", -1);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version list", e);
        }
    }

    public static <T> List<T> getVersionList(Class<?> clazz, String path, String childNameFilter, int maxDepth) {
        try {
            return getVersionList(clazz, getSession().getRootNode().getNode(JcrMapper.relativePath(path)),
                    childNameFilter, maxDepth);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version list", e);
        }
    }

    public static <T> List<T> getVersionList(Class<?> clazz, String path, String childNameFilter, int maxDepth,
            long startIndex, long resultSize) {
        try {
            return getVersionList(clazz, getSession().getRootNode().getNode(JcrMapper.relativePath(path)),
                    childNameFilter, maxDepth, startIndex, resultSize);
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
            return getVersionSize(getSession().getRootNode().getNode(JcrMapper.relativePath(path)));
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

    public static void removeVersion(String path, String versionName) {
        try {
            removeVersion(getSession().getRootNode().getNode(JcrMapper.relativePath(path)), versionName);
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
            restoreVersion(getSession().getRootNode().getNode(JcrMapper.relativePath(path)), versionName,
                    removeExisting);
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

    public static void setBaseVersionInfo(Object object, String name, Calendar created) throws JcrMappingException {
        JcrMapper.setBaseVersionInfo(object, name, created);
    }

    protected static Session getSession() {
        return JCR.getSession();
    }

    protected static <T> T getVersion(Class<T> clazz, Node node, String versionName, String childNodeFilter,
            int maxDepth) {
        try {
            VersionManager versionManager = JCR.getVersionManager();
            VersionHistory versionHistory = versionManager.getVersionHistory(node.getPath());
            Version version = versionHistory.getVersion(versionName);
            return (T) JcrMapper.fromNode(clazz, version.getNodes().nextNode(), childNodeFilter, maxDepth);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version", e);
        }
    }

    protected static <T> List<T> getVersionList(Class<?> clazz, Node node, String childNameFilter, int maxDepth) {
        try {
            List<T> versionList = new ArrayList<T>();
            VersionManager versionManager = JCR.getVersionManager();
            VersionHistory versionHistory = versionManager.getVersionHistory(node.getPath());
            VersionIterator versionIterator = versionHistory.getAllVersions();
            versionIterator.skip(1);
            while (versionIterator.hasNext()) {
                Version version = versionIterator.nextVersion();
                NodeIterator nodeIterator = version.getNodes();
                while (nodeIterator.hasNext()) {
                    T entityVersion = (T) JcrMapper.fromNode(clazz, nodeIterator.nextNode(), childNameFilter, maxDepth);
                    Version baseVersion = versionManager.getBaseVersion(node.getPath());
                    JcrMapper.setBaseVersionInfo(entityVersion, baseVersion.getName(), baseVersion.getCreated());
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
            VersionManager versionManager = JCR.getVersionManager();
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
                    versionList.add((T) JcrMapper.fromNode(clazz, nodeIterator.nextNode(), childNameFilter, maxDepth));
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
            VersionManager versionManager = JCR.getVersionManager();
            VersionHistory versionHistory = versionManager.getVersionHistory(node.getPath());
            return versionHistory.getAllVersions().getSize() - 1;
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not get version history size", e);
        }
    }

    protected static void removeVersion(Node node, String versionName) {
        try {
            VersionManager versionManager = JCR.getVersionManager();
            versionManager.getVersionHistory(node.getPath()).removeVersion(versionName);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not remove version", e);
        }
    }

    protected static void restoreVersion(Node node, String versionName, boolean removeExisting) {
        try {
            VersionManager versionManager = JCR.getVersionManager();
            versionManager.checkout(node.getPath());
            versionManager.restore(node.getPath(), versionName, removeExisting);
        } catch (RepositoryException e) {
            throw new JcrMappingException("Could not restore version", e);
        }
    }

    private JcrVersionMapper() {

    }
}
