
package org.apache.activemq.artemis.lockmanager.file;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

import org.apache.activemq.artemis.lockmanager.DistributedLock;

final class FileDistributedLock implements DistributedLock {

   private final String lockId;
   private final Consumer<String> onClosedLock;
   private boolean closed;
   private FileLock fileLock;
   private final FileChannel channel;

   FileDistributedLock(Consumer<String> onClosedLock, File locksFolder, String lockId) throws IOException {
      this.onClosedLock = onClosedLock;
      this.lockId = lockId;
      this.closed = false;
      this.fileLock = null;
      this.channel = FileChannel.open(new File(locksFolder, lockId).toPath(), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
   }

   private void checkNotClosed() {
      if (closed) {
         throw new IllegalStateException("This lock is closed");
      }
   }

   @Override
   public String getLockId() {
      checkNotClosed();
      return lockId;
   }

   @Override
   public boolean isHeldByCaller() {
      checkNotClosed();
      final FileLock fileLock = this.fileLock;
      if (fileLock == null) {
         return false;
      }
      return fileLock.isValid();
   }

   @Override
   public boolean tryLock() {
      checkNotClosed();
      final FileLock fileLock = this.fileLock;
      if (fileLock != null) {
         throw new IllegalStateException("unlock first");
      }
      final FileLock lock;
      try {
         lock = channel.tryLock();
      } catch (OverlappingFileLockException o) {
         // this process already hold this lock, but not this manager
         return false;
      } catch (Throwable t) {
         throw new IllegalStateException(t);
      }
      if (lock == null) {
         return false;
      }
      this.fileLock = lock;
      return true;
   }

   @Override
   public void unlock() {
      checkNotClosed();
      final FileLock fileLock = this.fileLock;
      if (fileLock != null) {
         this.fileLock = null;
         try {
            fileLock.close();
         } catch (IOException e) {
            // noop
         }
      }
   }

   @Override
   public void addListener(UnavailableLockListener listener) {
      checkNotClosed();
      // noop
   }

   @Override
   public void removeListener(UnavailableLockListener listener) {
      checkNotClosed();
      // noop
   }

   public boolean isClosed() {
      return closed;
   }

   public void close(boolean useCallback) {
      if (closed) {
         return;
      }
      try {
         if (useCallback) {
            onClosedLock.accept(lockId);
         }
         unlock();
         channel.close();
      } catch (IOException e) {
         // ignore it
      } finally {
         closed = true;
      }
   }

   @Override
   public void close() {
      close(true);
   }

   public FileChannel getChannel() {
      return channel;
   }
}