package play.modules.cream;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.TransientRepository;
import org.jcrom.Jcrom;
import org.jcrom.annotations.JcrNode;

import play.Invoker.InvocationContext;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;
import play.inject.Injector;
import play.modules.cream.annotations.JcrSession;
import play.modules.cream.annotations.NoJcrSession;
import play.modules.cream.helpers.JcrRepositoryCreationHelper;
import play.modules.cream.helpers.JcrRepositoryHelper;

/**
 * Cream: JCR Plugin for Play! Framework
 * 
 */
public class JcrPlugin extends PlayPlugin {

    private static ThreadLocal<JcrSessionSource> sessionSource = new ThreadLocal<JcrSessionSource>();

    public static Session getCurrentSession() {
        return sessionSource.get().getBeanOfType(Session.class);
    }

    @Override
    public void afterInvocation() {
        closeSession();
    }

    @Override
    public void beforeInvocation() {
        InvocationContext current = InvocationContext.current();
        if (!current.isAnnotationPresent(NoJcrSession.class)) {
            Session currentSession = createCurrentSession(current);
            sessionSource.set(new JcrSessionSource(currentSession));
            Injector.inject(sessionSource.get());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object bind(String name, Class clazz, java.lang.reflect.Type type, Annotation[] annotations,
            Map<String, String[]> params) {

        if (Model.class.isAssignableFrom(clazz)) {
            String keyName = Model.Manager.factoryFor(clazz).keyName();
            String idKey = name + "." + keyName;
            if (params.containsKey(idKey) && params.get(idKey).length > 0 && params.get(idKey)[0] != null
                    && params.get(idKey)[0].trim().length() > 0) {
                String id = params.get(idKey)[0];
                try {
                    Model o = (Model) JcrPersistence.loadByUUID(clazz, id);
                    if (o != null) {
                        return Model.edit(o, name, params, annotations);
                    }
                } catch (Exception e) {
                    throw new UnexpectedException(e);
                }
            }
            return Model.create(clazz, name, params, annotations);
        }
        return super.bind(name, clazz, type, annotations, params);
    }

    @Override
    public Object bind(String name, Object o, Map<String, String[]> params) {
        if (o instanceof Model) {
            return Model.edit(o, name, params, null);
        }
        return null;
    }

    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        new JcrEnhancer().enhanceThisClass(applicationClass);
    }

    @Override
    public void invocationFinally() {
        closeSession();
    }

    @Override
    public Model.Factory modelFactory(Class<? extends play.db.Model> modelClass) {
        return (modelClass.isAnnotationPresent(JcrNode.class)) ? new JcrModelLoader(modelClass) : null;
    }

    @Override
    public void onApplicationStart() {
        mapJcrEntities();
        setUpJcrRepository();
    }

    @Override
    public void onApplicationStop() {
        JcrRepositoryHelper.shutdown();
    }

    @Override
    public void onInvocationException(Throwable e) {
        closeSession();
    }

    private void closeSession() {
        JcrSessionSource currentSessionSource = sessionSource.get();
        if (currentSessionSource != null) {
            currentSessionSource.close();
            sessionSource.set(null);
        }
    }

    private Session createCurrentSession(InvocationContext current) {
        Session currentSession;
        JcrSession jcrConfig = current.getAnnotation(JcrSession.class);
        if (jcrConfig != null && StringUtils.isNotBlank(jcrConfig.workspace())) {
            currentSession = JcrRepositoryHelper.openSession(jcrConfig.workspace());
        } else {
            currentSession = JcrRepositoryHelper.openSession();
        }
        return currentSession;
    }

    private void mapJcrEntities() {
        Jcrom jcrom = new Jcrom();

        List<Class> jcrClasses = Play.classloader.getAnnotatedClasses(JcrNode.class);
        for (Class jcrClass : jcrClasses) {
            Logger.trace("Jcrom mapping of %s", jcrClass.getName());
            jcrom.map(jcrClass);
        }

        JcrMetadata jcrMetadata = JcrMetadata.getInstance();
        jcrMetadata.intialize(jcrom);

        JcrPersistence.jcrom = jcrom;
    }

    private void setUpJcrRepository() {
        Repository repository;
        Properties p = Play.configuration;
        String username = p.getProperty("cream.jcr.username", "anonymous");
        String password = p.getProperty("cream.jcr.password", "");
        String workspace = p.getProperty("cream.jcr.workspace", "default");
        String repoConfiguration = p.getProperty("cream.jcr.configuration", "/play/modules/cream/repository.xml");

        if ("transient".equals(p.getProperty("cream.jcr.mode"))) {
            repository = new TransientRepository();
        } else {
            String url = p.getProperty("cream.jcr.url", "");
            repository = JcrRepositoryCreationHelper.createRepository(url, repoConfiguration);
        }

        JcrRepositoryHelper.repository = repository;
        JcrRepositoryHelper.defaultCredentials = new SimpleCredentials(username, password.toCharArray());
        JcrRepositoryHelper.defaultWorkspace = workspace;
    }
}
