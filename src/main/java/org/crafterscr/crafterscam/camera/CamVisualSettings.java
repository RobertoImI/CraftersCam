package org.crafterscr.crafterscam.camera;

public class CamVisualSettings {
    public int fadeInTicks = 10;
    public int fadeOutTicks = 10;
    public boolean showBars = true;
    public boolean hideHudAll = true;

    public CamVisualSettings() {
    }

    public CamVisualSettings copy() {
        CamVisualSettings copy = new CamVisualSettings();
        copy.fadeInTicks = this.fadeInTicks;
        copy.fadeOutTicks = this.fadeOutTicks;
        copy.showBars = this.showBars;
        copy.hideHudAll = this.hideHudAll;
        return copy;
    }
}