import javax.jcr.RepositoryException;

import models.Company;
import models.Restaurant;

import org.junit.Before;
import org.junit.Test;

import play.modules.cream.JcrQuery;
import play.modules.cream.annotations.JcrSession;
import play.test.Fixtures;
import play.test.UnitTest;

@JcrSession
public class ModelTest extends UnitTest {

    @Before
    public void setup() {
        Fixtures.deleteAllModels();
    }

    @Test
    public void testModel() throws RepositoryException {
        Fixtures.loadModels("data.yml");
        // Session session = play.modules.cream.JcrPlugin.getCurrentSession();
        // Dumper.dumpTree(session.getRootNode());
        JcrQuery<Restaurant> restaurants = Restaurant.all("/restaurant");
        assertEquals(2, restaurants.count());
        assertEquals("petit", restaurants.first().chef.name);

        Company company = Company.get("/company/flashlight");
        assertNotNull(company);
        assertEquals(2, company.restaurants.size());
    }

}
