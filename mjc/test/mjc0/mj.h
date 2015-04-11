#include <stdlib.h>

//#include <uchar.h>
typedef uint16_t char16_t;

typedef unsigned char	jboolean;
typedef char			jbyte;
typedef char16_t		jchar;
typedef double			jdouble;
typedef float			jfloat;
typedef int				jint;
typedef long int		jlong;
typedef short int		jshort;

typedef void * errorType;  // TEMPORARY

#ifndef Class_DEFN
typedef struct Class_obj *Class;
#endif
extern struct Class_class_obj *Class_class_p;

extern void throwNPE();
