import javax.jcr.RepositoryException;
import javax.jcr.Session;

import play.exceptions.UnexpectedException;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.modules.cream.helpers.JcrRepositoryHelper;
import play.test.Fixtures;
import controllers.Application;

@OnApplicationStart
public class Bootstrap extends Job {

    // XXX wire the session in Jobs?
    // @Inject
    // private Session session;

    public void doJob() {
        Session session = JcrRepositoryHelper.openSession();
        try {
            if (!session.nodeExists(Application.RECIPES_PATH)) {
                Fixtures.loadModels("initial-data.yml");
            }
        } catch (RepositoryException e) {
            throw new UnexpectedException(e);
        } finally {
            session.logout();
        }
    }
}
