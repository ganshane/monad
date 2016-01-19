// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.extjs.internal;

import monad.extjs.annotations.ExtDirectMethod;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ValueEncoder;
import org.apache.tapestry5.annotations.RequestParameter;
import org.apache.tapestry5.func.F;
import org.apache.tapestry5.func.Flow;
import org.apache.tapestry5.func.Mapper;
import org.apache.tapestry5.func.Predicate;
import org.apache.tapestry5.internal.services.ComponentClassCache;
import org.apache.tapestry5.internal.transform.EventHandlerMethodParameterProvider;
import org.apache.tapestry5.internal.transform.EventHandlerMethodParameterSource;
import org.apache.tapestry5.ioc.OperationTracker;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.util.CollectionFactory;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.*;
import org.apache.tapestry5.runtime.ComponentEvent;
import org.apache.tapestry5.runtime.Event;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.TransformConstants;
import org.apache.tapestry5.services.ValueEncoderSource;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * ext direct method worker
 * @author jcai
 */
public class ExtDirectMethodWorker implements ComponentClassTransformWorker2
{
    private final Logger logger = LoggerFactory.getLogger(ExtDirectMethodWorker.class);
    private final Request request;

    private final ValueEncoderSource valueEncoderSource;

    private final ComponentClassCache classCache;

    private final OperationTracker operationTracker;


    private final InstructionBuilderCallback RETURN_TRUE = new InstructionBuilderCallback()
    {
        public void doBuild(InstructionBuilder builder)
        {
            builder.loadConstant(true).returnResult();
        }
    };

    /**
     * Encapsulates information needed to invoke a method as an event handler method, including the logic
     * to construct parameter values, and match the method against the
     */
    class EventHandlerMethod
    {
        final PlasticMethod method;

        final MethodDescription description;

        final String eventType, componentId;

        final EventHandlerMethodParameterSource parameterSource;

        int minContextValues = 0;

        EventHandlerMethod(PlasticMethod method)
        {
            this.method = method;
            description = method.getDescription();

            parameterSource = buildSource();

            String methodName = method.getDescription().methodName;

            //ExtDirectMethod onEvent = method.getAnnotation(ExtDirectMethod.class);

            eventType = "extjs_"+methodName;//extractEventType(methodName, onEvent);
            componentId = "";//extractComponentId(methodName, onEvent);
        }

        void buildMatchAndInvocation(InstructionBuilder builder, final LocalVariable resultVariable)
        {
            final PlasticField sourceField =
                    parameterSource == null ? null
                            : method.getPlasticClass().introduceField(EventHandlerMethodParameterSource.class, description.methodName + "$parameterSource").inject(parameterSource);

            builder.loadArgument(0).loadConstant(eventType).loadConstant(componentId).loadConstant(minContextValues);
            builder.invoke(ComponentEvent.class, boolean.class, "matches", String.class, String.class, int.class);

            builder.when(Condition.NON_ZERO, new InstructionBuilderCallback()
            {
                public void doBuild(InstructionBuilder builder)
                {
                    builder.loadArgument(0).loadConstant(method.getMethodIdentifier()).invoke(Event.class, void.class, "setMethodDescription", String.class);

                    builder.loadThis();

                    int count = description.argumentTypes.length;

                    for (int i = 0; i < count; i++)
                    {
                        builder.loadThis().getField(sourceField).loadArgument(0).loadConstant(i);

                        builder.invoke(EventHandlerMethodParameterSource.class, Object.class, "get",
                                ComponentEvent.class, int.class);

                        builder.castOrUnbox(description.argumentTypes[i]);
                    }

                    builder.invokeVirtual(method);

                    if (!method.isVoid())
                    {
                        builder.boxPrimitive(description.returnType);
                        builder.loadArgument(0).swap();

                        builder.invoke(Event.class, boolean.class, "storeResult", Object.class);

                        // storeResult() returns true if the method is aborted. Return true since, certainly,
                        // a method was invoked.
                        builder.when(Condition.NON_ZERO, RETURN_TRUE);
                    }

                    // Set the result to true, to indicate that some method was invoked.

                    builder.loadConstant(true).storeVariable(resultVariable);
                }
            });
        }


        private EventHandlerMethodParameterSource buildSource()
        {
            final String[] parameterTypes = method.getDescription().argumentTypes;

            if (parameterTypes.length == 0)
            {
                return null;
            }

            final List<EventHandlerMethodParameterProvider> providers = CollectionFactory.newList();

            int contextIndex = 0;

            for (int i = 0; i < parameterTypes.length; i++)
            {
                String type = parameterTypes[i];

                EventHandlerMethodParameterProvider provider = parameterTypeToProvider.get(type);

                if (provider != null)
                {
                    providers.add(provider);
                    continue;
                }

                RequestParameter parameterAnnotation = method.getParameters().get(i).getAnnotation(RequestParameter.class);

                if (parameterAnnotation != null)
                {
                    String parameterName = parameterAnnotation.value();

                    providers.add(createQueryParameterProvider(method, i, parameterName, type,
                            parameterAnnotation.allowBlank()));
                    continue;
                }

                // Note: probably safe to do the conversion to Class early (class load time)
                // as parameters are rarely (if ever) component classes.

                providers.add(createEventContextProvider(type, contextIndex++));
            }


            minContextValues = contextIndex;

            EventHandlerMethodParameterProvider[] providerArray = providers.toArray(new EventHandlerMethodParameterProvider[providers.size()]);

            return new EventHandlerMethodParameterSource(method.getMethodIdentifier(), operationTracker, providerArray);
        }
    }


    /**
     * Stores a couple of special parameter type mappings that are used when matching the entire event context
     * (either as Object[] or EventContext).
     */
    private final Map<String, EventHandlerMethodParameterProvider> parameterTypeToProvider = CollectionFactory.newMap();

    {
        // Object[] and List are out-dated and may be deprecated some day

        parameterTypeToProvider.put("java.lang.Object[]", new EventHandlerMethodParameterProvider()
        {

            public Object valueForEventHandlerMethodParameter(ComponentEvent event)
            {
                return event.getContext();
            }
        });

        parameterTypeToProvider.put(List.class.getName(), new EventHandlerMethodParameterProvider()
        {

            public Object valueForEventHandlerMethodParameter(ComponentEvent event)
            {
                return Arrays.asList(event.getContext());
            }
        });

        // This is better, as the EventContext maintains the original objects (or strings)
        // and gives the event handler method access with coercion
        parameterTypeToProvider.put(EventContext.class.getName(), new EventHandlerMethodParameterProvider()
        {
            public Object valueForEventHandlerMethodParameter(ComponentEvent event)
            {
                return event.getEventContext();
            }
        });
    }

    public ExtDirectMethodWorker(Request request, ValueEncoderSource valueEncoderSource, ComponentClassCache classCache, OperationTracker operationTracker,

                         @Symbol(SymbolConstants.UNKNOWN_COMPONENT_ID_CHECK_ENABLED)
                         boolean componentIdCheck)
    {
        this.request = request;
        this.valueEncoderSource = valueEncoderSource;
        this.classCache = classCache;
        this.operationTracker = operationTracker;
    }

    public void transform(PlasticClass plasticClass, TransformationSupport support, MutableComponentModel model)
    {
        Flow<PlasticMethod> methods = matchEventHandlerMethods(plasticClass);

        if (methods.isEmpty())
        {
            return;
        }

        addEventHandlingLogic(plasticClass, support.isRootTransformation(), methods, model);
    }


    private void addEventHandlingLogic(final PlasticClass plasticClass, final boolean isRoot, final Flow<PlasticMethod> plasticMethods, final MutableComponentModel model)
    {
        Flow<EventHandlerMethod> eventHandlerMethods = plasticMethods.map(new Mapper<PlasticMethod, EventHandlerMethod>()
        {
            public EventHandlerMethod map(PlasticMethod element)
            {
                return new EventHandlerMethod(element);
            }
        });

        implementDispatchMethod(plasticClass, isRoot, model, eventHandlerMethods);
    }

    private void implementDispatchMethod(final PlasticClass plasticClass, final boolean isRoot, final MutableComponentModel model, final Flow<EventHandlerMethod> eventHandlerMethods)
    {
        plasticClass.introduceMethod(TransformConstants.DISPATCH_COMPONENT_EVENT_DESCRIPTION).changeImplementation(new InstructionBuilderCallback()
        {
            public void doBuild(InstructionBuilder builder)
            {
                builder.startVariable("boolean", new LocalVariableCallback()
                {
                    public void doBuild(LocalVariable resultVariable, InstructionBuilder builder)
                    {
                        if (!isRoot)
                        {
                            // As a subclass, there will be a base class implementation (possibly empty).

                            builder.loadThis().loadArguments().invokeSpecial(plasticClass.getSuperClassName(), TransformConstants.DISPATCH_COMPONENT_EVENT_DESCRIPTION);

                            // First store the result of the super() call into the variable.
                            builder.storeVariable(resultVariable);
                            builder.loadArgument(0).invoke(Event.class, boolean.class, "isAborted");
                            builder.when(Condition.NON_ZERO, RETURN_TRUE);
                        } else
                        {
                            // No event handler method has yet been invoked.
                            builder.loadConstant(false).storeVariable(resultVariable);
                        }

                        for (EventHandlerMethod method : eventHandlerMethods)
                        {
                            method.buildMatchAndInvocation(builder, resultVariable);

                            logger.debug("add event {} to {} ",method.eventType,method.componentId);
                            model.addEventHandler(method.eventType);
                        }

                        builder.loadVariable(resultVariable).returnResult();
                    }
                });
            }
        });
    }

    private Flow<PlasticMethod> matchEventHandlerMethods(PlasticClass plasticClass)
    {
        return F.flow(plasticClass.getMethods()).filter(new Predicate<PlasticMethod>()
        {
            public boolean accept(PlasticMethod method)
            {
                return method.hasAnnotation(ExtDirectMethod.class);
            }
        });
    }


    private EventHandlerMethodParameterProvider createQueryParameterProvider(PlasticMethod method, final int parameterIndex, final String parameterName,
                                                                             final String parameterTypeName, final boolean allowBlank)
    {
        final String methodIdentifier = method.getMethodIdentifier();

        return new EventHandlerMethodParameterProvider()
        {
            @SuppressWarnings("unchecked")
            public Object valueForEventHandlerMethodParameter(ComponentEvent event)
            {
                try
                {
                    String parameterValue = request.getParameter(parameterName);

                    if (!allowBlank && InternalUtils.isBlank(parameterValue))
                        throw new RuntimeException(String.format(
                                "The value for query parameter '%s' was blank, but a non-blank value is needed.",
                                parameterName));

                    Class parameterType = classCache.forName(parameterTypeName);

                    ValueEncoder valueEncoder = valueEncoderSource.getValueEncoder(parameterType);

                    Object value = valueEncoder.toValue(parameterValue);

                    if (parameterType.isPrimitive() && value == null)
                        throw new RuntimeException(
                                String.format(
                                        "Query parameter '%s' evaluates to null, but the event method parameter is type %s, a primitive.",
                                        parameterName, parameterType.getName()));

                    return value;
                } catch (Exception ex)
                {
                    throw new RuntimeException(
                            String.format(
                                    "Unable process query parameter '%s' as parameter #%d of event handler method %s: %s",
                                    parameterName, parameterIndex + 1, methodIdentifier,
                                    InternalUtils.toMessage(ex)), ex);
                }
            }
        };
    }

    private EventHandlerMethodParameterProvider createEventContextProvider(final String type, final int parameterIndex)
    {
        return new EventHandlerMethodParameterProvider()
        {
            public Object valueForEventHandlerMethodParameter(ComponentEvent event)
            {
                return event.coerceContext(parameterIndex, type);
            }
        };
    }
}
