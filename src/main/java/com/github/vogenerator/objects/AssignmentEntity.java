package com.github.vogenerator.objects;

public class AssignmentEntity {

	private String value;
	private boolean instantiation = false;
	private String typeId;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isInstantiation() {
		return instantiation;
	}

	public void setInstantiation(boolean instantiation) {
		this.instantiation = instantiation;
	}

	public String getTypeId() {
		return typeId;
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	@Override
	public String toString() {
		return "AssignmentEntity [value=" + value + ", instantiation=" + instantiation + ", typeId=" + typeId + "]";
	}

}
