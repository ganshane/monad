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
