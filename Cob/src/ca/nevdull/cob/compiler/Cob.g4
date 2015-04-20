grammar Cob;

file
	:	klass EOF
	;

klass																	locals [ ClassSymbol defn ]
	:	'class' name=ID ':' ( base=ID | 'null' ) '{' member* '}'
	;

member																	locals [ Symbol defn ]
	:	stat='static'? type ID '(' arguments? ')' compoundStatement						# method
	|	'native' type ID '(' arguments? ')' ';'											# nativeMethod
	|	stat='static'? type ID ( '=' expression )? ( ',' ID ( '=' expression )? )* ';'	# field
	;

arguments
	:	argument ( ',' argument )* ( ',' '...' )?
	;
	
argument																locals [ Symbol defn ]
	:	type ID
	;
	
type																	locals [ Type tipe ]	
	: 	typeName ( '[' ']' )?
	;
	
typeName																locals [ Scope refScope, Type tipe ]
	:	'boolean'
	|	'byte'
	|	'char'
	|	'double'
	|	'float'
	|	'int'
	|	'long'
	|	'short'
	|	'void'
	|	ID
	;

primary																	locals [ Scope refScope, Type tipe ]
    :   ID																# namePrimary
    |	'this'															# thisPrimary
    |   Integer															# integerPrimary
    |   Floating														# floatingPrimary
    |   String+															# stringPrimary
    |	'null'															# nullPrimary
    |	'true'															# truePrimary
    |	'false'															# falsePrimary
    |   '(' sequence ')'												# parenPrimary
    |   primary '[' sequence ']'										# indexPrimary
    |   primary '(' expressionList? ')'									# callPrimary
    |   primary  '.' ID '(' expressionList? ')'							# invokePrimary
    |   primary '.' ID													# memberPrimary
    |   primary '++'													# incrementPrimary
    |   primary '--'													# decrementPrimary
    ;

expressionList
    :   assignment ( ',' assignment )?
    ;

unary																	locals [ Type tipe ]
    :   primary															# primaryUnary
    |   '++' unary														# incrementUnary
    |   '--' unary														# decrementUnary
    |   ( '&' | '*' | '+' | '-' | '~' | '!' ) cast						# operatorUnary
    ;

cast																	locals [ Type tipe ]
    :   unary															# unaryCast
    |   '(' typeName ')' cast											# typeCast
    ;

expression																locals [ Type tipe ]
    :   cast															# castExpression
    |   expression ( '*' | '/' | '%' ) expression						# multiplyExpression
    |   expression ( '+' | '-')  expression								# addExpression
    |   expression ( '<<' | '>>' ) expression							# shiftExpression
    |   expression ( '<' | '>' | '<=' | '>=') expression				# compareExpression
    |   expression ( '==' | '!=' ) expression							# equalExpression
    |   expression '&' expression										# andExpression
    |   expression '^' expression										# exclusiveExpression
    |   expression '|' expression										# orExpression
    |   expression '&&' expression										# andThenExpression
    |   expression '||' expression										# orElseExpression
    |   expression '?' expression ':' expression						# conditionalExpression
    ;

assignment																locals [ Type tipe ]
    :   expression
    |   unary ( '=' | '*=' | '/=' | '%=' | '+=' | '-=' | '<<=' | '>>=' | '&=' | '^=' | '|=' ) assignment
    ;

sequence																locals [ Type tipe ]
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

declaration																	locals [ Symbol defn ]
	:	type ID ( '=' expression )? ( ',' ID ( '=' expression )? )* ';'
		//TODO static local
	;

statement
    :   ID ':' statement													# labelStatement
    |   compoundStatement													# cmpdStatement
    |   sequence? ';'														# expressionStatement
    |   'if' '(' sequence ')' statement ('else' statement)?					# ifStatement
    |   'switch' '(' sequence ')' '{' switchItem+ '}'						# switchStatement
    |   'while' '(' sequence ')' statement									# whileStatement
    |   'do' statement 'while' '(' sequence ')' ';'							# doStatement
    |   'for' '(' sequence? ';' sequence? ';' sequence? ')' statement		# forStatement
    |   'for' '(' declaration sequence? ';' sequence? ')' statement			# forDeclStatement
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
	| 	'boolean' | 'byte' | 'class' | 'false' | 'native' | 'null' | 'super' | 'this' | 'true'  // Cob only
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
