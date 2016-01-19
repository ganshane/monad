// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include <stdio.h>

#include "emscripten.h"

int main(){
  EM_ASM(
    if (analytics_onready){
      analytics_onready();
    }
    analytics_loaded = true;
  );
  return 0;
}
