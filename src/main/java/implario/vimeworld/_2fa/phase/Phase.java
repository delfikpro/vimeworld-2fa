package implario.vimeworld._2fa.phase;

import implario.vimeworld._2fa.Account;
import implario.vimeworld._2fa.App;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.logging.Level;

@RequiredArgsConstructor
public abstract class Phase {

	@Getter
	protected final String name;
	protected final App app;
	protected final Account account;

	@Getter
	private long nextTickTime;

	protected Phase nextTickAfter(long millis) {
		this.nextTickTime = System.currentTimeMillis() + millis;
		return this;
	}

	protected abstract void tick0() throws Exception;

	public final void tick() {
		try {
			this.tick0();
		} catch (Exception exception) {
			app.getMainLogger().log(Level.SEVERE, "Error while ticking " + account, exception);
		}
	}

	public enum Type {

		REPAIR,
		WAITING,
		CAPTCHA,

	}

}
