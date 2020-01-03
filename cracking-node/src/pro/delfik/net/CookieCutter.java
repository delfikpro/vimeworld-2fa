package pro.delfik.net;

import lombok.Data;

@Data
public class CookieCutter {

	private final String name;
	private final String description;

	public Cookie cut(String value) {
		return new Cookie(this, value);
	}

}
