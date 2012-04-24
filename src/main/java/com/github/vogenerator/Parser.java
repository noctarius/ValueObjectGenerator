package com.github.vogenerator;

import org.parboiled.BaseParser;
import org.parboiled.MatcherContext;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.DontLabel;
import org.parboiled.annotations.MemoMismatches;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.matchers.CustomMatcher;

@BuildParseTree
public class Parser extends BaseParser<Object> {

	public Rule CompilationUnit() {
		return Sequence(Spacing(), PackageDeclaration(), CURLYOPEN, OneOrMore(PackageMembers()), CURLYCLOSE);
	}

	Rule PackageDeclaration() {
		return Sequence(PACKAGE, QualifiedIdentifier());
	}

	Rule PackageMembers() {
		return Sequence(Spacing(), FirstOf(ClassDeclaration(), ComponentDeclaration(), EnumDeclaration()), Spacing());
	}

	Rule ComponentDeclaration() {
		return Sequence(Optional(ClassDocumentingComment()), Optional(Secure()), COMPONENT, Identifier(), ClassBody());
	}

	Rule ClassDeclaration() {
		return Sequence(Optional(ClassDocumentingComment()), Optional(Secure()), CLASS, Identifier(),
				Optional(Sequence(Extends(), QualifiedIdentifier())), ClassBody());
	}

	Rule EnumDeclaration() {
		return Sequence(Optional(ClassDocumentingComment()), ENUM, Identifier(), EnumBody());
	}

	Rule ClassBody() {
		return Sequence(CURLYOPEN, ZeroOrMore(PropertyDeclarations()), CURLYCLOSE);
	}

	Rule PropertyDeclarations() {
		return FirstOf(SEMICOLON, OneOrMore(MemberDeclaration()));
	}

	Rule MemberDeclaration() {
		return Sequence(Optional(DocumentingComment()), Optional(FirstOf(Client(), Server())), PROPERTY,
				Optional(ReadOnly()), ReferenceType(), Identifier(),
				Optional(FirstOf(DirectAssignment(), MemberBinding())), SEMICOLON);
	}

	Rule MemberBinding() {
		return Sequence(Bind(), Spacing(), Identifier(), Spacing());
	}

	Rule EnumBody() {
		return Sequence(CURLYOPEN, ZeroOrMore(EnumConstantDeclarations()), CURLYCLOSE);
	}

	Rule EnumConstantDeclarations() {
		return FirstOf(SEMICOLON, OneOrMore(EnumConstantDeclaration()));
	}

	Rule EnumConstantDeclaration() {
		return Sequence(Optional(DocumentingComment()), Identifier(), FirstOf(COMMA, SEMICOLON));
	}

	Rule DirectAssignment() {
		return Sequence(EQUALS, Spacing(), FirstOf(Instantiation(), Literal()));
	}

	Rule Instantiation() {
		return Sequence(NEW, QualifiedIdentifier(), LPAR, RPAR);
	}

	Rule Type() {
		return Sequence(FirstOf(BasicType(), ClassType()), ZeroOrMore(Dim()));
	}

	/*
	 * array types []
	 */
	Rule Dim() {
		return Sequence(LBRK, RBRK);
	}

	/*
	 * java.lang.String
	 */
	Rule ClassType() {
		return Sequence(Identifier(), Optional(TypeArguments()),
				ZeroOrMore(DOT, Identifier(), Optional(TypeArguments())));
	}

	/*
	 * java.util.Map<int, Object>
	 */
	Rule TypeArguments() {
		return Sequence(LPOINT, TypeArgument(), ZeroOrMore(COMMA, TypeArgument()), RPOINT);
	}

	/*
	 * java.util.List<Foo extends Object> java.util.List<Object super Foo>
	 */
	Rule TypeArgument() {
		return FirstOf(ReferenceType(), Sequence(QUERY, Optional(FirstOf(EXTENDS, SUPER), ReferenceType())));
	}

	Rule ReferenceType() {
		return FirstOf(Sequence(BasicType(), OneOrMore(Dim())), Sequence(ClassType(), ZeroOrMore(Dim())));
	}

	@MemoMismatches
	Rule BasicType() {
		return Sequence(
				FirstOf("Object", "Byte", "Short", "Char", "Int", "Long", "Float", "Double", "Boolean", "String",
						"List"), TestNot(LetterOrDigit()), Spacing());
	}

	@DontLabel
	Rule Spacing() {
		return ZeroOrMore(FirstOf(Whitespaces(), Comment()));
	}

	@SuppressNode
	Rule Whitespaces() {
		return OneOrMore(AnyOf(" \t\r\n\f"));
	}

	@DontLabel
	Rule DocumentingComment() {
		return Sequence(String("/**"), ZeroOrMore(TestNot("*/").suppressNode(), ANY.suppressNode()).suppressSubnodes()
				.label("DocumentingComment"), String("*/").suppressNode(), Spacing());
	}

	@DontLabel
	Rule ClassDocumentingComment() {
		return Sequence(String("/**"), ZeroOrMore(TestNot("*/").suppressNode(), ANY.suppressNode()).suppressSubnodes()
				.label("ClassDocumentingComment"), String("*/").suppressNode(), Spacing());
	}

	@DontLabel
	Rule Comment() {
		return FirstOf(// traditional comment
				Sequence(String("/*"), TestNot("*"), ZeroOrMore(TestNot("*/").suppressNode(), ANY.suppressNode())
						.suppressSubnodes().label("MultiLineComment"), String("*/").suppressNode()),

				// end of line comment
				Sequence(String("//").suppressNode(), ZeroOrMore(NoneOf("\r\n").suppressNode(), ANY.suppressNode())
						.suppressSubnodes().label("Comment")));
	}

	Rule QualifiedIdentifier() {
		return Sequence(Identifier(), ZeroOrMore(DOT, Identifier()));
	}

	Rule Literal() {
		return Sequence(
				FirstOf(FloatLiteral(), IntegerLiteral(), CharLiteral(), StringLiteral(),
						Sequence("TRUE", TestNot(LetterOrDigit())), Sequence("FALSE", TestNot(LetterOrDigit())),
						Sequence("NULL", TestNot(LetterOrDigit()))), Spacing());
	}

	Rule FloatLiteral() {
		return DecimalFloat();
	}

	@SuppressSubnodes
	Rule DecimalFloat() {
		return FirstOf(Sequence(Optional(MINUS), OneOrMore(Digit()), '.', ZeroOrMore(Digit()), Optional(Exponent())),
				Sequence(Optional(MINUS), '.', OneOrMore(Digit()), Optional(Exponent())));
	}

	@SuppressSubnodes
	Rule IntegerLiteral() {
		return Sequence(Optional(MINUS), DecimalNumeral());
	}

	@SuppressSubnodes
	Rule OctalNumeral() {
		return Sequence('0', OneOrMore(CharRange('0', '7')));
	}

	@SuppressSubnodes
	Rule DecimalNumeral() {
		return FirstOf('0', Sequence(Optional(MINUS), CharRange('1', '9'), ZeroOrMore(Digit())));
	}

	Rule CharLiteral() {
		return Sequence('\'', FirstOf(Escape(), Sequence(TestNot(AnyOf("'\\")), ANY)).suppressSubnodes(), '\'');
	}

	Rule Exponent() {
		return Sequence(AnyOf("eE"), Optional(AnyOf("+-")), OneOrMore(Digit()));
	}

	Rule StringLiteral() {
		return Sequence('"', ZeroOrMore(FirstOf(Escape(), Sequence(TestNot(AnyOf("\r\n\"\\")), ANY)))
				.suppressSubnodes(), '"');
	}

	Rule Digit() {
		return CharRange('0', '9');
	}

	Rule UnicodeEscape() {
		return Sequence(OneOrMore('u'), HexDigit(), HexDigit(), HexDigit(), HexDigit());
	}

	Rule Escape() {
		return Sequence('\\', FirstOf(AnyOf("btnfr\"\'\\"), OctalEscape(), UnicodeEscape()));
	}

	Rule OctalEscape() {
		return FirstOf(Sequence(CharRange('0', '3'), CharRange('0', '7'), CharRange('0', '7')),
				Sequence(CharRange('0', '7'), CharRange('0', '7')), CharRange('0', '7'));
	}

	Rule HexDigit() {
		return FirstOf(CharRange('a', 'f'), CharRange('A', 'F'), CharRange('0', '9'));
	}

	@SuppressSubnodes
	@MemoMismatches
	Rule Identifier() {
		return Sequence(TestNot(Keyword()), Letter(), ZeroOrMore(LetterOrDigit()), Spacing());
	}

	Rule Letter() {
		return FirstOf(Sequence('\\', UnicodeEscape()), new LetterMatcher());
	}

	@MemoMismatches
	Rule LetterOrDigit() {
		return FirstOf(Sequence('\\', UnicodeEscape()), new LetterOrDigitMatcher());
	}

	// Keywords
	@MemoMismatches
	Rule Keyword() {
		return Sequence(
				FirstOf("server", "client", "property", "package", "class", "enum", "component", "readonly", "extends",
						"super", "new", "bind"), TestNot(LetterOrDigit()));
	}

	@DontLabel
	Rule Keyword(String keyword) {
		return Terminal(keyword, LetterOrDigit());
	}

	public final Rule PROPERTY = Keyword("property");
	public final Rule PACKAGE = Keyword("package");
	public final Rule CLASS = Keyword("class");
	public final Rule COMPONENT = Keyword("component");
	public final Rule ENUM = Keyword("enum");
	public final Rule EXTENDS = Keyword("extends");
	public final Rule SUPER = Keyword("super");
	public final Rule NEW = Keyword("new");
	public final Rule SECURE = Keyword("secure");
	public final Rule BIND = Keyword("bind");

	// KeywordRules
	Rule Server() {
		return Sequence("server", Spacing());
	}

	Rule Client() {
		return Sequence("client", Spacing());
	}

	Rule ReadOnly() {
		return Sequence("readonly", Spacing());
	}

	Rule Extends() {
		return Sequence("extends", Spacing());
	}

	Rule Secure() {
		return Sequence("secure", Spacing());
	}

	Rule Bind() {
		return Sequence("bind", Spacing());
	}

	// Symbols
	final Rule SEMICOLON = Terminal(";");
	final Rule CURLYOPEN = Terminal("{");
	final Rule CURLYCLOSE = Terminal("}");
	final Rule DOT = Terminal(".");
	final Rule LBRK = Terminal("[");
	final Rule RBRK = Terminal("]");
	final Rule QUERY = Terminal("?");
	final Rule COMMA = Terminal(",");
	final Rule LPOINT = Terminal("<");
	final Rule RPOINT = Terminal(">");
	final Rule EQUALS = Terminal("=", Ch('='));
	final Rule LPAR = Terminal("(");
	final Rule RPAR = Terminal(")");
	final Rule MINUS = Terminal("-");

	// Helper
	@Override
	protected Rule fromCharLiteral(char c) {
		return super.fromCharLiteral(c).suppressNode();
	}

	@DontLabel
	// @SuppressNode
	Rule Terminal(String value) {
		return Sequence(value, Spacing()).label("'" + value + "'");
	}

	@DontLabel
	@SuppressNode
	Rule Terminal(String value, Rule mustNotFollow) {
		return Sequence(value, TestNot(mustNotFollow), Spacing()).label("'" + value + "'");
	}

	private class LetterMatcher extends CustomMatcher {

		private LetterMatcher() {
			super("Letter");
		}

		@Override
		public final boolean isSingleCharMatcher() {
			return true;
		}

		@Override
		public final boolean canMatchEmpty() {
			return false;
		}

		@Override
		public boolean isStarterChar(char c) {
			return acceptChar(c);
		}

		@Override
		public final char getStarterChar() {
			return 'a';
		}

		public final <V> boolean match(MatcherContext<V> context) {
			if (!acceptChar(context.getCurrentChar())) {
				return false;
			}
			context.advanceIndex(1);
			context.createNode();
			return true;
		}

		private boolean acceptChar(char c) {
			return Character.isJavaIdentifierStart(c);
		}
	}

	private class LetterOrDigitMatcher extends CustomMatcher {

		private LetterOrDigitMatcher() {
			super("LetterOrDigit");
		}

		@Override
		public final boolean isSingleCharMatcher() {
			return true;
		}

		@Override
		public final boolean canMatchEmpty() {
			return false;
		}

		@Override
		public boolean isStarterChar(char c) {
			return acceptChar(c);
		}

		@Override
		public final char getStarterChar() {
			return 'a';
		}

		public final <V> boolean match(MatcherContext<V> context) {
			if (!acceptChar(context.getCurrentChar())) {
				return false;
			}
			context.advanceIndex(1);
			context.createNode();
			return true;
		}

		private boolean acceptChar(char c) {
			return Character.isJavaIdentifierPart(c);
		}
	}
}
