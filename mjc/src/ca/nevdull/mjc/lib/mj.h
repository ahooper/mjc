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
#ifndef Object_DEFN
typedef struct Object_obj *Object;
#endif
extern struct Object_class_obj *Object_class_p;
#ifndef String_DEFN
typedef struct String_obj *String;
#endif
extern struct String_class_obj *String_class_p;

extern void throwNPE();
#define checkPtr(p) ((p)?:throwNPE()) /*gcc extension*/
#define toBoolean(v) ((v)?1:0)
