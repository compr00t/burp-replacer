package burp;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class BurpExtender implements IBurpExtender, IHttpListener, ITab
{
    
    private IExtensionHelpers helpers;
    private static PrintWriter stdout;
    private JSplitPane splitPane;
    private JSplitPane splitPaneTop;
    private JSplitPane splitPaneBottom;
    private JSplitPane splitPaneFile;
    private JSplitPane splitPaneURL;
    private JTextField file;
    private JTextField targetURL;
    private JLabel labelFile;
    private JLabel labelURL;
    private JTextArea txtHelp;
    
    private final String SYSTEM_SETTING = "my.replace.file";
    private final String TARGET_URL = "my.replace.url";
    
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
    {
    	helpers = callbacks.getHelpers();
    	stdout = new PrintWriter(callbacks.getStdout(), true);
        
        callbacks.setExtensionName("Replacer");
        
        callbacks.registerHttpListener(this);
        
        SwingUtilities.invokeLater(new Runnable() 
        {
            @Override
            public void run()
            {
                // main split pane
                splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
                splitPaneTop = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
                splitPaneBottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
                splitPaneFile = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
                splitPaneURL = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
                
                labelFile = new JLabel("File", SwingConstants.CENTER);
                labelFile.setMinimumSize(new Dimension(100,50));
                file = new JTextField();
                
                labelURL = new JLabel("URL", SwingConstants.CENTER);
                labelURL.setMinimumSize(new Dimension(100,50));
                targetURL = new JTextField();
                
                txtHelp = new JTextArea();
                txtHelp.setText("This extensions searches for a specific URL in a request and replaces to reponse's body with the content of the configured file.\n\nFILE should be the path to a file such as /tmp/modified.js.\nURL should be a valid URL including a specific port such as https://www.target.com:443/replace.js.\n\nProvided by @compr00t");
                txtHelp.setEditable(false);
                
                JButton button = new JButton("Save to project options");
                button.setMaximumSize(new Dimension(200, 200));
                button.setMinimumSize(new Dimension(100, 100));
                
                button.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						callbacks.saveExtensionSetting(SYSTEM_SETTING, file.getText());
						callbacks.saveExtensionSetting(TARGET_URL, targetURL.getText());
					}
				});
                
                file.setText(callbacks.loadExtensionSetting(SYSTEM_SETTING));
                targetURL.setText(callbacks.loadExtensionSetting(TARGET_URL));
                
                splitPaneFile.add(labelFile);
                splitPaneFile.add(file);
                
                splitPaneURL.add(labelURL);
                splitPaneURL.add(targetURL);
                
                splitPaneTop.add(splitPaneFile);
                splitPaneTop.add(splitPaneURL);
                
                splitPaneBottom.add(button);
                splitPaneBottom.add(txtHelp);
                
                splitPane.add(splitPaneTop);
                splitPane.add(splitPaneBottom);
                
                callbacks.customizeUiComponent(splitPane);
                callbacks.addSuiteTab(BurpExtender.this);
                callbacks.registerHttpListener(BurpExtender.this);
            }
        });
    }
    
    @Override
    public String getTabCaption()
    {
        return "Replacer";
    }

    @Override
    public Component getUiComponent()
    {
        return splitPane;
    }

    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo)
    {
        if (!messageIsRequest)
        {   
            String url = helpers.analyzeRequest(messageInfo).getUrl().toString();
            
            if (url.equals(targetURL.getText())) {
            	
            	stdout.println("Hit on " + url);
            	messageInfo.setComment("Modified");
            	messageInfo.setHighlight("pink");
            	
            	try {
            		
            		Path path = Paths.get(file.getText());
            		stdout.println("Replacing the body with " + path.toString());
            		
            		int body_offset = helpers.analyzeResponse(messageInfo.getResponse()).getBodyOffset();
            		
            		byte[] headers = Arrays.copyOfRange(messageInfo.getResponse(), 0, body_offset);
            		byte[] body = Files.readAllBytes(path);
            		
            		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            		outputStream.write(headers);
            		outputStream.write(body);
            		byte[] response = outputStream.toByteArray();
            		
					messageInfo.setResponse(response);
					stdout.println("Replacing done!");
					
				} catch (Exception e) {
					throw new RuntimeException("Could not read file!");
				}
            }
        }
    }
}
