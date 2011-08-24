import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.junit.Test;

import play.modules.cream.annotations.NoJcrSession;
import play.modules.cream.helpers.JcrRepositoryHelper;
import play.test.UnitTest;

@NoJcrSession
public class RepositoryTest extends UnitTest {

    private static final String ANONYMOUS = "anonymous";

    @Test
    public void testSession() {
        SimpleCredentials credentials = new SimpleCredentials(ANONYMOUS, "".toCharArray());
        Session session = JcrRepositoryHelper.openSession(credentials);
        try {
            String user = session.getUserID();
            String name = JcrRepositoryHelper.repository.getDescriptor(Repository.REP_NAME_DESC);
            assertEquals(ANONYMOUS, user);
            assertEquals("Jackrabbit", name);
        } finally {
            session.logout();
        }
    }

}
