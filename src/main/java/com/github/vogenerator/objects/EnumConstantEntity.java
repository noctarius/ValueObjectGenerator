package com.github.vogenerator.objects;

public class EnumConstantEntity implements Comparable<EnumConstantEntity> {

	private String identifier;
	private CommentEntity commentEntity;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public CommentEntity getCommentEntity() {
		return commentEntity;
	}

	public void setCommentEntity(CommentEntity commentEntity) {
		this.commentEntity = commentEntity;
	}

	@Override
	public String toString() {
		return "EnumConstantEntity [identifier=" + identifier + ", commentEntity=" + commentEntity + "]";
	}

	public int compareTo(EnumConstantEntity o) {
		return identifier.compareTo(o.identifier);
	}

}
