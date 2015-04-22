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
		for (CobParser.MemberContext member : ctx.member()) {
			visit(member);
		}
		if (!staticPass) {
			writeDefn("};\n");
			writeDefn("struct ",name,"_Class {\n");
			writeDefn("  struct ClassTable class;\n");
			writeDefn("  struct ",name,"_Methods methods;\n");
			writeDefn("};\n");
			writeDefn("extern struct ",name,"_Class ",name,"_Class;\n");
			writeDefn("extern void ",name,"_",PassCommon.CLASSINIT,"();\n");		
			writeDefn("extern void ",name,"_",PassCommon.INSTANCEINIT,"(",name," this);\n");		
		} else {
			writeDefn("#endif /*",name,"_DEFN*/\n");
		}
		return null;
	}
	
	@Override public Void visitMethod(CobParser.MethodContext ctx) {
		//	'static'? type ID '(' arguments? ')' compoundStatement
		if ((ctx.stat != null)^staticPass) return null;  // two visits, one processing virtual methods, then static
		CobParser.KlassContext parent = (CobParser.KlassContext)ctx.getParent();
		String className = parent.name.getText();
		Type type = ctx.type().tipe;
		TerminalNode id = ctx.ID();
		String sep = "";
		if (staticPass) {
			writeDefn("extern ",type.getNameString()," ",type.getArrayString(),className,"_",id.getText(),"(");
		} else {
			writeDefn("  ",type.getNameString()," (*",type.getArrayString(),id.getText(),")(",className," this");
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
	
	@Override public Void visitConstructor(CobParser.ConstructorContext ctx) {
		//	ID '(' arguments? ')' compoundStatement
		if (!staticPass) return null;
		CobParser.KlassContext parent = (CobParser.KlassContext)ctx.getParent();
		String className = parent.name.getText();
		TerminalNode id = ctx.ID();
		assert id.getText().equals(className);
		String sep = "";
		writeDefn("extern void ",className,"_",id.getText(),"(");
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
		Type type = ctx.type().tipe;
		TerminalNode id = ctx.ID();
		String sep = "";
		writeDefn("extern ",type.getNameString()," ",type.getArrayString(),className,"_",id.getText(),"(");
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
		Type type = ctx.type().tipe;
		writeDefn(type.getNameString()," ",type.getArrayString(),ctx.ID().getText());
		return null;
	}
	
	@Override public Void visitFieldList(CobParser.FieldListContext ctx) {
		//	'static'? type ID ( '=' expression )? ( ',' ID ( '=' expression )? )* ';'
		if (ctx.stat == null) return null;  // non-static fields have already been placed in the object instance
		if (!staticPass) return null;  // not processing static fields on this visit
		for (CobParser.FieldContext field : ctx.field()) {
			visitField(field);
		}
		return null;
	}
	
	@Override public Void visitField(CobParser.FieldContext ctx) {
		//	ID ( '=' expression )?
		CobParser.FieldListContext list = (CobParser.FieldListContext)ctx.getParent();
		CobParser.KlassContext parent = (CobParser.KlassContext)list.getParent();
		String className = parent.name.getText();
		Type type = list.type().tipe;
		writeDefn(type.getNameString()," ",type.getArrayString(),className,"_",ctx.ID().getText(),";\n");
		return null;
	}
	
}
