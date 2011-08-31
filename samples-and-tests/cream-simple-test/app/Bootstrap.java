import javax.jcr.RepositoryException;
import javax.jcr.Session;

import play.exceptions.UnexpectedException;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.modules.cream.helpers.JcrRepositoryHelper;
import play.test.Fixtures;

@OnApplicationStart
public class Bootstrap extends Job {

    public void doJob() {
        Session session = JcrRepositoryHelper.openSession();
        try {
            Fixtures.deleteAllModels();
            Fixtures.loadModels("initial-data.yml");
        } finally {
            session.logout();
        }
    }

}
