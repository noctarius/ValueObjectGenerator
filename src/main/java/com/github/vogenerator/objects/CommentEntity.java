package com.github.vogenerator.objects;

public class CommentEntity {

	private String comment;
	private boolean multiline = false;

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public boolean isMultiline() {
		return multiline;
	}

	public void setMultiline(boolean multiline) {
		this.multiline = multiline;
	}

	@Override
	public String toString() {
		return "CommentEntity [comment=" + comment + ", multiline=" + multiline + "]";
	}

}
