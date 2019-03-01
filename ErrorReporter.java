package org.micromanager.UsreyAcq;

import java.io.PrintWriter;
import java.io.StringWriter;

/* ========================================================================== */
public abstract class ErrorReporter {
	protected Boolean error = false;
	protected String errMsg = "";
	/* ---------------------------------------------------------------------- */
	public Boolean anyErrors() {
		return error;
	}
	/* ---------------------------------------------------------------------- */
	public String errorMessage() {
		return errMsg;
	}
	/* ---------------------------------------------------------------------- */
	protected void addErrorMessageTrace(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		addErrorMessage(e.getMessage() + "\n" + sw.toString());
	}
	/* ---------------------------------------------------------------------- */
	protected void addErrorMessage(Exception e) {
		addErrorMessageTrace(e);
	}
	/* ---------------------------------------------------------------------- */
	protected void addErrorMessage(String msg) {
		error = true;
		if (errMsg.isEmpty()) {
			errMsg = msg;
		} else {
			errMsg += "\n" + msg;
		}
	}
	/* ---------------------------------------------------------------------- */
}
/* ========================================================================== */