package com.sapuseven.untis.utils;

public class FeatureInfo {
	private String title;
	private String desc;
	private int likes;
	private int id;
	private int hasVoted;

	public int getHasVoted() {
		return hasVoted;
	}

	public void setHasVoted(int hasVoted) {
		this.hasVoted = hasVoted;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public int getLikes() {
		return likes;
	}

	public void setLikes(int likes) {
		this.likes = likes;
	}
}
