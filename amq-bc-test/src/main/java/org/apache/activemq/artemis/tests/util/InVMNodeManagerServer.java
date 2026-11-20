package org.apache.activemq.artemis.tests.util;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.server.NodeManager;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;

import javax.management.MBeanServer;
import java.io.File;

public final class InVMNodeManagerServer extends ActiveMQServerImpl {

   final NodeManager nodeManager;

   public InVMNodeManagerServer(final NodeManager nodeManager) {
      super();
      this.nodeManager = nodeManager;
   }

   public InVMNodeManagerServer(final Configuration configuration, final NodeManager nodeManager) {
      super(configuration);
      this.nodeManager = nodeManager;
   }

   public InVMNodeManagerServer(final Configuration configuration,
                                final MBeanServer mbeanServer,
                                final NodeManager nodeManager) {
      super(configuration, mbeanServer);
      this.nodeManager = nodeManager;
   }

   public InVMNodeManagerServer(final Configuration configuration,
                                final ActiveMQSecurityManager securityManager,
                                final NodeManager nodeManager) {
      super(configuration, securityManager);
      this.nodeManager = nodeManager;
   }

   public InVMNodeManagerServer(final Configuration configuration,
                                final MBeanServer mbeanServer,
                                final ActiveMQSecurityManager securityManager,
                                final NodeManager nodeManager) {
      super(configuration, mbeanServer, securityManager);
      this.nodeManager = nodeManager;
   }

   @Override
   protected NodeManager createNodeManager(final File directory, boolean replicatingBackup) {
      return nodeManager;
   }

}