package pro.delfik.totpcracker;

import lombok.Getter;

@Getter
public class Worker extends Thread {

	private final long delay;
	public volatile int fuckups;
	public volatile int successfulCodes;
	private volatile boolean blocked;

	public Worker(Runnable runnable, String name, long delay) {
		super(runnable, name);
		this.delay = delay;
	}

	public void block() {
		blocked = true;
	}

	@Override
	public void run() {
		while (!blocked) {
			try {
				super.run();
				successfulCodes++;
			} catch (Throwable ex) {
				while (ex.getCause() != null) ex = ex.getCause();
				System.out.println("\u001b[31mfuckup from " + getName() + "\u001b[0m - " + ex.getClass().getSimpleName() + ", " + ex.getMessage());
				fuckups++;
				try {
					sleep(5000 + (int) (Math.random() * 10000));
				} catch (InterruptedException e) {
					break;
				}
			}
			try {
				sleep(delay);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

}
