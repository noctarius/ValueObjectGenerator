package com.github.vogenerator.objects;

import java.util.ArrayList;
import java.util.List;

public class ClassEntity extends PrototypeEntity {

	private final List<PropertyEntity> propertyEntities = new ArrayList<PropertyEntity>();
	private String superTypeId;

	public String getSuperTypeId() {
		return superTypeId;
	}

	public void setSuperTypeId(String superTypeId) {
		this.superTypeId = superTypeId;
	}

	public List<PropertyEntity> getPropertyEntities() {
		return propertyEntities;
	}

	@Override
	public String toString() {
		return "ClassEntity [propertyEntities=" + propertyEntities + ", superTypeId=" + superTypeId
				+ ", getIdentifier()=" + getIdentifier() + ", getPackageName()=" + getPackageName() + ", isEnumType()="
				+ isEnumType() + ", isComponentType()=" + isComponentType() + "]";
	}

}
