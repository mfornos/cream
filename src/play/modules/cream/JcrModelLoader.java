package play.modules.cream;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.persistence.Transient;

import org.jcrom.annotations.JcrBaseVersionCreated;
import org.jcrom.annotations.JcrBaseVersionName;
import org.jcrom.annotations.JcrCheckedout;
import org.jcrom.annotations.JcrCreated;
import org.jcrom.annotations.JcrPath;
import org.jcrom.annotations.JcrReference;
import org.jcrom.annotations.JcrUUID;
import org.jcrom.annotations.JcrVersionCreated;
import org.jcrom.annotations.JcrVersionName;

import play.db.Model;
import play.db.Model.Factory;
import play.db.Model.Property;
import play.exceptions.UnexpectedException;

public class JcrModelLoader implements Factory {
	private final Class<? extends Model> clazz;

	public JcrModelLoader(Class<? extends play.db.Model> modelClass) {
		this.clazz = modelClass;
	}

	@Override
	public Long count(List<String> searchFields, String keywords, String where) {
		// XXX Duplicate Query for count()... :(
		StringBuilder q = buildSelect(searchFields, keywords, where);

		JcrQuery<? extends Model> query;
		try {
			query = JcrPersistence.find(clazz.getName(), q.toString(), JcrPersistence.getDefaultPath(clazz));
		} catch (RepositoryException e) {
			throw new UnexpectedException(e);
		}

		return query.count();
	}

	@Override
	public void deleteAll() {
		try {
			Session session = JcrPersistence.getSession();
			try {
				Node node = session.getNode(JcrPersistence.getDefaultPath(clazz));
				if (node != null) {
					node.remove();
				}
			} catch (PathNotFoundException e) {
				//
			}
		} catch (RepositoryException e) {
			throw new UnexpectedException(e);
		}
	}

	@Override
	public List<Model> fetch(int offset, int size, String orderBy, String order, List<String> searchFields,
			String keywords, String where) {
		StringBuilder q = buildSelect(searchFields, keywords, where);

		if (orderBy != null) {
			appendOrderBy(orderBy, order, q);
		}

		JcrQuery<? extends Model> query;
		try {
			query = JcrPersistence.find(clazz.getName(), q.toString(), JcrPersistence.getDefaultPath(clazz));
		} catch (RepositoryException e) {
			throw new UnexpectedException(e);
		}

		int page = (offset > size) ? offset / size : 1;
		return (List<Model>) query.fetch(page, size);
	}

	@Override
	public Model findById(Object id) {
		if (id == null) {
			return null;
		}
		return JcrPersistence.loadByUUID(clazz, (String) id);
	}

	public String keyName() {
		return keyField().getName();
	}

	public Class<?> keyType() {
		return keyField().getType();
	}

	public Object keyValue(Model m) {
		try {
			return keyField().get(m);
		} catch (Exception ex) {
			throw new UnexpectedException(ex);
		}
	}

	@Override
	public List<Property> listProperties() {
		List<Model.Property> properties = new ArrayList<Model.Property>();
		Set<Field> fields = new LinkedHashSet<Field>();
		Class<?> tclazz = clazz;
		while (!tclazz.equals(Object.class)) {
			Collections.addAll(fields, tclazz.getDeclaredFields());
			tclazz = tclazz.getSuperclass();
		}
		for (Field f : fields) {
			if (Modifier.isTransient(f.getModifiers())) {
				continue;
			}
			if (f.isAnnotationPresent(Transient.class)) {
				continue;
			}
			Model.Property mp = buildProperty(f);
			if (mp != null) {
				properties.add(mp);
			}
		}
		return properties;
	}

	Model.Property buildProperty(final Field field) {
		Model.Property modelProperty = new Model.Property();
		modelProperty.type = field.getType();
		modelProperty.field = field;

		final boolean isReference = field.isAnnotationPresent(JcrReference.class);

		// Collection
		if (Collection.class.isAssignableFrom(field.getType())) {
			final Class<?> fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];

			if (isReference) {
				modelProperty.isRelation = true;
				modelProperty.isMultiple = true;
				modelProperty.relationType = fieldType;
				modelProperty.choices = new Model.Choices() {

					@SuppressWarnings("unchecked")
					public List<Object> list() {
						String path = JcrPersistence.getDefaultPath(fieldType);
						return (List<Object>) JcrPersistence.findAll(fieldType, path).fetch();
					}
				};
			}
		} else if (Model.class.isAssignableFrom(field.getType())) {
			if (isReference) {
				// Single node
				modelProperty.isRelation = true;
				modelProperty.relationType = field.getType();
				modelProperty.choices = new Model.Choices() {

					@SuppressWarnings("unchecked")
					public List<Object> list() {
						String path = JcrPersistence.getDefaultPath(field.getType());
						return (List<Object>) JcrPersistence.findAll(field.getType(), path).fetch();
					}
				};
			}

		}
		if (field.getType().isEnum()) {
			modelProperty.choices = new Model.Choices() {

				@SuppressWarnings("unchecked")
				public List<Object> list() {
					return (List<Object>) Arrays.asList(field.getType().getEnumConstants());
				}
			};
		}
		modelProperty.name = field.getName();
		if (field.getType().equals(String.class)) {
			modelProperty.isSearchable = true;
		}
		if (field.isAnnotationPresent(JcrUUID.class) || field.isAnnotationPresent(JcrCheckedout.class)
				|| field.isAnnotationPresent(JcrCreated.class) || field.isAnnotationPresent(JcrVersionCreated.class)
				|| field.isAnnotationPresent(JcrBaseVersionCreated.class)
				|| field.isAnnotationPresent(JcrVersionName.class) || field.isAnnotationPresent(JcrPath.class)
				|| field.isAnnotationPresent(JcrBaseVersionName.class) || field.isAnnotationPresent(JcrPath.class)) {
			modelProperty.isGenerated = true;
		}
		return modelProperty;
	}

	Field keyField() {
		Class c = clazz;
		try {
			while (!c.equals(Object.class)) {
				for (Field field : c.getDeclaredFields()) {
					if (field.isAnnotationPresent(JcrUUID.class)) {
						field.setAccessible(true);
						return field;
					}
				}
				c = c.getSuperclass();
			}
		} catch (Exception e) {
			throw new UnexpectedException("Error while determining the object @Id for an object of type " + clazz);
		}
		throw new UnexpectedException("Cannot get the object @JcrUUID for an object of type " + clazz);
	}

	private void appendOrderBy(String orderBy, String order, StringBuilder q) {
		if (order == null || (!order.equals("ASC") && !order.equals("DESC"))) {
			order = "ASC";
		}
		q.append(" order by ");
		q.append(orderBy);
		q.append(" ");
		q.append(order);
	}

	private StringBuilder buildSelect(List<String> searchFields, String keywords, String where) {
		StringBuilder q = new StringBuilder("select * from [nt:unstructured] where ischildnode('%s')");
		if (keywords != null && !keywords.equals("")) {
			String searchQuery = getSearchQuery(searchFields, keywords);
			if (!searchQuery.equals("")) {
				q.append(" and (");
				q.append(searchQuery);
				q.append(')');
			}
		}

		q.append(where != null ? " and " + where : "");
		return q;
	}

	private String getSearchQuery(List<String> searchFields, String keywords) {
		StringBuilder q = new StringBuilder();
		if (keywords != null && !keywords.equals("")) {
			for (Model.Property property : listProperties()) {
				if (property.isSearchable
						&& (searchFields == null || searchFields.isEmpty() ? true : searchFields
								.contains(property.name))) {
					if (q.length() > 1) {
						q.append(" or ");
					}
					q.append("contains(");
					q.append(property.name);
					q.append(" ");
					q.append(keywords);
					q.append(')');
				}
			}
		}
		return q.toString();
	}
}
