package dev.velocity;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import javax.swing.SwingUtilities;

public final class Extension implements BurpExtension {
    @Override public void initialize(MontoyaApi api) {
        api.extension().setName("Games");
        // Build and register synchronously so unload cannot race UI creation.
        Runnable init = () -> {
            GamesPanel panel = new GamesPanel();
            api.userInterface().registerSuiteTab("Games", panel);
            api.extension().registerUnloadingHandler(() -> SwingUtilities.invokeLater(panel::dispose));
            api.logging().logToOutput("Games ready. Open the Games tab and choose Sonic 3D, Asteroids or Breakout.");
        };
        if (SwingUtilities.isEventDispatchThread()) init.run();
        else try { SwingUtilities.invokeAndWait(init); }
        catch (Exception e) { throw new IllegalStateException("Could not create Games tab", e); }
    }
}
