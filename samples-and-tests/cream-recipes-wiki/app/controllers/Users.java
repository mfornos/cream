package controllers;

import java.util.Collection;
import java.util.List;

import models.Recipe;
import models.User;

import org.jcrom.JcrMappingException;

import play.data.validation.Required;
import play.data.validation.Valid;
import play.libs.Crypto;
import play.modules.cream.JcrQuery;

public class Users extends Application {

    private static final String USERS_PATH = "/users";

    @Secure.Admin
    public static void add(User user) {
        user = (user == null) ? new User() : user;
        render(user);
    }

    @Secure.Admin
    public static void create(@Valid User user, @Required String passwordConfirm) {
        if (!user.password.equals(passwordConfirm)) {
            validation.addError("passwordConfirm", "Passwords don't match!");
        }
        if (validation.hasErrors()) {
            validation.keep();
            params.flash();
            flash.error("Please correct these errors !");
            add(user);
        }
        user.path = USERS_PATH;
        user.password = Crypto.passwordHash(user.password);
        user.create();
        Users.index(1);
    }

    @Secure.Admin
    public static void delete(String id) {
        User user = loadUser(id);
        if (user.admin) {
            flash.error("The administrator cannot be deleted");
            Users.show(id, 1);
        }
        try {
            // Note: we need to delete all recipes
            // of the user before
            user.delete();
        } catch (JcrMappingException e) {
            flash.error("The user cannot be deleted (check references)");
            Users.show(id, 1);
        }
        flash.success("User %s deleted.", user.email);
        Users.index(1);
    }

    @Secure.Admin
    public static void edit(String id) {
        User user = loadUser(id);
        render(user);
    }

    public static void index(Integer page) {
        page = (page != null && page > 0) ? page : 1;
        JcrQuery<User> result = User.all(USERS_PATH);
        long nbUsers = result.count();
        Collection<User> users = result.fetch(page, pageSize);
        render(nbUsers, users, page);
    }

    public static void show(String id, Integer page) {
        page = (page != null && page > 0) ? page : 1;
        User user = loadUser(id);
        JcrQuery result = Recipe
                .find("select * from [nt:unstructured] where ISDESCENDANTNODE('%s') AND author = '%s' order by [jcr:created] desc",
                        Application.RECIPES_PATH, user.uuid);
        long nbRecipes = result.count();
        List<Recipe> recipes = result.fetch(page, pageSize);
        render(user, recipes, nbRecipes, page);
    }

    @Secure.Admin
    public static void update(@Valid User user) {
        if (validation.hasErrors()) {
            validation.keep();
            params.flash();
            flash.error("Please correct these errors !");
            edit(user.uuid);
        }
        user.merge();
        show(user.uuid);
    }

    private static User loadUser(String id) {
        User user = User.findById(id);
        notFoundIfNull(user);
        return user;
    }
}
