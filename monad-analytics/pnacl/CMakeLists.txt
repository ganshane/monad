# Copyright 2016 the original author or authors. All rights reserved.
# site: http://www.ganshane.com
AUX_SOURCE_DIRECTORY("." ANALYTICS_PNACL_SOURCE)
SET(PNACL_APP monad_analytics)
SET(PNACL_APP_FINAL ${PNACL_APP}.final)
ADD_EXECUTABLE(${PNACL_APP} ${ANALYTICS_PNACL_SOURCE})
TARGET_LINK_LIBRARIES(${PNACL_APP}  analytics ppapi_cpp ppapi)
ADD_CUSTOM_COMMAND(OUTPUT ${PNACL_APP_FINAL}
      COMMAND ${PNACL_TOOLCHAIN}/bin/pnacl-finalize ${PNACL_APP} -o ${PNACL_APP_FINAL}
      COMMENT "finalize analytics bitcode to pnacl")

ADD_CUSTOM_TARGET(analytics-final ALL DEPENDS ${PNACL_APP_FINAL})

ADD_DEPENDENCIES(analytics-final ${PNACL_APP})
