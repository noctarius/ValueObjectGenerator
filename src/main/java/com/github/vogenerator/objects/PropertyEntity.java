package com.github.vogenerator.objects;

public class PropertyEntity implements Comparable<PropertyEntity> {

	private String identifier;
	private String typeId;
	private TransmissionType transmissionType;
	private boolean readOnly;
	private AssignmentEntity assignmentEntity;
	private CommentEntity commentEntity;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getTypeId() {
		return typeId;
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	public int getTransmissionTypeId() {
		return transmissionType.ordinal();
	}

	public TransmissionType getTransmissionType() {
		return transmissionType;
	}

	public void setTransmissionType(TransmissionType transmissionType) {
		this.transmissionType = transmissionType;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public AssignmentEntity getAssignmentEntity() {
		return assignmentEntity;
	}

	public void setAssignmentEntity(AssignmentEntity assignmentEntity) {
		this.assignmentEntity = assignmentEntity;
	}

	public CommentEntity getCommentEntity() {
		return commentEntity;
	}

	public void setCommentEntity(CommentEntity commentEntity) {
		this.commentEntity = commentEntity;
	}

	@Override
	public String toString() {
		return "PropertyEntity [identifier=" + identifier + ", typeId=" + typeId + ", transmissionType="
				+ transmissionType + ", readOnly=" + readOnly + ", assignmentEntity=" + assignmentEntity
				+ ", commentEntity=" + commentEntity + "]";
	}

	public int compareTo(PropertyEntity o) {
		return identifier.compareTo(o.identifier);
	}

}
