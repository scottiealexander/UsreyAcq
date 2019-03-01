package org.micromanager.UsreyAcq;

import org.json.JSONObject;
import org.micromanager.MMStudio;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.FileDialogs;
import org.micromanager.utils.MMFrame;

import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;

import javax.swing.JOptionPane;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* ========================================================================== */
@SuppressWarnings("serial")
public class UsreyAcqDialog extends MMFrame {
    /* ---------------------------------------------------------------------- */
    private static String ACQ_SETTINGS_FILE = "usrey_acq_settings.json";
	/* ---------------------------------------------------------------------- */
	private JTextField rootDirField_;
	private JTextField nameField_;
	private JTextField portField_;

	//private UsreyAcqController ctrl_;
	private ScriptInterface app_;
	
	private JSONObject prop_;

	/* ---------------------------------------------------------------------- */
	public UsreyAcqDialog(ScriptInterface app) {
		super();

		app_ = app;

		getContentPane().setLayout(null);
	    setResizable(false);
	    setTitle("Usrey Data Acquisition");

	    //load the json file where we save the last parameters entered by a user
        loadPropertyFile();

	    /* --- Root directory text box --- */
		JLabel label_ = new JLabel();
		label_.setFont(new Font("Arial", Font.PLAIN, 10));
		label_.setText("Directory root:");
		label_.setBounds(30, 30, 70, 22);
		getContentPane().add(label_);

		rootDirField_ = new JTextField();
		rootDirField_.setFont(new Font("Arial", Font.PLAIN, 10));
        rootDirField_.setText(getProp("root_dir"));
		rootDirField_.setBounds(105, 30, 354, 22);
		getContentPane().add(rootDirField_);

		/* --- Root directory browse button --- */
		JButton browseButton_ = new JButton();
		browseButton_.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				setRootDirectory();
			}
		});

		browseButton_.setMargin(new Insets(2, 5, 2, 5));
		browseButton_.setFont(new Font("Dialog", Font.PLAIN, 10));
		browseButton_.setText("...");
		browseButton_.setBounds(465, 30, 50, 22);
		browseButton_.setToolTipText("Browse");
		getContentPane().add(browseButton_);

		/* --- Acquisition name text box --- */
		JLabel nameLabel_ = new JLabel();
		nameLabel_.setFont(new Font("Arial", Font.PLAIN, 10));
		nameLabel_.setText("Acquisition Name:");
		nameLabel_.setBounds(10, 65, 90, 22);
		getContentPane().add(nameLabel_);

		nameField_ = new JTextField();
		nameField_.setFont(new Font("Arial", Font.PLAIN, 10));
        nameField_.setText(getProp("acq_name"));
		nameField_.setBounds(105, 65, 100, 22);
		getContentPane().add(nameField_);

		/* --- Serial COM port text box --- */
		JLabel portLabel = new JLabel();
		portLabel.setFont(new Font("Arial", Font.PLAIN, 10));
		portLabel.setText("Spike2 COM port:");
		portLabel.setBounds(10, 100, 90, 22);
		getContentPane().add(portLabel);

		portField_ = new JTextField();
		portField_.setFont(new Font("Arial", Font.PLAIN, 10));
        portField_.setText(getProp("com_port"));
		portField_.setBounds(105, 100, 50, 22);
		getContentPane().add(portField_);

		/* --- Start acquisition button --- */
		JButton startButton_ = new JButton();
		startButton_.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				runAcquisition();
			}
		});

		startButton_.setMargin(new Insets(2, 5, 2, 5));
		startButton_.setFont(new Font("Dialog", Font.PLAIN, 10));
		startButton_.setText("Run!");
		startButton_.setBounds(212, 135, 45, 22);
		startButton_.setToolTipText("Begin acquisition");
		getContentPane().add(startButton_);

		/* --- Abort plugin button --- */
		JButton abortButton_ = new JButton();
		abortButton_.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				dispose();
			}
		});

		abortButton_.setMargin(new Insets(2, 5, 2, 5));
		abortButton_.setFont(new Font("Dialog", Font.PLAIN, 10));
		abortButton_.setText("Abort");
		abortButton_.setBounds(267, 135, 45, 22);
		abortButton_.setToolTipText("Abort UsreyAcq");
		getContentPane().add(abortButton_);

		loadAndRestorePosition(400, 300);
		setSize(525, 200);
	}
	/* ---------------------------------------------------------------------- */
	public void setRootDirectory() {
	      File result = FileDialogs.openDir(this,
	              "Please choose a directory root for image data",
	              MMStudio.MM_DATA_SET);

	      if (result != null) {
	    	  rootDirField_.setText(result.getAbsolutePath());
	      }
	}
	/* ---------------------------------------------------------------------- */
	public void runAcquisition() {

		String acqName = nameField_.getText();
		String rootDir = rootDirField_.getText();
		String comPort = portField_.getText();

		if (!verifyEntries()) {

			String str = "\n" + acqName + "\n" + rootDir + "\n" + comPort;
			JOptionPane.showMessageDialog(this,
				"You *MUST* specify a name, a directory, and a valid COM port!" + str,
				"ERROR",
				JOptionPane.ERROR_MESSAGE);

		} else {
			
            writePropertyFile(rootDir, acqName, comPort);
            makeRootDirectory(rootDir);

			UsreyAcqController ctrl = new UsreyAcqController(app_, rootDir, acqName, comPort);

			if (!ctrl.anyErrors()) {
				//UsreyAcqController implements the Runnable interface, so run()
				//is called within a new thread and thus thread.start() returns
				//more or less immediately allowing us to close up the dialog
				//window cleanly
				Thread thread = new Thread(ctrl);
				thread.start();

			} else {
				ctrl.displayError();
			}

			//close the dialog and release associated resources
			dispose();
		}
	}
	/* ---------------------------------------------------------------------- */
    private Boolean verifyEntries() {
        int ready = 1;
        ready &= nameField_.getText().isEmpty() ? 0 : 1;
        ready &= rootDirField_.getText().isEmpty() ? 0 : 1;

        String port = portField_.getText();
        ready &= (port.matches("COM\\d") || port.equals("DEBUG")) ? 1 : 0;

        return (ready > 0);
    }
    /* ---------------------------------------------------------------------- */
    private void makeRootDirectory(String dirStr) {
    	File rootDir = new File(dirStr);
    	if (!rootDir.exists()) {
    		rootDir.mkdirs();
    	}
    }
    /* ---------------------------------------------------------------------- */
    private void loadPropertyFile() {
        File propFile = getPropertyFile();

        if (propFile.exists()) {
        	try {
	            FileReader fr = new FileReader(propFile);
	            BufferedReader br = new BufferedReader(fr);
	            
	            String line = null;
	            String str = "";
	            
	            while((line = br.readLine()) != null) {
	                str += line;
	            }
	            
	            br.close();
	
	            JSONObject tmp = new JSONObject(str);

	            prop_ = defaultProperties(tmp);
	            
        	} catch (Exception e) {
        		
        		app_.logError(e);
        		
        		//fall back to default properties
        		prop_ = defaultProperties();
        	}

        } else {
            prop_ = defaultProperties();
        }
    }
    /* ---------------------------------------------------------------------- */
    private JSONObject defaultProperties(JSONObject base) {
        JSONObject def = defaultProperties();

        //freakin JSON library, get()/getString()  throw exceptions so we have
        //to wrap this in a try/catch block even though we never access a field
        //without checking that it first exists...
        try {
	        //deal with incrementsing (or getting default) acq_name first
	        if (base.has("acq_name")) {
	            base.put("acq_name", getNextAcqName(base.getString("acq_name")));
	        } else {
	            base.put("acq_name", def.get("acq_name"));
	        }
	
	        String fields[] = {"root_dir", "com_port"};
	        for (String field: fields) {
	            if (!base.has(field)) {
	                base.put(field, def.get(field));
	            }
	        }
	        
        } catch (Exception e) {
        	app_.logError(e);
        }
        
        return base;
    }
    /* ---------------------------------------------------------------------- */
    private JSONObject defaultProperties() {
        JSONObject props = new JSONObject();

        String date = new SimpleDateFormat("yyyyddMM").format(new Date());

        try {
	        props.put("root_dir", "E:\\" + date);
	        props.put("acq_name", "run_000");
	        props.put("com_port", "COM4");
        } catch (Exception e) {
        	//WTF? mapping a string to a string shouldn't ever cause an error
        	//(this is clear if you read the source for JSONObject.put()...)
        	app_.logError(e);
        }

        return props;
    }
    /* ---------------------------------------------------------------------- */
    private String getNextAcqName(String acqName) {
        Pattern r = Pattern.compile("(\\d+)$");
        Matcher m = r.matcher(acqName);
        
        int runNumber = -1;
        String prefix = "run_";
        
        if (m.find()) {
        	//turns out it is easier to greedily capture trailing digits and then
        	//remove them then it is to capture a prefix and trailing digits...
            prefix = acqName.replaceAll("\\d*$", "");
            try {
                runNumber = Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                runNumber = -1;
            }
        }

        return String.format("%s%03d", prefix, runNumber+1);
    }
    /* ---------------------------------------------------------------------- */
    private void writePropertyFile(String rootDir, String acqName, String comPort) {
    	//do we need to remove the escaping "\" for writing?
    	setProp("root_dir", rootDir);
		setProp("acq_name", acqName);
		setProp("com_port", comPort);

		try {
			File propFile = getPropertyFile();
			app_.logMessage("[USREYACQ]: Writing properties to: " + propFile.getPath());
	        FileWriter fw = new FileWriter(propFile);
			fw.write(prop_.toString(4));
			fw.flush();
			fw.close();
		} catch (Exception e) {
			app_.logError(e);
		}
    }
    /* ---------------------------------------------------------------------- */
    private File getPropertyFile() {
        File mmDir = new File(app_.getMMCore().getPrimaryLogFile());
        return new File(mmDir.getParentFile().getParent(), ACQ_SETTINGS_FILE);
    }
    /* ---------------------------------------------------------------------- */
    private String getProp(String key) {
    	String val = "";
    	try {
    		val = prop_.getString(key);
    	} catch (Exception e) {
    		app_.logError(e);
    	}
    	return val;
    }
    /* ---------------------------------------------------------------------- */
    private Boolean setProp(String key, String val) {
    	Boolean success = true;
    	try {
    		prop_.put(key, val);
    	} catch (Exception e) {
    		app_.logError(e);
    		success = false;
    	}
    	return success;
    }
    /* ---------------------------------------------------------------------- */
}
/* ========================================================================== */
