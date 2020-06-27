package com.dimonvideo.client.model;

public class Movie {
	private String title, thumbnailUrl, text, year, rating;

	public Movie() {
	}

	public Movie(String name, String thumbnailUrl, String year, String rating, String text) {
		this.title = name;
		this.text = text;
		this.thumbnailUrl = thumbnailUrl;
		this.year = year;
		this.rating = rating;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String name) {
		this.title = name;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getRating() {
		return rating;
	}

	public void setRating(String rating) {
		this.rating = rating;
	}


}
