import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
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
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

public class CBDocUtilGui extends JFrame {

	CBDocUtil cbUtil;
	File file;
	
	// Document panel controls
	
	JButton pnlDocsBtnDownload = new JButton("Download Dossier -> file, text");
	JButton pnlDocsBtnUpload = new JButton("Upload text -> CB");
	JButton pnlDocsBtnUploadDossierFile = new JButton("Upload Dossier file -> CB");
	JTextField pnlDocsTfStatus = new JTextField();
	JTextArea pnlDocsTADocument = new JTextArea();
	
	JLabel pnlDocsLbStatus = new JLabel(" Welcome", JLabel.LEFT);
	// Export Import tab
	
	JTextField pnlExpImpTfFilePath = new JTextField(40);
	JButton pnlExpImpBtnDownload = new JButton("Download");
	JButton pnlExpImpBtnUpload = new JButton("Upload");
	JTextArea pnlExpImpTALogarea = new JTextArea();	
	JLabel pnlExpImpLbStatus = new JLabel();
	JButton pnlExpImpBTNChoosePath = new JButton("File Path");
	
	
	// controls for second tab - QueryViews
	
	//JPanel queryViewPanel   = new JPanel();
	JButton pnlViewsBtnQueryExecute = new JButton("Process View");
	JToggleButton pnlViewsBtnDocOrView = new JToggleButton("as Doc");
	
	JTextField pnlViewsTfDesgnDoc = new JTextField("dev_ypto");
	String[] cbItems = new String[]{"listWork"};
	MyComboBoxModel mdlTFViewName = new MyComboBoxModel(cbItems);	
	JComboBox<String> pnlViewsCbViewName = new JComboBox<String>(mdlTFViewName);

	private DefaultListModel<String> mdlJLViewResult = new DefaultListModel<>();
	JList<String> pnlViewsJLResult = new JList<>(mdlJLViewResult);
	
	JTextArea pnlViewsTACBDocument = new JTextArea(5, 30);

    // Controls for Decode tab
	
	private enum Base64ButtonOp {ENCODE, DECODE};
	//JTextField pnlDecodeInput  = new JTextField();
	JButton pnlDecodeBtnDecode = new JButton("Decode");
	JButton pnlDecodeBtnEncode = new JButton("Encode");
	JButton pnlDecodeBtnClear = new JButton("Clear");
	JTextArea pnlDecodeResultTextArea = new JTextArea();
	JLabel pnlDecodeLbStatus = new JLabel(" Welcome", JLabel.LEFT);
	JTextField pnlDecodeTxtSearch = new JTextField("Search             ");// include spaces to show field wider
	
    Logger logger = java.util.logging.Logger.getLogger(this.getClass().getName());	
    
    final int STATE_DOC = 0;
    
    enum QueryResultType {
        asDoc, asView 
    }
    QueryResultType pnlViewsBtnDocOrViewState;
    
    
    
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
		super("Couchbase Utility "+cbUtil.getServerUrl()+"-"+cbUtil.getBucketName());
		this.cbUtil = cbUtil;
		
		this.addWindowListener(new WindowConfirmedCloseAdapter(this));
						
		
		Container content = getContentPane();
			        
        
 //   Create second tab panel  
        
		JTabbedPane tabPnl = new JTabbedPane();
		tabPnl.addTab("LoadStoreDocs",  createStoreLoadPanel());
		tabPnl.addTab("QueryViews",  createQueryViewPanel());
		tabPnl.addTab("Upload/Download",  createExportImportPanel());
		tabPnl.addTab("DecodeBase64",  createBase64DecodePanel());

		tabPnl.setSelectedIndex(0);
        content.add(tabPnl);
		
		

		this.pack();
		this.setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		pnlViewsCbViewName.setEditable(true);
		
		
		// add event listener whenever design doc name is changed
		pnlViewsTfDesgnDoc.addFocusListener( new MyFocusListener(cbUtil));
		
	}
	
	protected JPanel createQueryViewPanel() {
		JPanel queryViewPanel = new JPanel();
		queryViewPanel.setLayout(new BorderLayout());

		JPanel panelNorth = new JPanel();
		panelNorth.setLayout(new GridLayout(1, 5, 2, 2));
		//panelNorth.add(new JLabel("View Name"));	
		
		panelNorth.add(pnlViewsTfDesgnDoc);
		panelNorth.add(pnlViewsCbViewName);
		panelNorth.add(pnlViewsBtnQueryExecute);
		panelNorth.add(pnlViewsBtnDocOrView);
		
		//panelNorth.add(btnExpImp);	
		
		ItemListener pnlViewsBtnDocOrViewIL = new ItemListener() {
		        public void itemStateChanged(ItemEvent itemEvent) {
		          int state = itemEvent.getStateChange();
		          if (state == ItemEvent.SELECTED) {
		            pnlViewsBtnDocOrView.setText("asDoc");
		            pnlViewsBtnDocOrViewState = QueryResultType.asDoc;
		            System.out.println("JToggleButton Selected =  "+pnlViewsBtnDocOrViewState);
		          } else {
		            pnlViewsBtnDocOrView.setText("asView");
		            pnlViewsBtnDocOrViewState = QueryResultType.asView;
		            System.out.println("JToggleButton Deselected =  "+pnlViewsBtnDocOrViewState);

		          }
		        }
		};
		pnlViewsBtnDocOrView.addItemListener(pnlViewsBtnDocOrViewIL);
		//pnlViewsBtnDocOrView.
		
		
		
		pnlViewsBtnQueryExecute.addActionListener(new ALPnlViewsBtnQueryExecute(this));	
			
		
		panelNorth.add(new JLabel("                              "));
		queryViewPanel.add(panelNorth,BorderLayout.NORTH);
		
		pnlViewsJLResult.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		pnlViewsJLResult.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		pnlViewsJLResult.setVisibleRowCount(-1);
		ListSelectionModel lsm = pnlViewsJLResult.getSelectionModel();
		lsm.addListSelectionListener( new SLListBoxViewResult(this));
		JScrollPane listViewResultScroller = new JScrollPane(pnlViewsJLResult);
		listViewResultScroller.setPreferredSize(new Dimension(250, 80));	
		

		pnlViewsTACBDocument.setEditable(true);
		pnlViewsTACBDocument.setText("xxxxxxxxxxxxxx xxxxxxxxxxxxxxxx xxxxxxxxxxxxx xxxxxxxxx");
        JScrollPane scrollerDocumentArea = new JScrollPane(pnlViewsTACBDocument);		
		
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
        JScrollPane spTfJsonTextArea = new JScrollPane(pnlDocsTADocument);
        spTfJsonTextArea.setPreferredSize(new Dimension(175, 600));
        spTfJsonTextArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        main.add(spTfJsonTextArea, BorderLayout.CENTER);
        
        main.add(pnlDocsLbStatus, BorderLayout.SOUTH);
        JPanel form = new JPanel();
		form.setLayout(new FlowLayout());
		// This is an empty content area in the frame
		pnlDocsTfStatus.setPreferredSize(new Dimension(175, 30));
		form.add(pnlDocsTfStatus);

		form.add(pnlDocsBtnDownload);
		ALPnlDocsBtnDownload alDownload = new ALPnlDocsBtnDownload(this);
		pnlDocsBtnDownload.addActionListener(alDownload);	
		
		form.add(pnlDocsBtnUpload);
		ALPnlDocsBtnUpload alUpload = new ALPnlDocsBtnUpload(this);
		pnlDocsBtnUpload.addActionListener(alUpload);
		
		
		form.add(pnlDocsBtnUploadDossierFile);
		ALPnlDocsUploadDossierFile alUploadFile = new ALPnlDocsUploadDossierFile(this);
		pnlDocsBtnUploadDossierFile.addActionListener(alUploadFile);
		
		main.add(form, BorderLayout.NORTH);

		return main;
	}	
	

///////////////////////////////
	
	protected JPanel createBase64DecodePanel() {
		JPanel main = new JPanel();
		main.setLayout(new BorderLayout());
        main.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        main.setBackground(Color.LIGHT_GRAY);  
        pnlDecodeResultTextArea.setLineWrap(true);
        pnlDecodeResultTextArea.setWrapStyleWord(false);// use character boundary
        
        /*
        pnlDecodeResultTextArea.addCaretListener(new CaretListener() {            
            @Override
            public void caretUpdate(CaretEvent e) {
                if(e.getMark() == e.getDot()){
                    Highlighter hl = pnlDecodeResultTextArea.getHighlighter();
                    hl.removeAllHighlights();

                    String pattern = "<aa>";
                    String text = pnlDecodeResultTextArea.getText();        
                    int index = text.indexOf(pattern);
                    while(index >= 0){
                        try {                
                            Object o = hl.addHighlight(index, index + pattern.length(), DefaultHighlighter.DefaultPainter);
                            index = text.indexOf(pattern, index + pattern.length());
                        } catch (BadLocationException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

        });        
       */ 
        
        
        
        JScrollPane pnlDecodeSpDecodeResult = new JScrollPane(pnlDecodeResultTextArea);
        pnlDecodeSpDecodeResult.setPreferredSize(new Dimension(175, 600));
        pnlDecodeSpDecodeResult.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        main.add(pnlDecodeSpDecodeResult, BorderLayout.CENTER);
        
        main.add(pnlDecodeLbStatus, BorderLayout.SOUTH);
        JPanel form = new JPanel();
		form.setLayout(new FlowLayout());
		// This is an empty content area in the frame
		//pnlDecodeInput.setPreferredSize(new Dimension(175, 30));
		//form.add(pnlDecodeInput);

		form.add(pnlDecodeBtnDecode);
		form.add(pnlDecodeBtnEncode);
		form.add(pnlDecodeBtnClear);
		form.add(new JLabel("  Search:   "));// provide space
		form.add(pnlDecodeTxtSearch);
		
		pnlDecodeTxtSearch.addActionListener(new java.awt.event.ActionListener() {
		    public void actionPerformed(java.awt.event.ActionEvent e) {

                Highlighter hl = pnlDecodeResultTextArea.getHighlighter();
                hl.removeAllHighlights();

		        String pattern = pnlDecodeTxtSearch.getText();
                String text = pnlDecodeResultTextArea.getText();        
                int index = text.indexOf(pattern);
                while(index >= 0){
                    try {                
                        Object o = hl.addHighlight(index, index + pattern.length(), DefaultHighlighter.DefaultPainter);
                        index = text.indexOf(pattern, index + pattern.length());
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                }
		        
		    }
		});		

		pnlDecodeBtnClear.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				// Anonymous classes are just like local classes, besides they don’t have a name. In Java anonymous classes enables the developer to declare and instantiate a class at the same time.
				// Like other inner classes, an anonymous class has access to the members of its enclosing class
				pnlDecodeResultTextArea.setText("");				
			}});

		pnlDecodeBtnEncode.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				// Anonymous classes are just like local classes, besides they don’t have a name. In Java anonymous classes enables the developer to declare and instantiate a class at the same time.
				// Like other inner classes, an anonymous class has access to the members of its enclosing class
			    try{
					String text = pnlDecodeResultTextArea.getText();
					String b = Base64Utilities.encodeB64(text.getBytes());
					pnlDecodeResultTextArea.setText(b);
				}catch(Exception e1){
		            logger.log(java.util.logging.Level.INFO, 
		                     "Excepton  {0} {1} ", new Object[]{e1.getMessage(), getPrintStackTrace(e1)});
				}
			}});

		pnlDecodeBtnDecode.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				// Anonymous classes are just like local classes, besides they don’t have a name. In Java anonymous classes enables the developer to declare and instantiate a class at the same time.
				// Like other inner classes, an anonymous class has access to the members of its enclosing class
			    try{
					String text = pnlDecodeResultTextArea.getText();
					byte[] b = Base64Utilities.decodeB64(text);
					pnlDecodeResultTextArea.setText(new String(b));
				}catch(Exception e1){
		            logger.log(java.util.logging.Level.INFO, 
		               "Excepton  {0} {1} ", new Object[]{e1.getMessage(), getPrintStackTrace(e1)});

		                //me.pnlDocsTADocument.setText("!!!");
				}
			}});
		
		
		
		
		
		
		
		
		main.add(form, BorderLayout.NORTH);

		return main;
	}	
	
/////////////////////////////	
	
	protected JPanel 	createExportImportPanel() {	
		

		JPanel main = new JPanel();
		main.setLayout(new BorderLayout());
        main.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        main.setBackground(Color.LIGHT_GRAY);  
        pnlExpImpTALogarea.setLineWrap(true);
        pnlExpImpTALogarea.setEditable(false);
        JScrollPane sptaExpImpPnlLog = new JScrollPane(pnlExpImpTALogarea);
        sptaExpImpPnlLog.setPreferredSize(new Dimension(175, 600));
        sptaExpImpPnlLog.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        main.add(sptaExpImpPnlLog, BorderLayout.CENTER);
		pnlExpImpLbStatus.setPreferredSize(new Dimension(175, 30));
		main.add(pnlExpImpLbStatus, BorderLayout.SOUTH);

        
        JPanel form = new JPanel();
		form.setLayout(new FlowLayout());

		
		ALPnlExpImpChoosePath alChoosePath = new ALPnlExpImpChoosePath(this);
		pnlExpImpBTNChoosePath.addActionListener(alChoosePath);
		form.add(pnlExpImpBTNChoosePath);
		
		form.add(pnlExpImpTfFilePath);
		
		
		ALPnlExpImpBtnUpload alBtnUpload = new ALPnlExpImpBtnUpload(this);
		pnlExpImpBtnUpload.addActionListener(alBtnUpload);
		form.add(pnlExpImpBtnUpload);
		
		ALPnlExpImpBtnDownload albtnDownload = new ALPnlExpImpBtnDownload(this);
		pnlExpImpBtnDownload.addActionListener(albtnDownload);
		form.add(pnlExpImpBtnDownload);
		
		main.add(form, BorderLayout.NORTH);

		return main;
	}		
	
	
	
	
	
	
	
	private class ALPnlDocsUploadDossierFile implements ActionListener {

		CBDocUtilGui me;
        private ALPnlDocsUploadDossierFile(CBDocUtilGui me){
        	this.me = me;        	
        }

		@Override
		public void actionPerformed(ActionEvent e) {
	  		final JFileChooser fc = new JFileChooser();
			int returnVal = fc.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
		            me.file = fc.getSelectedFile();
		            me.cbUtil.upload(file);
		            me.pnlDocsLbStatus.setText("Uploaded "+file.getAbsolutePath());
			}        
			
		}
		
	}
	
	
	private class  ALPnlViewsBtnQueryExecute implements ActionListener {
		CBDocUtilGui me;
	    private ALPnlViewsBtnQueryExecute(CBDocUtilGui me){
	        	this.me = me;        	
	    }

		public void actionPerformed(ActionEvent e) {
			//e.
			String viewNameLoc = (String)me.pnlViewsCbViewName.getSelectedItem();
	        logger.log(java.util.logging.Level.INFO,"CB VIEW Combo Selected item = "+viewNameLoc);

			String desgnDocLoc = me.pnlViewsTfDesgnDoc.getText();
			try{
			cbUtil.paginatedQueryCreate(desgnDocLoc, viewNameLoc,10000,mdlJLViewResult);
			while(!cbUtil.paginatedQueryResult2( mdlJLViewResult)){
				logger.log(java.util.logging.Level.SEVERE,"Next Page!");
			}
	    	}catch(Exception e1){
	    		pnlExpImpLbStatus.setText("Exception caught: "+e1.getMessage());
	    		me.pnlExpImpTALogarea.append(getPrintStackTrace(e1));
	    		logger.log(java.util.logging.Level.SEVERE,"Exception caught: "+getPrintStackTrace(e1));
	    	}
		

			
		}
		
	}
	


	private class  ALPnlExpImpBtnDownload implements ActionListener {
		CBDocUtilGui me;
	    private ALPnlExpImpBtnDownload(CBDocUtilGui me){
	        	this.me = me;  
	        	pnlExpImpLbStatus.setText(""); // reset status
	    }

	    public void actionPerformed(ActionEvent e) {
	    	//we will be exporting all documents found in mdlJLViewResult
	    	String path = pnlExpImpTfFilePath.getText();
	    	if(path==null || path.length()==0){
	    		pnlExpImpLbStatus.setText("Path is empty!");
	    		return;
	    	}

	    	Enumeration s = mdlJLViewResult.elements();
	    	me.pnlExpImpTALogarea.append("\nDownloading keys into FS location '"+path+"':\n\n");
	    	while(s.hasMoreElements()){

	    		String item = (String) s.nextElement();
	    		me.pnlExpImpTALogarea.append(item+"; ");
	    	}

	    	try{
	    		me.cbUtil.downloadDir(mdlJLViewResult.elements(),path);
	    		me.pnlExpImpTALogarea.append("\nDownload completed!"); 

	    	}catch(Exception e1){
	    		pnlExpImpLbStatus.setText("Exception caught: "+e1.getMessage());
	    		me.pnlExpImpTALogarea.append(getPrintStackTrace(e1));
	    		logger.log(java.util.logging.Level.SEVERE,"Exception caught: "+getPrintStackTrace(e1));
	    	}

	    }
	}
	
	

	private class  ALPnlExpImpBtnUpload implements ActionListener {
		CBDocUtilGui me;
	    private ALPnlExpImpBtnUpload(CBDocUtilGui me){
	        	this.me = me;  
	        	pnlExpImpLbStatus.setText(""); // reset status
	    }

	    public void actionPerformed(ActionEvent e) {
	    	//we will be exporting all documents found in mdlJLViewResult
	    	String path = pnlExpImpTfFilePath.getText();
	    	if(path==null || path.length()==0){
	    		pnlExpImpLbStatus.setText("Path is empty!");
	    		return;
	    	}

	    	try{
	    		me.cbUtil.uploadDir(path);
	    		me.pnlExpImpTALogarea.append("\nUpload of files into CB completed!"); 

	    	}catch(Exception e1){
	    		pnlExpImpLbStatus.setText("Exception caught: "+e1.getMessage());
	    		me.pnlExpImpTALogarea.append(getPrintStackTrace(e1));
	    		logger.log(java.util.logging.Level.SEVERE,"Exception caught: "+getPrintStackTrace(e1));
	    	}

	    }
	}
	
	
	
	
	
	
	
	private class SLListBoxViewResult implements ListSelectionListener {

		CBDocUtilGui me;
        private SLListBoxViewResult(CBDocUtilGui me){
        	this.me = me;        	
        }



		@Override
		public void valueChanged(ListSelectionEvent e) {
			//e.
			ListSelectionModel lsm = (ListSelectionModel)e.getSource();
			boolean isAdjusting = e.getValueIsAdjusting();
			//lsm.
			if (lsm.isSelectionEmpty()) {
				logger.log(java.util.logging.Level.INFO,"None ");
	        } else if(!isAdjusting) {			
			  int minIndex = lsm.getMinSelectionIndex();
			  int firstIndex = e.getFirstIndex();
	          int lastIndex = e.getLastIndex();
	          Object src = e.getSource();
	   

	          String key = mdlJLViewResult.getElementAt(minIndex);
	          //viewResultModel.
	          logger.log(java.util.logging.Level.INFO,"Selected row = ["+minIndex+":"+firstIndex+":"+lastIndex+":"+src.getClass().getName()+":"+src+"] "+" value:isAdjusting "+key+":"+isAdjusting);
			  //String text = cbUtil.getDocument(key); // retrieve view document
	          
			  String viewNameLoc = (String)me.pnlViewsCbViewName.getSelectedItem();

			  String desgnDocLoc = me.pnlViewsTfDesgnDoc.getText();
			  
              try{
    		      logger.log(java.util.logging.Level.INFO,"About to execute query Doc:view:key = "+desgnDocLoc+":"+viewNameLoc+":"+key);
    		      if(pnlViewsBtnDocOrViewState == QueryResultType.asView){
            	    String text = me.cbUtil.QueryView(desgnDocLoc,viewNameLoc,key);
			        pnlViewsTACBDocument.setText(text);
    		      }else{
    		    	pnlViewsTACBDocument.setText(me.cbUtil.getDocument(key));  
    		      }
              }catch(Exception e2){
            	 e2.printStackTrace(); 
              }
			  
		    }        
			
		}
	
	}
	
	private class ALPnlDocsBtnUpload implements ActionListener {

		CBDocUtilGui me;
        private ALPnlDocsBtnUpload(CBDocUtilGui me){
        	this.me = me;        	
        }

		@Override
		public void actionPerformed(ActionEvent e) {
            String key = me.pnlDocsTfStatus.getText();
		    me.cbUtil.upload(key,me.pnlDocsTADocument.getText());
		    me.pnlDocsLbStatus.setText("Uploaded! "+key);
			}        
			
		}
		

	

	
	private class ALPnlDocsBtnDownload implements ActionListener {
		CBDocUtilGui me;
        private ALPnlDocsBtnDownload(CBDocUtilGui me){
        	this.me = me;        	
        }
		@Override
		public void actionPerformed(ActionEvent e) {
	    try{
			String dossier = me.pnlDocsTfStatus.getText();
			logger.log(java.util.logging.Level.INFO,"Downloading dossier "+dossier);
			me.file = me.cbUtil.download(dossier);
			me.pnlDocsLbStatus.setText("Downloaded "+me.file.getAbsolutePath());
			Path in = Paths.get(me.file.getAbsolutePath());
			byte[] doc=null;
			doc=Files.readAllBytes(in); 
			me.pnlDocsTADocument.setText(new String(doc));
		}catch(Exception e1){
                logger.log(java.util.logging.Level.INFO, 
                     "Excepton  {0} {1} ", new Object[]{e1.getMessage(), getPrintStackTrace(e1)});

                me.pnlDocsTADocument.setText("!!!");
		}
			
		}
		
	}
	
/*
	private class ALPnlDecodeBtnEncode implements ActionListener {
		CBDocUtilGui me;
        private ALPnlDecodeBtnEncode(CBDocUtilGui me){
        	this.me = me;        	
        }
		@Override
		public void actionPerformed(ActionEvent e) {
	    try{
			String text = me.pnlDecodeResultTextArea.getText();
			String b = Base64Utilities.encodeB64(text.getBytes());
			me.pnlDecodeResultTextArea.setText(b);
		}catch(Exception e1){
                logger.log(java.util.logging.Level.INFO, 
                     "Excepton  {0} {1} ", new Object[]{e1.getMessage(), getPrintStackTrace(e1)});

                //me.pnlDocsTADocument.setText("!!!");
		}
			
		}
		
	}
	
	private class ALPnlDecodeBtnDecode implements ActionListener {
		CBDocUtilGui me;
        private ALPnlDecodeBtnDecode(CBDocUtilGui me){
        	this.me = me;        	
        }
		@Override
		public void actionPerformed(ActionEvent e) {
	    try{
			String text = me.pnlDecodeResultTextArea.getText();
			byte[] b = Base64Utilities.decodeB64(text);
			me.pnlDecodeResultTextArea.setText(new String(b));
		}catch(Exception e1){
                logger.log(java.util.logging.Level.INFO, 
                     "Excepton  {0} {1} ", new Object[]{e1.getMessage(), getPrintStackTrace(e1)});

                //me.pnlDocsTADocument.setText("!!!");
		}
			
		}
		
	}
*/	
	
	
	private class WindowConfirmedCloseAdapter extends WindowAdapter {
		CBDocUtilGui me;
		private WindowConfirmedCloseAdapter(CBDocUtilGui me){
			this.me=me;
		}
	    public void windowClosing(WindowEvent e) {
	    	logger.log(java.util.logging.Level.INFO,"Stopping CBClient!");
           me.cbUtil.stop();
	      
	    }
	}
	
	
    private 	class MyComboBoxModel extends DefaultComboBoxModel<String> {
		    public MyComboBoxModel(String[] items) {
		        super(items);
		    }
		 
		    @Override
		    public String getSelectedItem() {
		        String selectedJob = (String) super.getSelectedItem();
		 
		        // do something with this job before returning...
		 
		        return selectedJob;
		    }
	}
	

  private class MyFocusListener implements  FocusListener{
	      CBDocUtil cbUtil;
	      public MyFocusListener(CBDocUtil cbUtil){
	    	  this.cbUtil = cbUtil;
	      }
		  public void focusGained( FocusEvent e ){
		     System.out.println("tfDesgnDoc focus gained");
		
		  }

		  public void focusLost( FocusEvent e ){
		     System.out.println("tfDesgnDoc FocusLost");
		     String desgnDoc = pnlViewsTfDesgnDoc.getText();
		     String[] s = cbUtil.listDesgnDocViews(desgnDoc);
		     System.out.println("looking up views "+desgnDoc+" .. "+s);
		     //tfViewNameModel.
		     MyComboBoxModel m = new MyComboBoxModel(s);
		     pnlViewsCbViewName.setModel(m);		
		     
		     
		  }   
  }
    
    
  
	    
	    
		private class ALPnlExpImpChoosePath implements ActionListener {
			
			CBDocUtilGui me;
	        private ALPnlExpImpChoosePath(CBDocUtilGui me){
	        	this.me = me; 
	        	pnlExpImpLbStatus.setText(""); // reset status
	        }

			@Override
			public void actionPerformed(ActionEvent e) {
		  		final JFileChooser jfc = new JFileChooser("C:\\");  
		  		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);  
		  		int returnVal = jfc.showOpenDialog(null);  	  		
		  		
		  		
				if (returnVal == JFileChooser.APPROVE_OPTION) {
			        String dir  = jfc.getSelectedFile().getPath();
			        pnlExpImpTfFilePath.setText(dir);
				}        
				
			}
			
		}
	    
	    
	    
	    
	    

  
   
	
	String getPrintStackTrace(Exception ex){	
		StringWriter w = new StringWriter();
		ex.printStackTrace(new PrintWriter(w));
		return w.toString();		
	}
	
}

