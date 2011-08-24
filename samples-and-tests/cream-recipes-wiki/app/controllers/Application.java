package controllers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import models.Recipe;
import models.Recipe.AccessLevel;
import models.User;

import org.apache.commons.lang.StringUtils;
import org.jcrom.JcrFile;

import play.Logger;
import play.Play;
import play.data.validation.Email;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.libs.MimeTypes;
import play.modules.cream.JcrPersistence;
import play.modules.cream.JcrQuery;
import play.modules.cream.annotations.JcrSession;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

@JcrSession
@With(Secure.class)
public class Application extends Controller {

    // If you need direct access to jcrSession
    // inject it and remove @JcrSession class annotation!
    // @Inject
    // static Session jcrSession;

    public static final String RECIPES_PATH = "/recipes";
    static Integer pageSize = Integer.parseInt(Play.configuration.getProperty("tables.pageSize", "5"));

    public static void add(Recipe recipe) {
        recipe = (recipe == null) ? new Recipe() : recipe;
        AccessLevel[] accessLevels = AccessLevel.values();
        render(recipe, accessLevels);
    }

    public static void authenticate(@Required @Email String email, @Required String password) {
        if (validation.hasErrors()) {
            validation.keep();
            params.flash();
            flash.error("Please correct these errors !");
            login();
        }

        User user = null;
        user = User.findByEmail(email).first();

        if (user == null || !user.checkPassword(password)) {
            flash.error("Bad email or bad password");
            flash.put("email", email);
            login();
        }
        Secure.connect(user);
        flash.success("Welcome back %s !", user.name);
        index(1);
    }

    @Secure.Authenticated
    public static void create(@Valid Recipe recipe, File image) {
        if (validation.hasErrors()) {
            validation.keep();
            flash.error("Please correct these errors !");
            add(recipe);
        }
        if (image != null) {
            addImageToRecipe(recipe, image);
        }
        recipe.name = recipe.title;
        recipe.path = RECIPES_PATH;
        recipe.author = Secure.connectedUser();
        recipe.create();
        index(1);
    }

    @Secure.Authenticated
    public static void delete(String id) {
        Recipe recipe = getRecipeAndCheck(id);
        recipe.delete();
        flash.success("Deleted %s", recipe.title);
        index(1);
    }

    public static void edit(String id) {
        Recipe recipe = getRecipeAndCheck(id);
        AccessLevel[] accessLevels = AccessLevel.values();
        render(recipe, accessLevels);
    }

    public static void getImage(String id) {
        Recipe recipe = Recipe.findById(id);
        notFoundIfNull(recipe);
        response.setContentTypeIfNotSet(recipe.image.getMimeType());
        renderBinary(recipe.image.getDataProvider().getInputStream());
    }

    public static void index(Integer page) {
        page = (page != null && page > 0) ? page : 1;
        JcrQuery result = Recipe.find(
                "select * from [nt:unstructured] where ISDESCENDANTNODE('%s') order by [jcr:created] desc",
                RECIPES_PATH);
        long nbRecipes = result.count();
        List<Recipe> recipes = result.fetch(page, pageSize);
        render(nbRecipes, recipes, page);
    }

    public static void login() {
        render();
    }

    @Secure.Authenticated
    public static void logout() {
        flash.success("You've been logged out");
        session.clear();
        index(1);
    }

    public static void search(String search) {
        // Naive sanitize
        search = search.replaceAll("[^\\w s]", "");

        List<Recipe> recipes = new ArrayList<Recipe>();
        if (StringUtils.isNotBlank(search)) {
            // XXX see
            // http://jackrabbit.510166.n4.nabble.com/Use-of-excerpt-with-SQL2-td3249018.html
            // waiting for excerpt support with SQL-2
            try {
                QueryManager qm = JcrPersistence.getQueryManager();
                @SuppressWarnings("deprecation")
                Query q = qm.createQuery(
                        "select excerpt(.) from nt:unstructured where jcr:path like '/recipes/%' and contains(., '"
                                + search + "') order by jcr:score desc", Query.SQL);
                QueryResult result = q.execute();
                for (RowIterator it = result.getRows(); it.hasNext();) {
                    Row r = it.nextRow();
                    Value excerpt = r.getValue("rep:excerpt(.)");
                    Recipe recipe = JcrPersistence.fromNode(Recipe.class, r.getNode());
                    recipe.description = excerpt.getString().replaceAll("&lt;.*?&gt;", "");
                    recipes.add(recipe);
                }

            } catch (RepositoryException e) {
                Logger.error(e.getMessage(), e);
            }
        }

        render(recipes, search);
    }

    public static void show(String id) {
        Recipe recipe = Recipe.findById(id);
        notFoundIfNull(recipe);
        boolean editable = Secure.checkRecipeAccess(recipe);
        render(recipe, editable);
    }

    public static void update(@Valid Recipe recipe, File image) {
        Secure.checkRecipeAccess(recipe);
        String uuid = recipe.uuid;
        if (validation.hasErrors()) {
            validation.keep();
            flash.error("Please correct these errors !");
            edit(uuid);
        }

        int depth = -1;
        if (image == null) {
            depth = 0;
        } else {
            addImageToRecipe(recipe, image);
        }

        recipe.merge("*", depth);
        show(uuid);
    }

    @Before
    static void globals() {
        renderArgs.put("pageSize", pageSize);
    }

    private static void addImageToRecipe(Recipe recipe, File image) {
        recipe.image = JcrFile.fromFile("picture", image, MimeTypes.getContentType(image.getName()));
    }

    private static Recipe getRecipeAndCheck(String id) {
        Recipe recipe = Recipe.findById(id);
        notFoundIfNull(recipe);
        Secure.checkRecipeAccess(recipe);
        return recipe;
    }
}
