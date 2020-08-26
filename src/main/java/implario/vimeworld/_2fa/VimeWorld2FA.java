package implario.vimeworld._2fa;

public class VimeWorld2FA {

	public static void main(String... cmdLineArgs) {

		App app;
		try {
			if ((app = App.load()) == null) return;
		} catch (Exception ex) {
			System.out.println("An error occurred while loading app from config:");
			ex.printStackTrace();
			System.out.println("Try removing 2fa.yml to let the application generate a fresh one.");
			return;
		}
		try {
			app.start();
		} catch (Exception ex) {
			app.getMainLogger().severe("An error occurred while attempting to start the application:");
			ex.printStackTrace();
		}

	}


	public static String pad2faCode(int number) {
		String s = String.valueOf(number);
		int pad = Math.max(6 - s.length(), 0);
		String zeroes = new String(new char[pad]);
		return zeroes.replace('\0', '0') + s;
	}



}
