package implario.vimeworld._2fa.phase;

import implario.vimeworld._2fa.Account;
import implario.vimeworld._2fa.App;
import implario.vimeworld._2fa.TotpFailException;
import io.mikael.urlbuilder.UrlBuilder;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsrfPhase extends Phase {

	private static final URI methodUri = UrlBuilder.fromString("https://cp.vimeworld.ru/security").toUri();
	private static final Pattern csrfPattern = Pattern.compile("csrf_token: '([0-9a-f]+)'");

	private final String code;

	public CsrfPhase(App app, Account account, String code) {
		super("confirmation with csrf-token", app, account);
		this.code = code;
	}

	@Override
	public void tick0() throws Exception {

		HttpRequest request = app.applyVimeWorldData(HttpRequest.newBuilder(methodUri), account).build();
		HttpResponse<String> response = app.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		Matcher matcher = csrfPattern.matcher(response.body());

		if (!matcher.find()) throw new TotpFailException("CSRF-token wasn't found.");
		String csrfToken = matcher.group(1);

		account.setPhase(new DisableTotpPhase(this.app, this.account, csrfToken, this.code));
		account.getPhase().tick();
		System.out.println(account.getUsername() + " CSRF-token retreived successfully.");

	}

}
