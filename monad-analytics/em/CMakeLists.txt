# Copyright 2016 the original author or authors. All rights reserved.
# site: http://www.ganshane.com
AUX_SOURCE_DIRECTORY("." ANALYTICS_EM_SOURCE)
SET(EM_APP monad_analytics)
ADD_EXECUTABLE(${EM_APP} ${ANALYTICS_EM_SOURCE})
TARGET_LINK_LIBRARIES(${EM_APP}  analytics )
