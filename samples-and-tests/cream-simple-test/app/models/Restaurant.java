package models;

import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrPath;
import org.jcrom.annotations.JcrProperty;
import org.jcrom.annotations.JcrReference;

import play.data.validation.Required;
import play.modules.cream.Model;

@JcrNode(mixinTypes = { "mix:created", "mix:lastModified", "mix:referenceable" })
public class Restaurant extends Model {

	@JcrName
	@Required
	public String name;

	@JcrProperty
	public String description;

	@JcrProperty
	public String phoneNumber;

	@JcrReference
	public Chef chef;

	public String toString() {
		return name;
	}
}
