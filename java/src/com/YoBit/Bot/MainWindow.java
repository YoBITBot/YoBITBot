package com.YoBit.Bot;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JSplitPane;
import javax.swing.JSlider;
import java.awt.Component;
import javax.swing.JRadioButton;

public class MainWindow extends JFrame
{
	private static final long serialVersionUID = 1L;

	private static Timer runTimer;
	private static boolean running = false;

	private SystemTray tray;
	private TrayIcon trayIcon;

	private static JTextArea output = null;
	public static JTextField textFieldAPIKey, textFieldAPISecret,
			textFieldNoonce, textFieldTradingPair, textFieldRiseTo;

	public static JTable tableSell, tableValue;

	public static JLabel rateLabel;

	public static boolean fixed = false;
	private static JButton btnStart;
	private JSplitPane splitPane;
	public static JSlider slider;
	private Box horizontalBox_1;
	private Box horizontalBox_1_1;
	private Component horizontalStrut;
	private JRadioButton rdbtnAbsoluteMinimumAmount;
	public static JTextField txtFixedamount;

	public static void main(String[] args)
	{
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run()
			{
				try
				{
					new MainWindow();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	public MainWindow()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e)
		{
		}

		Image img = Toolkit.getDefaultToolkit().createImage(ClassLoader.getSystemResource("favicon.png"));

		if (SystemTray.isSupported())
		{
			tray = SystemTray.getSystemTray();

			MenuItem menuItem = new MenuItem("Open");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					setVisible(true);
					setExtendedState(JFrame.NORMAL);
					tray.remove(trayIcon);
				}
			});

			PopupMenu popup = new PopupMenu();
			popup.add(menuItem);

			trayIcon = new TrayIcon(img, "YoBit Bot", popup);
			trayIcon.setImageAutoSize(true);
		}

		getContentPane().setLayout(new BorderLayout());
		setTitle("YoBit Bot");
		setIconImage(img);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setBounds(80, 80, 890, 500);
		setMinimumSize(new Dimension(600, 500));

		addWindowListener(new java.awt.event.WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent winEvt)
			{
				try
				{
					tray.add(trayIcon);
					setVisible(false);
				}
				catch (AWTException e)
				{
				}
				return;
			}

		});

		JMenuItem menuQuit = new JMenuItem("Quit", KeyEvent.VK_Q);
		menuQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.ALT_MASK));
		menuQuit.getAccessibleContext().setAccessibleDescription("Quit");
		menuQuit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event)
			{
				terminate();
			}
		});

		JMenu menu = new JMenu("Menu");
		menu.setMnemonic(KeyEvent.VK_M);
		menu.getAccessibleContext().setAccessibleDescription("Menu");
		menu.add(menuQuit);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(menu);
		menuBar.add(Box.createHorizontalGlue());

		JMenuItem menuHelp = new JMenuItem("Help", KeyEvent.VK_H);
		menuHelp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));
		menuHelp.getAccessibleContext().setAccessibleDescription("Help");
		menuHelp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event)
			{
				JOptionPane.showMessageDialog(null, "This application can be used to auto-trade on YoBit.net.\n\nIt performs small buy orders on a select trading pair on YoBIT.\n\nIt automatically re-sells the purchases, up enough to clear transaction fees and make a profit.\n\nIt keeps track of it's own sell orders, so it never purchases from itself.\n\nIt recombines sell orders to save on transaction fees.\n\nIt can be customized to trade all the way up to a pre-set target.\n\nIt will only create small buy orders, up to customizable max BTC value.\n\nIt will keep the market active, move the market up, and has proven to be profitable in a group effort.\n\nIt allows you to buy manually, off of the YoBIT website. The bot will perform sales for you at a profitable rate.\n\nHas been used on ORLY coin, TCR and POST. The more bots run, the faster increase of value.\n\nSee https://yobit.net/en/api/keys/ to create an 'info & trade & deposits' API Key and API Secret for this application.");
			}
		});
		menu = new JMenu("Help");
		menu.setMnemonic(KeyEvent.VK_H);
		menu.getAccessibleContext().setAccessibleDescription("Help");
		menu.add(menuHelp);
		menuBar.add(menu);

		setJMenuBar(menuBar);

		/* Main Screen */

		Box verticalBox = Box.createVerticalBox();
		getContentPane().add(verticalBox, BorderLayout.NORTH);

		JPanel p = new JPanel(new SpringLayout());
		verticalBox.add(p);

		JLabel l = new JLabel("YoBit API Key", JLabel.TRAILING);
		p.add(l);

		textFieldAPIKey = new JTextField();
		l.setLabelFor(textFieldAPIKey);
		p.add(textFieldAPIKey);

		l = new JLabel("YoBit API Secret", JLabel.TRAILING);
		p.add(l);

		textFieldAPISecret = new JTextField();
		l.setLabelFor(textFieldAPISecret);
		p.add(textFieldAPISecret);

		SpringUtilities.makeCompactGrid(p, 2, 2, //rows, cols
		6, 6, //initX, initY
		6, 6);

		textFieldAPIKey.setText(Settings.get("key.txt"));
		textFieldAPISecret.setText(Settings.get("secret.txt"));

		Box horizontalBox = Box.createHorizontalBox();
		verticalBox.add(horizontalBox);
		int w = l.getPreferredSize().width;

		l = new JLabel("Trading Pair", JLabel.TRAILING);
		horizontalBox.add(Box.createHorizontalStrut((w - l.getPreferredSize().width) + 6));
		horizontalBox.add(l);

		horizontalBox.add(Box.createHorizontalStrut(6));

		textFieldTradingPair = new JTextField(10);
		horizontalBox.add(textFieldTradingPair);
		textFieldTradingPair.setText("orly_btc");
		textFieldTradingPair.setToolTipText("The trading pair to work with. For example; 'orly_btc' or 'trump_btc'.");
		l.setLabelFor(textFieldTradingPair);

		horizontalBox.add(Box.createHorizontalStrut(10));

		l = new JLabel("NoOnce", JLabel.TRAILING);
		l.setToolTipText("");
		horizontalBox.add(l);
		l.setLabelFor(textFieldNoonce);

		horizontalBox.add(Box.createHorizontalStrut(6));

		textFieldNoonce = new JTextField(10);
		textFieldNoonce.setToolTipText("A number that may only be used one time. Once the number reaches it's maximum value, a new YoBit API Key must be created and used.");
		horizontalBox.add(textFieldNoonce);

		horizontalBox.add(Box.createHorizontalStrut(6));

		horizontalBox_1 = Box.createHorizontalBox();
		verticalBox.add(Box.createVerticalStrut(6));
		verticalBox.add(horizontalBox_1);

		l = new JLabel("Target Value", JLabel.TRAILING);
		horizontalBox_1.add(Box.createHorizontalStrut((w - l.getPreferredSize().width) + 6));
		horizontalBox_1.add(l);

		horizontalBox_1.add(Box.createHorizontalStrut(6));

		textFieldRiseTo = new JTextField(10);
		horizontalBox_1.add(textFieldRiseTo);
		textFieldRiseTo.setText("1000");
		textFieldRiseTo.setToolTipText("The value in SATOSHIs we are going to rise this market's value to.");
		l.setLabelFor(textFieldRiseTo);

		horizontalBox_1.add(Box.createHorizontalStrut(10));

		l = new JLabel("Trading Interval", JLabel.TRAILING);
		horizontalBox_1.add(l);

		horizontalBox_1.add(Box.createHorizontalStrut(6));

		slider = new JSlider();
		slider.setMinimum(5);
		slider.setMaximum(100);
		slider.setValue(10);
		final JLabel intervalLabel = new JLabel("Every 1.0 Seconds", JLabel.TRAILING);
		slider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0)
			{
				// TODO Auto-generated method stub
				JSlider source = (JSlider) arg0.getSource();
				if (!source.getValueIsAdjusting())
				{
					int fps = (int) source.getValue();
					intervalLabel.setText("Every " + Double.toString(fps / 10d) + " seconds");
				}
			}

		});
		horizontalBox_1.add(slider);

		horizontalBox_1.add(Box.createHorizontalStrut(6));
		horizontalBox_1.add(intervalLabel);

		horizontalBox_1.add(Box.createHorizontalStrut(6));

		horizontalBox_1_1 = Box.createHorizontalBox();
		verticalBox.add(Box.createVerticalStrut(6));
		verticalBox.add(horizontalBox_1_1);

		l = new JLabel("Buy orders", JLabel.TRAILING);
		horizontalBox_1_1.add(Box.createHorizontalStrut((w - l.getPreferredSize().width) + 6));
		horizontalBox_1_1.add(l);

		horizontalStrut = Box.createHorizontalStrut(6);
		horizontalBox_1_1.add(horizontalStrut);

		ActionListener radio = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				// TODO Auto-generated method stub
				if (arg0.getActionCommand().equals("fix"))
				{
					fixed = true;
				}
				else
				{
					fixed = false;
				}
			}
		};

		ButtonGroup group = new ButtonGroup();

		rdbtnAbsoluteMinimumAmount = new JRadioButton("Absolute minimum amount");
		rdbtnAbsoluteMinimumAmount.setActionCommand("min");
		rdbtnAbsoluteMinimumAmount.addActionListener(radio);
		rdbtnAbsoluteMinimumAmount.setSelected(true);
		fixed = false;
		group.add(rdbtnAbsoluteMinimumAmount);

		horizontalBox_1_1.add(rdbtnAbsoluteMinimumAmount);
		rdbtnAbsoluteMinimumAmount = new JRadioButton("Fixed amount");
		rdbtnAbsoluteMinimumAmount.setActionCommand("fix");
		rdbtnAbsoluteMinimumAmount.addActionListener(radio);
		group.add(rdbtnAbsoluteMinimumAmount);

		horizontalBox_1_1.add(rdbtnAbsoluteMinimumAmount);

		txtFixedamount = new JTextField();
		txtFixedamount.setText(YoBitAPI.decimal(150d));
		horizontalBox_1_1.add(txtFixedamount);
		txtFixedamount.setColumns(10);

		horizontalBox_1_1.add(Box.createHorizontalStrut(6));

		horizontalBox = Box.createHorizontalBox();
		verticalBox.add(Box.createVerticalStrut(6));
		verticalBox.add(horizontalBox);
		verticalBox.add(Box.createVerticalStrut(6));

		btnStart = new JButton("Start");
		horizontalBox.add(Box.createHorizontalStrut(6));
		horizontalBox.add(btnStart);
		horizontalBox.add(Box.createHorizontalStrut(6));
		rateLabel = new JLabel();
		horizontalBox.add(rateLabel);
		horizontalBox.add(Box.createHorizontalGlue());
		btnStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event)
			{
				if (running)
				{
					runTimer.cancel();
				}
				else
				{
					runTimer = new Timer();
					reschedule();
				}

				running = !running;
				btnStart.setText(running ? "Stop" : "Start");
			}
		});

		String[] columnNames = { "Symbol", "Available Amount", "Total Amount" };
		Object[][] data = {};
		DefaultTableModel dataModel = new DefaultTableModel(data, columnNames);
		tableValue = new JTable();
		tableValue.setRowSelectionAllowed(false);
		tableValue.setShowVerticalLines(false);
		tableValue.setModel(dataModel);

		JScrollPane scrollPane1 = new JScrollPane(tableValue);
		scrollPane1.setViewportBorder(null);
		Dimension d = tableValue.getPreferredSize();
		scrollPane1.setPreferredSize(new Dimension(d.width, tableValue.getRowHeight() * 10));

		columnNames = new String[] { "Symbol", "Amount", "Selling at", "BTC Value" };
		data = new Object[][] {};
		dataModel = new DefaultTableModel(data, columnNames);
		tableSell = new JTable();
		tableSell.setRowSelectionAllowed(false);
		tableSell.setShowVerticalLines(false);
		tableSell.setModel(dataModel);

		JScrollPane scrollPane2 = new JScrollPane(tableSell);
		scrollPane2.setViewportBorder(null);
		d = tableSell.getPreferredSize();
		scrollPane2.setPreferredSize(new Dimension(d.width, tableSell.getRowHeight() * 10));

		JSplitPane splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane1, scrollPane2);
		splitPane2.setResizeWeight(0.5d);

		Box horizontalBox_2 = Box.createHorizontalBox();
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane2, horizontalBox_2);
		splitPane.setResizeWeight(0.5d);
		splitPane.setDividerLocation(0.5d);

		getContentPane().add(splitPane, BorderLayout.CENTER);

		/*
		verticalBox.add(horizontalBox);
		verticalBox.add(Box.createVerticalStrut(6));*/

		verticalBox = Box.createVerticalBox();

		output = new JTextArea();
		DefaultCaret caret = (DefaultCaret) output.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		output.setEditable(false);

		JScrollPane sp = new JScrollPane(output);
		horizontalBox_2.add(sp);
		verticalBox.add(Box.createVerticalStrut(6));

		doOutput("Reading settings from " + Settings.getUserDataDirectory());
		Settings.getNoonce(false);

		setVisible(true);
		if (!textFieldAPIKey.getText().toString().equals("") && !textFieldAPISecret.getText().toString().equals(""))
		{
			btnStart.requestFocus();
		}
	}

	public static void reschedule()
	{
		runTimer.schedule(new TimerTask() {

			@Override
			public void run()
			{
				try
				{
					if (running) YoBitAPI.getRate();
					if (running) YoBitAPI.getFunds();
					if (running) YoBitAPI.getOrders();
					if (running) reschedule();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, slider.getValue() * 100);

	}

	public static void doOutput(String text)
	{
		if (output != null)
		{
			text = "[ " + new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()) + " ] " + text;

			if (!output.getText().equals(""))
			{
				output.setText(output.getText() + "\n" + text);
			}
			else
			{
				output.setText(text);
			}
		}
	}

	public static void doStop(String text)
	{
		doOutput("[ ERROR ] " + text);
		runTimer.cancel();
		running = false;
		btnStart.setText("Start");
	}

	private void terminate()
	{
		System.exit(0);
	}
}