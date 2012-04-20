package com.github.vogenerator.objects;

public interface Type {

	String getTypeId();

	String getTypeInLanguage();

	boolean isEnumType();

	boolean isGeneric();

	String getGenericTypeId();

}
