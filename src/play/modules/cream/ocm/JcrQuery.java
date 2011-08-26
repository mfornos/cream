package play.modules.cream.ocm;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;

import play.exceptions.UnexpectedException;
import play.modules.cream.helpers.JcrUtils;

public class JcrQuery {

    public static class JcrQueryBuilder {
        private final String query;
        private final Class<?> clazz;

        private Map<String, Object> params = new HashMap<String, Object>();

        public JcrQueryBuilder(Class<?> clazz, String query) {
            this.query = query;
            this.clazz = clazz;
        }

        public JcrQuery build() {
            return new JcrQuery(this);
        }

        public JcrQueryBuilder setBoolean(String name, boolean value) {
            put(name, value);
            return this;
        }

        public JcrQueryBuilder setDouble(String name, double value) {
            put(name, value);
            return this;
        }

        public JcrQueryBuilder setFloat(String name, float value) {
            put(name, value);
            return this;
        }

        public JcrQueryBuilder setInt(String name, int value) {
            put(name, value);
            return this;
        }

        public JcrQueryBuilder setLong(String name, long value) {
            put(name, value);
            return this;
        }

        public JcrQueryBuilder setNumber(String name, Number value) {
            put(name, value);
            return this;
        }

        public JcrQueryBuilder setObject(String name, Object value) {
            put(name, value);
            return this;
        }

        public JcrQueryBuilder setString(String name, String value) {
            if (value != null) {
                this.params.put(name, '\'' + JcrUtils.escapeSingleQuote(value) + '\'');
            }
            return this;
        }

        private void put(String name, Object value) {
            if (value != null) {
                params.put(name, value);
            }
        }
    }

    public static JcrQueryBuilder builder(Class<?> clazz, String query) {
        return new JcrQueryBuilder(clazz, query);
    }

    public static JcrQueryBuilder builder(String query) {
        return new JcrQueryBuilder(null, query);
    }

    private final String formattedQuery;

    private final Class<?> clazz;

    private JcrQuery(JcrQueryBuilder builder) {
        this.formattedQuery = format(builder.query, builder.params);
        this.clazz = builder.clazz;
    }

    public <T> JcrQueryResult<T> excute() {
        try {
            return (JcrQueryResult<T>) JcrMapper.executeQuery(clazz, this.formattedQuery);
        } catch (RepositoryException e) {
            throw new UnexpectedException(e);
        }
    }

    public String format(String query, Map<String, Object> params) {
        StringBuilder builder = new StringBuilder(query);

        for (Entry<String, Object> entry : params.entrySet()) {
            int start;
            String pattern = String.format("${%s}", entry.getKey());
            String value = entry.getValue().toString();

            // Replace every occurence of ${key} with value
            while ((start = builder.indexOf(pattern)) != -1) {
                builder.replace(start, start + pattern.length(), value);
            }
        }

        return builder.toString();
    }

    public String getQuery() {
        return this.formattedQuery;
    }

    @Override
    public String toString() {
        return "JcrQuery [query=" + formattedQuery + ", clazz=" + clazz + "]";
    }
}
