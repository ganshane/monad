// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.rpc.services

import com.google.protobuf.ExtensionRegistry

/**
 * config registry
 */
trait ProtobufExtensionRegistryConfiger {
  def config(registry: ExtensionRegistry)
}
