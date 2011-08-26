package models;

import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

import play.data.validation.Email;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.libs.Crypto;
import play.modules.cream.Model;
import play.modules.cream.ocm.JcrQueryResult;

@JcrNode(mixinTypes = { "mix:created", "mix:lastModified", "mix:referenceable" })
public class User extends Model {

    @JcrName
    @Required
    public String name;

    @JcrProperty
    @Required
    @Email
    public String email;

    @JcrProperty
    @MinSize(5)
    public String password;

    @JcrProperty
    public boolean admin;

    public boolean checkPassword(String password2) {
        return password.equals(Crypto.passwordHash(password2));
    }

    public static JcrQueryResult<User> findByEmail(String email) {
        return findBy("email = %s", email);
    }
}
