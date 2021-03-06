package thread2.util;

import lombok.Data;

@Data
class RollBack {

	private boolean needRoolBack;

	public RollBack() { }

	public RollBack(boolean needRoolBack) {
		this.needRoolBack = needRoolBack;
	}
}