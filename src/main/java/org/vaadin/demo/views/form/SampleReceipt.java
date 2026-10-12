package org.vaadin.demo.views.form;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Renders a fictional receipt as a PNG, so the form filler demo has a document
 * to extract values from.
 */
final class SampleReceipt {

    private static final byte[] PNG = render();

    private SampleReceipt() {
    }

    /** The receipt as PNG bytes (rendered once). */
    static byte[] png() {
        return PNG;
    }

    private static byte[] render() {
        int width = 480;
        int height = 640;
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xfbfaf7));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(0x222222));

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        g.drawString("Café Kanava", 40, 70);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        String[] lines = {
                "Ruoholahdenkatu 21, 00180 Helsinki",
                "VAT ID FI12345678",
                "",
                "Receipt #20260917-0042",
                "Date: 17.09.2026   Time: 12:41",
                "",
                "2 x Lunch of the day       29.80",
                "2 x Espresso                7.00",
                "1 x Cinnamon bun            4.20",
                "--------------------------------",
                "Total EUR                  41.00",
                "incl. VAT 14%               5.04",
                "",
                "Paid with: VISA **** 4242",
                "",
                "Guests: 2 (customer meeting,",
                "Vaadin 25.3 release planning)",
                "",
                "        Kiitos käynnistä!",
        };
        int y = 110;
        for (String line : lines) {
            g.drawString(line, 40, y);
            y += 24;
        }
        g.dispose();

        try (var out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
