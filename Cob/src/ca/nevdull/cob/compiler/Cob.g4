grammar Cob;

file
	:	klass EOF
	;

klass
	:	'class' name=ID ':' parent=ID '{' member* '}'
	;

member
	:	'static'? type ID '(' arguments? ')' compoundStatement				# method
	|	'static'? type ID ( '=' expression )? ( ',' ID ( '=' expression )? )* ';'	# field
	;

arguments
	:	argument ( ',' argument )* ( ',' '...' )?
	;
	
argument
	:	type ID
	;
	
type
	: 	typeName ( '[' ']' )?
	;
	
typeName
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

primary
    :   ID																# namePrimary
    |	'this'															# thisPrimary
    |   Number															# numberPrimary
    |   String+															# stringPrimary
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

unary
    :   primary															# primaryUnary
    |   '++' unary														# incrementUnary
    |   '--' unary														# decrementUnary
    |   ( '&' | '*' | '+' | '-' | '~' | '!' ) cast						# operatorUnary
    ;

cast
    :   unary															# unaryCast
    |   '(' typeName ')' cast											# typeCast
    ;

expression
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

assignment
    :   expression
    |   unary ( '=' | '*=' | '/=' | '%=' | '+=' | '-=' | '<<=' | '>>=' | '&=' | '^=' | '|=' ) assignment
    ;

sequence
    :   assignment ( ',' assignment )*
    ;

constantExpression
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
	:	type ID ( '=' expression )? ( ',' ID ( '=' expression )? )* ';'	
	;

statement
    :   ID ':' statement
    |   compoundStatement
    |   sequence? ';'
    |   'if' '(' sequence ')' statement ('else' statement)?
    |   'switch' '(' sequence ')' '{' switchItem+ '}'
    |   'while' '(' sequence ')' statement
    |   'do' statement 'while' '(' sequence ')' ';'
    |   'for' '(' sequence? ';' sequence? ';' sequence? ')' statement
    |   'for' '(' declaration sequence? ';' sequence? ')' statement
    |   'continue' ';'
    |   'break' ';'
    |   'return' sequence? ';'
    ;

switchItem
    :   'case' constantExpression ':' statement
    |   'default' ':' statement
    ;

Reserved
	:	'break' | 'case' | 'char' | 'const' | 'continue' | 'default' | 'do' | 'double' | 'else' | 'enum' | 'extern'
	|	'float' | 'for' | 'if' | 'int' | 'long' | 'return' | 'short' | 'signed' | 'sizeof' | 'static' | 'struct'
	|	'switch' | 'this' | 'typedef' | 'unsigned' | 'void' | 'while' | 'boolean' | 'byte' | 'class'
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
	
fragment IDfirst
	:	[A-Za-z_]
	;
	
fragment IDrest
	:	IDfirst
	|	[0-9]
	;

Number
    :   [1-9] Digits? [lL]?
    |   Fractional Exponent? [fFlL]?
    |   Digits Exponent [fFlL]?
    |   '0' [xX] HexaNumber
    |	'0'
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
HexaNumber
    :   HexaDigits [lL]?
    |   HexaFractional BinaryExponent [fFlL]?
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
