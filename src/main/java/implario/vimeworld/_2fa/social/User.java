package implario.vimeworld._2fa.social;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
public class User {

	private final int vkId;
	private String nickname;
	private Rank rank;

	@Getter
	@RequiredArgsConstructor
	public enum Rank {

		SAILOR("Моряк"),
		PILOT("Лоцман"),
		CAPTAIN("Капитан");

		private final String title;

		public boolean isAtLeast(Rank rank) {
			return this.ordinal() >= rank.ordinal();
		}

	}

}
