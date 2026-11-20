package org.apache.activemq.artemis.tests.util;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.FileConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.NodeManager;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.activemq.artemis.core.server.impl.FileLockNodeManager;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;

import javax.management.MBeanServer;
import java.io.File;

public class ColocatedActiveMQServer extends ActiveMQServerImpl {

   private final NodeManager nodeManagerPrimary;
   private final NodeManager nodeManagerBackup;
   boolean backup = false;
   public ColocatedActiveMQServer backupServer;

   public ColocatedActiveMQServer(FileConfiguration fc,
                                  ActiveMQSecurityManager sm,
                                  NodeManager nodeManagerPrimary,
                                  NodeManager nodeManagerBackup) {
      super(fc, sm);
      this.nodeManagerPrimary = nodeManagerPrimary;
      this.nodeManagerBackup = nodeManagerBackup;
   }

   public ColocatedActiveMQServer(Configuration backupServerConfiguration,
                                  ActiveMQServer parentServer,
                                  NodeManager nodeManagerBackup,
                                  NodeManager nodeManagerPrimary) {
      super(backupServerConfiguration, null, null, parentServer);
      this.nodeManagerPrimary = nodeManagerPrimary;
      this.nodeManagerBackup = nodeManagerBackup;
   }

   public ColocatedActiveMQServer(Configuration configuration,
                                  MBeanServer platformMBeanServer,
                                  ActiveMQSecurityManager securityManager,
                                  NodeManager nodeManagerPrimary,
                                  NodeManager nodeManagerBackup) {
      super(configuration, platformMBeanServer, securityManager);
      this.nodeManagerPrimary = nodeManagerPrimary;
      this.nodeManagerBackup = nodeManagerBackup;
   }

   @Override
   protected NodeManager createNodeManager(final File directory, boolean replicatingBackup) {
      if (replicatingBackup) {
         return new FileLockNodeManager(directory, replicatingBackup, getConfiguration().getJournalLockAcquisitionTimeout(), null);
      } else {
         if (backup) {
            return nodeManagerBackup;
         } else {
            return nodeManagerPrimary;
         }
      }
   }

   @Override
   public ActiveMQServer createBackupServer(Configuration configuration) {
      ColocatedActiveMQServer backup = new ColocatedActiveMQServer(configuration, this, nodeManagerBackup, nodeManagerPrimary);
      backup.backup = true;
      this.backupServer = backup;
      return backup;
   }
}