package controllers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import models.Recipe;
import models.User;

import org.jcrom.JcrMappingException;

import play.mvc.Before;
import play.mvc.Controller;

public class Secure extends Controller {

    private static final String CONNECTED = "connected";
    private static final String LOGGED = "logged";

    @Before
    static void checkSecure() throws JcrMappingException, ItemNotFoundException, RepositoryException {
        Authenticated authenticated = getActionAnnotation(Secure.Authenticated.class);
        Admin admin = getActionAnnotation(Secure.Admin.class);
        if (authenticated != null || admin != null) {
            if (session.contains(LOGGED)) {
                // The user is authenticated,
                // add User object to the renderArgs scope
                User user = User.findById(session.get(LOGGED));
                renderArgs.put(CONNECTED, user);
            } else {
                // The user is not authenticated,
                // redirect to the login form
                Application.login();
            }
        }

        if (admin != null) {
            // The action method is annotated with @Admin,
            // check the permission
            if (!renderArgs.get(CONNECTED, User.class).admin) {
                // The connected user is not admin;
                forbidden("You must be admin to see this page");
            }
        }

        renderArgs.put(CONNECTED, connectedUser());
    }

    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Admin {
    }

    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Authenticated {
    }

    // ~~~~~~~~~~~~ Some utils

    static void connect(User user) {
        session.put(LOGGED, user.uuid);
    }

    static User connectedUser() {
        String userId = session.get(LOGGED);
        User user = null;
        try {
            if (userId != null) {
                user = User.findById(userId);
            }
        } catch (Exception ex) {
            session.clear();
        }
        return user;
    }

    static boolean checkRecipeAccess(Recipe recipe) {
        User connected = connectedUser();
        return (recipe.isPublic() || (connected != null && (connected.admin || recipe.isOwner(connected.uuid))));
    }

}
