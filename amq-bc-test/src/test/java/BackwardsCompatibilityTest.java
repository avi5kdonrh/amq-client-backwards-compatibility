import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.security.*;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ActiveMQServers;
import org.apache.activemq.artemis.spi.core.security.ActiveMQBasicSecurityManager;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class BackwardsCompatibilityTest extends ActiveMQTestBase {
    private static final Logger log = LoggerFactory.getLogger(BackwardsCompatibilityTest.class);
    ActiveMQServer server1;

    private void generateKs() throws  Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        X500Name dnName = new X500Name("CN=localhost, OU=IT, O=MyCompany, C=US");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + (365L * 24 * 60 * 60 * 1000)); // 1 year

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                dnName, serial, notBefore, notAfter, dnName, keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));

        // 4. Save into a PKCS12 KeyStore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("mydomain", keyPair.getPrivate(), "password".toCharArray(), new Certificate[]{cert});

        try (FileOutputStream fos = new FileOutputStream("src/test/resources/broker.ks")) {
            keyStore.store(fos, "password".toCharArray());
        }
        System.out.println("Secure PKCS12 Keystore generated dynamically!");
    }

    @Test
    public void testRedistributor() throws Exception {

        generateKs();

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("host", "0.0.0.0");
        map.put("port", 61611);
        map.put("sslEnabled", "true");
        map.put("keyStorePath", "src/test/resources/broker.ks");
        map.put("keyStorePassword", "password");
        map.put("verifyHost", "false");



        ConfigurationImpl config1 =  createBasicConfig(0);
        config1.setName("broker1");
        config1.setSecurityEnabled(true);
        config1.getConnectorConfigurations().put("local", new TransportConfiguration(NETTY_CONNECTOR_FACTORY,map));
        config1.getAcceptorConfigurations().add(new TransportConfiguration(NETTY_ACCEPTOR_FACTORY,map));
        config1.setPopulateValidatedUser(true);


        Role role = new Role("programmers", true, true, true, true, true, true, true, true, true, true, true, true);
        HashSet<Role> roles = new HashSet<>();
        roles.add(role);

        config1.putSecurityRoles("#", roles);



        Map<String, String> securityMap = new HashMap<>();
        securityMap.put(ActiveMQBasicSecurityManager.BOOTSTRAP_USER, "admin");
        securityMap.put(ActiveMQBasicSecurityManager.BOOTSTRAP_PASSWORD, "admin");
        securityMap.put(ActiveMQBasicSecurityManager.BOOTSTRAP_ROLE, "admin");

        ActiveMQBasicSecurityManager activeMQBasicSecurityManager =  new ActiveMQBasicSecurityManager().init(securityMap);


      //  activeMQBasicSecurityManager.addNewUser("admin","admin", "admin");

        //server1 = createServer(false, config1);
        server1 = addServer(ActiveMQServers.newActiveMQServer(config1, ManagementFactory.getPlatformMBeanServer(), activeMQBasicSecurityManager, true));
        server1.start();


        // Running the client in a separate process so it doesn't inherit server's 2.53.x libraries.

        log.info("Testing the client without topology ");
        String val = testClient(false);
        log.info("This client works {}",val);
        log.info("Testing the client with topology ");
        val =  testClient(true);
        log.info("This client doesn't work {}",val);
        server1.stop();
        super.tearDown();

    }

    public String testClient(boolean useTopologyForLoadBalancing) throws  Exception {
        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-Djavax.net.ssl.trustStore=../amq-bc-test/src/test/resources/broker.ks");
        commands.add("-Djavax.net.ssl.trustStorePassword=password");
        commands.add("-jar");
        commands.add("../amq-bc-client/target/amq-bc-client-1.0-jar-with-dependencies.jar");
        commands.add(""+useTopologyForLoadBalancing); // Toggling useTopologyForLoadBalancing
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        AtomicReference<String> data = new AtomicReference<>("Success");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            boolean predicate = reader.lines().anyMatch(line -> {
                log.info(line);
                if (line.contains("AMQ219013: Timed out waiting to receive cluster topology")) {
                    data.set(line);
                    return true;
                }
                return false;
            });
            if  (predicate) {

                return data.get();
            }
        }
        process.waitFor();

        return data.get();
    }
}
