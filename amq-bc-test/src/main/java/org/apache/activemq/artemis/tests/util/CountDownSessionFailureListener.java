package org.apache.activemq.artemis.tests.util;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.SessionFailureListener;

import java.util.concurrent.CountDownLatch;

public final class CountDownSessionFailureListener implements SessionFailureListener {

   private final CountDownLatch latch;
   private final ClientSession session;

   public CountDownSessionFailureListener(ClientSession session) {
      this(1, session);
   }

   public CountDownSessionFailureListener(int n, ClientSession session) {
      latch = new CountDownLatch(n);
      this.session = session;
   }

   public CountDownSessionFailureListener(CountDownLatch latch, ClientSession session) {
      this.latch = latch;
      this.session = session;
   }

   @Override
   public void connectionFailed(ActiveMQException exception, boolean failedOver, String scaleDownTargetNodeID) {
      connectionFailed(exception, failedOver);
   }

   @Override
   public void connectionFailed(ActiveMQException exception, boolean failedOver) {
      latch.countDown();
      session.removeFailureListener(this);

   }

   public CountDownLatch getLatch() {
      return latch;
   }

   @Override
   public void beforeReconnect(ActiveMQException exception) {
      // No-op
   }

}