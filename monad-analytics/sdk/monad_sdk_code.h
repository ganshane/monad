#ifndef MONAD_SDK_CODE_H_
#define MONAD_SDK_CODE_H_
#ifdef __cplusplus
extern "C" {
#endif
enum MONAD_CODE{
    MONAD_OK = 0,
    MONAD_FAIL_PUT_COLLECTION,
    MONAD_WRONG_ID_NUM,/* 错误的身份证号码 */
    MONAD_LAST /* never use */
};
#ifdef __cplusplus
}
#endif
#endif //MONAD_SDK_CODE_H_
