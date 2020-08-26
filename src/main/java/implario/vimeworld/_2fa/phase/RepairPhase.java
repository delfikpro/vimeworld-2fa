package implario.vimeworld._2fa.phase;

import implario.vimeworld._2fa.Account;
import implario.vimeworld._2fa.App;

public class RepairPhase extends Phase {

	private final String previousPassword;

	public RepairPhase(App app, Account account) {
		super("repair", app, account);
		this.previousPassword = account.getPassword();
	}

	@Override
	protected void tick0() {
		if (this.account.getPassword().equals(this.previousPassword)) return;
		this.account.setPhase(new WaitingPhase(this.app, this.account));
	}

}
