grammar Cob;

file
	:	impourt* klass EOF
	;

impourt
	:	'import' ID ';'
	;

klass																	locals [ ClassSymbol defn ]
	:	'class' name=ID ':' ( base=ID | 'null' ) '{' member* '}'
	;

member																	locals [ Symbol defn ]
	:	stat='static'? type ID '(' arguments? ')' body					# method
	|	ID '(' arguments? ')' body										# constructor
	|	'native' type ID '(' arguments? ')' ';'							# nativeMethod
	|	stat='static'? type field ( ',' field )* ';'					# fieldList
	|	stat='static'? compoundStatement								# initializer
	;

arguments
	:	argument ( ',' argument )* ( ',' '...' )?
	;
	
argument																locals [ Symbol defn ]
	:	type ID
	;

body
	:	compoundStatement
	|	';'
	;
	
type																	locals [ Type tipe ]	
	: 	typeName ( '[' ']' )?
	;
	
typeName																locals [ Scope refScope, Type tipe ]
	:	primitiveType
	|	ID
	;
	
primitiveType															locals [ Type tipe ]
	:	'boolean'
	|	'byte'
	|	'char'
	|	'double'
	|	'float'
	|	'int'
	|	'long'
	|	'short'
	|	'void'
	;

field
	:	ID ( '=' expression )?
	;

primary																	locals [ Scope refScope, Type tipe, Exp exp ]
    :   ID																# namePrimary
    |	'this'															# thisPrimary
    |   Integer															# integerPrimary
    |   Floating														# floatingPrimary
    |   String+															# stringPrimary
    |	'null'															# nullPrimary
    |	'true'															# truePrimary
    |	'false'															# falsePrimary
    |   '(' sequence ')'												# parenPrimary
    |   'new' ID '(' expressionList? ')'								# newPrimary
    |   primary '[' sequence ']'										# indexPrimary
    |   primary '(' expressionList? ')'									# callPrimary
    |   primary  '.' ID '(' expressionList? ')'							# invokePrimary
    |   primary '.' ID													# memberPrimary
    |   primary op=( '++' | '--' )										# incrementPrimary
    ;

expressionList															locals [ Exp exp ]
    :   assignment ( ',' assignment )*
    ;

unary																	locals [ Type tipe, Exp exp ]
    :   primary															# primaryUnary
    |   op=( '++' | '--' ) unary										# incrementUnary
    |   op=( '+' | '-' | '~' | '!' ) cast								# operatorUnary
    ;

cast																	locals [ Type tipe, Exp exp ]
    :   unary															# unaryCast
    |   '(' typeName ')' cast											# typeCast
    ;

expression																locals [ Type tipe, Exp exp ]
    :   cast															# castExpression
    |   l=expression op=( '*' | '/' | '%' ) r=expression				# multiplyExpression
    |   l=expression op=( '+' | '-')  r=expression						# addExpression
    |   l=expression op=( '<<' | '>>' ) r=expression					# shiftExpression
    |   l=expression op=( '<' | '>' | '<=' | '>=') r=expression			# compareExpression
    |   l=expression op=( '==' | '!=' ) r=expression					# equalExpression
    |   l=expression op='&' r=expression								# andExpression
    |   l=expression op='^' r=expression								# exclusiveExpression
    |   l=expression op='|' r=expression								# orExpression
    |   l=expression op='&&' r=expression								# andThenExpression
    |   l=expression op='||' r=expression								# orElseExpression
    |   c=expression op='?' t=expression ':' f=expression				# conditionalExpression
    ;

assignment																locals [ Type tipe, Exp exp ]
    :   expression
    |   l=unary op=( '=' | '*=' | '/=' | '%=' | '+=' | '-=' | '<<=' | '>>=' | '&=' | '^=' | '|=' ) r=assignment
    ;

sequence																locals [ Type tipe, Exp exp ]
    :   assignment ( ',' assignment )*
    ;

constantExpression														locals [ Type tipe ]
    :   expression
    ;

compoundStatement
    :   '{' blockItem* '}'
    ;

blockItem
    :   declaration
    |   statement
    ;

declaration
	:	type variable ( variable )* ';'
		//TODO static local
	;
	
variable																	locals [ Symbol defn ]
	:	ID ( '=' expression )?
	;

statement
    :   ID ':' statement													# labelStatement
    |   compoundStatement													# cmpdStatement
    |   sequence? ';'														# expressionStatement
    |   'if' '(' sequence ')' t=statement ('else' f=statement)?				# ifStatement
    |   'switch' '(' sequence ')' '{' switchItem+ '}'						# switchStatement
    |   'while' '(' sequence ')' statement									# whileStatement
    |   'do' statement 'while' '(' sequence ')' ';'							# doStatement
    |   'for' '(' b=sequence? ';' w=sequence? ';' a=sequence? ')' statement	# forStatement
    |   'for' '(' declaration w=sequence? ';' a=sequence? ')' statement		# forDeclStatement
    |   'continue' ';'														# continueStatement
    |   'break' ';'															# breakStatement
    |   'return' sequence? ';'												# returnStatement
    ;

switchItem
    :   'case' constantExpression ':' statement
    |   'default' ':' statement
    ;

Reserved
	:	'break' | 'case'  | 'continue' | 'default' | 'do' | 'else'								// Cob and C 
	|	'for' | 'if' | 'return' | 'static' | 'switch' | 'while' 
	|	'char' | 'double' | 'float' | 'int' | 'long' | 'short' | 'void'							// Cob and C types
	| 	'boolean' | 'byte' | 'class' | 'false' | 'native' | 'new' | 'null' | 'super' | 'this' | 'true'  // Cob only
	|	'const' | 'enum' | 'extern' | 'signed' | 'sizeof' | 'struct' | 'typedef' | 'unsigned'	// C only
	;

LeftParen : '(';
RightParen : ')';
LeftBracket : '[';
RightBracket : ']';
LeftBrace : '{';
RightBrace : '}';

Less : '<';
LessEqual : '<=';
Greater : '>';
GreaterEqual : '>=';
LeftShift : '<<';
RightShift : '>>';

Plus : '+';
PlusPlus : '++';
Minus : '-';
MinusMinus : '--';
Star : '*';
Div : '/';
Mod : '%';

And : '&';
Or : '|';
AndAnd : '&&';
OrOr : '||';
Caret : '^';
Not : '!';
Tilde : '~';

Question : '?';
Colon : ':';
Semi : ';';
Comma : ',';

Assign : '=';
// '*=' | '/=' | '%=' | '+=' | '-=' | '<<=' | '>>=' | '&=' | '^=' | '|='
StarAssign : '*=';
DivAssign : '/=';
ModAssign : '%=';
PlusAssign : '+=';
MinusAssign : '-=';
LeftShiftAssign : '<<=';
RightShiftAssign : '>>=';
AndAssign : '&=';
XorAssign : '^=';
OrAssign : '|=';

Equal : '==';
NotEqual : '!=';

Dot : '.';
Ellipsis : '...';

ID
	:	IDfirst IDrest*
	;
	
fragment
IDfirst
	:	[A-Za-z_]
	;
	
fragment
IDrest
	:	IDfirst
	|	[0-9]
	;

Integer
    :   [1-9] Digits? [lL]?
    |   '0' [xX] HexaDigits [lL]?
    |	'0'
    ;

Floating
	:   Fractional Exponent? [fFlL]?
    |   Digits Exponent [fFlL]?
    |	'0' [xX] HexaFloat
	;

fragment
Fractional
    :   Digits? '.' Digits
    |   Digits '.'
    ;

fragment
Exponent
    :   [eE] [+\-]? Digits
    ;

fragment
Digits
    :   [0-9]+
    ;
    
fragment
HexaFloat
    :   HexaFractional BinaryExponent [fFlL]?
    |   HexaDigits BinaryExponent [fFlL]?
    ;

fragment
HexaDigits
    :   [0-9a-fA-F]+
    ;

fragment
HexaFractional
    :   HexaDigits? '.' HexaDigits
    |   HexaDigits '.'
    ;

fragment
BinaryExponent
    :   [pP] [+\-]? Digits
    ;
	
String
	:	'"' SChar* '"'
	;	

fragment
SChar
    :   ~["\\\r\n]
    |   '\\' .
    ;
	
Space
    :   [ \t\r\n]+ -> skip ;
   
Comment
    :   '/*' .*? '*/' -> skip ;
    
LineComment
    :   '//' ~[\r\n]* -> skip ;
