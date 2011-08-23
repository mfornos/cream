package models;

import java.util.List;

import org.jcrom.annotations.JcrChildNode;
import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrPath;
import org.jcrom.annotations.JcrProperty;
import org.jcrom.annotations.JcrReference;

import play.data.validation.Required;
import play.modules.cream.Model;

@JcrNode(mixinTypes = { "mix:created", "mix:lastModified", "mix:referenceable" })
public class Company extends Model {

	@JcrName
	@Required
	public String name;

	@JcrProperty
	@Required
	public String title;

	@JcrProperty
	public String description;

	@JcrProperty
	public String phoneNumber;

	@JcrProperty
	public int rating;

	@JcrReference
	public List<Restaurant> restaurants;

	public String toString() {
		return name;
	}

}
