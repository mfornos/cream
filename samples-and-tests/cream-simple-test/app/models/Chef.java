package models;

import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrNode;

import play.data.validation.Required;
import play.modules.cream.Model;

@JcrNode(mixinTypes = { "mix:created", "mix:lastModified", "mix:referenceable" })
public class Chef extends Model {

    @JcrName
    @Required
    public String name;

    public String toString() {
        return name;
    }

}
