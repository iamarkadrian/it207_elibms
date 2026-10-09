package edu.bulsu.lms.ui.kit;

import java.awt.Color;

/**
 * The colors a card and its form controls use. The librarian screens are {@link #LIGHT}; the
 * administrator dashboard is {@link #DARK}. Components take a palette instead of hard-coding colors.
 */
public record Palette(Color card, Color field, Color text, Color muted, Color hint, boolean dark) {

    public static final Palette LIGHT = new Palette(
            new Color(250, 250, 250), Color.WHITE, Theme.INK, Theme.MUTED, Theme.MUTED, false);

    /** White page with light-gray input boxes: the sign-in screen. */
    public static final Palette LOGIN = new Palette(
            Color.WHITE, Theme.LOGIN_FIELD, Theme.INK, Theme.MUTED, Theme.MUTED, false);

    public static final Palette DARK =new Palette(
            new Color(30, 30, 30), new Color(37, 37, 37), Color.WHITE, Theme.MUTED, new Color(143, 143, 143), true);
}
