package implario.vimeworld._2fa.phase;

import clepto.vk.proxy.QueryContext;
import clepto.vk.reverse.message.OutcomingMessage;
import implario.vimeworld._2fa.Account;
import implario.vimeworld._2fa.App;
import io.mikael.urlbuilder.UrlBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginPhase extends Phase {

	private static final Pattern loginValidPatern = Pattern.compile("<div class=\"wrap\">(.*)<div class=\"form-signin-heading\">");
	private static final Pattern loginResultPatern = Pattern.compile("</button>(.*)</div>");
	private static final URI methodUri = UrlBuilder.fromString("https://cp.vimeworld.ru/login").toUri();

	private final String gRecaptchaResponse;

	public LoginPhase(App app, Account account, String gRecaptchaResponse) {
		super("login", app, account);
		this.gRecaptchaResponse = gRecaptchaResponse;
	}

	@Override
	protected void tick0() throws Exception {

		HttpRequest.Builder builder = HttpRequest.newBuilder(methodUri);
		new QueryContext()
				.body("username", account.getUsername())
				.body("password", account.getPassword())
				.body("g-recaptcha-response", gRecaptchaResponse)
				.body("login", "Войти")
				.body("remember", true)
				.body("server", "lobby")
				.applyBody(builder);

		app.applyVimeWorldData(builder, account);

		HttpResponse<String> response = app.syncRequest(builder);

		Optional<String> locationHeader = response.headers().firstValue("location");
		if (locationHeader.isEmpty()) throw new RuntimeException("No location header!");
		String location = locationHeader.get();
		if (location.equals("login_2fa")) {
			this.account.setPhase(new GuessPhase(this.app, this.account));
			return;
		}
		if (location.equals("/login")) {
			this.account.setPhase(new RepairPhase(this.app, this.account));
			this.app.getVkSession().sendMessage(new OutcomingMessage("На лодочке " + account + " порвался парус, и она не смогла стартовать. " +
					"Требуется вмешательстов лоцмана/капитана"), this.account.getOwnerVkId());
			this.app.getMainLogger().severe("Account " + account + " has incorrect password!");

			return;
		}
		if (location.equals("index")) {
			this.app.getMainLogger().severe("Account " + account + " doesn't have 2fa!");
			this.account.setPhase(new FinishedPhase(this.app, this.account, false));
			return;
		}

		throw new IllegalStateException("Invalid login redirect location '" + location + "'");


	}

}
