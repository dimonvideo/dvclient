/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.model;

public class FeedCats {
	//Data Variables
	private String title, razdel;
	private int cid, count;

	//Getters and Setters

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getCid() {
		return cid;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getCount() {
		return count;
	}

	public void setCid(int cid) {
		this.cid = cid;
	}

	public String getRazdel() {
		return razdel;
	}

	public void setRazdel(String razdel) {
		this.razdel = razdel;
	}
}
