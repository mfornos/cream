import org.junit.Test;

import play.modules.cream.annotations.NoJcrSession;
import play.modules.cream.ocm.JcrQuery;
import play.test.UnitTest;

@NoJcrSession
public class QueryBuilderTest extends UnitTest {
    @Test
    public void testQuery() {
        JcrQuery q = JcrQuery
                .builder(
                        "select * from [nt:unstructured] where ISDESCENDANTNODE(${path}) AND (email = ${email} AND size = ${size}) OR (email != ${email})")
                .setString("path", "/path").setString("email", "test@email.com").setLong("size", 10).build();
        assertEquals(
                "select * from [nt:unstructured] where ISDESCENDANTNODE('/path') AND (email = 'test@email.com' AND size = 10) OR (email != 'test@email.com')",
                q.getQuery());
    }

    @Test
    public void testBadChars() {
        JcrQuery q = JcrQuery.builder("select * from test where name = ${name}").setString("name", "' inject or ...")
                .build();
        assertEquals("select * from test where name = '\\'' inject or ...'", q.getQuery());
    }
}
