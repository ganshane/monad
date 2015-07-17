// Copyright (c) 2015 The Monad Authors. All rights reserved.

#include "ppapi/cpp/instance.h"
#include "ppapi/cpp/module.h"
#include "ppapi/cpp/var.h"
#include "ppapi/cpp/var_dictionary.h"

#include "sparse_bit_set_wrapper.h"

namespace monad {
  /** set id search api url **/
  const char* const kSetUrlCommand= "SetUrl";
  const char* const kQueryCommand="Query";
  const char* const kReplyString = "hello wellfrom find NaCl";
  class AnalyticsInstance : public pp::Instance {
  public:
    explicit AnalyticsInstance(PP_Instance instance)
        : pp::Instance(instance) {}
    virtual ~AnalyticsInstance() {}

    virtual void HandleMessage(const pp::Var& var_message) {
      //json object
      if (!var_message.is_dictionary())
        return;
      pp::VarDictionary parameters(var_message);
      pp::Var var_command = parameters.Get("cmd");
      if (!var_command.is_string()) {
        fprintf(stderr, "Expect dict item \"command\" to be a string.\n");
        return;
      }
      std::string command = var_command.AsString();
      if (command == kSetUrlCommand ) {
        pp::Var var_reply(kReplyString);
        PostMessage(var_reply);
      }else if(command == kQueryCommand){
        SparseBitSetWrapper wrapper;
        wrapper.NewSeg(1,10000);
        for(int i=0;i<10000;i++){
          wrapper.FastSet(i);
        }
        pp::Var var_reply(wrapper.BitCount());
        PostMessage(var_reply);
      }

    }
  };
  class AnalyticsModule : public pp::Module {
  public:
    AnalyticsModule() : pp::Module() {}
    virtual ~AnalyticsModule() {}
    virtual pp::Instance* CreateInstance(PP_Instance instance) {
      return new AnalyticsInstance(instance);
    }
  };
}//namespace monad

namespace pp {
  Module* CreateModule() {
    return new monad::AnalyticsModule();
  }
}  // namespace pp
