#ifndef MONAD_TYPES_H_
#define MONAD_TYPES_H_
namespace monad {
  //禁止默认的copy构造和copy赋值操作
  class Uncopyable{
    protected:
      Uncopyable(){}
      ~Uncopyable(){}
    private:
      Uncopyable(const Uncopyable&);
      Uncopyable& operator=(const Uncopyable&);
  };
}
#endif //MONAD_TYPES_H_
