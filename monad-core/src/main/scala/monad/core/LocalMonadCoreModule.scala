// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core

import monad.core.config.LocalStoreConfigSupport
import monad.core.internal._
import monad.core.services._
import monad.rpc.internal.RpcServerFinderWithZk
import monad.rpc.services.RpcServerFinder
import org.apache.tapestry5.ioc._
import org.apache.tapestry5.ioc.annotations.{Contribute, Local, Match}
import org.apache.tapestry5.ioc.services.{FactoryDefaults, ServiceOverride, SymbolProvider}
import org.apache.tapestry5.plastic.MethodInvocation
import org.slf4j.Logger

import scala.util.control.NonFatal

/**
 * local monad core module
 */
object LocalMonadCoreModule {
  def bind(binder: ServiceBinder) {
    binder.bind(classOf[MachineHeartbeat], classOf[MachineHeartbeatImpl]).
      withId("MachineHeartbeat")
    binder.bind(classOf[RpcServerFinder], classOf[RpcServerFinderWithZk]).
      withId("RpcServerFinderWithZk")
    binder.bind(classOf[ErrorReporter], classOf[ErrorReporterImpl]).
      withId("ZKErrorReporter")
    binder.bind(classOf[MetricsService], classOf[MetricsServiceImpl]).
      withId("MetricsService")
    binder.bind(classOf[RpcMetricsAdvice], classOf[RpcMetricsAdviceImpl]).
      withId("RpcMetricsAdvice")
  }

  @Contribute(classOf[ServiceOverride])
  def provideZkRpcFinder(configuration: MappedConfiguration[Class[_], Object],
                         @Local rpcServerFinder: RpcServerFinder) {
    configuration.add(classOf[RpcServerFinder], rpcServerFinder)
  }

  def buildLocalSimpleStore(config: LocalStoreConfigSupport): LocalSimpleStore = {
    new LocalSimpleStore(config.localStoreDir)
  }


  @Contribute(classOf[SymbolProvider])
  @FactoryDefaults
  def provideFactoryDefaults(configuration: MappedConfiguration[String, AnyRef]) {
    configuration.add(MonadCoreSymbols.MONAD_MODE, "production")
  }

  @Match(Array("NettyRpcClient", "RpcClientMessageHandler", "RpcServerMessageHandler"))
  def adviseRpc(receiver: MethodAdviceReceiver, @Local rpcAdvice: RpcMetricsAdvice) {
    rpcAdvice.advice(receiver)
  }

  @Match(Array("PeriodicExecutor"))
  def advisePeriodicExecutor(receiver: MethodAdviceReceiver, logger: Logger) {
    val advice = new org.apache.tapestry5.plastic.MethodAdvice() {
      def advise(invocation: MethodInvocation) {
        if (invocation.getMethod.getName == "addJob") {
          val runnable = invocation.getParameter(2).asInstanceOf[Runnable]
          invocation.setParameter(2, new Runnable {
            override def run(): Unit = {
              try {
                runnable.run()
              } catch {
                case NonFatal(e) =>
                  logger.error(e.getMessage, e)
              }
            }
          })
        }
        invocation.proceed()
      }
    }

    receiver.adviseAllMethods(advice)
  }
}
