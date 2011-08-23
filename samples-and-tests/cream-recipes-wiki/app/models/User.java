package models;

import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrPath;
import org.jcrom.annotations.JcrProperty;
import org.jcrom.annotations.JcrUUID;

import play.data.validation.Email;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.libs.Crypto;
import play.modules.cream.JcrQuery;
import play.modules.cream.Model;

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

	public static JcrQuery<User> findByEmail(String email) {
		return find("select * from [nt:unstructured] where ISDESCENDANTNODE('%s') AND email = '%s'", "/users", email);
	}
}
