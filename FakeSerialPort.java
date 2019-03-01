package org.micromanager.UsreyAcq;
/* ========================================================================== */
public final class FakeSerialPort extends ErrorReporter implements MasterCommunicator {
	public int nTrial;
	public int trial = -1;
	public Double label = 0.0;
	
	/* ---------------------------------------------------------------------- */
	public FakeSerialPort() {
		this(3);
	}
	/* ---------------------------------------------------------------------- */
	public FakeSerialPort(int nTrial) {
		super();
		this.nTrial = nTrial;		
	}
	/* ---------------------------------------------------------------------- */
	@Override
	public void initialize() {
		// nothing to do for FakeSerialPort
	}
	/* ---------------------------------------------------------------------- */
	@Override
	public void close() {
		// nothing to do for FakeSerialPort
	}
	/* ---------------------------------------------------------------------- */
	@Override
	public String waitForMessage() throws InterruptedException{
		return waitForMessage(1000);
	}
	/* ---------------------------------------------------------------------- */
	@Override
	public String waitForMessage(int hardTimeoutMs) throws InterruptedException {
		// NOTE: ignore the hard timeout and just wait for 2 sec before returning
		String res = "";
		
		Thread.sleep(2000);
		
		if (trial < 0) {
			res = "{\"stimulus_duration\": 2.0, \"baseline_duration\": 1.0, \"ntrial\": 3}";
			trial++;
		} else if (trial >= nTrial) {
			res = "STOP";
		} else {
			res = label.toString();
			label = (label + 45.0) % 360.0;
			trial++;
		}
		return res;
	}
	/* ---------------------------------------------------------------------- */
}
/* ========================================================================== */
