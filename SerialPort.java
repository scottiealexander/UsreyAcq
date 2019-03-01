package org.micromanager.UsreyAcq;

import mmcorej.CMMCore;
/* ========================================================================== */
public final class SerialPort extends ErrorReporter implements MasterCommunicator {
	/* ---------------------------------------------------------------------- */
	private int sleepDurationMs = 20;
	private int hardTimeoutMs = 60000;
	private Boolean init = false;
	private CMMCore mmc;
	
	public String spName = "S2COM";
	public String spPort = "COM4";
	public String spStop = "1";
	public String spBaud = "115200";
	public String spParity = "None";
	public String spTermChar = "\n";
	public String spAnswerTO = "2000.0";
	/* ---------------------------------------------------------------------- */
	public SerialPort(CMMCore core, String port) {
		super();
		mmc = core;
		spPort = port;
	}
	/* ---------------------------------------------------------------------- */
	@Override
	public void initialize() {
		try {
			mmc.loadDevice(spName, "SerialManager", spPort);
			mmc.setProperty(spName, "StopBits", spStop);
			mmc.setProperty(spName, "Parity", spParity);
			mmc.setProperty(spName, "BaudRate", spBaud);
			mmc.setProperty(spName, "AnswerTimeout", spAnswerTO);
			mmc.initializeDevice(spName);
			init = true;
		} catch (Exception e) {
			init = false;
			addErrorMessage(e);
		}
	}
	/* ---------------------------------------------------------------------- */
	@Override
	public void close() {
		if (init) {
			try {
				mmc.unloadDevice(spName);
				init = false;
			} catch (Exception e) {
				addErrorMessage(e);
			}
		}
	}
	/* ---------------------------------------------------------------------- */
	@Override
	public String waitForMessage() throws InterruptedException {
		return waitForMessage(hardTimeoutMs);
	}
	/* ---------------------------------------------------------------------- */
	@Override
	public String waitForMessage(int hardTimeoutMs) throws InterruptedException {
		String res = "";
		Long tstart = System.currentTimeMillis();
		while (System.currentTimeMillis()  < (tstart + hardTimeoutMs)) {
			try {
				res = mmc.getSerialPortAnswer(spName, spTermChar);
				if (!res.isEmpty()) {
					mmc.setSerialPortCommand(spName, "AWK", spTermChar);
					break;
				}
			} catch (Exception e) {
				// pause for sleepDurationMs and go around again waiting for instructions...
				Thread.sleep(sleepDurationMs);
			}
		}
		return res;
	}
	/* ---------------------------------------------------------------------- */
}
/* ========================================================================== */