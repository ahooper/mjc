package ca.nevdull.cob.compiler;

// Produce the class methods list structure to the class definition file 

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public class MethodsPass extends PassCommon {
	
	boolean staticPass;
	
	public MethodsPass(PassData data, boolean staticPass) {
		super(data);
		this.staticPass = staticPass;
		//NB staticPass must follow non-static
	}

	@Override public Void visitKlass(CobParser.KlassContext ctx) {
		String name = ctx.name.getText();
		if (!staticPass) {
			writeDefn("struct ",name,"_Methods {\n");
			Token base = ctx.base;
			if (base != null) {
				writeDefn("  struct ",base.getText(),"_Methods *_base;\n");
			}
		}
		for (CobParser.MemberContext decl : ctx.member()) {
			visit(decl);
		}
		if (!staticPass) {
			writeDefn("};\n");
			writeDefn("struct ",name,"_Class {\n");
			writeDefn("  struct ClassTable class;\n");
			writeDefn("  struct ",name,"_Methods methods;\n");
			writeDefn("};\n");
			writeDefn("extern struct ",name,"_Class ",name,"_Class;\n");
		} else {
			writeDefn("#endif /*",name,"_DEFN*/\n");
		}
		return null;
	}
	
	@Override public Void visitMethod(CobParser.MethodContext ctx) {
		//	'static'? type ID '(' arguments? ')' '{' code '}'
		if ((ctx.stat != null)^staticPass) return null;  // two visits, one processing virtual methods, then static
		CobParser.KlassContext parent = (CobParser.KlassContext)ctx.getParent();
		String className = parent.name.getText();
		CobParser.TypeContext type = ctx.type();
		String typeName = type.typeName().getText();
		String array = "";
		if (type.getChildCount() > 1) array = "*";
		TerminalNode id = ctx.ID();
		String sep = "";
		if (staticPass) {
			writeDefn("extern ",typeName," ",array,className,"_",id.getText(),"(");
		} else {
			writeDefn("  ",typeName," (*",array,id.getText(),")(",className," this");
			sep = ",";
		}
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				writeDefn(sep);  sep = ",";
				visit(argument);
			}
		}
		writeDefn(");\n");
		return null;
	}
	
	@Override public Void visitNativeMethod(CobParser.NativeMethodContext ctx) {
		//	'native' type ID '(' arguments? ')' ';'
		if (!staticPass) return null;
		CobParser.KlassContext parent = (CobParser.KlassContext)ctx.getParent();
		String className = parent.name.getText();
		CobParser.TypeContext type = ctx.type();
		String typeName = type.typeName().getText();
		String array = "";
		if (type.getChildCount() > 1) array = "*";
		TerminalNode id = ctx.ID();
		String sep = "";
		writeDefn("extern ",typeName," ",array,className,"_",id.getText(),"(");
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				writeDefn(sep);  sep = ",";
				visit(argument);
			}
		}
		writeDefn(");\n");
		return null;
	}
	
	@Override public Void visitArgument(CobParser.ArgumentContext ctx) {
		CobParser.TypeContext type = ctx.type();
		String typeName = type.typeName().getText();
		String array = "";
		if (type.getChildCount() > 1) array = "*";
		writeDefn(typeName," ",array,ctx.ID().getText());
		return null;
	}
	
	@Override public Void visitField(CobParser.FieldContext ctx) {
		//	'static'? type ID ( '=' code )? ( ',' ID ( '=' code )? )* ';'
		if (ctx.stat == null) return null;  // non-static fields have already been placed in the object instance
		if (!staticPass) return null;  // not processing static fields on this visit
		CobParser.KlassContext parent = (CobParser.KlassContext)ctx.getParent();
		String className = parent.name.getText();
		CobParser.TypeContext type = ctx.type();
		String typeName = type.typeName().getText();
		String array = "";
		if (type.getChildCount() > 1) array = "*";
		for (TerminalNode id : ctx.ID()) {
			writeDefn(typeName," ",array,className,"_",id.getText(),";\n");
		}
		return null;
	}
	
}
