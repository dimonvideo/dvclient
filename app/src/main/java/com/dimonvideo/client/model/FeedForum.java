package com.dimonvideo.client.model;

import com.dimonvideo.client.Config;

public class FeedForum {
	//Data Variables
	private String imageUrl, last_poster_name, title, text, date, category, state, user, pinned;
	private int id, comments, hits;
	private Long time;

	//Getters and Setters
	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		if (!imageUrl.startsWith("http")) {
			imageUrl = Config.BASE_URL + imageUrl;
		}
		this.imageUrl = imageUrl;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getState() {
		return state;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getComments() {
		return comments;
	}

	public void setComments(int comments) {
		this.comments = comments;
	}

	public int getHits() {
		return hits;
	}

	public void setHits(int hits) {
		this.hits = hits;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getLast_poster_name() {
		return last_poster_name;
	}

	public void setLast_poster_name(String last_poster_name) {
		this.last_poster_name = last_poster_name;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPinned() {
		return pinned;
	}

	public void setPinned(String pinned) {
		this.pinned = pinned;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}
}
