sub gen {
	my($P) = @_;
	#print "\t\@Override public void enter${P}(\@NotNull ${GRAMMAR}Parser.${P}Context ctx) { }\n";
	#print "\t\@Override public void exit${P}(\@NotNull ${GRAMMAR}Parser.${P}Context ctx) { }\n";
	print <<"END_VISIT";

	\@Override public Void visit${P}(\@NotNull ${GRAMMAR}Parser.${P}Context ctx) {
		return visitChildren(ctx);
	}
END_VISIT
}

open(G,"J1.g4");
while (<G>) {
	chomp;
	if (/^\s*grammar (\w+)\s*;/) {
		$GRAMMAR = $1;
		print <<"END_HEAD";
import org.antlr.v4.runtime.misc.NotNull;

public class ModelVisitor extends ${GRAMMAR}BaseVisitor<Void> {

	public ModelVisitor() {
		// TODO Auto-generated constructor stub
	}
END_HEAD
	} elsif (/^([A-Z]|fragment)/) {
	} elsif (/^([a-z])(\S+)\s*$/) {
		gen((uc $1) . $2);
	} elsif (/#\s*(\S)(\S+)\s*$/) {
		gen((uc $1) . $2);
	}
}
close G;
print <<"END_TAIL";

}
END_TAIL
