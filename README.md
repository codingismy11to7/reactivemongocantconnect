Connection race condition (?)
=============================

Issue
-----

Rarely, the first reactivemongo connection on the driver fails to connect, and will never connect.
This seems to me to be a race condition because of how sporadically it happens, and turning on 
reactivemongo debug logging seems to keep it from happening (because things are running slower in
this case?)

Environment
-----------

* 2018 MacBook Pro, 2.9GHz Core i9, 32GB ram, NVMe storage
* MacOS 10.14.6
* OpenJDK Runtime Environment Corretto-8.202.08.1 (build 1.8.0_202-b08)
* Mongo Community v4.2.0, same machine, default settings, no authentication
* ReactiveMongo v0.18.4

Reproduction
------------

* Check out repo
* `sbt pack`
* `./target/pack/bin/test-app`
* (to eliminate any confounding factors, a new JVM is started for each test)

Output
------

```
...
sucessfully inserted, retrying


09:52:21.728 [test-akka.actor.default-dispatcher-2] INFO akka.event.slf4j.Slf4jLogger - Slf4jLogger started
09:52:21.788 [main] INFO reactivemongo.api.Driver - No mongo-async-driver configuration found
09:52:21.816 [main] INFO reactivemongo.api.Driver - [Supervisor-1] Creating connection: Connection-1
09:52:21.839 [reactivemongo-akka.actor.default-dispatcher-5] INFO reactivemongo.core.actors.MongoDBSystem - [Supervisor-1/Connection-1] Starting the MongoDBSystem
09:52:21.850 [reactivemongo-akka.actor.default-dispatcher-5] INFO reactivemongo.core.netty.Pack - Instantiated reactivemongo.core.netty.Pack
09:52:22.016 [nioEventLoopGroup-2-1] INFO reactivemongo.core.nodeset.ChannelFactory - [Supervisor-1/Connection-1] Skip channel init as host is null
Success(DefaultWriteResult(true,1,List(),None,None,None))
09:52:22.216 [main] INFO reactivemongo.api.Driver - [Supervisor-1] Closing instance of ReactiveMongo driver
09:52:22.216 [reactivemongo-akka.actor.default-dispatcher-2] INFO reactivemongo.core.actors.MongoDBSystem - [Supervisor-1/Connection-1] Stopping the MongoDBSystem
[INFO] [08/28/2019 09:52:22.218] [reactivemongo-akka.actor.default-dispatcher-6] [akka://reactivemongo/user/Connection-1] Message [reactivemongo.core.actors.ChannelDisconnected] without sender to Actor[akka://reactivemongo/user/Connection-1#1201853241] was not delivered. [1] dead letters encountered. If this is not an expected behavior, then [Actor[akka://reactivemongo/user/Connection-1#1201853241]] may have terminated unexpectedly, This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
[INFO] [08/28/2019 09:52:22.218] [reactivemongo-akka.actor.default-dispatcher-6] [akka://reactivemongo/user/Connection-1] Message [scala.Tuple2] from Actor[akka://reactivemongo/user/Connection-1#1201853241] to Actor[akka://reactivemongo/user/Connection-1#1201853241] was not delivered. [2] dead letters encountered. If this is not an expected behavior, then [Actor[akka://reactivemongo/user/Connection-1#1201853241]] may have terminated unexpectedly, This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
09:52:22.224 [reactivemongo-akka.actor.default-dispatcher-4] INFO reactivemongo.api.Driver - [Supervisor-1] Stopping the monitor...
09:52:22.250 [reactivemongo-akka.actor.default-dispatcher-3] INFO reactivemongo.core.nodeset.ChannelFactory - [Supervisor-1/Connection-1] Cannot create channel to 'localhost:27017' from inactive factory
09:52:22.252 [reactivemongo-akka.actor.default-dispatcher-3] WARN reactivemongo.core.actors.MongoDBSystem - [Supervisor-1/Connection-1] Cannot create connection for Node(localhost:27017,Primary,Vector(Connection([id: 0x32f23d54, L:0.0.0.0/0.0.0.0:55588 ! R:localhost/127.0.0.1:27017],Connecting,Set(),None)),Set(),None,ProtocolMetadata(2.6, 4.0),PingInfo(96703834, 0, -1, None),false)
reactivemongo.core.errors.GenericDriverException: MongoError['Cannot create channel to 'localhost:27017' from inactive factory (Supervisor-1/Connection-1)']
sucessfully inserted, retrying


09:52:27.427 [test-akka.actor.default-dispatcher-3] INFO akka.event.slf4j.Slf4jLogger - Slf4jLogger started
09:52:27.487 [main] INFO reactivemongo.api.Driver - No mongo-async-driver configuration found
09:52:27.513 [main] INFO reactivemongo.api.Driver - [Supervisor-1] Creating connection: Connection-1
09:52:27.536 [reactivemongo-akka.actor.default-dispatcher-3] INFO reactivemongo.core.actors.MongoDBSystem - [Supervisor-1/Connection-1] Starting the MongoDBSystem
09:52:27.547 [reactivemongo-akka.actor.default-dispatcher-3] INFO reactivemongo.core.netty.Pack - Instantiated reactivemongo.core.netty.Pack
09:52:35.743 [reactivemongo-akka.actor.default-dispatcher-3] WARN reactivemongo.api.MongoConnection - [Supervisor-1/Connection-1] Timeout after 8200 milliseconds while probing the connection monitor: IsPrimaryAvailable#120478350?

Failed! will continue to retry

reactivemongo.core.actors.Exceptions$PrimaryUnavailableException: MongoError['No primary node is available! (Supervisor-1/Connection-1)']
	at reactivemongotest.Query$.dbF$1(TestApp.scala:51)
	at reactivemongo.api.MongoConnection.database(MongoConnection.scala:90)
Caused by: reactivemongo.core.actors.Exceptions$InternalState: 
	at reactivemongo.ChannelConnected(8a2cf3f2, {{NodeSet None Node[localhost:27017: Unknown (0/0/1 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430206800402536>)
	at reactivemongo.Start({{NodeSet None Node[localhost:27017: Unknown (0/0/1 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430206776717669>)
16:38:36.747 [reactivemongo-akka.actor.default-dispatcher-5] WARN reactivemongo.api.MongoConnection - [Supervisor-1/Connection-1] Timeout after 8200 milliseconds while probing the connection monitor: IsPrimaryAvailable#252277567?

Failed! will continue to retry

reactivemongo.core.actors.Exceptions$PrimaryUnavailableException: MongoError['No primary node is available! (Supervisor-1/Connection-1)']
	at reactivemongotest.Query$.dbF$1(TestApp.scala:51)
	at reactivemongo.api.MongoConnection.database(MongoConnection.scala:90)
Caused by: reactivemongo.core.actors.Exceptions$InternalState: 
	at reactivemongo.ChannelConnected(0044e94b, {{NodeSet None Node[localhost:27017: Unknown (9/9/10 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430216806482548>)
	at reactivemongo.ChannelConnected(bb5e8435, {{NodeSet None Node[localhost:27017: Unknown (8/8/10 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430216805993747>)
	at reactivemongo.ChannelConnected(a6de23a7, {{NodeSet None Node[localhost:27017: Unknown (7/7/10 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430216805468788>)
	at reactivemongo.ChannelConnected(f75bde2c, {{NodeSet None Node[localhost:27017: Unknown (6/6/10 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430216804988739>)
	at reactivemongo.ChannelConnected(f6203e22, {{NodeSet None Node[localhost:27017: Unknown (5/5/10 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430216804493386>)
	at reactivemongo.ChannelConnected(b7b04123, {{NodeSet None Node[localhost:27017: Unknown (4/4/10 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430216803925691>)
	at reactivemongo.ChannelConnected(9d6e7ceb, {{NodeSet None Node[localhost:27017: Unknown (3/3/10 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430216803484263>)
	at reactivemongo.ChannelConnected(18721078, {{NodeSet None Node[localhost:27017: Unknown (2/2/10 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430216802946840>)
	at reactivemongo.ChannelConnected(4568a3da, {{NodeSet None Node[localhost:27017: Unknown (1/1/10 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430216802394205>)
	at reactivemongo.ConnectAll({{NodeSet None Node[localhost:27017: Unknown (1/1/1 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430216801564036>)
	at reactivemongo.RefreshAll({{NodeSet None Node[localhost:27017: Unknown (1/1/1 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430216792364735>)
	at reactivemongo.ChannelConnected(8a2cf3f2, {{NodeSet None Node[localhost:27017: Unknown (0/0/1 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430206800402536>)
	at reactivemongo.Start({{NodeSet None Node[localhost:27017: Unknown (0/0/1 available connections), latency=9223372036854775807ns, authenticated={}] }})(<time:430206776717669>)
16:38:45.969 [reactivemongo-akka.actor.default-dispatcher-6] WARN reactivemongo.api.MongoConnection - [Supervisor-1/Connection-1] Timeout after 8200 milliseconds while probing the connection monitor: IsPrimaryAvailable#664070838?

Failed! will continue to retry
...
```