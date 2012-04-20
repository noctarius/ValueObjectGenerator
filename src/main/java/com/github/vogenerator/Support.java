package com.github.vogenerator;

public class Support {

	public String toLowerCamelCase(String value) {
		if (value == null)
			return null;

		if (value.length() == 0)
			return value;

		return toCamelCase(value);
	}

	public String toUpperCamelCase(String value) {
		String lowerCamelCase = toLowerCamelCase(value);
		return String.valueOf(Character.toUpperCase(lowerCamelCase.toCharArray()[0])) + lowerCamelCase.substring(1);
	}

	public String replaceTabs(String value) {
		return value.replace("\t", "");
	}

	private String toCamelCase(String value) {
		StringBuilder camelCase = new StringBuilder();
		char[] characters = value.toCharArray();

		for (int i = 0; i < characters.length; i++) {
			i = ignoreWhitespace(characters, i);
			if (i == -1)
				break;

			if (camelCase.length() == 0)
				camelCase.append(Character.toLowerCase(characters[i]));
			else
				camelCase.append(Character.toUpperCase(characters[i]));

			int nextWhitespace = nextWhitespace(characters, i);
			if (nextWhitespace == -1)
				nextWhitespace = characters.length;

			camelCase.append(value.substring(i + 1, nextWhitespace));
			i = nextWhitespace;
		}

		return camelCase.toString();
	}

	private int ignoreWhitespace(char[] characters, int offset) {
		for (int i = offset; i < characters.length; i++) {
			char c = characters[i];
			if (!Character.isWhitespace(c) && '-' != c && '_' != c)
				return i;
		}
		return -1;
	}

	private int nextWhitespace(char[] characters, int offset) {
		for (int i = offset + 1; i < characters.length; i++) {
			char c = characters[i];
			if (Character.isWhitespace(c) || '-' == c || '_' == c)
				return i;
		}
		return -1;
	}
}
