package ca.nevdull.mjc.compiler;

public enum Access {
	DEFAULT		{public String toString() { return "";}},
	PUBLIC		{public String toString() { return "public";}},
	PROTECTED	{public String toString() { return "protected";}},
	PRIVATE		{public String toString() { return "private";}}
}

