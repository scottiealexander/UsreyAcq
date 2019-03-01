package org.micromanager.UsreyAcq;

import org.micromanager.acquisition.TaggedImageStorageMultipageTiff;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.ImageUtils;

import mmcorej.CMMCore;

import org.json.JSONObject;

import java.io.FileWriter;
import java.io.File;

import javax.swing.JOptionPane;

/* ========================================================================== */
public class UsreyAcqController extends ErrorReporter implements Runnable {

	private Boolean state = true;
	private int trial = 0;
	private Double label = -1.0;

	private Double frameRate;
	private int stimFrames_;
	private int baselineFrames_;

	private int totalTrials = -1;
	private int framesPerTrial = -1;
	private int hardTimeoutMs = 1000 * 5 * 60; //5 minutes

	private CMMCore mmc;
	private ScriptInterface gui;
	private MasterCommunicator com;
    private JSONObject jo;

    private String rootDir_;
    private String acqName_;

	/* ---------------------------------------------------------------------- */
	public UsreyAcqController(ScriptInterface app, String rootDir,
			String acqName, String comPort) {

		gui = app;
		mmc = gui.getMMCore();

		rootDir_ = rootDir;
		acqName_ = acqName;

		if (comPort.equals("DEBUG")) {
			frameRate = 10.0;
			com = new FakeSerialPort();
		} else {
			com = new SerialPort(mmc, comPort);
			getFrameRate();
		}

		com.initialize();
	}
	/* ---------------------------------------------------------------------- */
	@Override
	public Boolean anyErrors() {
		return com.anyErrors() & error;
	}
	/* ---------------------------------------------------------------------- */
	@Override
	public String errorMessage() {
		if (com.anyErrors()) {
			addErrorMessage(com.errorMessage());
		}
		return errMsg;
	}
	/* ---------------------------------------------------------------------- */
	private void getFrameRate() {

		String camName = mmc.getCameraDevice();

		try {
			if (mmc.hasProperty(camName, "FrameRate")) {
				String fr = mmc.getProperty(camName, "FrameRate");
				frameRate = Double.parseDouble(fr);
			} else {
				addErrorMessage("Cannot get frame rate for camera: " + camName);
			}
		} catch (Exception e) {
			addErrorMessage(e);
		}
	}
	/* ---------------------------------------------------------------------- */
	public void close() {
		com.close();
	}
	/* ---------------------------------------------------------------------- */
	public String waitForSettings() {
		return waitForSettings(hardTimeoutMs);
	}
	/* ---------------------------------------------------------------------- */
	public String waitForSettings(int hardTimeoutMs) {
		int ready = 1;
		Double trialDur = 0.0;
		String msg = "";
		try {
			msg = com.waitForMessage(hardTimeoutMs);

			jo = new JSONObject(msg);

			if ((ready &= checkJSONField("stimulus_duration")) > 0) {
				trialDur += jo.getDouble("stimulus_duration");

				//keep track of the predicted number of stimulus frames so that
				//we know when to stop accumulating frames on each trial
				stimFrames_ = (int) secToFrames(trialDur);
			}

			if ((ready &= checkJSONField("baseline_duration")) > 0) {
				Double baseDur = jo.getDouble("baseline_duration");

				//we also need to know the number of baseline frames to know when
				//to start averaging (for online analysis)
				baselineFrames_ = secToFrames(baseDur);
				trialDur += baseDur;
			}

			//the number of frames that we'll collect for each trial
			//we add 10% to be on the safe side...
			framesPerTrial = (int) secToFrames(trialDur * 1.1);			

			if ((ready &= checkJSONField("ntrial")) > 0) {
				totalTrials = jo.getInt("ntrial");
			}
			
			gui.logMessage("[SERIAL-REC]: " + msg);

		} catch (Exception e) {
			ready = 0;
			addErrorMessage(e);
		}

		return (ready > 0) ? msg : "";
	}
	/* ---------------------------------------------------------------------- */
	private int checkJSONField(String field) {
		Boolean b = jo.has(field);
		if (!b) {
			addErrorMessage("MISSING JSON FIELD: " + field);
		}
		return b ? 1 : 0;
	}
	/* ---------------------------------------------------------------------- */
	public int secToFrames(Double durSec) {
		//annoyingly, mmc.startSequenceAcquisition (see run) requires the nFrame
		//input to be an int, so we might as well cast here and save the memory
		return (int) Math.round(durSec * frameRate);
	}
	/* ---------------------------------------------------------------------- */
	public Boolean waitForCmd() throws InterruptedException {
		String msg = com.waitForMessage();

		if (msg.equals("STOP")) {
			state = false;
			gui.logMessage("[SERIAL-REC]: " + msg);
		} else if (msg.isEmpty()) {
			addErrorMessage("Timed out while waiting for trial start command from Spike2!");
			state = false;
		} else {
			state = true;
			trial++;
			label = Double.parseDouble(msg);
		}

		return state;
	}
	/* ---------------------------------------------------------------------- */
	private void writeParFile(String settings) throws Exception {
		//write the json/par file to disk within the acquisition directory
		String parFilePath = rootDir_ + File.separator
				+ acqName_ + File.separator
				+ acqName_ + ".json";

		File parFile = new File(parFilePath);
		parFile.createNewFile();

		FileWriter fw = new FileWriter(parFile);
		fw.write(settings);
		fw.flush();
		fw.close();
	}
	/* ---------------------------------------------------------------------- */
	@Override
	public void run() {

		try {

			mmc.setAutoShutter(false);
			mmc.setShutterOpen(true);

			// get run settings from Spike2 (we'll wait for max 1 minute)
			String settings = waitForSettings();
			if (settings.isEmpty()) {
				close();
				addErrorMessage("Failed to recieve settings from Spike2!");
				return;
			}

			// make sure we are writing to a multi-page "Tiff stack"
			ImageUtils.setImageStorageClass(TaggedImageStorageMultipageTiff.class);

			gui.openAcquisition(acqName_, rootDir_, framesPerTrial * totalTrials,
				1,    //number of channels
				1,    //number of slices
				1,    //number of positions
				true, //show live window
				true  //save to file (write to disk as opposed to pool in RAM)
				);

			gui.initializeAcquisition(acqName_,
				(int) mmc.getImageWidth(),
				(int) mmc.getImageHeight(),
				(int) mmc.getBytesPerPixel(),
				(int) mmc.getImageBitDepth()
				);

			//int trialFrames = 0;
			int totalFrames = 0;

			Double exposureMs = mmc.getExposure();
			String cameraName = mmc.getCameraDevice();
			Double sleepDur = Math.min(0.5 * exposureMs, 20.0);

			mmc.prepareSequenceAcquisition(cameraName);

			// main idle loop
			while (waitForCmd()) {

				//startSequenceAcquisition is non-blocking (image spooling runs on
				//another thread) so we are free to update the display and add
				//images to the Tiff stack as needed (which is what
				//addImageToAcquisition does)
				mmc.startSequenceAcquisition(framesPerTrial,
					0.0, //intervalMs
					true //stopOnOverflow
					);

				while (mmc.getRemainingImageCount() > 0 || mmc.isSequenceRunning(cameraName)) {
					if (mmc.getRemainingImageCount() > 0) {
						gui.addImageToAcquisition(acqName_, totalFrames, 0, 0, 0, mmc.popNextTaggedImage());
						totalFrames++;
					} else {
						mmc.sleep(sleepDur);
					}
				}

				mmc.stopSequenceAcquisition();
			}

			close();

			gui.closeAcquisitionWindow(acqName_);
			gui.closeAllAcquisitions();

			//write the json/par file to disk within the acquisition directory
			writeParFile(settings);

		} catch (Exception e) {
			addErrorMessage(e);
			cleanUp();
			displayError();
		}
	}
	/* ---------------------------------------------------------------------- */
	private void cleanUp() {
		close();

		try {
			mmc.stopSequenceAcquisition();
		} catch (Exception e) {
			addErrorMessage(e);
		}

		try {
			gui.closeAcquisition(acqName_);
		} catch (Exception e) {
			addErrorMessage(e);
		}
	}
	/* ---------------------------------------------------------------------- */
	public void displayError() {
		JOptionPane.showMessageDialog(null, errorMessage(),
			"ERROR", JOptionPane.ERROR_MESSAGE);
	}
	/* ---------------------------------------------------------------------- */
}
/* ========================================================================== */
