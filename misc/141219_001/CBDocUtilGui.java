import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class CBDocUtilGui extends JFrame {

	CBDocUtil cbUtil;
	File file;
	JButton btnDownload = new JButton("Download Dossier -> file, text");
	JButton btnUpload = new JButton("Upload text -> CB");
	JButton btnUploadDossierFile = new JButton("Upload Dossier file -> CB");
	JTextField entry1 = new JTextField();
	JTextArea jsonTextArea = new JTextArea();
	JPanel queryViewPanel   = new JPanel();
	JButton queryExecute = new JButton("Process View");
	JTextField tfViewName = new JTextField("View Name");
	JTextField tfDesgnDoc = new JTextField("Design Doc");
	JTextArea cbDocumentArea = new JTextArea(5, 30);
	JLabel status = new JLabel(" Welcome", JLabel.LEFT);
	private DefaultListModel<String> viewResultModel;
	JList<String> listViewResult = new JList<>(viewResultModel = new DefaultListModel<>());
	
	public static void main(String args[]) {
		// 0="http://10.251.12.48:8091/pools", 1="LFP_Dossiers", 2=""
		System.out.println("args size "+args.length);
		if(args.length<2){System.out.println("Usage params: url bucket password \n http://10.251.12.48:8091/pools LFP_Dossiers ");System.exit(-1);
		}
		String url = args[0];
		String bucket = args[1];
		String bucketPw = (args.length>=3)?args[2]:"";
		CBDocUtil util = new CBDocUtil(url,bucket,bucketPw); 
		CBDocUtilGui frame = new CBDocUtilGui(util);
	}
	
	
	public CBDocUtilGui(CBDocUtil cbUtil){
		// Add a window listner for close button
		super("Couchbase Lisa Dossiers");
		this.cbUtil = cbUtil;
		
		this.addWindowListener(new WindowConfirmedCloseAdapter(this));
						
		
		Container content = getContentPane();
			        
        
 //   Create second tab panel  
        
 /*    
  
   [view name] [process]
   
   [grid with keys]
   
   [textarea with document]      
        
  */      
   
        
        
        
		JTabbedPane tabPnl = new JTabbedPane();
		tabPnl.addTab("LoadStoreDocs",  createStoreLoadPanel());
		tabPnl.addTab("QueryViews",  createQueryViewPanel());

		tabPnl.setSelectedIndex(0);
        content.add(tabPnl);
		
		

		//jlbempty.setPreferredSize(new Dimension(175, 100));
		//this.getContentPane().add(jlbempty, BorderLayout.CENTER);
		this.pack();
		this.setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		
		
	}
	
	protected JPanel createQueryViewPanel() {
		JPanel queryViewPanel = new JPanel();
		queryViewPanel.setLayout(new BorderLayout());

		JPanel panelNorth = new JPanel();
		panelNorth.setLayout(new GridLayout(1, 5, 2, 2));
		//panelNorth.add(new JLabel("View Name"));	
		
		panelNorth.add(tfDesgnDoc);
		panelNorth.add(tfViewName);
		panelNorth.add(queryExecute);	
		queryExecute.addActionListener(new ALQueryExecute(this));	
		
		
		
		
		panelNorth.add(new JLabel("                              "));
		queryViewPanel.add(panelNorth,BorderLayout.NORTH);
		
		

		
		
		
		
		
		
		listViewResult.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		listViewResult.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		listViewResult.setVisibleRowCount(-1);
		ListSelectionModel lsm = listViewResult.getSelectionModel();
		lsm.addListSelectionListener( new VCListBoxViewResult(this));
		JScrollPane listViewResultScroller = new JScrollPane(listViewResult);
		listViewResultScroller.setPreferredSize(new Dimension(250, 80));	
		

		cbDocumentArea.setEditable(true);
		cbDocumentArea.setText("xxxxxxxxxxxxxx xxxxxxxxxxxxxxxx xxxxxxxxxxxxx xxxxxxxxx");
        JScrollPane scrollerDocumentArea = new JScrollPane(cbDocumentArea);		
		
        JSplitPane splitPane = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT,
        listViewResultScroller,
        scrollerDocumentArea);
         
		queryViewPanel.add(splitPane,BorderLayout.CENTER);
		return queryViewPanel;
	}	
	
	protected JPanel createStoreLoadPanel() {
		JPanel main = new JPanel();
		main.setLayout(new BorderLayout());
        main.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        main.setBackground(Color.LIGHT_GRAY);                
        JScrollPane jsonTextAreaSp = new JScrollPane(jsonTextArea);
        jsonTextAreaSp.setPreferredSize(new Dimension(175, 600));
        jsonTextAreaSp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        main.add(jsonTextAreaSp, BorderLayout.CENTER);
        
        main.add(status, BorderLayout.SOUTH);
        JPanel form = new JPanel();
		form.setLayout(new FlowLayout());
		// This is an empty content area in the frame
		entry1.setPreferredSize(new Dimension(175, 30));
		form.add(entry1);

		form.add(btnDownload);
		ALDownload alDownload = new ALDownload(this);
		btnDownload.addActionListener(alDownload);	
		
		form.add(btnUpload);
		ALUpload alUpload = new ALUpload(this);
		btnUpload.addActionListener(alUpload);
		
		
		form.add(btnUploadDossierFile);
		ALUploadDossierFile alUploadFile = new ALUploadDossierFile(this);
		btnUploadDossierFile.addActionListener(alUploadFile);
		
		main.add(form, BorderLayout.NORTH);

		return main;
	}	
	
	
	
	private class ALUploadDossierFile implements ActionListener {

		CBDocUtilGui me;
        private ALUploadDossierFile(CBDocUtilGui me){
        	this.me = me;        	
        }

		@Override
		public void actionPerformed(ActionEvent e) {
	  		final JFileChooser fc = new JFileChooser();
			int returnVal = fc.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
		            me.file = fc.getSelectedFile();
		            me.cbUtil.upload(file);
		            me.status.setText("Uploaded "+file.getAbsolutePath());
			}        
			
		}
		
	}
	
	
	private class  ALQueryExecute implements ActionListener {
		CBDocUtilGui me;
	    private ALQueryExecute(CBDocUtilGui me){
	        	this.me = me;        	
	    }

		public void actionPerformed(ActionEvent e) {
			String viewNameLoc = me.tfViewName.getText();
			String desgnDocLoc = me.tfDesgnDoc.getText();
			cbUtil.queryView(desgnDocLoc, viewNameLoc, viewResultModel);
			// retrieve view result.
/*
 * 
		panelNorth.add(desgnDoc);
		panelNorth.add(queryExecute);	
		queryExecute.addActionListener(new ALQueryExecute(this));	

	
			
		
			
			
			viewResultModel.addElement("AAAAAAAAAA");
			viewResultModel.addElement("BBBBBBBBBB");
			viewResultModel.addElement("CCCCCCCCCC");
			viewResultModel.addElement("AAAAAAAAAA");
			viewResultModel.addElement("BBBBBBBBBB");
			viewResultModel.addElement("CCCCCCCCCC");
			viewResultModel.addElement("AAAAAAAAAA");
			viewResultModel.addElement("BBBBBBBBBB");
			viewResultModel.addElement("CCCCCCCCCC");
			viewResultModel.addElement("AAAAAAAAAA");
			viewResultModel.addElement("BBBBBBBBBB");
			viewResultModel.addElement("CCCCCCCCCC");
			*/			

			
		}
		
	}
	

	
	private class VCListBoxViewResult implements ListSelectionListener {

		CBDocUtilGui me;
        private VCListBoxViewResult(CBDocUtilGui me){
        	this.me = me;        	
        }



		@Override
		public void valueChanged(ListSelectionEvent e) {
			//e.
			ListSelectionModel lsm = (ListSelectionModel)e.getSource();
			boolean isAdjusting = e.getValueIsAdjusting();
			//lsm.
			if (lsm.isSelectionEmpty()) {
				System.out.println("None ");
	        } else if(!isAdjusting) {			
			  int minIndex = lsm.getMinSelectionIndex();
			  int firstIndex = e.getFirstIndex();
	          int lastIndex = e.getLastIndex();

	          String key = viewResultModel.getElementAt(firstIndex);
			  System.out.println("Selected row = "+minIndex+" value "+key+" isAdjusting  "+isAdjusting);
			  String text = cbUtil.getDocument(key);
			  cbDocumentArea.setText(text);
		    }        
			
		}
	
	}
	
	private class ALUpload implements ActionListener {

		CBDocUtilGui me;
        private ALUpload(CBDocUtilGui me){
        	this.me = me;        	
        }

		@Override
		public void actionPerformed(ActionEvent e) {
            String key = me.entry1.getText();
		    me.cbUtil.upload(key,me.jsonTextArea.getText());
		    me.status.setText("Uploaded! "+key);
			}        
			
		}
		

	

	
	private class ALDownload implements ActionListener {
		CBDocUtilGui me;
        private ALDownload(CBDocUtilGui me){
        	this.me = me;        	
        }
		@Override
		public void actionPerformed(ActionEvent e) {
			String dossier = me.entry1.getText();
			System.out.println("Downloading dossier "+dossier);
			me.file = me.cbUtil.download(dossier);
			me.status.setText("Downloaded "+me.file.getAbsolutePath());
			Path in = Paths.get(me.file.getAbsolutePath());
			byte[] doc=null;
			try{
			doc=Files.readAllBytes(in); 
			}catch(Exception e1){
				
			}
			me.jsonTextArea.setText(new String(doc));
		}
		
	}
	
	
	private class WindowConfirmedCloseAdapter extends WindowAdapter {
		CBDocUtilGui me;
		private WindowConfirmedCloseAdapter(CBDocUtilGui me){
			this.me=me;
		}
	    public void windowClosing(WindowEvent e) {
	    	System.out.println("Stopping CBClient!");
           me.cbUtil.stop();
	      
	    }
	}	
	
}

