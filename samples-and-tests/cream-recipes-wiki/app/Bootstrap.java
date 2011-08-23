import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import models.Recipe;
import models.User;

import org.apache.jackrabbit.commons.JcrUtils;

import play.exceptions.UnexpectedException;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.libs.Crypto;
import play.modules.cream.JcrPersistence;
import play.modules.cream.helpers.JcrRepositoryHelper;

@OnApplicationStart
public class Bootstrap extends Job {

	// XXX wire the session in Jobs?
	// @Inject
	private Session session;

	// TODO load xml with xmlFixture?
	public void doJob() {
		session = JcrRepositoryHelper.openSession();
		try {
			Node root = session.getRootNode();
			if (!root.hasNode("recipes")) {
				Node recipes = JcrUtils.getOrAddNode(root, "recipes");
				Node users = JcrUtils.getOrAddNode(root, "users");
				session.save();
				populateData(users, recipes);
			}
		} catch (RepositoryException e) {
			throw new UnexpectedException(e);
		} finally {
			session.logout();
		}
	}

	private void populateData(Node users, Node recipes) throws AccessDeniedException, ItemExistsException,
			ReferentialIntegrityException, ConstraintViolationException, InvalidItemStateException, VersionException,
			LockException, NoSuchNodeTypeException, RepositoryException {
		User demo = new User();
		demo.name = "demo";
		demo.email = "demo@demo.info";
		demo.password = Crypto.passwordHash("demo");
		demo.admin = false;

		User admin = new User();
		admin.name = "admin";
		admin.email = "admin@demo.info";
		admin.password = Crypto.passwordHash("admin");
		admin.admin = true;

		Recipe recipe = new Recipe();
		recipe.name = "mom recipe";
		recipe.description = "This is my mother's recipe. Everyone, German or not, loves it. It is easy to double the recipe as well. I often bring it to potlucks, and we also have it at home on special occasions. There are usually no leftovers, but if there are, they don't last long!";
		recipe.title = "My mother's recipe";
		recipe.body = "bla bla bla";
		recipe.accessLevel = models.Recipe.AccessLevel.PUBLIC;

		recipe.author = demo;

		JcrPersistence.addNode(users, demo);
		JcrPersistence.addNode(users, admin);
		JcrPersistence.addNode(recipes, recipe);
		session.save();
	}
}
