package implario.vimeworld._2fa;

public class TotpFailException extends RuntimeException {

	public TotpFailException(String message) {
		super(message, null, false, false);
	}

}
