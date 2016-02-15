# NB: result will need some hand touch ups for special cases
while (<>) {
	chomp;
	if (/^\s*grammar (\w+)\s*;/) {
		$Grammar = $1;
		$grammar = lc $Grammar;
		print <<"_HEAD_";
package ca.nevdull.$grammar.compiler;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.Token;

public class ModelVisitor {
	boolean traceVisit = false;
	int nest = 0;
	String indent = "    ";
	
	void traceIn(String m) {
		for (int i = 0;  i < nest;  i += 1) System.out.print(indent);
		System.out.print('/');
		System.out.println(m);
		nest += 1;
	}
	void traceOut(String m) {
		nest -= 1;
		for (int i = 0;  i < nest;  i += 1) System.out.print(indent);
		System.out.print('\\');
		System.out.println(m);
	}
	void traceDisc(String m) {
		for (int i = 0;  i < nest;  i += 1) System.out.print(indent);
		System.out.println(m);
	}
	void fail(String m) {
		Main.error(m);
	}
	private void put(TerminalNode id) {
		put(id.getSymbol().getText());
	}
	private void put(String text) {
		System.out.print(text);
	}
_HEAD_
	} elsif (/^\s*\/\*/) {
		while (<G>) {
			chomp;
			last if /^\s*\*\//;
		}
	} elsif (/^\s*\/\//) {
	} elsif (/^([A-Z]|fragment)/) {
	} elsif (/^(\S)(\S+)\s*($|locals)/) {
		$Prod = (uc $1) . $2;  $Main = $Prod;
		print "    public Void visit${Prod}(${Grammar}Parser.${Prod}Context ctx) {\n";
		#print "        if (traceVisit) traceIn(\"visit${Prod}\");\n";
		@discrim = ();  $else = '';
		while (<>) {
			chomp;
			last if /^\s*;/;
			print "        // ",$_,"\n";
			s/\/\*.*\*\///g;
			if (s/#\s*(\S)(\S+)\s*$//) {
				$p = (uc $1) . $2;
				#print "        if (traceVisit) traceOut(\"visit${Prod}\");\n";
				print "        return null;\n";
				print "    }\n";
				$Prod = $p;
				push @discrim, "        ${else}if (ctx instanceof ${Grammar}Parser.${Prod}Context) visit${Prod}((${Grammar}Parser.${Prod}Context) ctx);\n";
				$else = 'else ';
				print "    public Void visit${Prod}(${Grammar}Parser.${Prod}Context ctx) {\n";
				#print "        if (traceVisit) traceIn(\"visit${Prod}\");\n";
			}
			@terms = split;
			shift @terms;  %count = ();
			for $term (@terms) {
				if ($term =~ /^\/\//) {
					last;
				} elsif ($term =~ /^\'(.*)\'$/) {
					print "        put(\"$1\");\n";
				} elsif ($term =~ /^\(/) {
				} elsif ($term =~ /^\)/) {
				} elsif ($term =~ /^\</) {
				} elsif ($term =~ /^\|/) {
				} elsif ($term =~ /^[A-Z]/) {
					print "        put(ctx.$term());\n";
				} else {
					$mult = ($term =~ s/([*+?])$//) ? $1 : '';
					$term =~ s/\)$//;
					($v,$nont) = split(/=/,$term,2);
					if ($nont) {
						$term = $nont;
					}
					$Term = (uc substr $term,0,1).substr($term,1);
					$var = substr $term,0,1;
					$seq = $count{$var};
					#print "        visit${Term}(ctx.${term}());\n";
					#print "        ${Grammar}Parser.${Term}Context ${var}${seq} = ctx.${term}(${seq});  if (${var}${seq} != null) ${var}${seq}.accept(this);\n";
					if ($mult eq "*" || mult eq "+") {
						print "        for (${Grammar}Parser.${Term}Context ${var} : ctx.${term}()) visit${Term}(${var});\n";
					} elsif ($nont) {
						print "        if (ctx.${v} != null) visit${Term}(ctx.${v});\n";
					} else {
						print "        ${Grammar}Parser.${Term}Context ${var}${seq} = ctx.${term}(${seq});  if (${var}${seq} != null) visit${Term}(${var}${seq});\n";
					}
					$count{$var} += 1;
				}
			}
		}	
		#print "        if (traceVisit) traceOut(\"visit${Prod}\");\n";
		print "        return null;\n";
		print "    }\n";
		if (@discrim) {
			print "    /*Replacement discriminated version*/\n";
			print "    public Void visit${Main}(${Grammar}Parser.${Main}Context ctx) {\n";
			#print "        if (traceVisit) traceDisc(\"visit${Main}\");\n";
			print @discrim;
			print "        else fail(\"visit${Main} unrecognized \"+ctx.getClass().getSimpleName());\n";
			print "        return null;\n";
			print "    }\n";
		}
	}
}
print <<'_TAIL_';
}
_TAIL_
