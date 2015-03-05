/* From https://raw.githubusercontent.com/antlr/grammars-v4/master/java/Java.g4
 [The "BSD licence"]
 Copyright (c) 2013 Terence Parr, Sam Harwell
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

grammar J1;

compilationUnit
	:	methodDeclaration+ EOF
	;

methodDeclaration									locals [ Method defn, Type tipe ]
    :   (type|'void') Identifier formalParameters dimensions
        (   methodBody
        |   ';'
        )
    ;
    
dimensions											locals [ int dim ]
	:	('[' ']')*
	;
	
formalParameters
    :   '(' formalParameterList? ')'
    ;

formalParameterList
    :   formalParameter (',' formalParameter)*
    ;

formalParameter										locals [ Variable defn, Type tipe ]
    :   type variableDeclaratorId
    ;

methodBody
    :   block
    ;

variableDeclarators
    :   variableDeclarator (',' variableDeclarator)*
    ;

variableDeclarator									locals [ Variable defn, Type tipe ]
    :   variableDeclaratorId ('=' variableInitializer)?
    ;

variableDeclaratorId
    :   Identifier dimensions
    ;

variableInitializer
    :   expression
    ;

type												locals [ Type tipe ]
    :   classOrInterfaceType dimensions				# objectType
    |   primitiveType dimensions					# primType
    ;

classOrInterfaceType								locals [ Scope refScope, Type tipe ]
    :   Identifier
    ;

primitiveType										locals [ Type tipe ]
    :   'boolean'
    |   'char'
    |   'byte'
    |   'short'
    |   'int'
    |   'long'
    |   'float'
    |   'double'
    ;

block
    :   '{' blockStatement* '}'
    ;

blockStatement
    :   localVariableDeclarationStatement
    |   statement
    //|   typeDeclaration
    ;

localVariableDeclarationStatement
    :    localVariableDeclaration ';'
    ;

localVariableDeclaration
    :   type variableDeclarators
    ;

statement
    :   block										# blkStatement
    |   'if' parExpression statement ('else' statement)?	# ifStatement
    |   'while' parExpression statement				# whileStatement
    |   'return' expression? ';'					# returnStatement
    |   'break' Identifier? ';'						# breakStatement
    |   'continue' Identifier? ';'					# continueStatement
    |   ';'											# emptyStatement
    |   expression ';'								# exprStatement
    ;

parExpression
    :   '(' expression ')'
    ;

expressionList
    :   expression (',' expression)*
    ;

expression											locals [ Type tipe ]
    :   primary										# primaryExpression
    |   expression '.' Identifier					# memberExpression
    |   expression '[' expression ']'				# indexExpression
    |   expression '(' expressionList? ')'			# callExpression
    |   'new' creator								# newExpression
    |   '(' type ')' expression						# castExpression
    |   ('+'|'-') expression						# plusExpression
    |   ('~'|'!') expression						# notExpression
    |   expression ('*'|'/'|'%') expression			# mulitplyExpression
    |   expression ('+'|'-') expression				# addExpression
    |   expression ('<' '<' | '>' '>' '>' | '>' '>') expression	# shiftExpression
    |   expression ('<=' | '>=' | '>' | '<') expression	# compareExpression
    |   expression 'instanceof' type				# instanceExpression
    |   expression ('==' | '!=') expression			# equalsExpression
    |   expression '&' expression					# binAndExpression
    |   expression '^' expression					# binExclExpression
    |   expression '|' expression					# binOrExpression
    |   expression '&&' expression					# conAndExpression
    |   expression '||' expression					# conOrExpression
    |   <assoc=right> expression
        (   '='
        )
        expression									# assignExpression
    ;

primary												locals [ Scope refScope, Type tipe ]
    :   '(' expression ')'							# parenPrimary
    |   'this'										# thisPrimary
    |   'super'										# superPrimary
    |   literal										# literalPrimary
    |   Identifier									# identPrimary
    ;

creator
    :   createdName (arrayCreatorRest | classCreatorRest)
    ;

createdName											locals [ Scope refScope, Type tipe ]
    :   Identifier
    |   primitiveType
    ;

arrayCreatorRest
    :   '['
        expression ']' ('[' expression ']')* ('[' ']')*
    ;

classCreatorRest
    :   arguments
    ;

arguments
    :   '(' expressionList? ')'
    ;

literal												locals [ Type tipe ]
    :   IntegerLiteral								# intLiteral
    |   FloatingPointLiteral						# floatLiteral
    |   CharacterLiteral							# charLiteral
    |   StringLiteral								# strLiteral
    |   BooleanLiteral								# boolLiteral
    |   'null'										# nullLiteral
    ;
    
// Lexer definitions

IntegerLiteral
    :   DecimalNumeral IntegerTypeSuffix?
    |   HexNumeral IntegerTypeSuffix?
    |   BinaryNumeral IntegerTypeSuffix?
    ;

fragment
IntegerTypeSuffix
    :   [lL]
    ;

fragment
DecimalNumeral
    :   '0'
    |   NonZeroDigit (Digits? | Underscores Digits)
    ;

fragment
Digits
    :   Digit (DigitOrUnderscore* Digit)?
    ;

fragment
Digit
    :   '0'
    |   NonZeroDigit
    ;

fragment
NonZeroDigit
    :   [1-9]
    ;

fragment
DigitOrUnderscore
    :   Digit
    |   '_'
    ;

fragment
Underscores
    :   '_'+
    ;

fragment
HexNumeral
    :   '0' [xX] HexDigits
    ;

fragment
HexDigits
    :   HexDigit (HexDigitOrUnderscore* HexDigit)?
    ;

fragment
HexDigit
    :   [0-9a-fA-F]
    ;

fragment
HexDigitOrUnderscore
    :   HexDigit
    |   '_'
    ;

fragment
BinaryNumeral
    :   '0' [bB] BinaryDigits
    ;

fragment
BinaryDigits
    :   BinaryDigit (BinaryDigitOrUnderscore* BinaryDigit)?
    ;

fragment
BinaryDigit
    :   [01]
    ;

fragment
BinaryDigitOrUnderscore
    :   BinaryDigit
    |   '_'
    ;

FloatingPointLiteral
    :   Digits '.' Digits? ExponentPart? FloatTypeSuffix?
    |   '.' Digits ExponentPart? FloatTypeSuffix?
    |   Digits ExponentPart FloatTypeSuffix?
    |   Digits FloatTypeSuffix
    |   HexSignificand BinaryExponent FloatTypeSuffix?
    ;

fragment
ExponentPart
    :   ExponentIndicator SignedInteger
    ;

fragment
ExponentIndicator
    :   [eE]
    ;

fragment
SignedInteger
    :   Sign? Digits
    ;

fragment
Sign
    :   [+-]
    ;

fragment
FloatTypeSuffix
    :   [fFdD]
    ;

fragment
HexSignificand
    :   HexNumeral '.'?
    |   '0' [xX] HexDigits? '.' HexDigits
    ;

fragment
BinaryExponent
    :   BinaryExponentIndicator SignedInteger
    ;

fragment
BinaryExponentIndicator
    :   [pP]
    ;

BooleanLiteral
    :   'true'
    |   'false'
    ;

CharacterLiteral
    :   '\'' SingleCharacter '\''
    |   '\'' EscapeSequence '\''
    ;

fragment
SingleCharacter
    :   ~['\\]
    ;
StringLiteral
    :   '"' StringCharacters? '"'
    ;

fragment
StringCharacters
    :   StringCharacter+
    ;

fragment
StringCharacter
    :   ~["\\]
    |   EscapeSequence
    ;

fragment
EscapeSequence
    :   '\\' [btnfr"'\\]
    |   UnicodeEscape
    ;

fragment
UnicodeEscape
    :   '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;

Identifier
    :   IdentifierStart IdentifierPart*
    ;

fragment
IdentifierStart
    :   [a-zA-Z$_]
    |   ~[\u0000-\u00FF\uD800-\uDBFF]
        {Character.isJavaIdentifierStart(_input.LA(-1))}?
    |   [\uD800-\uDBFF] [\uDC00-\uDFFF]
        {Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
    ;

fragment
IdentifierPart
    :   [a-zA-Z0-9$_]
    |   ~[\u0000-\u00FF\uD800-\uDBFF]
        {Character.isJavaIdentifierPart(_input.LA(-1))}?
    |   [\uD800-\uDBFF] [\uDC00-\uDFFF]
        {Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
    ;

WHITESPACE
	:  [ \t\r\n\u000C]+ -> skip
    ;

COMMENT
    :   '/*' .*? '*/' -> skip
    ;

LINE_COMMENT
    :   '//' ~[\r\n]* -> skip
    ;