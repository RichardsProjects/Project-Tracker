package net.richardsprojects.projecttracker.windows;

import net.richardsprojects.projecttracker.Main;
import net.richardsprojects.projecttracker.ThemeUtils;
import net.richardsprojects.projecttracker.Utils;
import net.richardsprojects.projecttracker.actionlisteners.NewProjectHandler;
import net.richardsprojects.projecttracker.data.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainWindow extends JFrame {

	private JPanel mainJPanel;
	
	public MainWindow() {
		// create JFrame
		super("Project Manager");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	
		BufferedImage icon;
		try {
			//noinspection ConstantConditions
			icon = ImageIO.read(this.getClass().getClassLoader().getResource("icon.png"));
			setIconImage(icon);
		} catch (Exception e) {
			System.out.println("[WARNING] Unable to load icon");
		}        
		
		// create Menu
		MenuBar menubar = new MenuBar();
		
		// create Projects Menu
		Menu projectsMenu = new Menu("Projects");
		MenuItem newProject = new MenuItem("New Project");
		newProject.addActionListener(new NewProjectHandler());
		projectsMenu.add(newProject);
		
		Menu viewMenu = new Menu("View");
		MenuItem completedProjects = new MenuItem("Completed Projects");
		completedProjects.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Main.currentScreen = Screens.COMPLETED;
				updatePanel();
			}			
		});
		MenuItem inProgressProjects = new MenuItem("In Progress Projects");
		inProgressProjects.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Main.currentScreen = Screens.IN_PROGRESS;
				updatePanel();
			}			
		});
		viewMenu.add(completedProjects);
		viewMenu.add(inProgressProjects);
		
		// add Projects Menu to Menubar
		menubar.add(projectsMenu);
		menubar.add(viewMenu);
		
		setMenuBar(menubar); // add Menubar to JFrame
		
		createPanel();

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				if (Main.currentDialog != null) {
					Main.closeRequested = true;
					Main.currentDialog.dispatchEvent(new WindowEvent(Main.currentDialog, WindowEvent.WINDOW_CLOSING));
				} else {
					Main.save();
					Main.deleteLockFile();
					System.exit(0);
				}
			}
		});

		pack();

		// make JFrame visible
	    setExtendedState(JFrame.MAXIMIZED_BOTH);
	    setVisible(true);
	}
	
	private void createPanel() {
		String headerText = "";

		mainJPanel = ThemeUtils.createBasicPanel(false);
		mainJPanel.setLayout(new BorderLayout());

		if (Main.currentScreen == Screens.IN_PROGRESS) headerText = "Projects - In Progress";
		if (Main.currentScreen == Screens.COMPLETED) headerText = "Projects - Completed";
		if (Main.currentScreen == Screens.HISTORY) headerText = "History - " + Main.historyProject.getName();
	    mainJPanel.add(ThemeUtils.createHeader(headerText), BorderLayout.PAGE_START);

	    if (Main.currentScreen == Screens.HISTORY) {
			createHistoryPanel();
		} else {
			createMainPanel(Main.currentScreen);
	    }
		
		add(mainJPanel);
	}

	private void createHistoryPanel() {
		try {
			SimpleDateFormat dayFormatter = new SimpleDateFormat("yyyy-MM-dd");
			Date today = dayFormatter.parse(dayFormatter.format(new Date()));
			ArrayList<TimeSession> sessionsToday = new ArrayList<TimeSession>();

			for (TimeSession session : Main.historyProject.getTimeSessions()) {
				if (session.getStartTime().after(today)) {
					sessionsToday.add(session);
				}
			}

			String totalTimeToday = Utils.totalTimeSessionList(sessionsToday);
			JPanel historyPanel = ThemeUtils.createBasicPanel(true);
			historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.PAGE_AXIS));

			// back button
			JButton backButton = ThemeUtils.createGradientButton("Back");
			backButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (Main.previousScreen != null) {
						Main.currentScreen = Main.previousScreen;
						Main.previousScreen = null;
					} else {
						Main.currentScreen = Screens.IN_PROGRESS;
					}
					updatePanel();
				}
			});
			historyPanel.add(backButton);

			ThemeUtils.addSeperatorToJPanel(historyPanel);
			historyPanel.add(ThemeUtils.createLabel("Time today: " + totalTimeToday, ThemeUtils.getSmallFont()));

			mainJPanel.add(historyPanel, BorderLayout.LINE_START);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createMainPanel(Screens screen) {
		if (Main.projects.size() > 0) {
			JPanel projectsList = ThemeUtils.createBasicPanel(false);
			projectsList.setBorder(new EmptyBorder(8, 8, 8, 8));
			projectsList.setLayout(new BoxLayout(projectsList, BoxLayout.PAGE_AXIS));

			for (final Project p : Main.projects) {
				boolean shouldDisplay = false;
				if (Main.currentScreen == Screens.COMPLETED && p.getProjectStatus() == ProjectStatus.FINISHED)
					shouldDisplay = true;
				if (Main.currentScreen == Screens.IN_PROGRESS && (p.getProjectStatus() == ProjectStatus.IN_PROGRESS || p.getProjectStatus() == ProjectStatus.PRIORITY))
					shouldDisplay = true;

				if (shouldDisplay) {

					// main Project Panel
					JPanel projectPanel = ThemeUtils.createBasicPanel(true);
					projectPanel.setLayout(new BorderLayout());

					// title
					JPanel titlePanel = ThemeUtils.createBasicPanel(false);
					titlePanel.setLayout(new BorderLayout());
					String title = p.getName() + " - " + ProjectType.getName(p.getProjectType());
					Font font = new Font(new JLabel().getFont().getName(), Font.PLAIN, 24);
					titlePanel.add(ThemeUtils.createLabel(title, font), BorderLayout.LINE_START);

					JButton button = ThemeUtils.createGradientButton("Edit");

					button.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent arg0) {
							EditProjectWindow editProject = new EditProjectWindow(p);
						}
					});
					button.setMaximumSize(new Dimension(60, 20));
					titlePanel.add(button, BorderLayout.LINE_END);

					// information panel
					JPanel informationPanel = ThemeUtils.createBasicPanel(false);
					informationPanel.setLayout(new BoxLayout(informationPanel, BoxLayout.PAGE_AXIS));

					String dateInfo = "Date Added: " + Main.formatter.format(p.getDateAdded());
					String status = "Status: " + ProjectStatus.getName(p.getProjectStatus());
					String monthlyTime = "Monthly Time: " + p.getMonthlyTime();
					String monthlyIncome = "Income: $" + p.getMonthlyIncome();

					informationPanel.add(ThemeUtils.createLabel(dateInfo, ThemeUtils.getSmallFont()));
					informationPanel.add(ThemeUtils.createLabel(status, ThemeUtils.getSmallFont()));
					ThemeUtils.addSeperatorToJPanel(informationPanel);
					informationPanel.add(ThemeUtils.createLabel(monthlyTime, ThemeUtils.getSmallFont()));
					informationPanel.add(ThemeUtils.createLabel(monthlyIncome, ThemeUtils.getSmallFont()));
					ThemeUtils.addSeperatorToJPanel(informationPanel);
					informationPanel.add(ThemeUtils.createLabel("Total Time: " + p.getTotalTime(), ThemeUtils.getSmallFont()));
					informationPanel.add(ThemeUtils.createLabel("Total Income: $" + p.getTotalIncome(), ThemeUtils.getSmallFont()));

					JButton histButton = ThemeUtils.createGradientButton("History");
					histButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent arg0) {
							Main.previousScreen = Main.currentScreen;
							Main.currentScreen = Screens.HISTORY;
							Main.historyProject = p;
							updatePanel();
						}
					});
					informationPanel.add(histButton);

					// setup panel
					projectPanel.add(titlePanel, BorderLayout.PAGE_START);
					projectPanel.add(informationPanel, BorderLayout.LINE_START);
					projectPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, projectPanel.getMinimumSize().height));

					projectsList.add(projectPanel);
					JSeparator separator = new JSeparator();
					separator.setForeground(Color.GRAY);
					separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
					projectsList.add(separator);
				}
			}
			JScrollPane projectsListPane = new JScrollPane(projectsList);
			projectsListPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			projectsListPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			mainJPanel.add(projectsListPane, BorderLayout.CENTER);
		}
	}

	public void updatePanel() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
        		// reset and re-create panel
        		remove(mainJPanel);
        		createPanel();
        		
        		// update Panel
        		revalidate();
        		repaint();
            }
        });
	}

}
