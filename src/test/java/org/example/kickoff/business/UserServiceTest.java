package org.example.kickoff.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.ejb.EJB;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.example.kickoff.arquillian.ArquillianDBUnitTestBase;
import org.example.kickoff.jpa.JPA;
import org.example.kickoff.model.BaseEntity;
import org.example.kickoff.model.User;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xml.sax.InputSource;

@RunWith(Arquillian.class)
public class UserServiceTest extends ArquillianDBUnitTestBase {

	@Deployment
	public static Archive<?> createDeployment() {
		MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class).loadMetadataFromPom("pom.xml");

		WebArchive archive = ShrinkWrap.create(WebArchive.class);

		archive.addClass(ArquillianDBUnitTestBase.class);

		archive.addClasses(UserService.class);
		archive.addClasses(InvalidCredentialsException.class, JPA.class, ValidationException.class);
		archive.addPackage(BaseEntity.class.getPackage());
		archive.addAsWebInfResource("test-web.xml", "web.xml");

		archive.addAsResource("test-persistence.xml", "META-INF/persistence.xml");
		archive.addAsResource("META-INF/User.xml", "META-INF/User.xml");

		archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

		archive.addAsLibraries(resolver.artifact("com.h2database:h2").resolveAsFiles());
		archive.addAsLibraries(resolver.artifact("org.dbunit:dbunit").resolveAsFiles());

		archive.addAsResource("dbunit/user_service_test.xml");

		return archive;
	}

	@EJB
	private UserService userService;

	@Test
	public void testRegisterUser() {
		User user = new User();
		user.setEmail("test2@test.test");

		userService.registerUser(user, "TeSt");

		assertNotNull(user.getCredentials());
		assertNotNull(user.getId());

		User loggedInUser = userService.getUserByCredentials("test2@test.test", "TeSt");

		assertNotNull(loggedInUser);
		assertEquals(user, loggedInUser);
	}

	@Test
	public void testGetUserByCredentials() {
		User user = userService.getUserByCredentials("test@test.test", "TeSt");
		assertNotNull(user);
		assertEquals("test@test.test", user.getEmail());

		try {
			userService.getUserByCredentials("Test1", "wrong_password");
			fail();
		}
		catch (InvalidCredentialsException e) {
			// Exception should be thrown here
		}

		try {
			userService.getUserByCredentials("non_existant_username", "password");
			fail();
		}
		catch (InvalidCredentialsException e) {
			// Exception should be thrown here
		}
	}

	@Test
	public void testUpdatePassword() {
		User user = userService.getUserByCredentials("test@test.test", "TeSt");

		userService.updatePassword(user, "TeSt2");

		try {
			userService.getUserByCredentials("test@test.test", "TeSt");
			fail();
		}
		catch (Exception e) {
			// Exception should be thrown here
		}

		User loggedInUser = userService.getUserByCredentials("test@test.test", "TeSt2");
		assertNotNull(loggedInUser);
		assertEquals(user, loggedInUser);
	}

	@Override
	protected String getLookupName() {
		return "java:app/KickoffApp/kickoffUnitTestDS";
	}

	@Override
	protected IDataSet getTestDataSet() throws DataSetException {
		return new FlatXmlDataSet(new FlatXmlProducer(new InputSource(this.getClass().getResourceAsStream("/dbunit/user_service_test.xml"))));
	}

}
