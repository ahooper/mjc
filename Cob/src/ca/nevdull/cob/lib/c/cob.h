#ifndef COB_DEFN
#define COB_DEFN

typedef unsigned char	cob_boolean;
typedef signed char		cob_byte;
typedef unsigned char	cob_char;
typedef double			cob_double;
typedef float			cob_float;
typedef int				cob_int;
typedef long int		cob_long;
typedef short int		cob_short;

#define cob_null			((void *)0)
#define cob_true			(cob_boolean)1
#define cob_false			(cob_boolean)0

#define COB_CLASS_INIT(klass) \
	if (!klass ## _ClassInfo.class.classInitialized) __INIT_ ## klass();
#define COB_ENTER_METHOD(klass,methodStr)
#define COB_EXIT_METHOD()
#define COB_SOURCE_FILE(fileNameStr) \
	static char *_sourceFile=fileNameStr;
#define COB_SOURCE_LINE(lineNum)

struct Class_Object;

struct ClassTable {
    int             classInitialized;
    int				classInitializationBegan;
    const char*     className;
    const char*     packageName;
//  const char*     enclosingClassName;
//  const char*     enclosingMethodName;
    struct Class_Object	*classClass;
    struct Class_Object	*baseClass;
    struct Class_Object *arrayClass;
};

#endif /*COB_DEFN*/
