import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import play.Logger;

public class Dumper {

    public static void dumpTree(Node node) {
        NodeIterator itor;
        try {
            PropertyIterator pitor = node.getProperties();
            while (pitor.hasNext()) {
                Property p = pitor.nextProperty();
                try {
                    if (!node.getPath().startsWith("/jcr:system")) {
                        Logger.info("[%s] %s = %s", node.getPath(), p.getName(), p.getValue());
                    }
                } catch (Exception e) {

                }
            }
            itor = node.getNodes();
            if (itor.getSize() > 0) {
                while (itor.hasNext()) {
                    dumpTree(itor.nextNode());
                }
            }
        } catch (PathNotFoundException e) {
            Logger.error(e, e.getMessage());
        } catch (RepositoryException e) {
            Logger.error(e, e.getMessage());
        }
    }

}
