package com.apiary.abm.ui;

import com.apiary.abm.entity.TreeNodeEntity;
import com.apiary.abm.entity.blueprint.ABMEntity;
import com.apiary.abm.entity.blueprint.ActionsEntity;
import com.apiary.abm.entity.blueprint.ResourceGroupsEntity;
import com.apiary.abm.entity.blueprint.ResourcesEntity;
import com.apiary.abm.enums.NodeTypeEnum;
import com.apiary.abm.enums.TreeNodeTypeEnum;
import com.apiary.abm.renderer.ABMTreeCellRenderer;
import com.apiary.abm.utility.ConfigPreferences;
import com.apiary.abm.utility.Network;
import com.apiary.abm.utility.Preferences;
import com.apiary.abm.utility.Utils;
import com.apiary.abm.view.ImageButton;
import com.apiary.abm.view.JBackgroundPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.Tree;

import net.miginfocom.swing.MigLayout;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;


public class ABMToolWindowMain extends JFrame
{
	private ToolWindow mToolWindow;
	private JLabel mInformationIcon;


	public ABMToolWindowMain(ToolWindow toolWindow)
	{
		mToolWindow = toolWindow;
		mToolWindow.getContentManager().removeAllContents(true);

		initLayout();
	}


	private void initLayout()
	{
		// create UI
		final JBackgroundPanel myToolWindowContent = new JBackgroundPanel("drawable/img_background.png", JBackgroundPanel.JBackgroundPanelType.BACKGROUND_REPEAT);
		final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
		final Content content = contentFactory.createContent(myToolWindowContent, "", false);
		mToolWindow.getContentManager().removeAllContents(true);
		mToolWindow.getContentManager().addContent(content);

		// MIGLAYOUT ( params, columns, rows)
		// insets TOP LEFT BOTTOM RIGHT
		myToolWindowContent.setLayout(new MigLayout("insets 0, flowy, fillx, filly", "[fill, grow]", "[fill,top][fill, grow][fill,bottom]"));

		final JBackgroundPanel topPanel = new JBackgroundPanel("drawable/img_box_top.png", JBackgroundPanel.JBackgroundPanelType.PANEL);
		final JPanel middlePanel = new JPanel();
		final JBScrollPane middleScrollPanel = new JBScrollPane(middlePanel, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final JBackgroundPanel bottomPanel = new JBackgroundPanel("drawable/img_box_bottom.png", JBackgroundPanel.JBackgroundPanelType.PANEL);

		topPanel.setMinimumSize(new Dimension(0, Utils.reDimension(90)));
		bottomPanel.setMinimumSize(new Dimension(0, Utils.reDimension(90)));

		topPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Utils.reDimension(90)));
		bottomPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Utils.reDimension(90)));

		// add elements
		topPanel.setLayout(new MigLayout("insets 0 " + Utils.reDimension(20) + " " + Utils.reDimension(20) + " 0, flowx, fillx, filly", "[grow, center][]", "[center, top]"));
		middlePanel.setLayout(new MigLayout("insets 0 " + Utils.reDimension(15) + " 0 " + Utils.reDimension(15) + ", flowy, fillx, filly", "[fill, grow]", "[fill, grow]"));
		bottomPanel.setLayout(new MigLayout("insets " + Utils.reDimension(18) + " 0 0 0, flowy, fillx, filly", "[grow, center]", "[center, top]"));

		topPanel.setOpaque(false);
		middlePanel.setOpaque(false);
		middleScrollPanel.setOpaque(false);
		middleScrollPanel.getViewport().setOpaque(false);
		middleScrollPanel.setBorder(BorderFactory.createEmptyBorder());
		middleScrollPanel.getVerticalScrollBar().setUnitIncrement(15);
		bottomPanel.setOpaque(false);

		myToolWindowContent.add(topPanel);
		myToolWindowContent.add(middleScrollPanel);
		myToolWindowContent.add(bottomPanel);

		// refresh and analyze blueprint
		ABMEntity object;
		List<TreeNodeEntity> treeNodeList = null;
		try
		{
			String blueprint = Utils.readFileAsString(Network.refreshBlueprint(), Charset.forName("UTF-8"));
			String json = Network.requestJSONFromBlueprint(blueprint);
			object = Utils.parseJsonBlueprint(json);
			if(object.getError()==null) treeNodeList = analyzeBlueprint(object);
			treeNodeList = analyzeTreeNodeList(treeNodeList);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		// information icon
		mInformationIcon = new JLabel();
		mInformationIcon.setOpaque(false);
		mInformationIcon.setHorizontalAlignment(SwingConstants.CENTER);
		topPanel.add(mInformationIcon, "gap " + Utils.reDimension(27) + "px"); // config button gaps (10 + 10) + size (35) = 55 / 2 = 27


		// tree structure
		final JBackgroundPanel middleTreePanel = new JBackgroundPanel("drawable/img_background_panel.9.png", JBackgroundPanel.JBackgroundPanelType.NINE_PATCH);
		middleTreePanel.setLayout(new MigLayout("insets " + Utils.reDimension(12) + " " + Utils.reDimension(12) + " " + Utils.reDimension(18) + " " + Utils.reDimension(19) + ", flowy, fillx, filly", "[fill, grow]", "[fill, grow]"));
		middleTreePanel.setOpaque(false);

		final JTree tree = new Tree(initTreeStructure(treeNodeList));
		tree.setRootVisible(false);
		tree.setOpaque(false);
		tree.setCellRenderer(new ABMTreeCellRenderer());

		middleTreePanel.add(tree);
		middlePanel.add(middleTreePanel);

		// refresh button
		final ImageButton button = new ImageButton();
		button.setImage("drawable/img_button_refresh.png");
		button.setSize(Utils.reDimension(70), Utils.reDimension(70));

		button.addMouseListener(new MouseAdapter()
		{
			private boolean progress;


			public void mouseClicked(MouseEvent e)
			{
				if(progress) return;
				progress = true;

				button.setImage("drawable/animation_refresh.gif");
				button.setSize(Utils.reDimension(70), Utils.reDimension(70));

				Thread t = new Thread(new Runnable()
				{
					public void run()
					{
						ABMEntity object;
						List<TreeNodeEntity> treeNodeList = null;
						try
						{
							Preferences prefs = new Preferences();
							String blueprint = Utils.readFileAsString(Network.refreshBlueprint(), Charset.forName("UTF-8"));
							String json;
							if(blueprint.equals("")) json = prefs.getBlueprintTmpFileLocation();
							else json = Network.requestJSONFromBlueprint(blueprint);
							object = Utils.parseJsonBlueprint(json);
							if(object.getError()==null) treeNodeList = analyzeBlueprint(object);
							treeNodeList = analyzeTreeNodeList(treeNodeList);
						}
						catch(IOException e)
						{
							e.printStackTrace();
						}

						final JTree tree = new Tree(initTreeStructure(treeNodeList));
						tree.setRootVisible(false);
						tree.setOpaque(false);
						tree.setCellRenderer(new ABMTreeCellRenderer());
						tree.addMouseListener(new MouseAdapter()
						{
							public void mousePressed(MouseEvent e)
							{
								int row = tree.getRowForLocation(e.getX(), e.getY());
								TreePath path = tree.getPathForLocation(e.getX(), e.getY());
								if(row!=-1 && e.getClickCount()==2)
									onTreeNodeDoubleClick((TreeNodeEntity) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject());
							}
						});

						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								middleTreePanel.removeAll();
								middleTreePanel.add(tree);
								middleTreePanel.validate();
								middleTreePanel.repaint();

								button.setImage("drawable/img_button_refresh.png");
								button.setSize(Utils.reDimension(70), Utils.reDimension(70));

								progress = false;
							}
						});
					}
				});
				t.start();
			}


			public void mousePressed(MouseEvent e)
			{
				if(progress) return;
				button.setImage("drawable/img_button_refresh_pressed.png");
				button.setSize(Utils.reDimension(70), Utils.reDimension(70));
			}


			public void mouseReleased(MouseEvent e)
			{
				if(progress) return;
				button.setImage("drawable/img_button_refresh.png");
				button.setSize(Utils.reDimension(70), Utils.reDimension(70));
			}
		});
		bottomPanel.add(button);

		// config button
		final ImageButton buttonConfig = new ImageButton();
		buttonConfig.setImage("drawable/img_button_config.png");
		buttonConfig.setSize(Utils.reDimension(35), Utils.reDimension(35));

		buttonConfig.addMouseListener(new MouseAdapter()
		{
			private boolean progress;


			public void mouseClicked(MouseEvent e)
			{
				if(progress) return;
				buttonConfig.setImage("drawable/img_button_config_pressed.png");
				buttonConfig.setSize(Utils.reDimension(35), Utils.reDimension(35));
				progress = true;

				new ABMToolWindowConfiguration(mToolWindow);
			}


			public void mousePressed(MouseEvent e)
			{
				if(progress) return;
				buttonConfig.setImage("drawable/img_button_config_pressed.png");
				buttonConfig.setSize(Utils.reDimension(35), Utils.reDimension(35));
			}


			public void mouseReleased(MouseEvent e)
			{
				if(progress) return;
				buttonConfig.setImage("drawable/img_button_config.png");
				buttonConfig.setSize(Utils.reDimension(35), Utils.reDimension(35));
			}
		});
		topPanel.add(buttonConfig, "right, gap 0px " + Utils.reDimension(10) + "px " + Utils.reDimension(10) + "px 0px ");


		// Tree click listener
		tree.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				int row = tree.getRowForLocation(e.getX(), e.getY());
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());
				if(row!=-1 && e.getClickCount()==2)
					onTreeNodeDoubleClick((TreeNodeEntity) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject());
			}
		});

		// todo remove
		//		List<PsiPackage> packList = getPackages();
		//		for(PsiPackage pack : packList)
		//		{
		//			Log.d("Package: " + pack.getSubPackages()[0].getSubPackages()[0].getName());
		//		}
	}


	private void onTreeNodeDoubleClick(TreeNodeEntity entity)
	{
		if(entity.getTreeNodeType()==TreeNodeTypeEnum.CONFIGURATION) new ABMToolWindowConfiguration(mToolWindow);
		else if(entity.getTreeNodeType()==TreeNodeTypeEnum.CANNOT_RECOGNIZE) new ABMToolWindowCannotRecognize(mToolWindow, entity);
		else if(entity.getTreeNodeType()==TreeNodeTypeEnum.NOT_IMPLEMENTED_REQUEST) new ABMToolWindowCannotRecognize(mToolWindow, entity);
	}


	private List<TreeNodeEntity> analyzeBlueprint(ABMEntity object)
	{
		if(object==null || object.getError()!=null) return null;

		List<TreeNodeEntity> outputList = new ArrayList<TreeNodeEntity>();
		for(ResourceGroupsEntity resourceGroupsEntity : object.getAst().getResourceGroups())
		{
			for(ResourcesEntity resourcesEntity : resourceGroupsEntity.getResources())
			{
				for(ActionsEntity actionsEntity : resourcesEntity.getActions())
				{
					TreeNodeEntity entity = new TreeNodeEntity();
					entity.setUri(resourcesEntity.getUriTemplate());
					entity.setParameters(resourcesEntity.getParameters());
					entity.setName(actionsEntity.getName().replace(" ", ""));
					entity.setDescription(actionsEntity.getDescription());
					entity.setMethod(actionsEntity.getMethod());
					entity.setNodeType(NodeTypeEnum.REQUEST);
					outputList.add(entity);

					TreeNodeEntity entityResponse = new TreeNodeEntity(entity);
					entityResponse.setNodeType(NodeTypeEnum.RESPONSE);
					outputList.add(entityResponse);

					//					for(ExamplesEntity examplesEntity : actionsEntity.getExamples())
					//					{
					//						for(ResponsesEntity responsesEntity : examplesEntity.getResponses())
					//						{
					//							TreeNodeEntity entityParser = new TreeNodeEntity(entity);
					//							entityParser.setResponseCode(responsesEntity.getName());
					//							entityParser.setResponseHeaders(responsesEntity.getHeaders());
					//							entityParser.setResponseBody(responsesEntity.getBody());
					//							entityParser.setNodeType(NodeTypeEnum.PARSER);
					//							outputList.add(entityParser);
					//						}
					//					}
				}
			}
		}
		return outputList;
	}


	private List<TreeNodeEntity> analyzeTreeNodeList(List<TreeNodeEntity> list)
	{
		ConfigPreferences cp = new ConfigPreferences();

		// TODO check status of each entity
		for(TreeNodeEntity entity : list)
		{
			if(entity.getName().equals("")) entity = cp.tryToFillTreeNodeEntity(entity);
			if(entity.getName().equals("")) entity.setTreeNodeType(TreeNodeTypeEnum.CANNOT_RECOGNIZE);
			else if(entity.getNodeType()==NodeTypeEnum.REQUEST) entity.setTreeNodeType(TreeNodeTypeEnum.NOT_IMPLEMENTED_REQUEST);
			else if(entity.getNodeType()==NodeTypeEnum.RESPONSE) entity.setTreeNodeType(TreeNodeTypeEnum.NOT_IMPLEMENTED_ENTITY);
		}

		Iterator<TreeNodeEntity> iterator = list.iterator();
		while(iterator.hasNext())
		{
			TreeNodeEntity entity = iterator.next();
			if(entity.getTreeNodeType()==TreeNodeTypeEnum.CANNOT_RECOGNIZE && entity.getNodeType()!=NodeTypeEnum.REQUEST) iterator.remove();
		}

		return list;
	}


	private DefaultMutableTreeNode initTreeStructure(List<TreeNodeEntity> nodeList)
	{
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(new TreeNodeEntity(TreeNodeTypeEnum.ROOT, "Root object"));

		DefaultMutableTreeNode categoryConfiguration = new DefaultMutableTreeNode(new TreeNodeEntity(TreeNodeTypeEnum.CONFIGURATION_ROOT, "Configuration"));
		DefaultMutableTreeNode categoryCannotRecognize = new DefaultMutableTreeNode(new TreeNodeEntity(TreeNodeTypeEnum.CANNOT_RECOGNIZE_ROOT, "Cannot recognize"));
		DefaultMutableTreeNode categoryNotImplemented = new DefaultMutableTreeNode(new TreeNodeEntity(TreeNodeTypeEnum.NOT_IMPLEMENTED_ROOT, "Not implemented"));
		DefaultMutableTreeNode categoryNotImplementedRequest = new DefaultMutableTreeNode(new TreeNodeEntity(TreeNodeTypeEnum.NOT_IMPLEMENTED_REQUEST_ROOT, "Request"));
		DefaultMutableTreeNode categoryNotImplementedEntity = new DefaultMutableTreeNode(new TreeNodeEntity(TreeNodeTypeEnum.NOT_IMPLEMENTED_ENTITY_ROOT, "Response entity"));
		DefaultMutableTreeNode categoryModified = new DefaultMutableTreeNode(new TreeNodeEntity(TreeNodeTypeEnum.MODIFIED_ROOT, "Modified"));
		DefaultMutableTreeNode categoryRemoved = new DefaultMutableTreeNode(new TreeNodeEntity(TreeNodeTypeEnum.REMOVED_ROOT, "Removed"));
		DefaultMutableTreeNode item;

		Integer categoryConfigurationValue = 0;
		Integer categoryCannotRecognizeValue = 0;
		Integer categoryNotImplementedValue = 0;
		Integer categoryNotImplementedRequestValue = 0;
		Integer categoryNotImplementedEntityValue = 0;
		Integer categoryModifiedValue = 0;
		Integer categoryRemovedValue = 0;

		for(TreeNodeEntity entity : nodeList)
		{
			item = new DefaultMutableTreeNode(entity);
			switch(entity.getTreeNodeType())
			{
				case CONFIGURATION:
					categoryConfiguration.add(item);
					categoryConfigurationValue++;
				case CANNOT_RECOGNIZE:
					categoryCannotRecognize.add(item);
					categoryCannotRecognizeValue++;
					break;
				case NOT_IMPLEMENTED_REQUEST:
					categoryNotImplementedRequest.add(item);
					categoryNotImplementedRequestValue++;
					categoryNotImplementedValue++;
					break;
				case NOT_IMPLEMENTED_ENTITY:
					categoryNotImplementedEntity.add(item);
					categoryNotImplementedEntityValue++;
					categoryNotImplementedValue++;
					break;
				case MODIFIED:
					categoryModified.add(item);
					categoryModifiedValue++;
					break;
				case REMOVED:
					categoryRemoved.add(item);
					categoryRemovedValue++;
					break;
			}
		}

		((TreeNodeEntity) categoryCannotRecognize.getUserObject()).setValue(categoryCannotRecognizeValue);
		((TreeNodeEntity) categoryNotImplemented.getUserObject()).setValue(categoryNotImplementedValue);
		((TreeNodeEntity) categoryNotImplementedRequest.getUserObject()).setValue(categoryNotImplementedRequestValue);
		((TreeNodeEntity) categoryNotImplementedEntity.getUserObject()).setValue(categoryNotImplementedEntityValue);
		((TreeNodeEntity) categoryModified.getUserObject()).setValue(categoryModifiedValue);
		((TreeNodeEntity) categoryRemoved.getUserObject()).setValue(categoryRemovedValue);

		if(categoryCannotRecognizeValue!=0) root.add(categoryCannotRecognize);
		if(categoryNotImplementedValue!=0)
		{
			if(categoryNotImplementedRequestValue!=0) categoryNotImplemented.add(categoryNotImplementedRequest);
			if(categoryNotImplementedEntityValue!=0) categoryNotImplemented.add(categoryNotImplementedEntity);

			root.add(categoryNotImplemented);
		}
		if(categoryModifiedValue!=0) root.add(categoryModified);
		if(categoryRemovedValue!=0) root.add(categoryRemoved);


		try
		{
			final BufferedImage tmpImageCross = ImageIO.read(JBackgroundPanel.class.getClassLoader().getResourceAsStream("drawable/img_cross.png"));
			final BufferedImage tmpImageExclamation = ImageIO.read(JBackgroundPanel.class.getClassLoader().getResourceAsStream("drawable/img_exclamation_mark.png"));
			final BufferedImage tmpImageCheck = ImageIO.read(JBackgroundPanel.class.getClassLoader().getResourceAsStream("drawable/img_check.png"));
			if(categoryCannotRecognizeValue>0 || categoryConfigurationValue>0) SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					mInformationIcon.setIcon(new ImageIcon(tmpImageCross.getScaledInstance(75, 75, Image.SCALE_SMOOTH)));
				}
			});
			else if(categoryNotImplementedValue>0 || categoryModifiedValue>0) SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					mInformationIcon.setIcon(new ImageIcon(tmpImageExclamation.getScaledInstance(75, 75, Image.SCALE_SMOOTH)));
				}
			});
			else SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						mInformationIcon.setIcon(new ImageIcon(tmpImageCheck.getScaledInstance(75, 75, Image.SCALE_SMOOTH)));
					}
				});
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		return root;
	}
}
