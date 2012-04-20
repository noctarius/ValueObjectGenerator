package com.github.vogenerator.objects;

import java.util.ArrayList;
import java.util.List;

public class EnumEntity extends PrototypeEntity {

	private final List<EnumConstantEntity> constantEntities = new ArrayList<EnumConstantEntity>();

	public EnumEntity() {
		setEnumType(true);
	}

	public List<EnumConstantEntity> getConstants() {
		return constantEntities;
	}

	@Override
	public String toString() {
		return "EnumEntity [constantEntities=" + constantEntities + ", getIdentifier()=" + getIdentifier()
				+ ", getPackageName()=" + getPackageName() + ", isEnumType()=" + isEnumType() + ", isComponentType()="
				+ isComponentType() + "]";
	}

}
