package org.micromanager.UsreyAcq;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

//import javax.swing.JOptionPane;

import org.micromanager.UsreyAcq.UsreyAcqDialog;

public class UsreyAcqPlugin implements MMPlugin {
   public static final String menuName = "UsreyAcq";
   public static final String tooltipDescription =
      "Usrey lab imaging acqusition controller";

   // Provides access to the Micro-Manager Java API (for GUI control and high-
   // level functions).
   private ScriptInterface app_;

	@Override
	public String getCopyright() {
		return "University of California, Davis 2017";
	}

	@Override
	public String getDescription() {
		return tooltipDescription;
	}

	@Override
	public String getInfo() {
		return "Spike2-MM acquisition control interface for use in Usrey lab"
				+ " imagaing experiments";
	}

	@Override
	public String getVersion() {
		return "0.0.1";
	}

	@Override
	public void dispose() {
		//anything to do here?
	}

	@Override
	public void setApp(ScriptInterface app) {
		app_ = app;
	}

	@Override
	public void show() {
		//NOTE: nothing in this function is blocking, so it returns as soon as the
		//UsreyAcqDialog is created
		UsreyAcqDialog dlg = new UsreyAcqDialog(app_);
		dlg.setVisible(true);
	}

}
