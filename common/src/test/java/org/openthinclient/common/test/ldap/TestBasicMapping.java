package org.openthinclient.common.test.ldap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Group;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.DirectoryFacade;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.Mapping;
import org.openthinclient.ldap.TypeMapping;
import org.openthinclient.ldap.Util;

 @Ignore
// FIXME once it runs
public class TestBasicMapping extends AbstractEmbeddedDirectoryTest {
	private static Class[] groupClasses = {Location.class, UserGroup.class,
			Application.class, ApplicationGroup.class, Printer.class, Device.class,
			HardwareType.class};

	private static Class[] objectClasses = {Location.class, UserGroup.class,
			Application.class, ApplicationGroup.class, Printer.class, Device.class,
			HardwareType.class, User.class, Client.class};

	private static String baseDN = "dc=test,dc=test";
	private static String envDN = "ou=NeueUmgebung," + baseDN;

	@Test
	public void createEnvironment() throws IOException, DirectoryException,
			NamingException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(baseDN);

		createOU("NeueUmgebung", LDAPDirectory.openEnv(lcd));

		final LDAPConnectionDescriptor lcdNew = getConnectionDescriptor();

		lcdNew.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcdNew);

		final Realm realm = initRealm(dir, "");
		realm.setConnectionDescriptor(lcdNew);

		// Serversettings
		realm.setValue("Serversettings.Hostname", realm.getConnectionDescriptor()
				.getHostname());

		final Short s = new Short(realm.getConnectionDescriptor().getPortNumber());

		realm.setValue("Serversettings.Portnumber", s.toString());
		final String schemaProviderName = realm.getConnectionDescriptor()
				.getHostname();

		realm.setValue("Serversettings.SchemaProviderName", schemaProviderName);

		final LdapContext ctx = lcdNew.createDirectoryFacade().createDirContext();
		initOUs(ctx, dir);

		initAdmin(dir, realm, "administrator", "cn=administrator,ou=users," + envDN); //$NON-NLS-1$

		final Set<Realm> currentRealms = dir.list(Realm.class);

		realm.refresh();

		final Set<OrganizationalUnit> currentOUs = dir
				.list(OrganizationalUnit.class);

		Assert.assertTrue("RealmConfigurations wasn't created!", currentRealms
				.size() > 0);
		Assert.assertTrue("Not all the OUs were created!",
				currentOUs.size() > objectClasses.length + 2);

		Assert.assertNotNull("The group Administrators wasn't created!", realm
				.getAdministrators());
		Assert.assertNotNull("ReadOnlyPrinzipal wasn't created!", realm
				.getReadOnlyPrincipal());

		Assert.assertTrue("No Adminstrator was created!", dir.list(User.class)
				.size() > 0);
	}

	@Test
	public void createNewObjects() throws DirectoryException {

		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();

		final Location location = new Location();

		final HardwareType hwtype = new HardwareType();

		final Device device = new Device();

		final User user = new User();

		final UserGroup group = new UserGroup();

		final Client client = new Client();

		final Application application = new Application();

		final ApplicationGroup appGroup = new ApplicationGroup();

		final Printer printer = new Printer();

		final Set<DirectoryObject> objects = new HashSet<DirectoryObject>();

		location.setName("l1");
		objects.add(location);

		hwtype.setName("h1");
		objects.add(hwtype);

		device.setName("d1");
		objects.add(device);

		user.setName("u1");
		objects.add(user);

		group.setName("g1");
		objects.add(group);

		printer.setName("p1");
		objects.add(printer);

		client.setName("tc1");
		final Set<Device> devices = new HashSet<Device>();
		devices.add(device);
		client.setLocation(location);
		objects.add(client);

		application.setName("a1");
		objects.add(application);

		appGroup.setName("ag1");
		objects.add(appGroup);

		lcd.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		for (final DirectoryObject obj : objects)
			dir.save(obj);

		for (final Class clazz : objectClasses)
			Assert.assertTrue("Not all the objects were created!", dir.list(clazz)
					.size() > 0);

	}

	@Test
	public void assignObjects() throws DirectoryException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		final Set<User> users = dir.list(User.class);

		final Set<Location> locations = dir.list(Location.class);

		final Set<UserGroup> userGroups = dir.list(UserGroup.class);

		final Set<Client> clients = dir.list(Client.class);

		final Set<Application> applications = dir.list(Application.class);

		final Set<ApplicationGroup> applicationGroups = dir
				.list(ApplicationGroup.class);

		final Set<HardwareType> hwtypeGroups = dir.list(HardwareType.class);

		final Set<Printer> printers = dir.list(Printer.class);

		final Set<Device> devices = dir.list(Device.class);

		final Set<DirectoryObject> groupSet = new HashSet<DirectoryObject>();

		final Hashtable<String, Set<String>> memberGroup = new Hashtable<String, Set<String>>();

		for (final UserGroup group : userGroups) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();

			group.setMembers(users);
			members.addAll(users);

			final Set<String> memberNames = new HashSet<String>();
			for (final DirectoryObject o : members)
				memberNames.add(o.getName());

			memberGroup.put(group.getName(), memberNames);

			groupSet.add(group);
		}

		for (final HardwareType group : hwtypeGroups) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();
			group.setMembers(clients);

			members.addAll(clients);

			final Set<String> memberNames = new HashSet<String>();
			for (final DirectoryObject o : members)
				memberNames.add(o.getName());

			memberGroup.put(group.getName(), memberNames);

			groupSet.add(group);
		}

		for (final Device group : devices) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();
			group.setMembers(hwtypeGroups);
			members.addAll(hwtypeGroups);

			final Set<String> memberNames = new HashSet<String>();
			for (final DirectoryObject o : members)
				memberNames.add(o.getName());

			memberGroup.put(group.getName(), memberNames);

			groupSet.add(group);
		}

		for (final Application group : applications) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();

			members.addAll(users);
			members.addAll(userGroups);
			members.addAll(clients);
			members.addAll(applicationGroups);

			group.setMembers(members);

			final Set<String> memberNames = new HashSet<String>();
			for (final DirectoryObject o : members)
				memberNames.add(o.getName());

			memberGroup.put(group.getName(), memberNames);

			groupSet.add(group);
		}

		for (final ApplicationGroup group : applicationGroups) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();

			members.addAll(users);
			members.addAll(userGroups);
			members.addAll(clients);

			group.setMembers(members);

			final Set<String> memberNames = new HashSet<String>();
			for (final DirectoryObject o : members)
				memberNames.add(o.getName());

			memberGroup.put(group.getName(), memberNames);

			groupSet.add(group);
		}

		for (final Printer group : printers) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();

			members.addAll(users);
			members.addAll(userGroups);
			members.addAll(clients);
			members.addAll(locations);

			group.setMembers(members);

			final Set<String> memberNames = new HashSet<String>();
			for (final DirectoryObject o : members)
				memberNames.add(o.getName());

			memberGroup.put(group.getName(), memberNames);

			groupSet.add(group);
		}

		for (final DirectoryObject o : groupSet)
			dir.save(o);

		for (final DirectoryObject o : groupSet)
			if (o instanceof Group) {
				dir.refresh(o);
				final DirectoryObject obj = dir.load(o.getClass(), o.getDn());

				final Group g = (Group) obj;
				final Set<DirectoryObject> currentMembers = g.getMembers();

				final Set<String> currentMemberNames = new HashSet<String>();
				for (final DirectoryObject dobj : currentMembers)
					currentMemberNames.add(dobj.getName());

				final Object[] oldMemberArray = memberGroup.get(obj.getName())
						.toArray();
				final Object[] currentMemberArray = currentMemberNames.toArray();

				Arrays.sort(oldMemberArray);
				Arrays.sort(currentMemberArray);

				final boolean isEquals = Arrays.equals(oldMemberArray,
						currentMemberArray);

				Assert.assertTrue("Not all Objects were assigned: " + o.getName(),
						isEquals);
			}

	}

	@Test
	public void removeAssignements() throws DirectoryException {

		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		final Set<Group> groupSet = new HashSet<Group>();

		for (final Class cl : groupClasses)
			for (final Object o : dir.list(cl))
				if (o instanceof Group)
					groupSet.add((Group) o);

		final Set<DirectoryObject> currentMembers = new HashSet<DirectoryObject>();

		for (final Group group : groupSet) {
			group.setMembers(new HashSet<DirectoryObject>());
			dir.save(group);
		}

		for (final Group o : groupSet) {
			dir.refresh(o);
			currentMembers.addAll(o.getMembers());
		}
		Assert.assertTrue("Not all Assignements were deleted!", currentMembers
				.size() == 0);
	}

	@Test
	public void assignObjectsAgain() throws DirectoryException {
		assignObjects();
	}

	@Test
	public void renameObjects() throws DirectoryException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		final Set<User> users = dir.list(User.class);

		final Set<Location> locations = dir.list(Location.class);

		final Set<UserGroup> userGroups = dir.list(UserGroup.class);

		final Set<Client> clients = dir.list(Client.class);

		final Set<Application> applications = dir.list(Application.class);

		final Set<ApplicationGroup> applicationGroups = dir
				.list(ApplicationGroup.class);

		final Set<HardwareType> hwtypeGroups = dir.list(HardwareType.class);

		final Set<Printer> printers = dir.list(Printer.class);

		final Set<Device> devices = dir.list(Device.class);

		final Set<Realm> realms = LDAPDirectory.listRealms(lcd);

		final String prefixName = "New_";

		final Map<String, Set> countMember = new HashMap<String, Set>();

		for (final User user : users) {
			user.setName(prefixName + user.getName());
			dir.save(user);
		}

		for (final Location loc : locations) {
			loc.setName(prefixName + loc.getName());
			dir.save(loc);
		}

		for (final UserGroup ug : userGroups) {
			countMember.put(ug.getName(), ug.getMembers());
			ug.setName(prefixName + ug.getName());
			dir.save(ug);
		}

		for (final Client client : clients) {

			client.setName(prefixName + client.getName());
			dir.save(client);
		}

		for (final Application appl : applications) {
			countMember.put(appl.getName(), appl.getMembers());
			appl.setName(prefixName + appl.getName());
			dir.save(appl);
		}

		for (final ApplicationGroup appl : applicationGroups) {
			countMember.put(appl.getName(), appl.getMembers());
			appl.setName(prefixName + appl.getName());
			dir.save(appl);
		}

		for (final HardwareType hwt : hwtypeGroups) {
			countMember.put(hwt.getName(), hwt.getMembers());
			hwt.setName(prefixName + hwt.getName());
			dir.save(hwt);
		}

		for (final Printer printer : printers) {
			countMember.put(printer.getName(), printer.getMembers());
			printer.setName(prefixName + printer.getName());
			dir.save(printer);
		}

		for (final Device device : devices) {
			countMember.put(device.getName(), device.getMembers());
			device.setName(prefixName + device.getName());
			dir.save(device);
		}

		final Set<String> allNames = new HashSet<String>();

		// Assert: rename Objects
		for (final Class objClass : objectClasses) {
			Set<DirectoryObject> objectsList = dir.list(objClass);
			for (DirectoryObject obj : objectsList) {
				allNames.add(obj.getName());
			}
		}

		for (final String name : allNames) {
			Assert.assertTrue("The object: " + name + " wasn't renamed!", name
					.startsWith(prefixName));

		}

		// Assert: rename UniqueMember
		for (final Class groupClass : groupClasses) {
			final Set<DirectoryObject> objects = dir.list(groupClass);
			for (final DirectoryObject obj : objects) {
				if (obj instanceof Group) {
					Group group = (Group) obj;
					dir.refresh(group);
					final Set<DirectoryObject> members = group.getMembers();

					final String oldName = obj.getName().replace(prefixName, "");
					final int count = countMember.get(oldName).size();

					Assert.assertTrue("There are some false uniqueMembers: "
							+ obj.getName(), count == members.size());
				}
			}
		}

		// Assert: location
		for (Client client : clients) {
			dir.refresh(client);
			Assert.assertNotNull("Location of " + client.getName() + " is false!",
					client.getLocation());
		}

		for (final Realm realm : realms) {
			Set<User> member = realm.getAdministrators().getMembers();
			Assert.assertTrue("Admin wasn't renamed in RealmConfiguration!", member
					.size() > 0);
		}
	}

	@Test
	public void changeAndDeleteAttributes() throws DirectoryException {

		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		final Set<User> users = dir.list(User.class);

		for (final User user : users) {
			user.setDescription("JUnit-Test");
			dir.save(user);
		}

		for (final User user : users) {
			dir.refresh(user);
			Assert.assertTrue("Discription wasn't added: " + user.getName(),
					null != user.getDescription());
		}

		for (final User user : users) {
			user.setDescription(null);
			dir.save(user);
		}

		for (final User user : users) {
			dir.refresh(user);
			Assert.assertTrue("Discription wasn't deleted: " + user.getName(),
					null == user.getDescription());
		}
	}

	@Test
	public void changeProperties() throws DirectoryException {

		// Bug => Changed properties won't be saved

		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		final Set<Realm> realms = LDAPDirectory.listRealms(lcd);

		for (final Realm realm : realms) {
			realm.setValue("Hostname", lcd.getHostname());
			dir.save(realm);
		}

		for (final Realm realm : realms) {
			dir.refresh(realm);
			Assert.assertNotNull("No Properties saved: " + realm.getName(), realm
					.getValue("Hostname"));
		}

	}
	
	@Test
	public void useHybrid() {
		//FIXME
	}
	
	@Test
	public void destroySomething() {
		//FIXME
	}

	@Test
	public void deleteObjects() throws DirectoryException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		final Set<DirectoryObject> objects = new HashSet<DirectoryObject>();

		for (final Class cl : objectClasses)
			for (final Object o : dir.list(cl))
				if (o instanceof DirectoryObject)
					objects.add((DirectoryObject) o);

		if (objects.size() > 0)
			for (final DirectoryObject obj : objects) {
				dir.delete(obj);
				final Set<DirectoryObject> currentObjects = (Set<DirectoryObject>) dir
						.list(obj.getClass());

				for (final DirectoryObject o : currentObjects)
					Assert.assertTrue("Object: " + obj.getName() + " wasn't deleted!",
							obj != o);

				for (final Class clazz : groupClasses) {
					final Set<DirectoryObject> currentGroups = dir.list(clazz);
					for (final DirectoryObject group : currentGroups)
						if (group instanceof Group) {
							final Group g = (Group) group;
							final Set<DirectoryObject> members = g.getMembers();
							for (final DirectoryObject member : members)
								Assert.assertTrue("The UniqueMember: " + obj.getDn()
										+ " wasn't deleted!", !member.getDn().equals(obj.getDn()));
						}
				}
			}
	}

	@Test
	public void deleteEnvironment() throws NamingException, DirectoryException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final DirectoryFacade df = lcd.createDirectoryFacade();

		final Name targetName = df.makeRelativeName("");
		final LdapContext ctx = df.createDirContext();
		try {
			Util.deleteRecursively(ctx, targetName);

		} finally {
			final LDAPConnectionDescriptor baseLcd = getConnectionDescriptor();
			baseLcd.setBaseDN(baseDN);

			final Set<Realm> realms = LDAPDirectory.listRealms(baseLcd);

			Assert.assertTrue("Not all environments were deleted!",
					realms.size() == 0);

			ctx.close();
		}
	}

	// -----------------------------------------------------------------------------------------------

	private void createOU(String newFolderName, LDAPDirectory directory)
			throws DirectoryException {
		final OrganizationalUnit ou = new OrganizationalUnit();
		ou.setName(newFolderName);
		ou.setDescription("openthinclient.org Console"); //$NON-NLS-1$
		directory.save(ou, "");
	}

	private static void initAdmin(LDAPDirectory dir, Realm realm, String name,
			String baseDN) throws DirectoryException {

		baseDN = "cn=" + name + "," + baseDN;
		final User admin = new User();
		admin.setName(name);
		admin.setDescription("Initialer administrativer Benutzer"); //$NON-NLS-1$
		admin.setNewPassword("openthinclient"); //$NON-NLS-1$
		admin.setSn("Admin");
		admin.setGivenName("Joe");

		final UserGroup administrators = realm.getAdministrators();

		dir.save(admin);
		administrators.getMembers().add(admin);
		realm.setAdministrators(administrators);

		dir.save(administrators);
		dir.save(realm);
	}

	private static void initOUs(DirContext ctx, LDAPDirectory dir)
			throws DirectoryException {
		try {
			final Mapping rootMapping = dir.getMapping();

			final Collection<TypeMapping> typeMappers = rootMapping.getTypes()
					.values();
			for (final TypeMapping mapping : typeMappers) {
				final OrganizationalUnit ou = new OrganizationalUnit();
				final String baseDN = mapping.getBaseRDN();

				// we create only those OUs for which we have a base DN
				if (null != baseDN) {
					ou.setName(baseDN.substring(baseDN.indexOf("=") + 1)); //$NON-NLS-1$

					dir.save(ou, ""); //$NON-NLS-1$
				}
			}

		} catch (final Exception e) {
			throw new DirectoryException("Kann OU-Struktur nicht initialisieren", e); //$NON-NLS-1$
		}
	}

	private static Realm initRealm(LDAPDirectory dir, String description)
			throws DirectoryException {
		try {
			final Realm realm = new Realm();
			realm.setDescription(description);
			final UserGroup admins = new UserGroup();
			admins.setName("administrators"); //$NON-NLS-1$
			// admins.setAdminGroup(true);
			realm.setAdministrators(admins);

			final String date = new Date().toString();
			realm.setValue("invisibleObjects.initialized", date); //$NON-NLS-1$

			final User roPrincipal = new User();
			roPrincipal.setName("roPrincipal");
			roPrincipal.setSn("Read Only User");
			roPrincipal.setNewPassword("secret");

			realm.setReadOnlyPrincipal(roPrincipal);

			realm.setName("RealmConfiguration");

			dir.save(realm, "");

			return realm;
		} catch (final Exception e) {
			throw new DirectoryException("Kann Umgebung nicht erstellen", e); //$NON-NLS-1$
		}
	}

}