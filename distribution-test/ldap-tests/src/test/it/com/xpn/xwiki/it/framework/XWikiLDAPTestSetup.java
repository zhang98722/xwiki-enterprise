package com.xpn.xwiki.it.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.server.core.configuration.MutablePartitionConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.unit.AbstractServerTest;

import junit.framework.Test;

import com.xpn.xwiki.test.XWikiTestSetup;

/**
 * JUnit TestSetup extension that starts/stops embedded LDAP server and modify xwiki.cfg file to use LDAP as
 * authentication system. This class is meant to wrap a JUnit TestSuite. For example:
 * 
 * <pre>
 * &lt;code&gt;
 * public static Test suite()
 * {
 *     // Create some TestSuite object here
 *     return new LDAPXWikiTestSetup(suite);
 * }
 * &lt;/code&gt;
 * </pre>
 * 
 * @version $Id$
 */
public class XWikiLDAPTestSetup extends XWikiTestSetup
{
    /**
     * The name of the LDAP property containing user unique id (cn).
     */
    public static final String LDAP_USERUID_FIELD = "cn";

    /**
     * The name of the LDAP property containing user unique id (uid).
     */
    public static final String LDAP_USERUID_FIELD_UID = "uid";

    /**
     * The name of the system property containing the LDAP embedded server port.
     */
    public static final String SYSPROPNAME_LDAPPORT = "ldap_port";

    /**
     * The directory where is the instance of XWiki Enterprise used for theses tests.
     */
    public static final String EXECUTION_DIRECTORY = System.getProperty("xwikiExecutionDirectory");

    /**
     * The xwiki.cfg file used by the instance of XWiki Enterprise used for theses tests.
     */
    public static final String XWIKI_CFG_FILE = EXECUTION_DIRECTORY + "/webapps/xwiki/WEB-INF/xwiki.cfg";

    /**
     * The log4j.properties used by the instance of XWiki Enterprise used for theses tests.
     */
    public static final String XWIKI_LOG_FILE = EXECUTION_DIRECTORY + "/webapps/xwiki/WEB-INF/classes/log4j.properties";

    // Somes datas examples

    /**
     * The LDAP DN of user Horatio Hornblower.
     */
    public static final String HORATIOHORNBLOWER_DN = "cn=Horatio Hornblower,ou=people,o=sevenSeas";

    /**
     * The LDAP unique id of user Horatio Hornblower.
     */
    public static final String HORATIOHORNBLOWER_CN = "Horatio Hornblower";

    /**
     * The LDAP password of user Horatio Hornblower.
     */
    public static final String HORATIOHORNBLOWER_PWD = "pass";

    /**
     * The LDAP DN of user Thomas Quist.
     */
    public static final String THOMASQUIST_DN = "cn=Thomas Quist,ou=people,o=sevenSeas";

    /**
     * The LDAP unique id of user Thomas Quist.
     */
    public static final String THOMASQUIST_CN = "Thomas Quist";

    /**
     * The LDAP password of user Thomas Quist.
     */
    public static final String THOMASQUIST_PWD = "pass";

    /**
     * The LDAP DN of user William Bush.
     */
    public static final String WILLIAMBUSH_DN = "cn=William Bush,ou=people,o=sevenSeas";

    /**
     * The LDAP password of user William Bush.
     */
    public static final String WILLIAMBUSH_PWD = "pass";

    /**
     * The LDAP unique id of user William Bush.
     */
    public static final String WILLIAMBUSH_UID = "wbush";

    /**
     * The LDAP unique id with mixed case of user William Bush.
     */
    public static final String WILLIAMBUSH_UID_MIXED = "wBush";

    /**
     * The LDAP DN of user User.With.Points.
     */
    public static final String USERWITHPOINTS_DN = "cn=User.With.Points,ou=people,o=sevenSeas";

    /**
     * The LDAP password of user William Bush.
     */
    public static final String USERWITHPOINTS_PWD = "pass";

    /**
     * The LDAP unique id of user William Bush.
     */
    public static final String USERWITHPOINTS_UID = "user.with.points";

    /**
     * The LDAP DN of group HMS Lydia.
     */
    public static final String HMSLYDIA_DN = "cn=HMS Lydia,ou=crews,ou=groups,o=sevenSeas";

    /**
     * The LDAP DN of group to exclude from login.
     */
    public static final String EXCLUSIONGROUP_DN = "cn=Exlude Group,ou=crews,ou=groups,o=sevenSeas";

    /**
     * The LDAP members of group HMS Lydia.
     */
    public static final Set<String> HMSLYDIA_MEMBERS = new HashSet<String>();

    static {
        HMSLYDIA_MEMBERS.add(HORATIOHORNBLOWER_DN.toLowerCase());
        HMSLYDIA_MEMBERS.add(WILLIAMBUSH_DN.toLowerCase());
        HMSLYDIA_MEMBERS.add("cn=Thomas Quist,ou=people,o=sevenSeas".toLowerCase());
        HMSLYDIA_MEMBERS.add("cn=Moultrie Crystal,ou=people,o=sevenSeas".toLowerCase());
        HMSLYDIA_MEMBERS.add("cn=User.With.Points,ou=people,o=sevenSeas".toLowerCase());
    }

    /**
     * The xwiki.cfg properties modified for the test.
     */
    public static Properties CURRENTXWIKICONF;

    // ///

    /**
     * Tool to start and stop embedded LDAP server.
     */
    private LDAPRunner ldap = new LDAPRunner();

    /**
     * The default xwiki.cfg properties.
     */
    private Properties initialXWikiConf;

    /**
     * The log4j.properties properties.
     */
    private Properties logProperties;

    /**
     * @return return the port of the current instance of LDAP server.
     */
    public static int getLDAPPort()
    {
        return Integer.parseInt(System.getProperty(SYSPROPNAME_LDAPPORT));
    }

    public XWikiLDAPTestSetup(Test test) throws IOException
    {
        super(test);

        // Prepare xwiki.cfg properties

        FileInputStream fis = new FileInputStream(XWIKI_CFG_FILE);
        this.initialXWikiConf = new Properties();
        this.initialXWikiConf.load(fis);
        fis.close();

        fis = new FileInputStream(XWIKI_CFG_FILE);
        CURRENTXWIKICONF = new Properties();
        CURRENTXWIKICONF.load(fis);
        fis.close();

        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap", "1");
        CURRENTXWIKICONF.setProperty("xwiki.authentication.authclass",
            "com.xpn.xwiki.user.impl.LDAP.XWikiLDAPAuthServiceImpl");
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.server", "localhost");
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.base_DN", "o=sevenSeas");
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.bind_DN", "cn={0},ou=people,o=sevenSeas");
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.bind_pass", "{1}");
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.UID_attr", LDAP_USERUID_FIELD);
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.fields_mapping", "name=" + LDAP_USERUID_FIELD
            + ",last_name=sn,first_name=givenname,fullname=description,email=mail,ldap_dn=dn");
        /*
         * CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.group_mapping", "XWiki.XWikiAdminGroup=cn=HMS
         * Lydia,ou=crews,ou=groups,o=sevenSeas");
         */
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.groupcache_expiration", "1");
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.user_group", HMSLYDIA_DN);
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.exclude_group", EXCLUSIONGROUP_DN);
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.validate_password", "0");
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.update_user", "1");
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.trylocal", "1");
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.mode_group_sync", "always");
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.ssl", "0");
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.ssl.keystore", "");

        // Prepare log4j.properties properties
        this.logProperties = new Properties();
        this.logProperties.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        this.logProperties.setProperty("log4j.appender.stdout.Target", "System.out");
        this.logProperties.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        this.logProperties.setProperty("log4j.appender.stdout.layout.ConversionPattern",
            "%d [%X{url}] [%t] %-5p %-30.30c{2} %x - %m %n");
        this.logProperties.setProperty("log4j.rootLogger", "warn, stdout");
        this.logProperties.setProperty("log4j.logger.com.xpn.xwiki.plugin.ldap", "debug");
        this.logProperties.setProperty("log4j.logger.com.xpn.xwiki.user.impl.LDAP", "debug");
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.test.XWikiTestSetup#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        this.ldap.start();

        System.setProperty(SYSPROPNAME_LDAPPORT, "" + ldap.getPort());
        CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.port", "" + ldap.getPort());

        FileOutputStream fos = new FileOutputStream(XWIKI_CFG_FILE);
        CURRENTXWIKICONF.store(fos, null);
        fos.close();

        fos = new FileOutputStream(XWIKI_LOG_FILE);
        this.logProperties.store(fos, null);
        fos.close();

        super.setUp();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.test.XWikiTestSetup#tearDown()
     */
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();

        FileOutputStream fos = new FileOutputStream(XWIKI_CFG_FILE);
        initialXWikiConf.store(fos, null);
        fos.close();

        this.ldap.stop();
    }
}

/**
 * Tool to start and stop embedded LDAP server.
 * 
 * @version $Id$
 */
class LDAPRunner extends AbstractServerTest
{
    /**
     * Start the server.
     */
    public void start() throws Exception
    {
        // Add partition 'sevenSeas'
        MutablePartitionConfiguration pcfg = new MutablePartitionConfiguration();
        pcfg.setName("sevenSeas");
        pcfg.setSuffix("o=sevenseas");

        // Create some indices
        Set<String> indexedAttrs = new HashSet<String>();
        indexedAttrs.add("objectClass");
        indexedAttrs.add("o");
        pcfg.setIndexedAttributes(indexedAttrs);

        // Create a first entry associated to the partition
        Attributes attrs = new BasicAttributes(true);

        // First, the objectClass attribute
        Attribute attr = new BasicAttribute("objectClass");
        attr.add("top");
        attr.add("organization");
        attrs.put(attr);

        // The the 'Organization' attribute
        attr = new BasicAttribute("o");
        attr.add("sevenseas");
        attrs.put(attr);

        // Associate this entry to the partition
        pcfg.setContextEntry(attrs);

        // As we can create more than one partition, we must store
        // each created partition in a Set before initialization
        Set<PartitionConfiguration> pcfgs = new HashSet<PartitionConfiguration>();
        pcfgs.add(pcfg);

        configuration.setContextPartitionConfigurations(pcfgs);

        // Create a working directory
        File workingDirectory = new File("server-work");
        configuration.setWorkingDirectory(workingDirectory);

        // Now, let's call the upper class which is responsible for the
        // partitions creation
        setUp();

        // Load a demo ldif file
        importLdif(this.getClass().getResourceAsStream("init.ldif"));
    }

    /**
     * Shutdown the server.
     */
    public void stop() throws Exception
    {
        tearDown();
    }

    /**
     * @return the port to connect to LDAP server.
     */
    public int getPort()
    {
        return port;
    }
}
