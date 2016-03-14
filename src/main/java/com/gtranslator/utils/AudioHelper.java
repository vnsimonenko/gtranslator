package com.gtranslator.utils;

import com.gtranslator.BaseException;
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class AudioHelper {

    private static final Logger logger = Logger.getLogger(AudioHelper.class);

    public static AudioInputStream getAudioInputStream(File file)
            throws UnsupportedAudioFileException, IOException {
        if (!file.exists()) {
            String error = "File not found: " + file.getAbsolutePath();
            logger.error(error);
            throw new FileNotFoundException(error);
        }
        if (file.getName().endsWith(".mp3")) {
            return convertToWave(file);
        } else {
            return AudioSystem.getAudioInputStream(file);
        }
    }

    static boolean playCompleted = true;
    public static synchronized void play(String fileName) {
        playCompleted = false;
        File audioFile = new File(fileName);
        try (AudioInputStream in = getAudioInputStream(audioFile)) {
            SourceDataLine line = null;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                    in.getFormat());
            try {
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(in.getFormat());
                line.start();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                IOUtils.copy(in, out);
                byte[] data = out.toByteArray();
                line.write(data, 0, data.length);
                line.drain();
                line.close();
            } catch (LineUnavailableException | IOException ex) {
                throw new BaseException(ex);
            }
        } catch (UnsupportedAudioFileException | IOException ex) {
            throw new BaseException(ex);
        }
    }

    private static AudioInputStream convertToWave(File mp3File) throws UnsupportedAudioFileException,
            IOException {
        AudioInputStream mp3In = AudioSystem.getAudioInputStream(mp3File);
        AudioFormat audioFormat = createAudioFormat(mp3In.getFormat().getSampleRate());
        MpegFormatConversionProvider cnv = new MpegFormatConversionProvider();
        return cnv.isConversionSupported(audioFormat, mp3In.getFormat()) ? cnv
                .getAudioInputStream(audioFormat, mp3In) : null;
    }

    private static AudioFormat createAudioFormat(float sampleRate) {
        boolean bigEndian = false;
        boolean signed = true;
        int bits = 16;
        int channels = 1;
        return new AudioFormat(sampleRate, bits, channels,
                signed, bigEndian);
    }

//    public static void main(String ... args) throws InterruptedException {
//        for (int i = 0; i < 5; i++) {
//            AudioHelper.play("/home/vns/workspace/desktop/gtranslator/target/gtranslator/ivona/AM/specifies.mp3");
//            AudioHelper.play("/home/vns/workspace/desktop/gtranslator/target/gtranslator/ivona/BR/specifies.mp3");
//        }
//
//        //AudioHelper.play("/home/vns/workspace/desktop/gtranslator/target/gtranslator/oxford/AM/format_y4hmyZTLkHJtw6Z0.mp3");
//    }
}