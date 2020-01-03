package pro.delfik.totpcracker;

import lombok.Data;

@Data
public class Root {

	private final CrackingNode node;

	public Root() {
		this.node = new CrackingNode();
	}

	public static void main(String[] args) {

		Root root = new Root();


	}

}
