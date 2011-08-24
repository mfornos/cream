package models;

import org.jcrom.JcrFile;
import org.jcrom.annotations.JcrFileNode;
import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;
import org.jcrom.annotations.JcrReference;

import play.data.validation.Required;
import play.modules.cream.Model;

@JcrNode(mixinTypes = { "mix:created", "mix:lastModified", "mix:versionable" })
public class Recipe extends Model {

    public enum AccessLevel {
        PUBLIC, PRIVATE
    }

    @JcrName
    public String name;

    @JcrProperty
    @Required
    public String title;

    @JcrProperty
    @Required
    public String description;

    @JcrProperty
    @Required
    public String body;

    @JcrReference
    public User author;

    @JcrProperty
    public AccessLevel accessLevel;

    @JcrFileNode
    public JcrFile image;

    public boolean isOwner(String id) {
        return author.uuid.equals(id);
    }

    public boolean isPublic() {
        return AccessLevel.PUBLIC.equals(accessLevel);
    }
}
