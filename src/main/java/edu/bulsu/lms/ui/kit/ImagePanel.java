package edu.bulsu.lms.ui.kit;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** A panel that paints a picture stretched over its whole area, with child components on top. */
public final class ImagePanel extends JPanel {

    private final Image image;

    public ImagePanel(String imageFile, LayoutManager layout) {
        super(layout);
        this.image = Theme.icon(imageFile).getImage();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        g2.dispose();
    }
}
