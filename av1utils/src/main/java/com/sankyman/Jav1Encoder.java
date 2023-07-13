package com.sankyman;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class Jav1Encoder 
{
    public interface EncoderProgressListener
    {
        public void onProgress(int frames, int totalFrames, double fps, double speed, int n);
    }

	public interface EncoderStatusListener
	{
		public void onStatus(String strStatus);
	}

	//this is a simple builder class to create command line arguments for the command
	//this can be passed as array to exec command
	class ExecCommand
	{
		ArrayList<String> params = new ArrayList<>();

		public ExecCommand(String command)
		{
			params.add(command);
		}

		public ExecCommand add(String paramName, String paramValue)
		{
			return add(paramName, paramValue, false);
		}

		public ExecCommand add(String paramName, String paramValue, boolean isQuoted)
		{
			params.add(paramName);
			if(isQuoted)
			{
				params.add("\"" + paramValue + "\"");
			}
			else
			{
				params.add(paramValue);
			}

			return this;
		}

		public ExecCommand add(String paramName)
		{
			return add(paramName, false);
		}

		public ExecCommand add(String paramName, boolean isQuoted)
		{
			if(isQuoted)
			{
				params.add("\"" + paramName + "\"");
			}
			else
			{
				params.add(paramName);
			}

			return this;
		}

		
		public ExecCommand addIf(boolean ifTrue, String paramName, String paramValue)
		{
			if(ifTrue)
			{
				return add(paramName, paramValue, false);
			}
			return this;
		}

		public ExecCommand addIf(boolean ifTrue, String paramName, String paramValue, boolean isQuoted)
		{
			if(ifTrue)
			{
				return add(paramName, paramValue, isQuoted);
			}

			return this;
		}

		public ExecCommand addIf(boolean ifTrue, String paramName)
		{
			if(ifTrue)
			{
				return add(paramName);
			}

			return this;
		}

		public ExecCommand addIf(boolean ifTrue, String paramName, boolean isQuoted)
		{
			if(ifTrue)
			{
				return add(paramName, isQuoted);
			}

			return this;
		}


		public String[] toArray()
		{
			if(params.isEmpty())
			{
				return null;
			}

			return params.toArray(new String[params.size()]);
		}

		@Override
		public String toString()
		{
			return String.join(" ", params);
		}
	}

	private EncoderProgressListener listener;
	private EncoderStatusListener statusListener;
	private Process ffmpegProcess;
	private HashMap<String, Integer> srcProbedValues;
	private ExecCommand command;

	public void updateStatus(String strStatus)
	{
		if(statusListener != null)
		{
			statusListener.onStatus("Jav1Encoder::"+ strStatus);
		}
	}

	public HashMap<String,Integer> probeSource(String inputFilename) throws Exception
	{

		ExecCommand ffprobeCommand = new ExecCommand("ffprobe")
													.add("-hide_banner")
													.add("-loglevel", "error")
													.add("-select_streams", "v:0")
													.add("-show_entries", "stream=width,height,nb_frames", true)
													.add("-of", "compact")
													.add("-i",  inputFilename, true);
										
		System.out.println("FFProbe ExecCommand==>" + ffprobeCommand);

		updateStatus(ffprobeCommand.toString());


		final Process process = Runtime.getRuntime().exec(ffprobeCommand.toArray());

		final BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));

		
		final Thread t = new Thread(() -> {
			try
			{
				for(String line = err.readLine(); line != null; line = err.readLine())
				{
					System.out.println("Err:"+line);
					updateStatus("Err:" + line);

				}
			}
			catch(final Exception ex2) {
				ex2.printStackTrace();
			}
		});

		t.start();

		final BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

		final HashMap<String,Integer> probeValues = new HashMap<>();


		Thread t1 = new Thread(() -> {
		try
		{
			String line = in.readLine();
            
            if(line != null)
			{
				System.out.println(line);

				String[] tokens = line.split("\\|");

				for(String token : tokens)
				{
					String[] subTokens = token.split("=");

					if(subTokens.length == 2)
					{
						try
						{
							probeValues.put(subTokens[0], Integer.parseInt(subTokens[1]));
						}
						catch(Exception ex) {}
					}
				}

				System.out.println(probeValues);
				updateStatus(probeValues.toString());

				err.close();
			}

			err.close();
		}
		catch(Exception ex3) {
			ex3.printStackTrace();
		}
		});

		t1.start();

		t1.join();

		return probeValues;
	}

	public void destroy()
	{
		Thread t = new Thread(() -> {
			if(ffmpegProcess != null)
			{
				try
				{
					Process procToBeKilled = ffmpegProcess.destroyForcibly();
	
					procToBeKilled.waitFor();
					updateStatus("Transcoding process force kill requested");
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
					updateStatus("Err:" + ex);
				}
				finally
				{
					updateStatus("ffmpeg command used:" + this.command);
				}
			}
		});
		
		t.start();
	}

	private void init(HashMap<String, Integer> srcProbedValues, ExecCommand command) throws Exception
	{
		this.srcProbedValues = srcProbedValues;
		this.command = command;
		System.out.println(command);
		updateStatus(command.toString());

	}

	public void start() throws Exception
	{
		ffmpegProcess = Runtime.getRuntime().exec(command.toArray());

		
		final BufferedReader err = new BufferedReader(new InputStreamReader(ffmpegProcess.getErrorStream()));

		
		Thread t = new Thread(() -> {
			try
			{
				for(String line = err.readLine(); line != null; line = err.readLine())
				{
					System.out.println("Err:"+line);
					updateStatus("Err:" + line);
				}
			}
			catch(Exception ex2) {
				ex2.printStackTrace();
			}
		});

		t.start();


		final BufferedReader in = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));


		Thread t1 = new Thread(() -> {
			try
			{
				int totalFrames = srcProbedValues.get("nb_frames");

				int currentFrame = 0;
				String fps = "";
				String speed = "";
				int n = 0;

				for(String line = in.readLine(); line != null; line = in.readLine())
				{
					//System.out.println(line);
					String[] tokens = line.split("=");

					if(tokens.length == 2)
					{
						String key = tokens[0];
						String value = tokens[1];

						switch(key)
						{
						case "frame":
						{

							currentFrame = Integer.parseInt(value);

							break;
						}
						case "fps":
						{
							fps = value;
							break;
						}
						case "speed":
						{
							speed = value;
							if(listener != null)
							{
								speed = speed.replaceAll("x","");
								try
								{
									listener.onProgress(currentFrame, totalFrames, Double.parseDouble(fps), Double.parseDouble(speed), ++n);
								}
								catch(NumberFormatException ne)
								{
									//discard
								}
								catch(Exception exListener)
								{
									exListener.printStackTrace();
								}
							}	

							//reset
							currentFrame = 0;
							fps = "";
							speed = "";		
				
							break;
						}
						default:
						{

						}
						}//switch
					}//if
				}//for

			}
			catch(Exception ex3) {
				ex3.printStackTrace();
			}

			try
			{
				in.close();
				err.close();
			}
			catch(Exception ex4){}
		});

		t1.start();

		t1.join();

		ffmpegProcess.waitFor();
		updateStatus("Transcoding Process Exited");
		updateStatus("ffmpeg command used:" + this.command);

		this.srcProbedValues = null;
		this.command = null;
	}


	public static String format(java.time.Duration d) 
	{
    	long days = d.toDays();
   		d = d.minusDays(days);
		long hours = d.toHours();
		d = d.minusHours(hours);
		long minutes = d.toMinutes();
		d = d.minusMinutes(minutes);
		long seconds = d.getSeconds() ;
		return 
            (days ==  0?"":days+" days,")+ 
            (hours == 0?"":hours+" hours,")+ 
            (minutes ==  0?"":minutes+" minutes,")+ 
            (seconds == 0?"":seconds+" seconds,");
	}

	public Jav1Encoder(String inputFilename, String outputFilename, String scaleFactor, EncoderProgressListener listener, EncoderStatusListener statusListener) throws Exception
	{
		this.listener = listener;
		this.statusListener = statusListener;
		ExecCommand ffmpegCommand = new ExecCommand("ffmpeg")
									.add("-y")
									.add("-hide_banner")
									.add("-loglevel", "error")
									.add("-hwaccel", "cuda")
									.add("-hwaccel_output_format", "cuda")
									.add("-i", inputFilename, true)
									.add("-filter:v", "fps=60, scale_cuda=" + scaleFactor + ":interp_algo=lanczos", true)
									.add("-progress", "-")
									.add("-acodec:a", "aac")
									.add("-b:a", "320k")
									.add("-movflags", "+faststart")
									.add("-vcodec:v", "av1_nvenc")
									.add("-cq:v", "15")
									.add("-maxrate:v", "64M")
									.add("-g:v", "60")
									.add("-bf:v", "2")
									.add("-preset:v", "p5")
									.add("-tune:v", "hq")
									.add("-colorspace:v", "bt709", true)
									.add("-color_primaries:v", "bt709", true)
									.add("-color_trc:v", "bt709", true)
									.add("-color_range:v", "tv", true)
									.add("-rc:v", "vbr")
									.add("-multipass:v", "1")
									.add("-rc-lookahead:v", "32")
									.add("-b_ref_mode:v", "2")
									.add("-highbitdepth:v", "true")
									.add(outputFilename, true);

		//init(probeSource(inputFilename), String.format("ffmpeg.exe -y -hide_banner -loglevel error -hwaccel cuda -hwaccel_output_format cuda -i %s -pix_fmt yuv420p -filter:v \"fps=60, scale_cuda=%s:interp_algo=lanczos\" -progress - -acodec:a aac -b:a 320k -movflags +faststart -vcodec:v av1_nvenc -cq:v 15 -maxrate:v 64M -g:v 60 -bf:v 2 -preset:v p5 -tune:v hq -colorspace:v \"bt709\" -color_primaries:v \"bt709\" -color_trc:v \"bt709\" -color_range:v \"tv\" -rc:v vbr -multipass:v 1 -rc-lookahead:v 32 -b_ref_mode:v 2 -highbitdepth:v true %s", inputFilename, scaleFactor, outputFilename));
		init(probeSource(inputFilename), ffmpegCommand);
	}


	public Jav1Encoder(String inputFilename, String outputFilename, EncoderProgressListener listener, EncoderStatusListener statusListener) throws Exception
	{
		this.listener = listener;
		this.statusListener = statusListener;

		boolean isGpuDecoding = false; //set this to true to decode using gpu
		boolean canDoSharpening = false; //set this to true to do sharpening, default do not sharpen
		boolean canDoEdgeSharpening = false; //set this to true to do edge sharpening, some errors in the edge sharpening, need to fix
		boolean isCopyAudio = false; //set this to false to encode audio

		String pixelFormat = "p010le"; //yuv420p for SDR //change to p010le for 10 bit HDR
		String fps = "59.940"; //change to 60 or 30 however feels right
		String audioBitrate = "384k"; //change to different value as needed
		String nvidiaEncoderName = "av1_nvenc"; //change to hevc_nvenc when this is missing

		String cqpValue = "24"; //lower this value, higher the quality, for av1 22 or 23 gives nice encoding
		String maxVideoBitrate = "54M"; //increase this value for higher bitrate encoding
		String gopValue = "59"; //set this to sane amount, say for 30 HZ use 30, for 60 HZ use 60, 59 or 30
		String bFramesCount = "3"; //higher the bframes better the quality, but don't go overboard with this
		String bRefMode = (bFramesCount.equals("3") ? "2": "1"); //ref mode is middle or 2 if 3 bframes, else 1
		
		boolean isFastEncoding = true; //if you have 40 series card, set low latency and p4 or lower to use dual av1/hevc encoder
		String encoderPresetLevel = isFastEncoding ? "p4": "p5"; //p5 is same as slow and good quality, p6 is better quality, p4 is medium quality
		String encoderTune =  isFastEncoding ? "ll" : "hq";

		boolean enforce709Colors =  true; //set this to true to convert nvidia shadowplay encoded bt601 to bt709

		boolean isMultipass = true; //do multipass encoding, disable this to encode faster with slightly lower quality
		int lookaheadFrames = 32; //don't give more than 32 as its useless

		boolean is10bitColors = true; //set this to true to make the output quality 10 bit colors and high quality

		//if edge sharpening disable sharpening
		canDoSharpening = canDoEdgeSharpening ? false : canDoSharpening;

		ExecCommand ffmpegCommand = new ExecCommand("ffmpeg")
												.add("-y")
												.add("-hide_banner")
												.add("-loglevel", "error")
												.addIf(isGpuDecoding, "-hwaccel", "cuda")
												.addIf(isGpuDecoding, "-hwaccel_output_format", "cuda")
												.add("-i", inputFilename, true)
												.addIf(!isGpuDecoding, "-pix_fmt", pixelFormat)
												.addIf(canDoSharpening, "-filter:v", "fps="+fps+",unsharp=5:5:0.3:5:5:0.0", true)
												.addIf(canDoEdgeSharpening, "-filter:v", "fps="+fps+",smartblur=1.5:-0.35:-3.5:0.65:0.25:2.0", true)
												.addIf(!(canDoSharpening || canDoEdgeSharpening), "-filter:v", "fps="+fps, true)
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
		init(probeSource(inputFilename), ffmpegCommand);
		//rigaya NVEncC64 command with weak sharpening preserving edges and edge sharpening vpp filter (edge sharpening with av1util default)
		//NVEncC64.exe -i .\FH5-VtecKickedInYo-CivicTypeR.mp4 --audio-copy --vbr 0 --codec av1 --output-depth 10 --profile main10 --tier high --level 6.2 --multipass 2pass-quarter --max-bitrate 64000 --vbr-quality 20 --bref-mode middle --bframes 3 --ref 4 --gop-len 30 --lookahead 32 --slices 4 --strict-gop --vpp-edgelevel strength=10.0,threshold=16.0,black=0,white=0 --vpp-pmd --videoformat ntsc --colormatrix bt709 --colorprim bt709 --transfer bt709 --atc-sei bt709 --colorrange tv --aud --mv-precision q-pel --cabac --avsync forcecfr -o .\FH5-VtecKickedInYo-CivicTypeR-av1-cfr.mp4
	}

	

	// public static void main(String[] args) throws Exception
	// {
	// 	if(args.length >= 3)
	// 	{
	// 		new Jav1Encoder(args[0], args[1], args[2], (frame,frames,fps,speed,n)-> {
	// 				int percentage =  frame * 100 / frames;
	// 				int remainingSeconds = (int)Math.round((frames - frame) / (60 * speed));

	// 				java.time.Duration durationRemaining = java.time.Duration.ofSeconds(remainingSeconds);

	// 				System.out.println(String.format("%s::%d %% completed [%d / %d], fps=%s, speed=%s, remaining=%s", new java.util.Date(), percentage, frame, frames, ""+fps, ""+speed, format(durationRemaining)));
	// 			}, null);
	// 	}
	// 	else if(args.length >= 2)
	// 	{
			

	// 		new Jav1Encoder(args[0], args[1], (frame,frames,fps,speed,n) -> {
	// 				int percentage =  frame * 100 / frames;
	// 				int remainingSeconds = (int)Math.round((frames - frame) / (60 * speed));
	// 				int secondsComplete = (int)Math.round(frame / fps);

	// 				java.time.Duration durationRemaining = java.time.Duration.ofSeconds(remainingSeconds);

	// 				System.out.println(String.format("%s::%d %% completed [%d / %d], fps=%s, speed=%s, remaining=%s", new java.util.Date(), percentage, frame, frames, ""+fps, ""+speed, format(durationRemaining)));
	// 			}, null);
	// 	}
	// 	else
	// 	{
	// 		System.out.println("Syntax: java Jav1Encoder <src media filename> <dest media filename> [scale_width:scale_height]");
	// 	}
	// }
}