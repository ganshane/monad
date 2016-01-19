// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core;

import com.google.protobuf.GeneratedMessage;
import monad.core.internal.ProtobufResponseResultProcessor;
import monad.core.services.ProtobufProcessor;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.annotations.EagerLoad;
import org.apache.tapestry5.ioc.services.StrategyBuilder;
import org.apache.tapestry5.ioc.util.StrategyRegistry;
import org.apache.tapestry5.services.ComponentEventResultProcessor;

import java.util.Map;

/**
 * 针对protobuf processor的处理
 * @author jcai
 */
public class ProtobufProcessorModule {
    @EagerLoad
    public static ProtobufProcessor buildProtobufProcessor(StrategyBuilder strategyBuilder,
                                                           Map<Class, ProtobufProcessor> configuration) {
        final StrategyRegistry<ProtobufProcessor> registry = StrategyRegistry.newInstance(
                ProtobufProcessor.class, configuration);
        return strategyBuilder.build(registry);
    }
    public static void contributeComponentEventResultProcessor(MappedConfiguration<Class, ComponentEventResultProcessor> configuration) {
        configuration.addInstance(GeneratedMessage.class, ProtobufResponseResultProcessor.class);
    }
}
