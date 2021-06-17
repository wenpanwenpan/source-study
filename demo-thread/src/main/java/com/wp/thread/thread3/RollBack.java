package com.wp.thread.thread3;

import lombok.Data;

@Data
class RollBack {

	private boolean needRoolBack;

	public RollBack(boolean needRoolBack) {
		this.needRoolBack = needRoolBack;
	}
}