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
							.format("public static play.modules.cream.JcrQuery all(String rootPath) { return  play.modules.cream.JcrPersistence.findAll(\"%s\", rootPath);	}",
									entityName), ctClass);
			ctClass.addMethod(all);

			// FindAll
			CtMethod findAll = CtMethod
					.make(String
							.format("public static java.util.List findAll(String rootPath) { return play.modules.cream.JcrPersistence.findAll(\"%s\", rootPath).fetch();}",
									entityName), ctClass);
			ctClass.addMethod(findAll);

			// ById
			CtMethod byId = CtMethod
					.make(String
							.format("public static play.modules.cream.Model findById(Object id) { return play.modules.cream.JcrPersistence.loadByUUID(\"%s\", (String) id);}",
									entityName), ctClass);
			ctClass.addMethod(byId);

			// Find
			CtMethod find = CtMethod
					.make(String
							.format("public static play.modules.cream.JcrQuery find(String query, Object[] params) { return play.modules.cream.JcrPersistence.find(\"%s\", query, params);}",
									entityName), ctClass);
			ctClass.addMethod(find);

			// Get
			CtMethod get = CtMethod
					.make(String
							.format("public static play.modules.cream.Model get(String path) { return play.modules.cream.JcrPersistence.get(\"%s\", path);}",
									entityName), ctClass);
			ctClass.addMethod(get);

			// Done.
			applicationClass.enhancedByteCode = ctClass.toBytecode();
			ctClass.defrost();
		}
	}

}
