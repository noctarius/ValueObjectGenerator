package com.github.vogenerator.objects;

public class PrototypeEntity {
	private String identifier;
	private String packageName;
	private boolean enumType = false;
	private boolean componentType = false;
	private boolean secureType = false;
	private CommentEntity commentEntity;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public boolean isEnumType() {
		return enumType;
	}

	public void setEnumType(boolean enumType) {
		this.enumType = enumType;
	}

	public boolean isComponentType() {
		return componentType;
	}

	public void setComponentType(boolean componentType) {
		this.componentType = componentType;
	}

	public boolean isSecureType() {
		return secureType;
	}

	public void setSecureType(boolean secureType) {
		this.secureType = secureType;
	}

	public CommentEntity getCommentEntity() {
		return commentEntity;
	}

	public void setCommentEntity(CommentEntity commentEntity) {
		this.commentEntity = commentEntity;
	}

	@Override
	public String toString() {
		return "PrototypeEntity [identifier=" + identifier + ", packageName=" + packageName + ", enumType=" + enumType
				+ ", componentType=" + componentType + ", secureType=" + secureType + "commentEntity=" + commentEntity
				+ "]";
	}

}
