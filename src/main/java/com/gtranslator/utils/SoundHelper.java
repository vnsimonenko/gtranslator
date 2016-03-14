package com.gtranslator.utils;

import com.gtranslator.BaseException;
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider;
import org.apache.log4j.Logger;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SoundHelper {
	static AudioFormat WAVE_FORMAT_16000;

	static AudioFormat WAVE_FORMAT_44100;

	private static final Logger logger = Logger.getLogger(SoundHelper.class);
	static {
		int sampleRate_44100 = 44100;
		int sampleRate_16000 = 16000;
		boolean bigEndian = false;
		boolean signed = true;
		int bits = 16;
		int channels = 1;
		WAVE_FORMAT_44100 = new AudioFormat(sampleRate_44100, bits, channels,
				signed, bigEndian);
		WAVE_FORMAT_16000 = new AudioFormat(sampleRate_16000, bits, channels,
				signed, bigEndian);
	}

	public static AudioInputStream convertWave(File mp3File,
			AudioFormat audioFormat) throws UnsupportedAudioFileException,
			IOException {
		AudioInputStream mp3In = AudioSystem.getAudioInputStream(mp3File);
		MpegFormatConversionProvider cnv = new MpegFormatConversionProvider();
		return cnv.isConversionSupported(audioFormat, mp3In.getFormat()) ? cnv
				.getAudioInputStream(audioFormat, mp3In) : null;
	}

	public static void playFile(File mp3File) {
		AudioInputStream in;
		try {
			in = getAudioInputStreamOfMp3File(mp3File);
		} catch (UnsupportedAudioFileException | IOException ex) {
			throw new BaseException(ex.getMessage());
		}
		try {
			SourceDataLine line = null;
			DataLine.Info info = new DataLine.Info(SourceDataLine.class,
					WAVE_FORMAT_44100);
			try {
				line = (SourceDataLine) AudioSystem.getLine(info);
				line.open(in.getFormat());
			} catch (Exception ex) {
				throw new BaseException(ex.getMessage());
			}

			line.start();
			int EXTERNAL_BUFFER_SIZE = 1024; // 128Kb
			int nBytesRead = 0;
			byte[] data = new byte[EXTERNAL_BUFFER_SIZE];
			while (nBytesRead != -1) {
				try {
					nBytesRead = in.read(data, 0, data.length);
				} catch (IOException ex) {
					throw new BaseException(ex.getMessage());
				}
				if (nBytesRead >= 0) {
					line.write(data, 0, nBytesRead);
				}
			}

			line.drain();
			line.close();
		} finally {
			try {
				in.close();
			} catch (IOException ex) {
				logger.error(ex);
			}
		}
	}

	private static AudioInputStream getAudioInputStreamOfMp3File(File file)
			throws UnsupportedAudioFileException, IOException {
		AudioFileFormat aff = AudioSystem.getAudioFileFormat(file);
		AudioInputStream in = null;
		if (aff.getFormat().getSampleRate() == 16000f) {
			in = convertWave(file, WAVE_FORMAT_16000);
			in = AudioSystem.getAudioInputStream(WAVE_FORMAT_44100, in);
		} else {
			in = convertWave(file, WAVE_FORMAT_44100);
		}
		return in;
	}

	public static void main(String ... args) {
		SoundHelper.playFile(new File("/home/vns/workspace/desktop/gtranslator/target/gtranslator/ivona/AM/specifies.mp3"));
		SoundHelper.playFile(new File("/home/vns/workspace/desktop/gtranslator/target/gtranslator/ivona/BR/specifies.mp3"));
		//SoundHelper.playFile(new File("/home/vns/workspace/desktop/gtranslator/target/gtranslator/oxford/AM/format_y4hmyZTLkHJtw6Z0.mp3"));
	}
}
