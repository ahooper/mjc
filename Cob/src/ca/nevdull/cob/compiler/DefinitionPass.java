package ca.nevdull.cob.compiler;

// Collect the class scope and symbol type structure, and attach it to the parse tree

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public class DefinitionPass extends PassCommon {
	Scope currentScope;

	public DefinitionPass(PassData data) {
		super(data);
		BaseScope globals = new BaseScope("globals",null);
		data.globals = globals;
		enterScope(globals);
	}
    
    private void enterScope(Scope newScope) {
    	Main.debug("enter %s enclosing=%s", newScope, newScope.getEnclosingScope());
	    currentScope = newScope;
    }
    
    private void leaveScope() {
    	Main.debug("leave %s", currentScope);
	    currentScope = currentScope.getEnclosingScope();
    }

	@Override public Void visitKlass(CobParser.KlassContext ctx) {
		Token nameToken = ctx.name;
		Token baseToken = ctx.base;
		ClassSymbol baseClass = null;
		if (baseToken == null) {
			baseClass = null;
		} else {
			String baseName = baseToken.getText();
			Symbol baseSymbol = currentScope.find(baseName);
			if (baseSymbol == null) Main.error(baseToken,baseName+" is not defined");
			else if (baseSymbol instanceof ClassSymbol) baseClass = (ClassSymbol)baseSymbol;
			else Main.error(baseToken,baseName+" is not a class");
		}
		ClassSymbol thisClass = new ClassSymbol(nameToken, currentScope, baseClass);
		thisClass.setType(thisClass);
		ctx.defn = thisClass;
		currentScope.add(thisClass);
		enterScope(thisClass);
		for (CobParser.MemberContext member : ctx.member()) {
			visit(member);
		}
		leaveScope();
		return null;
	}
	
	@Override public Void visitMethod(CobParser.MethodContext ctx) {
		//	'static'? type ID '(' arguments? ')' compoundStatement
		CobParser.TypeContext typeCtx = ctx.type();
		visitType(typeCtx);
		TerminalNode id = ctx.ID();
		MethodSymbol methSym = new MethodSymbol(id.getSymbol(),currentScope,typeCtx.tipe);
		methSym.setStatic(ctx.stat != null);
		ctx.defn = methSym;
		currentScope.add(methSym);
		enterScope(methSym);
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				visit(argument);
			}
		}
		visitCompoundStatement(ctx.compoundStatement());
		leaveScope();
		return null;
	}
	
	@Override public Void visitConstructor(CobParser.ConstructorContext ctx) {
		//	ID '(' arguments? ')' compoundStatement
		TerminalNode id = ctx.ID();
		String name = id.getText();
		if (currentScope instanceof ClassSymbol
				&& name.equals(((ClassSymbol)currentScope).getName()) ) {
		} else {
			Main.error(id,"Constructor name must match class name");
		}
		MethodSymbol methSym = new MethodSymbol(id.getSymbol(),currentScope,PrimitiveType.voidType);
		methSym.setStatic(true);
		currentScope.add(methSym);
		enterScope(methSym);
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				visit(argument);
			}
		}
		visitCompoundStatement(ctx.compoundStatement());
		leaveScope();
		return null;
	}
	
	@Override public Void visitNativeMethod(CobParser.NativeMethodContext ctx) {
		//	'native' type ID '(' arguments? ')' ';'
		CobParser.TypeContext typeCtx = ctx.type();
		visitType(typeCtx);
		TerminalNode id = ctx.ID();
		MethodSymbol methSym = new MethodSymbol(id.getSymbol(),currentScope,typeCtx.tipe);
		methSym.setNative(true);
		currentScope.add(methSym);
		enterScope(methSym);
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				visit(argument);
			}
		}
		leaveScope();
		return null;
	}
	
	@Override public Void visitInitializer(CobParser.InitializerContext ctx) {
		//	'static'? compoundStatement
		visitCompoundStatement(ctx.compoundStatement());
		return null;
	}
	
	@Override public Void visitFieldList(CobParser.FieldListContext ctx) {
		//	'static'? type ID ( '=' expression )? ( ',' ID ( '=' expression )? )* ';'
		CobParser.TypeContext typeCtx = ctx.type();
		visitType(typeCtx);
		for (CobParser.FieldContext field : ctx.field()) {
			visitField(field);
		}
		return null;
	}
	
	@Override public Void visitField(CobParser.FieldContext ctx) {
		//	ID ( '=' expression )?
		CobParser.FieldListContext list = (CobParser.FieldListContext)ctx.getParent();
		VariableSymbol varSym = new VariableSymbol(ctx.ID().getSymbol(),list.type().tipe);
		varSym.setStatic(list.stat != null);
		currentScope.add(varSym);
		CobParser.ExpressionContext e = ctx.expression();
		if (e != null) {
			visit(e);  // get reference scopes for initializations
		}
		return null;
	}
	
	@Override public Void visitArgument(CobParser.ArgumentContext ctx) {
		CobParser.TypeContext typeCtx = ctx.type();
		visitType(typeCtx);
		TerminalNode id = ctx.ID();
		VariableSymbol varSym = new VariableSymbol(id.getSymbol(),typeCtx.tipe);
		currentScope.add(varSym);
		ctx.defn = varSym;
		return null;
	}
	
	@Override public Void visitType(CobParser.TypeContext ctx) {
		visitTypeName(ctx.typeName());
		if (ctx.getChildCount() > 1) {
			ctx.tipe = new ArrayType(ctx.typeName().tipe);
		} else {
			ctx.tipe = ctx.typeName().tipe;
		}
		return null;
	}
	
	@Override public Void visitTypeName(CobParser.TypeNameContext ctx) {
		ctx.refScope = currentScope;
		//TODO following should be in a later pass
		TerminalNode id = ctx.ID();
		if (id == null) {
			ctx.tipe = PrimitiveType.getByName(ctx.start.getText());
			assert ctx.tipe != null;
		} else {
			Symbol defn = currentScope.find(id.getText());
			if (defn == null) {
				Main.error(id,id.getText()+" is not defined");
				ctx.tipe = UnknownType.instance;
			} else if (defn instanceof ClassSymbol){
				ctx.tipe = (ClassSymbol)defn;
			} else {
				Main.error(id,id.getText()+" is not a type");
			}
		}
		return null;
	}
	
	@Override public Void visitNamePrimary(CobParser.NamePrimaryContext ctx) {
		ctx.refScope = currentScope;
		return null;
	}
	
	@Override public Void visitCompoundStatement(CobParser.CompoundStatementContext ctx) {
		enterScope(new LocalScope(ctx.start.getLine(),currentScope));
		for (CobParser.BlockItemContext item : ctx.blockItem()) {
			visitBlockItem(item);
		}
		leaveScope();
		return null;
	}
	
	@Override public Void visitDeclaration(CobParser.DeclarationContext ctx) {
		//	type ID ( '=' expression )? ( ',' ID ( '=' expression )? )* ';'	
		CobParser.TypeContext typeCtx = ctx.type();
		visitType(typeCtx);
		for (CobParser.VariableContext var : ctx.variable()) {
			visitVariable(var);
		}
		return null;
	}
	
	@Override public Void visitVariable(CobParser.VariableContext ctx) {
		//	ID ( '=' expression )?
		CobParser.DeclarationContext list = (CobParser.DeclarationContext)ctx.getParent();
		VariableSymbol varSym = new VariableSymbol(ctx.ID().getSymbol(),list.type().tipe);
		varSym.setStatic(false);
		currentScope.add(varSym);
		CobParser.ExpressionContext e = ctx.expression();
		if (e != null) {
			visit(e);  // get reference scopes for initializations
		}
		return null;
	}
	
	@Override public Void visitForDeclStatement(CobParser.ForDeclStatementContext ctx) {
		enterScope(new LocalScope(ctx.start.getLine(),currentScope));
        visitChildren(ctx);
		leaveScope();
		return null;
	}
	
    @Override public Void visitStringPrimary(CobParser.StringPrimaryContext ctx) {
    	//TODO assign a static struct to hold unique strings
        visitChildren(ctx);
        return null;
    }
	
}
