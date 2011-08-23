package play.modules.cream.helpers;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

import play.Logger;
import play.Play;
import play.exceptions.UnexpectedException;
import play.vfs.VirtualFile;

import javax.jcr.Repository;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Jcr and Jackrabbit utilities for repository creation
 * 
 * @author igor.vaynberg
 */
public class JcrRepositoryCreationHelper {

	/**
	 * Copies a resource to a {@link java.io.File}
	 * 
	 * @param source
	 *            classpath or virtual file path to resource
	 * @param destination
	 *            destination file
	 */
	public static void copyResourceToFile(String source, java.io.File destination) {
		VirtualFile conf = Play.getVirtualFile(source);
		final InputStream in;
		if (conf == null || !conf.exists()) {
			in = JcrRepositoryCreationHelper.class.getResourceAsStream(source);
		} else {
			in = conf.inputstream();
		}

		if (in == null) {
			throw new UnexpectedException("Resource: " + source + " does not exist");
		}

		try {
			final FileOutputStream fos = new FileOutputStream(destination);
			copyStream(in, fos, 4096);
			fos.close();
			in.close();
		} catch (IOException e) {
			throw new UnexpectedException("Could not copy class resource: " + source + " to destination: "
					+ destination.getAbsolutePath());
		}
	}

	/**
	 * Writes the input stream to the output stream. Input is done without a
	 * Reader object, meaning that the input is copied in its raw form.
	 * 
	 * @param in
	 *            The input stream
	 * @param out
	 *            The output stream
	 * @param bufSize
	 *            The buffer size. A good value is 4096.
	 * @return Number of bytes copied from one stream to the other
	 * @throws IOException
	 */
	public static int copyStream(final InputStream in, final OutputStream out, final int bufSize) throws IOException {
		if (bufSize <= 0) {
			throw new IllegalArgumentException("The parameter 'bufSize' must not be <= 0");
		}

		final byte[] buffer = new byte[bufSize];
		int bytesCopied = 0;
		while (true) {
			int byteCount = in.read(buffer, 0, buffer.length);
			if (byteCount <= 0) {
				break;
			}
			out.write(buffer, 0, byteCount);
			bytesCopied += byteCount;
		}
		return bytesCopied;
	}

	/**
	 * Creates a repository at the location specified by the url. Url must start
	 * with <code>file://</code>.
	 * 
	 * @param url
	 *            repository home url
	 * @param repoConfiguration
	 * @return repository instance
	 * @throws RuntimeException
	 *             if repository could not be created
	 */
	public static Repository createFileRepository(String url, String repoConfiguration) {
		try {
			// ensure home dir exists
			final File home = new File(url.substring(6));
			mkdirs(home);

			// create default config file if one is not present
			File cfg = new File(home, "repository.xml");
			if (!cfg.exists()) {
				copyResourceToFile(repoConfiguration, cfg);
			}

			// TODO: try to clean from Jackrabbit dependency
			InputStream configStream = new FileInputStream(cfg);
			RepositoryConfig config = RepositoryConfig.create(configStream, home.getAbsolutePath());

			return RepositoryImpl.create(config);

			//
			// --> EXAMPLE FOR REAL JCR2 INIT, REQUIRES JAVA6, THEREFORE NOT
			// IMPLEMENTED!!!
			//
			// Properties properties = new Properties();
			// properties.load(configStream);
			//
			// Repository repository = null;
			//
			// for (RepositoryFactory factory :
			// ServiceLoader.load(RepositoryFactory.class)) {
			// repository = factory.getRepository(properties);
			// if(repository != null) {
			// break;
			// }
			// }
			//
			// return repository;
		} catch (Exception e) {
			throw new RuntimeException("Could not create file repository at url: " + url, e);
		}
	}

	/**
	 * Creates a jackrabbit repository based on the url. Accepted urls are
	 * <code>rmi://</code> and <code>file://</code>
	 * 
	 * @param url
	 *            repository url
	 * @param repoConfiguration
	 *            location of the repository.xml in classpath
	 * @return repository instance
	 * @throws RuntimeException
	 *             if repository could not be created
	 */
	public static Repository createRepository(String url, String repoConfiguration) {
		if (url.startsWith("rmi://")) {
			return createRmiRepository(url);
		} else if (url.startsWith("file://")) {
			return createFileRepository(url, repoConfiguration);
		} else {
			throw new RuntimeException(
					"Unsupported repository location url. Only prefix rmi:// and file:// are supported");
		}
	}

	/**
	 * Creates a repository at the location specified by the url. Url must start
	 * with <code>rmi://</code>.
	 * 
	 * @param url
	 *            repository home url
	 * @return repository instance
	 * @throws RuntimeException
	 *             if repository could not be created
	 */
	public static Repository createRmiRepository(String url) {
		try {
			JcrRepositoryCreationHelper.class.getClassLoader().loadClass(
					"org.apache.jackrabbit.rmi.client.ClientRepositoryFactory");

			return RmiRepositoryFactory.getRmiRepository(url);
		} catch (Exception e) {
			throw new RuntimeException("Could not create rmi repository instance at url: " + url, e);
		}
	}

	/**
	 * {@link java.io.File#mkdirs()} that throws runtime exception if it fails
	 * 
	 * @param file
	 */
	public static void mkdirs(java.io.File file) {
		if (!file.exists()) {
			if (!file.mkdirs()) {
				throw new RuntimeException("Could not create directory: " + file.getAbsolutePath());
			}
		}
	}

	/**
	 * Constructor
	 */
	private JcrRepositoryCreationHelper() {

	}
}
