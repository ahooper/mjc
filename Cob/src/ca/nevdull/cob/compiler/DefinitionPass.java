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
    	System.out.print("enter ");
	    System.out.print(newScope);
	    System.out.print(" enclosing=");
	    System.out.print(newScope.getEnclosingScope());
	    System.out.println();
	    currentScope = newScope;
    }
    
    private void leaveScope() {
    	System.out.print("leave ");
	    System.out.println(currentScope);
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
		for (CobParser.MemberContext decl : ctx.member()) {
			visit(decl);
		}
		leaveScope();
		return null;
	}
	
	@Override public Void visitMethod(CobParser.MethodContext ctx) {
		//	'static'? type ID '(' arguments? ')' '{' code '}'
		CobParser.TypeContext typeCtx = ctx.type();
		visitType(typeCtx);
		TerminalNode id = ctx.ID();
		MethodSymbol methSym = new MethodSymbol(id.getSymbol(),currentScope,typeCtx.tipe);
		methSym.setStatic(ctx.stat != null);
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
	
	@Override public Void visitField(CobParser.FieldContext ctx) {
		//	'static'? type ID ( '=' code )? ( ',' ID ( '=' code )? )* ';'
		CobParser.TypeContext typeCtx = ctx.type();
		visitType(typeCtx);
		for (TerminalNode id : ctx.ID()) {
			VariableSymbol varSym = new VariableSymbol(id.getSymbol(),typeCtx.tipe);
			varSym.setStatic(ctx.stat != null);
			currentScope.add(varSym);
		}
		for (CobParser.ExpressionContext e : ctx.expression()) {
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
		for (TerminalNode id : ctx.ID()) {
			VariableSymbol varSym = new VariableSymbol(id.getSymbol(),typeCtx.tipe);
			currentScope.add(varSym);
		}
		for (CobParser.ExpressionContext e : ctx.expression()) {
			visit(e);  // get reference scopes for initializations
		}
		return null;
	}

    @Override public Void visitStringPrimary(CobParser.StringPrimaryContext ctx) {
    	//TODO assign a static struct to hold unique strings
        visitChildren(ctx);
        return null;
    }
	
}
