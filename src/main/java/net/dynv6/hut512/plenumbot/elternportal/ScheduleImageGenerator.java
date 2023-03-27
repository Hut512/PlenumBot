package net.dynv6.hut512.plenumbot.elternportal;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ScheduleImageGenerator {
    private BufferedImage image;

    public ScheduleImageGenerator(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    public void generateImage(Schedule schedule) {

    }

    public void writeToFile(File outputFile, String formatName) throws IOException {
        ImageIO.write(image, formatName, outputFile);
    }
}
