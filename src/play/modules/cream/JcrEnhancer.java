package play.modules.cream;

import javassist.CtClass;
import javassist.CtMethod;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;

public class JcrEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);

        if (ctClass.subtypeOf(classPool.get(Model.class.getName()))) {
            String entityName = ctClass.getName();

            Logger.trace("Enhancing jcr entity %s", entityName);

            // All
            CtMethod all = CtMethod
                    .make(String
                            .format("public static play.modules.cream.ocm.JcrQueryResult all(String rootPath) { return  play.modules.cream.ocm.JcrMapper.findAll(\"%s\", rootPath);	}",
                                    entityName), ctClass);
            ctClass.addMethod(all);

            CtMethod allDefault = CtMethod
                    .make(String
                            .format("public static play.modules.cream.ocm.JcrQueryResult all() { return  play.modules.cream.ocm.JcrMapper.findAll(\"%s\"); }",
                                    entityName), ctClass);
            ctClass.addMethod(allDefault);

            // Find All
            CtMethod findAll = CtMethod
                    .make(String
                            .format("public static java.util.List findAll(String rootPath) { return play.modules.cream.ocm.JcrMapper.findAll(\"%s\", rootPath).fetch();}",
                                    entityName), ctClass);
            ctClass.addMethod(findAll);

            CtMethod findAllDefault = CtMethod
                    .make(String
                            .format("public static java.util.List findAll() { return play.modules.cream.ocm.JcrMapper.findAll(\"%s\").fetch();}",
                                    entityName), ctClass);
            ctClass.addMethod(findAllDefault);

            // Find
            CtMethod find = CtMethod
                    .make(String
                            .format("public static play.modules.cream.ocm.JcrQueryResult find(String query, Object[] params) { return play.modules.cream.ocm.JcrMapper.find(\"%s\", query, params);}",
                                    entityName), ctClass);
            ctClass.addMethod(find);

            // Find By
            CtMethod findBy = CtMethod
                    .make(String
                            .format("public static play.modules.cream.ocm.JcrQueryResult findByPath(String path, String where, Object[] params) { return play.modules.cream.ocm.JcrMapper.findByPath(\"%s\", path, where, params);}",
                                    entityName), ctClass);
            ctClass.addMethod(findBy);

            CtMethod findByDefault = CtMethod
                    .make(String
                            .format("public static play.modules.cream.ocm.JcrQueryResult findBy(String where, Object[] params) { return play.modules.cream.ocm.JcrMapper.findBy(\"%s\", where, params);}",
                                    entityName), ctClass);
            ctClass.addMethod(findByDefault);

            // ById
            CtMethod byId = CtMethod
                    .make(String
                            .format("public static play.modules.cream.Model findById(Object id) { return play.modules.cream.ocm.JcrMapper.loadByUUID(\"%s\", (String) id);}",
                                    entityName), ctClass);
            ctClass.addMethod(byId);

            // Get
            CtMethod get = CtMethod
                    .make(String
                            .format("public static play.modules.cream.Model get(String path) { return play.modules.cream.ocm.JcrMapper.get(\"%s\", path);}",
                                    entityName), ctClass);
            ctClass.addMethod(get);

            CtMethod getDefault = CtMethod
                    .make(String
                            .format("public static play.modules.cream.Model get() { return play.modules.cream.ocm.JcrMapper.get(\"%s\");}",
                                    entityName), ctClass);
            ctClass.addMethod(getDefault);

            // TODO version

            // TODO buildJcrQuery

            // Done.
            applicationClass.enhancedByteCode = ctClass.toBytecode();
            ctClass.defrost();
        }
    }

}
