package com.logcat.offline;

import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;

import com.android.ddmuilib.ITableFocusListener;
import com.android.ddmuilib.ImageLoader;
import com.logcat.offline.view.ddmuilib.logcat.LogCatMessageParser;
import com.logcat.offline.view.ddmuilib.logcat.LogCatPanel;
import com.logcat.offline.view.ddmuilib.logcat.OfflinePreferenceStore;

public class UIThread {
	private static final int MINIMAL_HEIGHT = 20;
    private static final String APP_NAME = "LogcatViewer";
	private static UIThread uiThread;
	
	private static final String PREFERENCE_LOGSASH_H = "logSashLocation.h";
	private static final String PREFERENCE_LOGSASH_V = "logSashLocation.v";
	private static final String PREFERENCE_LAST_OPEN_FOLDER = "log.last.openfolder";
	
	public static final int PANEL_ID_MAIN = 0;
	public static final int PANEL_ID_EVENTS = 1;
	public static final int PANEL_ID_RADIO = 2;
	
	private Display mDisplay;
	private Label mStatusLine;
	
	private PreferenceStore mPreferenceStore;
	private LogCatPanel mLogCatPanel_main;
	private LogCatPanel mLogCatPanel_event;
	private LogCatPanel mLogCatPanel_radio;
	
	private Clipboard mClipboard;
    private MenuItem mCopyMenuItem;
    private MenuItem mSelectAllMenuItem;
    private MenuItem mPreviousMenuItem;
    private TableFocusListener mTableListener;
    private MenuItem mNextMenuItem;
	
	private UIThread(){
	}
	
	public static UIThread getInstance(){
		if (uiThread == null){
			uiThread = new UIThread();
		}
		return uiThread;
	}
	
	private class TableFocusListener implements ITableFocusListener {

        private IFocusedTableActivator mCurrentActivator;

        @Override
        public void focusGained(IFocusedTableActivator activator) {
            mCurrentActivator = activator;
            if (mCopyMenuItem.isDisposed() == false) {
                mCopyMenuItem.setEnabled(true);
                mSelectAllMenuItem.setEnabled(true);
                mPreviousMenuItem.setEnabled(true);
                mNextMenuItem.setEnabled(true);
            }
        }

        @Override
        public void focusLost(IFocusedTableActivator activator) {
            // if we move from one table to another, it's unclear
            // if the old table lose its focus before the new
            // one gets the focus, so we need to check.
            if (activator == mCurrentActivator) {
                activator = null;
                if (mCopyMenuItem.isDisposed() == false) {
                    mCopyMenuItem.setEnabled(false);
                    mSelectAllMenuItem.setEnabled(false);
                    mPreviousMenuItem.setEnabled(false);
                    mNextMenuItem.setEnabled(false);
                }
            }
        }

        public void copy(Clipboard clipboard) {
            if (mCurrentActivator != null) {
                mCurrentActivator.copy(clipboard);
            }
        }

        public void selectAll() {
            if (mCurrentActivator != null) {
                mCurrentActivator.selectAll();
            }
        }

        public void previous() {
            if (mCurrentActivator != null) {
                mCurrentActivator.previous();
            }
        }
        
        public void next() {
            if (mCurrentActivator != null) {
                mCurrentActivator.next();
            }
        }
    }
	
	public void runUI(String[] args) {
        Display.setAppName(APP_NAME);
        mDisplay = Display.getDefault();
        Shell shell = new Shell(mDisplay, SWT.SHELL_TRIM);
        shell.setImage(ImageLoader.getDdmUiLibLoader().loadImage("ddms-128.png", mDisplay));
        shell.setText("LogcatOfflineView");
        mPreferenceStore = OfflinePreferenceStore.getPreferenceStore();
        createMenus(shell);
        createWidgets(shell);
        shell.pack();
        shell.setMaximized(true);
        if(args.length == 1)
        {
        	LogCatMessageParser.getInstance().parseLogFile(args[0], PANEL_ID_MAIN);	
        }
        shell.open();
        while (!shell.isDisposed()) {
            if (!mDisplay.readAndDispatch())
                mDisplay.sleep();
        }
        ImageLoader.dispose();
        mDisplay.dispose();
        OfflinePreferenceStore.save();
    }
	
	private void createMenus(final Shell shell){
		// create menu bar
		Menu menuBar = new Menu(shell, SWT.BAR);

        // create top-level items
        MenuItem fileItem = new MenuItem(menuBar, SWT.CASCADE);
        fileItem.setText("&File");
        MenuItem editItem = new MenuItem(menuBar, SWT.CASCADE);
        editItem.setText("&Edit");
        MenuItem aboutItem = new MenuItem(menuBar, SWT.CASCADE);
        aboutItem.setText("&Help");
        
        Menu fileMenu = new Menu(menuBar);
        fileItem.setMenu(fileMenu);
        Menu editMenu = new Menu(menuBar);
        editItem.setMenu(editMenu);
        Menu aboutMenu = new Menu(menuBar);
        aboutItem.setMenu(aboutMenu);

        MenuItem item;
        // create File menu items
        item = new MenuItem(fileMenu, SWT.NONE);
        item.setText("&Open File\tCtrl-O");
        item.setAccelerator('O' | SWT.MOD1);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String filePath = new FileDialog(shell).open();
                LogCatMessageParser.getInstance().parseLogFile(filePath, PANEL_ID_MAIN);
            }
        });
        
        // create Open bugreport menu items
        item = new MenuItem(fileMenu, SWT.NONE);
        item.setText("&Open bugreport(dumpstate) file\tCtrl-B");
        item.setAccelerator('B' | SWT.MOD1);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String filePath = new FileDialog(shell).open();
                LogCatMessageParser.getInstance().parseDumpstateFile(filePath);
            }
        });
        
        item = new MenuItem(fileMenu, SWT.NONE);
        item.setText("Open Log &Folder\tCtrl-F");
        item.setAccelerator('F' | SWT.MOD1);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(shell);
				String lastFolder = mPreferenceStore
						.getString(PREFERENCE_LAST_OPEN_FOLDER);
				if (lastFolder != null) {
					directoryDialog.setFilterPath(lastFolder);
				}
				String folderPath = directoryDialog.open();
				if (folderPath != null) {
					LogCatMessageParser.getInstance()
							.parseLogFolder(folderPath);
					if (lastFolder != null
							&& folderPath.compareTo(lastFolder) != 0) {
						mPreferenceStore.setValue(PREFERENCE_LAST_OPEN_FOLDER,
								folderPath);
					}
				}
			}
        });
        
        new MenuItem(fileMenu, SWT.SEPARATOR);
        
        item = new MenuItem(fileMenu, SWT.NONE);
        item.setText("E&xit\tCtrl-Q");
        item.setAccelerator('Q' | SWT.MOD1);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                shell.close();
            }
        });
        
     // create edit menu items
        mCopyMenuItem = new MenuItem(editMenu, SWT.NONE);
        mCopyMenuItem.setText("&Copy\tCtrl-C");
        mCopyMenuItem.setAccelerator('C' | SWT.MOD1);
        mCopyMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mTableListener.copy(mClipboard);
            }
        });

        new MenuItem(editMenu, SWT.SEPARATOR);

        mSelectAllMenuItem = new MenuItem(editMenu, SWT.NONE);
        mSelectAllMenuItem.setText("Select &All\tCtrl-A");
        mSelectAllMenuItem.setAccelerator('A' | SWT.MOD1);
        mSelectAllMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mTableListener.selectAll();
            }
        });
        
        new MenuItem(editMenu, SWT.SEPARATOR);

        mPreviousMenuItem = new MenuItem(editMenu, SWT.NONE);
        mPreviousMenuItem.setText("&Previous item\tCtrl-,");
        mPreviousMenuItem.setAccelerator(',' | SWT.MOD1);
        mPreviousMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mTableListener.previous();
            }
        });
        mNextMenuItem = new MenuItem(editMenu, SWT.NONE);
        mNextMenuItem.setText("&Next item\tCtrl-.");
        mNextMenuItem.setAccelerator('.' | SWT.MOD1);
        mNextMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mTableListener.next();
            }
        });
        item = new MenuItem(aboutMenu, SWT.NONE);
        item.setText("&Discuss-group");
        item.addSelectionListener(new SelectionAdapter(){
        	@Override
            public void widgetSelected(SelectionEvent e) {
        		try {
					Runtime.getRuntime().exec("cmd /c start " +
							"http://groups.google.com/group/androidlogcatviewer");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            }
        });
        
        item = new MenuItem(aboutMenu, SWT.NONE);
        item.setText("&Project site");
        item.addSelectionListener(new SelectionAdapter(){
        	@Override
            public void widgetSelected(SelectionEvent e) {
        		try {
					Runtime.getRuntime().exec("cmd /c start " +
							"http://code.google.com/p/androidlogcatviewer/");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            }
        });
        
        item = new MenuItem(aboutMenu, SWT.NONE);
        item.setText("&About");
        item.addSelectionListener(new SelectionAdapter(){
        	@Override
            public void widgetSelected(SelectionEvent e) {
        		String msg = " Email : m41m41.a@gmail.com\n"
        					+" Email : yuru_1012@163.com";
        		MessageDialog.openInformation(shell, "About Tool", msg);
            }
        });
        
        // tell the shell to use this menu
        shell.setMenuBar(menuBar);
	}
	
	private void createWidgets(final Shell shell) {
        Color darkGray = shell.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
        shell.setLayout(new GridLayout(1, false));
        final Composite panelArea = new Composite(shell, SWT.BORDER);
        panelArea.setLayoutData(new GridData(GridData.FILL_BOTH));
        mStatusLine = new Label(shell, SWT.NONE);
        mStatusLine.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mStatusLine.setText("Initializing...");

        Composite mainPanel = new Composite(panelArea, SWT.NONE);
        final Sash sash_h = new Sash(panelArea, SWT.HORIZONTAL);
        sash_h.setBackground(darkGray);
        Composite eventPanel = new Composite(panelArea, SWT.NONE);
        final Sash sash_v = new Sash(panelArea, SWT.VERTICAL);
        sash_v.setBackground(darkGray);
        Composite radioPanel = new Composite(panelArea, SWT.NONE);

        panelArea.setLayout(new FormLayout());
        createMainPanel(mainPanel);
        createEventPanel(eventPanel);
        createRadioPanel(radioPanel);
        
        mClipboard = new Clipboard(panelArea.getDisplay());

        // form layout data
        FormData data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(sash_h, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        mainPanel.setLayoutData(data);

        final FormData sashData_h = new FormData();
        if (mPreferenceStore != null && mPreferenceStore.contains(PREFERENCE_LOGSASH_H)) {
        	sashData_h.top = new FormAttachment(0, mPreferenceStore.getInt(
                    PREFERENCE_LOGSASH_H));
        } else {
        	sashData_h.top = new FormAttachment(50,0); // 50% across
        }
        sashData_h.left = new FormAttachment(0, 0);
        sashData_h.right = new FormAttachment(100, 0);
        sash_h.setLayoutData(sashData_h);

        data = new FormData();
        data.top = new FormAttachment(sash_h, 0);
        data.bottom = new FormAttachment(100, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(sash_v, 0);
        eventPanel.setLayoutData(data);
        
        final FormData sashData_v = new FormData();
        sashData_v.top = new FormAttachment(sash_h, 0);
        sashData_v.bottom = new FormAttachment(100, 0);
        if (mPreferenceStore != null && mPreferenceStore.contains(PREFERENCE_LOGSASH_V)) {
        	sashData_v.left = new FormAttachment(0, mPreferenceStore.getInt(
                    PREFERENCE_LOGSASH_V));
        } else {
        	sashData_v.left = new FormAttachment(50,0); // 50% across
        }
        sash_v.setLayoutData(sashData_v);

        data = new FormData();
        data.top = new FormAttachment(sash_h, 0);
        data.bottom = new FormAttachment(100, 0);
        data.left = new FormAttachment(sash_v, 0);
        data.right = new FormAttachment(100, 0);
        radioPanel.setLayoutData(data);

        sash_h.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                Rectangle sashRect = sash_h.getBounds();
                Rectangle panelRect = panelArea.getClientArea();
                int bottom = panelRect.height - sashRect.height - MINIMAL_HEIGHT;
                e.y = Math.max(Math.min(e.y, bottom), MINIMAL_HEIGHT);
                if (e.y != sashRect.y) {
                	sashData_h.top = new FormAttachment(0, e.y);
                    if (mPreferenceStore != null) {
                    	mPreferenceStore.setValue(PREFERENCE_LOGSASH_H, e.y);
                    }
                    panelArea.layout();
                }
            }
        });
        
        sash_v.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                Rectangle sashRect = sash_v.getBounds();
                Rectangle panelRect = panelArea.getClientArea();
                int right = panelRect.width - sashRect.width - 100;
                e.x = Math.max(Math.min(e.x, right), 100);
                if (e.x != sashRect.x) {
                	sashData_v.left = new FormAttachment(0, e.x);
                    if (mPreferenceStore != null) {
                    	mPreferenceStore.setValue(PREFERENCE_LOGSASH_V, e.x);
                    }
                    panelArea.layout();
                }
            }
        });
        
     // add a global focus listener for all the tables
        mTableListener = new TableFocusListener();

        mLogCatPanel_main.setTableFocusListener(mTableListener);
        mLogCatPanel_event.setTableFocusListener(mTableListener);
        mLogCatPanel_radio.setTableFocusListener(mTableListener);

        mStatusLine.setText("");
    }
	
	private void createMainPanel(Composite parent) {
        mLogCatPanel_main = new LogCatPanel(mPreferenceStore, PANEL_ID_MAIN, "main buffer");
        mLogCatPanel_main.createControl(parent);
        addDropSupport(parent, PANEL_ID_MAIN);
    }

	private void createEventPanel(Composite parent) {
        mLogCatPanel_event = new LogCatPanel(mPreferenceStore, PANEL_ID_EVENTS, "events buffer");
        mLogCatPanel_event.createControl(parent);
        addDropSupport(parent, PANEL_ID_EVENTS);
	}
	
	private void createRadioPanel(Composite parent) {
        mLogCatPanel_radio = new LogCatPanel(mPreferenceStore, PANEL_ID_RADIO, "radio buffer");
        mLogCatPanel_radio.createControl(parent);
        addDropSupport(parent, PANEL_ID_RADIO);
	}

    private void addDropSupport(Composite parent, final int panelIdMain) {
        final FileTransfer fileTransfer = FileTransfer.getInstance();
        DropTarget target = new DropTarget(parent, DND.DROP_MOVE | DND.Drop | DND.DROP_DEFAULT);
        target.setTransfer(new Transfer[] { fileTransfer });
        target.addDropListener(new DropTargetListener() {

            @Override
            public void dropAccept(DropTargetEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void drop(DropTargetEvent event) {
                if (fileTransfer.isSupportedType(event.currentDataType)) {
                    String[] files = (String[]) event.data;
                    for (int i = 0; i < files.length; i++) {
                        LogCatMessageParser.getInstance().parseLogFile(files[i], panelIdMain);
                        break;
                    }
                }
            }

            @Override
            public void dragOver(DropTargetEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void dragOperationChanged(DropTargetEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void dragLeave(DropTargetEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void dragEnter(DropTargetEvent arg0) {
                // TODO Auto-generated method stub

            }
        });
    }
}