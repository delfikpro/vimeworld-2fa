package pro.delfik.net;

import lombok.Data;

@Data
public class Cookie {

	private final CookieCutter cutFrom;
	private final String value;

	@Override
	public String toString() {
		return getCutFrom().getName() + "=" + getValue();
	}

}
