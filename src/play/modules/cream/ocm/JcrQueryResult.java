package play.modules.cream.ocm;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.NodeIterator;

public class JcrQueryResult<T> {

    private final NodeIterator iterator;
    private final Class<T> entityClass;

    public JcrQueryResult(Class<T> clazz, NodeIterator nodeIterator) {
        this.iterator = nodeIterator;
        this.entityClass = clazz;
    }

    public long count() {
        return iterator.getSize();
    }

    /**
     * Retrieve all results of the query
     * 
     * @return A list of entities
     */
    public List<T> fetch() {
        return JcrMapper.toList(entityClass, iterator, "*", -1);
    }

    /**
     * Retrieve results of the query
     * 
     * @param max
     *            Max results to fetch
     * @return A list of entities
     */
    public List<T> fetch(int max) {

        // long count = iterator.getSize();

        int c = 0;
        List<T> list = new ArrayList<T>();
        while (iterator.hasNext() && c < max) {
            list.add(JcrMapper.fromNode(entityClass, iterator.nextNode()));
            c++;
        }
        return list;

    }

    /**
     * Retrieve a page of result
     * 
     * @param page
     *            Page number (start at 1)
     * @param length
     *            (page length)
     * @return a list of entities
     */
    public List<T> fetch(int page, int length) {
        List<T> list = new ArrayList<T>();
        if (!isEmpty()) {
            int offset = (Math.max(page, 1) - 1) * length;

            if (offset > 0) {
                iterator.skip(offset);
            }

            int c = 0;

            while (iterator.hasNext() && c < length) {
                list.add(JcrMapper.fromNode(entityClass, iterator.nextNode()));
                c++;
            }
        }
        return list;
    }

    public T first() {
        return (iterator.hasNext()) ? JcrMapper.fromNode(entityClass, iterator.nextNode()) : null;
    }

    /**
     * Set the position to start
     * 
     * @param position
     *            Position of the first element
     * @return A new query
     */
    public JcrQueryResult from(int position) {
        iterator.skip(position);
        return this;
    }

    public boolean isEmpty() {
        return iterator.getSize() < 1;
    }
}
