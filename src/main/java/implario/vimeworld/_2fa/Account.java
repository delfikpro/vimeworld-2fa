package implario.vimeworld._2fa;

import implario.vk.GroupSession;
import implario.vk.model.message.OutcomingMessage;
import implario.vimeworld._2fa.phase.Phase;
import lombok.Data;

@Data
public class Account {

	private final int minigamesId;
	private final String username;
	private String password;
	private final int ownerVkId;
	private final long createdTimeStamp;
	private String phpsessid;
	private long triedCodes;

	private transient Phase phase;

	public void setPhase(Phase phase) {
		Phase previous = this.phase;
		this.phase = phase;
		App.getInstance().getMainLogger().info("Аккаунт " + this + " вошёл в следующую фазу: " + phase.getName());
		if (previous != null && previous != phase) {
			GroupSession session = App.getInstance().getVkSession();
			if (session != null) session.sendMessage(new OutcomingMessage(
					this.getEmoji() + " " + this.getUsername() + " [" + previous.getName() + "] вошёл в фазу " + this.phase.getName()
			), this.ownerVkId);
		}
	}

	@Override
	public String toString() {
		return this.username;
	}

	private static final String[] emojis = {"\uD83D\uDEA4", "\uD83D\uDEF6", "\u26F5", "\uD83D\uDEF3", "\u26F4", "\uD83D\uDEE5", "\uD83D\uDEA2"};
	public String getEmoji() {
		return emojis[Math.abs(username.hashCode() % emojis.length)];
	}

	public String getTitle(boolean censore) {
		if (censore) {
			String name = this.username.substring(0, 2) +
					new String(new char[this.username.length() - 2]).replace('\0', '\u2022');
			return this.getEmoji() + " [id" + ownerVkId + "|" + name + "]";
		}
		return "[id" + ownerVkId + "|" + this.getEmoji() + "] " + this.username;
	}

}
