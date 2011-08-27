package play.modules.cream.helpers;

import org.apache.commons.lang.StringUtils;

public class JcrUtils {
    public static String buildSelect(final String path, final String where, String nodeType) {
        StringBuilder queryString = new StringBuilder("select * from ");
        queryString.append('[');
        queryString.append(nodeType);
        queryString.append(']');
        boolean hasPath = StringUtils.isNotBlank(path);
        boolean hasWhere = StringUtils.isNotBlank(where);
        if (hasPath || hasWhere) {
            queryString.append(" where ");
            if (hasPath) {
                queryString.append("ISCHILDNODE('");
                queryString.append(JcrUtils.escapeSingleQuote(path));
                queryString.append("')");
            }
            if (hasPath && hasWhere && !where.toLowerCase().startsWith("order by")) {
                queryString.append(" and ");
            }
            queryString.append(where);
        }
        return queryString.toString();
    }

    /**
     * Escapes characters that expects to be escaped by a preceding '{@code \}'
     * or for quote like characters by the character itself.
     * <p/>
     * According to 6.6.4.9 of the * JCR-170 specification, the apostrophe (')
     * and quotation mark(") must be escaped according to the standard rules of
     * XPath with regard to string literals: If the literal is delimited by
     * apostrophes, two adjacent apostrophes within the literal are interpreted
     * as a single apostrophe. Similarly, if the literal is delimited by
     * quotation marks, two adjacent quotation marks within the literal are
     * interpreted as one quotation mark.
     * 
     * @param keywords
     *            the string to escape
     * @return a String where characters that QueryParser expects to be escaped,
     *         are escaped by a preceding ' {@code \}' or for quote like
     *         characters by itself
     */
    public static String escapeSingleQuote(String keywords) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keywords.length(); i++) {
            char c = keywords.charAt(i);
            if (c == '\'') {
                sb.append('\\');
                sb.append(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static String queryFormat(String queryString, Object[] params) {
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof String) {
                params[i] = "'" + escapeSingleQuote((String) params[i]) + "'";
            }
        }
        return String.format(queryString, params);
    }
}
