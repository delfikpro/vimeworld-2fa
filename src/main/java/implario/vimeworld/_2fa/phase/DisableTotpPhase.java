package implario.vimeworld._2fa.phase;

import implario.vk.query.QueryContext;
import implario.vimeworld._2fa.Account;
import implario.vimeworld._2fa.App;
import implario.vimeworld._2fa.VimeWorld2FA;
import io.mikael.urlbuilder.UrlBuilder;
import lombok.Data;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ThreadLocalRandom;

public class DisableTotpPhase extends Phase {

	private static final URI methodUri = UrlBuilder.fromString("https://cp.vimeworld.ru/ajax/security.php").toUri();

	private final String csrfToken;
	private String currentCode;

	public DisableTotpPhase(App app, Account account, String csrfToken, String currentCode) {
		super("disabling two-factor protection", app, account);
		this.csrfToken = csrfToken;
		this.currentCode = currentCode;
	}

	@Override
	protected void tick0() throws Exception {

		HttpRequest.Builder builder = HttpRequest.newBuilder(methodUri)
				.header("referer", "https://cp.vimeworld.ru/security")
				.header("x-requested-with", "XMLHttpRequest");

		new QueryContext()
				.body("action", "totp-disable")
				.body("totp", this.currentCode)
				.body("csrf_token", this.csrfToken)
				.applyBody(builder);

		this.app.applyVimeWorldData(builder, this.account);

		HttpResponse<String> response = this.app.syncRequest(builder);

		this.app.getMainLogger().info(response.body());
		Response result = this.app.getGson().fromJson(response.body(), Response.class);

		this.currentCode = VimeWorld2FA.pad2faCode(ThreadLocalRandom.current().nextInt(1_000_000));

		this.nextTickAfter(0);

		if (!result.getState().equals("success")) return;

		this.account.setPhase(new FinishedPhase(this.app, this.account, true));
		this.app.getMainLogger().info("Аккаунт " + account + " был успешно восстановлен от двухфакторной защиты");

	}

	@Data
	private static class Response {
		private final String state, msg;
	}

}
