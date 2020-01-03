package pro.delfik.net;

import lombok.Getter;

import java.net.Proxy;
import java.util.Arrays;
import java.util.Collection;

public abstract class PostArchetype {

	@Getter
	private final Collection<Cookie> cookies;

	protected PostArchetype(Cookie... cookies) {
		this(Arrays.asList(cookies));
	}

	protected PostArchetype(Collection<Cookie> cookies) {
		this.cookies = cookies;
	}

	protected abstract Request execute(String... args);

	public Response accept(Proxy proxy, String... args) {
		return execute(args).execute(proxy).args(args);
	}

}
