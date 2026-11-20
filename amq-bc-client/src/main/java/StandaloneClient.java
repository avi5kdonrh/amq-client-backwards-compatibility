import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;

public class StandaloneClient {

    private static final Logger log = LoggerFactory.getLogger(StandaloneClient.class);

    public static void main(String[] args) throws  Exception {

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                "tcp://localhost:61611?" +
                        "sslEnabled=true&" +
                        "useTopologyForLoadBalancing="+args[0]);
        connectionFactory.setUseTopologyForLoadBalancing(false);
        connectionFactory.setUser("admin");
        connectionFactory.setPassword("admin");

        // Creating multiple connection so the client tries to utilize the full topology
        for (int i = 0; i < 5; i++) {
            log.info("Waiting till callTimeout={} to create a connection ................",connectionFactory.getCallTimeout());
            Connection connection = connectionFactory.createConnection();
            log.info("Connection created {}",connection);
           // connection.close();
        }
    }
}
