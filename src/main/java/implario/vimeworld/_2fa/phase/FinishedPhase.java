package implario.vimeworld._2fa.phase;

import clepto.vk.reverse.message.OutcomingMessage;
import implario.vimeworld._2fa.Account;
import implario.vimeworld._2fa.App;

public class FinishedPhase extends Phase {

	private final boolean glory;

	public FinishedPhase(App app, Account account, boolean glory) {
		super("finished", app, account);
		this.glory = glory;
	}

	@Override
	protected void tick0() throws Exception {
		this.nextTickAfter(Integer.MAX_VALUE);
		if (this.glory) this.app.getVkSession().sendMessage(new OutcomingMessage(
				"Здесь ты забудешь о своих проблемах,\n" +
						"Свободно бороздя заливы долгих лет.\n" +
						"Качают твой корабль волны-перемены,\n" +
						"Попутный ветер шлёт тебе привет."
		), this.account.getOwnerVkId());
		this.app.getVkSession().sendMessage(new OutcomingMessage(
				"✅ Лодочка " + account + " пришла к финишу. Мои поздравления, юный матрос"
		), this.account.getOwnerVkId());
	}

}
