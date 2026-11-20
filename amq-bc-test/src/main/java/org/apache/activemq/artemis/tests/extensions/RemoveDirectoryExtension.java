
package org.apache.activemq.artemis.tests.extensions;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.apache.activemq.artemis.utils.FileUtil;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveDirectoryExtension implements AfterEachCallback, Extension {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   private final String directoryPath;

   public RemoveDirectoryExtension(String directoryPath) {
      Objects.requireNonNull(directoryPath, "Directory to delete must not be null");

      this.directoryPath = directoryPath;
   }

   @Override
   public void afterEach(ExtensionContext context) throws Exception {
      logger.trace("Removing directory: {}",  directoryPath);

      FileUtil.deleteDirectory(new File(directoryPath));
   }
}