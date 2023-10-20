package com.sankyman;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.TableModel;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class App extends JFrame implements Jav1Encoder.EncoderProgressListener, Jav1Encoder.EncoderStatusListener
{
    JButton btnSource;
    JButton btnConvert;
    JButton btnCancel;
    JProgressBar progressBar;
    File selectedFile;
    Jav1Encoder av1Encoder;
    JTextArea txtArea;
    JScrollPane txtScrollPane;
    JSplitPane splitPaneMain;
    JTable tblFfmpegOptions;
    JScrollPane tableScrollPane;


    void initLookAndFeel()
    {
        try {
            // Set cross-platform Java L&F (also called "Metal")
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException e) {
            // handle exception
        } catch (ClassNotFoundException e) {
            // handle exception
        } catch (InstantiationException e) {
            // handle exception
        } catch (IllegalAccessException e) {
            // handle exception
        }
    }

    private TransferHandler handler = new TransferHandler() {
        public boolean canImport(TransferHandler.TransferSupport support) {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false;
            }

            boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;

            if (!copySupported) {
                return false;
            }

            support.setDropAction(COPY);

            return true;
        }

        public boolean importData(TransferHandler.TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            
            Transferable t = support.getTransferable();

            try {
                Object tFlavor = t.getTransferData(DataFlavor.javaFileListFlavor);

                if(!(tFlavor instanceof java.util.List<?>)) return false;

                java.util.List<File> listOfFiles = (java.util.List<File>)tFlavor;


                if(!listOfFiles.isEmpty())
                {
                    File firstFile = listOfFiles.get(0);

                    synchronized(App.this)
                    {
                        if(av1Encoder == null)
                        {
                            selectedFile = firstFile;
                            appendStatus(String.format("File Chosen To Be Encoded: %s", selectedFile.getAbsolutePath()));

                        }
                    }

                    transcode();
                }
            } catch (UnsupportedFlavorException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
            catch(Exception ex2)
            {
                return false;
            }

            return true;
        }
    };

    void initFrame()
    {
        setTitle("Simple Nvidia AV1 Transcoder UI Wrapper over ffmpeg");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 

        URL iconResource = getClass().getResource("/images/128px-AV1_logo_2018.svg.png");
        System.out.println(iconResource);
        setIconImage(Toolkit.getDefaultToolkit().getImage(iconResource));
        setVisible(true);
    }

    void adjustScreenSize()
    {
        setSize(Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setTransferHandler(handler);
        txtArea.setTransferHandler(handler);
        txtScrollPane.setTransferHandler(handler);
        txtScrollPane.requestFocus();
    }

    void initComponents()
    {
        getRootPane().setLayout(new BorderLayout());

        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;

        gbc.gridx = 1;
        btnSource = new JButton("Source MP4/MOV to Convert");
        pnlButtons.add(btnSource, gbc);

        gbc.anchor = GridBagConstraints.BASELINE;

        gbc.gridx = 2;
        btnConvert = new JButton("Transcode to AV1");
        pnlButtons.add(btnConvert, gbc);

        gbc.gridx = 3;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        btnCancel = new JButton("Cancel Transcoding");
        pnlButtons.add(btnCancel, gbc);

        getRootPane().add(pnlButtons, BorderLayout.NORTH);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        getRootPane().add(progressBar, BorderLayout.SOUTH);


        txtArea = new JTextArea();
        txtArea.setColumns(80);
        //txtArea.setEditable(false);
        txtArea.setWrapStyleWord(true);
        txtScrollPane = new JScrollPane(txtArea);


        tblFfmpegOptions = new FfmpegOptionsTable();
        tableScrollPane = new JScrollPane(tblFfmpegOptions);

        splitPaneMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScrollPane, txtScrollPane);
        splitPaneMain.setOneTouchExpandable(true);
        splitPaneMain.setDividerLocation(225);

        getRootPane().add(splitPaneMain, BorderLayout.CENTER);

        pnlButtons.setTransferHandler(handler);
        setupComponentEvents();

        adjustScreenSize();

        tblFfmpegOptions.setFillsViewportHeight(true);
    }

    void appendStatus(String strStatus)
    {
        SwingUtilities.invokeLater(()-> {
            txtArea.append(strStatus + "\n");
            txtArea.setCaretPosition(txtArea.getDocument().getLength());
            System.out.println(strStatus);
        });
    }

    Action cancelAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            cancelTranscoding();
        }
    };

    void cancelTranscoding()
    {
        synchronized(App.this)
        {
            if(av1Encoder != null)
            {
                appendStatus("Requested cancelling of transcoding ...");
                av1Encoder.destroy();
                appendStatus("Transcoding forcefully cancelled");
            }
            else
            {
                appendStatus("No transcoding to cancel");
            }

        }
    }

    void setupComponentEvents()
    {
        btnSource.addActionListener((ae) -> {

            synchronized(App.this)
            {
                if(av1Encoder != null)
                {
                    appendStatus("Encoding Process Already Running, Try after Completion or after Cancelling");
                    return;
                }
            }

            JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

            if(jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            {
                selectedFile = jfc.getSelectedFile();
                System.out.println(selectedFile);
                appendStatus(String.format("File Chosen To Be Encoded: %s", selectedFile.getAbsolutePath()));
            }
            else
            {
                selectedFile = null;
                appendStatus("No File Selected");
            }
        });

        btnCancel.addActionListener((ae) ->{
            cancelTranscoding();
        });

        btnCancel.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "cancelTranscoding");
        btnCancel.getActionMap().put("cancelTranscoding", cancelAction);

        txtArea.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "cancelTranscoding");
        txtArea.getActionMap().put("cancelTranscoding", cancelAction);


        txtScrollPane.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "cancelTranscoding");
        txtScrollPane.getActionMap().put("cancelTranscoding", cancelAction);

        
        getRootPane().getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "cancelTranscoding");
        getRootPane().getActionMap().put("cancelTranscoding", cancelAction);

        btnConvert.addActionListener((ae) -> {

            synchronized(App.this)
            {
                if(av1Encoder != null)
                {
                    appendStatus("Encoding Process Already Running, Try after Completion or after Cancelling");
                    return;
                }
            }

            transcode();
        });
    }

    private void transcode() {
        if(selectedFile != null)
        {
            if(!selectedFile.exists())
            {
                System.out.println(selectedFile + " Does not exist");
            }
            String strInputFilename = selectedFile.getName();
            String strOutputFilename = selectedFile.getAbsolutePath() + "-av1.mkv";

            int lastDot = strInputFilename.lastIndexOf(".");

            if(lastDot != -1)
            {
                String inputFilenameWithoutExt =  strInputFilename.substring(0, lastDot);
                File fAv1OutputFile = new File(selectedFile.getParentFile(), inputFilenameWithoutExt + "-av1.mkv");
                strOutputFilename = fAv1OutputFile.getAbsolutePath();
            }

            final File selectedFileFinal = selectedFile;
            final String strOutputFilenameFinal = strOutputFilename;

            Thread t = new Thread(() -> {
                try {
                    synchronized(App.this)
                    {
                        //no scaling
                        String fixedPath = selectedFileFinal.toString();
                        System.out.println("Fixed Path:" + fixedPath);

                        TableModel model = tblFfmpegOptions.getModel();

                        if(model instanceof FfpmegOptions)
                        {
                            FfpmegOptions ffmpegOptionsModel = (FfpmegOptions)model;
                            av1Encoder = new Jav1Encoder(fixedPath, strOutputFilenameFinal, ffmpegOptionsModel.getOptionsMap(), App.this, App.this);

                        }
                        else
                        {
                            av1Encoder = new Jav1Encoder(fixedPath, strOutputFilenameFinal, App.this, App.this);
                        }
                        //scaling
                        //av1Encoder = new Jav1Encoder("\"" + selectedFileFinal.getAbsolutePath() + "\"", "\"" + strOutputFilenameFinal + "\"", "3840:2160", App.this, App.this);
                    }

                    av1Encoder.start(); //only now the encoding begins

                    SwingUtilities.invokeLater(() -> {
                        progressBar.setString("Conversion Completed Successfully");
                        appendStatus("Conversion Completed Successfully");
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setString("Conversion Cancelled");
                        appendStatus("Conversion Cancelled with err:" + e);
                    });
                }
                finally {


                    synchronized(App.this) {
                        av1Encoder = null;
                    }
                }
            });

            t.start();
        }
        else
        {
            appendStatus("No Source Selected to be transcoded to AV1");
        }
    }



    public App()
    {
        initLookAndFeel();
        initFrame();
        initComponents();
    }

    @Override
    public void onProgress(int frames, int totalFrames, double fps, double speed, int n, boolean isEnd) {

        //find end marker or when the limit exceeds beyond correct frame calculation
        if(frames > totalFrames || isEnd)
        {
             frames = totalFrames; //fix progress going in negative
        }

        final int percentage =  frames * 100 / totalFrames;
        final int remainingSeconds = (int)Math.round((totalFrames - frames) / (60 * speed));
        //int secondsComplete = (int)Math.round(frames / fps);

        String strProgressTextVar = "";
        if(frames == totalFrames || isEnd || remainingSeconds == 0)
        {

            strProgressTextVar =  String.format(
                "%d %% completed [%d / %d], fps=%s, speed=%s, finalizing", 
                                                                            percentage, 
                                                                            frames, 
                                                                            totalFrames, 
                                                                            ""+fps, 
                                                                            ""+speed
                                                                            
                                                    );
        }
        else
        {
            java.time.Duration durationRemaining = java.time.Duration.ofSeconds(remainingSeconds);

            strProgressTextVar =  String.format(
                "%d %% completed [%d / %d], fps=%s, speed=%s, remaining=%s", 
                                                                            percentage, 
                                                                            frames, 
                                                                            totalFrames, 
                                                                            ""+fps, 
                                                                            ""+speed, 
                                                                            Jav1Encoder.format(durationRemaining)
                                                    );
        }
        
        System.out.println(strProgressTextVar);    


        final String strProgressText = strProgressTextVar;

        SwingUtilities.invokeLater(() -> {

            progressBar.setValue(percentage);
            progressBar.setString(strProgressText);
            appendStatus(strProgressText);
        });
    }

    @Override
    public void onStatus(String strStatus) {
        appendStatus(strStatus);
    }                                               

    public static void main( String[] args )
    {
        new App();
    }




}
