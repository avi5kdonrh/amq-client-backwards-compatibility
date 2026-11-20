# amq-client-backwards-compatibility

- Run 'mvn test' that starts a local broker but starts the client in a different JVM (amq-bc-client)
- Client that uses useTopologyForLoadBalancing=false works fine.
- Client that uses the default settings times out after 30 seconds with:
```shell
2026-11-20 07:45:25,045 INFO  [BackwardsCompatibilityTest] Caused by: ActiveMQConnectionTimedOutException[errorType=CONNECTION_TIMEDOUT message=AMQ219013: Timed out waiting to receive cluster topology. Group:null]
```