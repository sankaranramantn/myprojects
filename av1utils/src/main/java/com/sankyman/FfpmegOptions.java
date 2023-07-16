package com.sankyman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.table.AbstractTableModel;

class FfmpegChoices
{
    private int selectedIndex = 0;
    private ArrayList<String> choices = new ArrayList<>();
    private String defaultChoice;

    public FfmpegChoices(String defaultChoice, String... items)
    {
        this.defaultChoice = defaultChoice;

        if(items == null)
        {
            //since no data is passed, use defaultChoice
            String[] tempItems = new String[] { defaultChoice};
            choices = new ArrayList<>(Arrays.asList(tempItems));
        }
        else
        {
            choices = new ArrayList<>(Arrays.asList(items));

        }

        selectedIndex = choices.indexOf(defaultChoice);
    }

    public void setSelectedItem(String choice)
    {
        int index = choices.indexOf(choice);

        if(index != -1)
        {
            selectedIndex = index;
        }

    }

    public String getSelectedItem()
    {
        if(selectedIndex != -1)
        {
            return choices.get(selectedIndex);
        }

        return defaultChoice;
    }

    public String[] getItems()
    {
        String[] items = new String[choices.size()];
        return choices.toArray(items);
    }

    @Override
    public String toString()
    {
        return getSelectedItem();
    }
}

public class FfpmegOptions extends AbstractTableModel {

    /*
     * 
     * 	

		ExecCommand ffmpegCommand = new ExecCommand("ffmpeg")
												.add("-y")
												.add("-hide_banner")
												.add("-loglevel", "error")
												.addIf(isGpuDecoding, "-hwaccel", "cuda")
												.addIf(isGpuDecoding, "-hwaccel_output_format", "cuda")
												.add("-i", inputFilename, true)
												.addIf(!isGpuDecoding, "-pix_fmt", pixelFormat)
												.addIf(canDoSharpening, "-filter:v", "fps="+fps+",unsharp=5:5:0.3:5:5:0.0", true)
												.addIf(!canDoSharpening, "-filter:v", "fps="+fps, true)
												.add("-progress", "-")
												.addIf(isCopyAudio,"-acodec:a", "copy")
												.addIf(!isCopyAudio, "-acodec:a", "aac")
												.addIf(!isCopyAudio, "-b:a", audioBitrate)
												.add("-movflags", "+faststart")
												.add("-vcodec:v", nvidiaEncoderName)
												.add("-cq:v", cqpValue)
												.add("-maxrate:v", maxVideoBitrate)
												.add("-g:v", gopValue)
												.add("-bf:v", bFramesCount)
												.add("-preset:v", encoderPresetLevel)
												.add("-tune:v", encoderTune)
												.addIf(enforce709Colors, "-colorspace:v", "bt709", true)
												.addIf(enforce709Colors,"-color_primaries:v", "bt709", true)
												.addIf(enforce709Colors, "-color_trc:v", "bt709", true)
												.addIf(enforce709Colors, "-color_range:v", "tv", true)
												.add("-rc:v", "vbr")
												.addIf(isMultipass, "-multipass:v", "1")
												.add("-rc-lookahead:v", String.valueOf(lookaheadFrames))
												.add("-b_ref_mode:v", bRefMode)
												.addIf(is10bitColors, "-highbitdepth:v", "true")
												.add(outputFilename, true);

     * 
     */

    private LinkedHashMap<String, Object> optionsMap;

    public FfpmegOptions()
    {
        optionsMap = new LinkedHashMap<>();

        optionsMap.put("isGpuDecoding", Boolean.valueOf(false));                //set this to true to decode using gpu
        //yuv420p for SDR //change to p010le for 10 bit HDR
        optionsMap.put("pixelFormat", new FfmpegChoices("p010le", "yuv420p", "p010le"));                            
        optionsMap.put("frameRate", Float.valueOf(59.940f));                    //change to 60 or 30 however feels right
        optionsMap.put("canDoSharpening", Boolean.valueOf(false));              //set this to true to do sharpening, default do not sharpen
        optionsMap.put("unsharp", "5:5:0.3:5:5:0.0");                       //unsharp mask in case sharpening is enabled
        optionsMap.put("isCopyAudio", Boolean.valueOf(true));                   //set this to false to encode audio, by default audio is muxed                 
        
        //aac or opus, to make this work enable isCopyAudio to false
        optionsMap.put("audioCodec", new FfmpegChoices("aac", "aac", "opus"));  

        optionsMap.put("audioBitrate", "384k");                             //audio bitrate in case its encoded
        
        //this is the codec to be used
        optionsMap.put("videoCodec", new FfmpegChoices("av1_nvenc", "av1_nvenc", "hevc_nvenc", "h264_nvenc"));                          
        optionsMap.put("cq", Integer.valueOf(20));                              //cq controls the quality level, lower the value, higher the video output quality
        optionsMap.put("maxVideoBitrate", "67M");                           //based on the cq level video will be encoded however this sets the max ceiling of the encoding output
        optionsMap.put("gop", Integer.valueOf(59));                             //group of frames, for 60fps this should be near it, for 30 fps, 29 will be appropriate
        optionsMap.put("bFramesCount", Integer.valueOf(3));                     //number of b frames
        optionsMap.put("isFastEncoding", Boolean.valueOf(true));                //use fast encoding to utilize dual gpu encoders of 40 series
        //whats the preset to be used, p4, p5, p6 ...
        optionsMap.put("encoderPresetLevel", new FfmpegChoices("p4", "p1", "p2", "p3", "p4", "p5", "p6", "p7"));
        //whats the tune, ll for low latency (this is auto enabled when fast gpu encoding is enabled), hq for high quality (this disables fast encoding)
        optionsMap.put("encoderTune", new FfmpegChoices("ll", "hq", "ll", "ull"));                                
        optionsMap.put("enforce709Colors", Boolean.valueOf(true));              //enforce bt709 colors
        optionsMap.put("isMultipass", Boolean.valueOf(true));                   //is multi pass 
        optionsMap.put("lookaheadFrames", Integer.valueOf(32));                 //how many frames to look ahead
        optionsMap.put("is10bitColors", Boolean.valueOf(true));                 //is 10 bit colors enabled
    }



    //clone and return a copy, never return internal state variable which maintains all the options
    public synchronized LinkedHashMap<String, Object> getOptionsMap() {
        return new LinkedHashMap<String, Object>(optionsMap);
    }

    Map.Entry<String,Object> getEntryAt(int rowIndex)
    {
        Set<Map.Entry<String,Object>> entrySet = optionsMap.entrySet();
        
        int index = 0;

        for(Map.Entry<String,Object> entry : entrySet)
        {
            if(index == rowIndex)
            {
                return entry;
            }

            index++;
        }

        return null;
    }

    @Override
    public String getColumnName(int column)
    {
        return (column == 0)?"Enocder Option":"Value";
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        if(columnIndex == 1)
        {
            return true;
        }

        return false;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        Map.Entry<String, Object> entry = getEntryAt(rowIndex);

        if(entry != null)
        {
            if(columnIndex == 1)
            {
                String strKey = entry.getKey();
                Object value = entry.getValue();

                if(value instanceof Boolean)
                {
                    optionsMap.put(strKey, Boolean.valueOf(String.valueOf(aValue)));

                }
                else if(value instanceof Integer)
                {
                    //for integer objects validate if the final value is integer, otherwise reset with old value
                    try
                    {
                        Integer intValue = Integer.valueOf(String.valueOf(aValue));
                   
                        optionsMap.put(strKey, intValue);
                    }
                    catch(NumberFormatException ne) {
                        //not a number, set the old value
                        optionsMap.put(strKey, value);
                    }
                }
                else if(value instanceof Float)
                {
                    try
                    {
                        Float floatValue = Float.valueOf(String.valueOf(aValue));
                   
                        optionsMap.put(strKey, floatValue);
                    }
                    catch(NumberFormatException ne) {
                        //not a number, set the old value
                        optionsMap.put(strKey, value);
                    }
                }
                else if(value instanceof FfmpegChoices)
                {
                    FfmpegChoices choices = (FfmpegChoices)value;
                    choices.setSelectedItem(String.valueOf(aValue));
                    optionsMap.put(strKey, choices);
                }
                else
                {
                    //set the value
                    optionsMap.put(strKey, aValue);
                }

                fireTableCellUpdated(rowIndex, columnIndex);

            }
        }

    }


    @Override
    public int getRowCount() {

        return optionsMap.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        Map.Entry<String,Object> entry = getEntryAt(rowIndex);

        if(entry != null)
        {
            if(columnIndex == 0)
            {
                return entry.getKey();
            }
            else
            {
                return entry.getValue();
            }
        }

        return "None";
    }
    
}
